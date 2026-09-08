(ns edsupport.edsupportadvisor
  "EdSupportOps-LLM client -- the *contained intelligence node* for
  the educational-support-services actor.

  It normalizes client intake, drafts a per-jurisdiction educational-
  testing/counseling evidence checklist, screens clients for an
  unresolved assessment-administration irregularity and for an
  uncleared background check, and drafts the placement/referral-
  finalization action. CRITICAL: it is a smart-but-untrusted advisor.
  It returns a *proposal* (with a rationale + the fields it cited),
  never a committed record or a real placement finalization. Every
  output is censored downstream by `edsupport.governor` before
  anything touches the SSoT, and `:actuation/finalize-placement`
  proposals NEVER auto-commit at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/finalize-placement | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [kotoba.lang.text :as str]
            [edsupport.facts :as facts]
            [edsupport.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the client, assessment or jurisdiction. High
  confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "クライアント記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :client/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-assessment
  "Per-jurisdiction educational-testing/counseling evidence checklist
  draft. `:no-spec?` injects the failure mode we must defend against:
  proposing a checklist for a jurisdiction with NO official spec-basis
  in `edsupport.facts` -- the Support Services Governor must reject
  this (never invent a jurisdiction's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [c (store/client db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction c))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "edsupport.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :assessment/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :assessment/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-integrity
  "Assessment-administration-irregularity screening draft.
  `:assessment-administration-irregularity-unresolved?` on the client
  record injects the failure mode: the Support Services Governor must
  HOLD, un-overridably, on any unresolved irregularity."
  [db {:keys [subject]}]
  (let [c (store/client db subject)]
    (cond
      (nil? c)
      {:summary "対象クライアント記録が見つかりません" :rationale "no client record"
       :cites [] :effect :integrity/set :value {:client-id subject :assessment-administration-irregularity-unresolved? nil}
       :stake nil :confidence 0.0}

      (true? (:assessment-administration-irregularity-unresolved? c))
      {:summary    (str (:client-name c) ": 評価実施の不正/セキュリティ上の疑義が未解決")
       :rationale  "スクリーニングが未解決状態を検出。人手確認とホールドが必須。"
       :cites      [:integrity-check]
       :effect     :integrity/set
       :value      {:client-id subject :assessment-administration-irregularity-unresolved? true}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:client-name c) ": 評価実施の疑義は解決済み")
       :rationale  "評価実施インテグリティ・スクリーニング完了。"
       :cites      [:integrity-check]
       :effect     :integrity/set
       :value      {:client-id subject :assessment-administration-irregularity-unresolved? false}
       :stake      nil
       :confidence 0.9})))

(defn- screen-background-check
  "Client-facing background-check screening draft -- the SAME literal
  concept `school.schooladvisor`'s advisor established first,
  `sports.sportsadvisor` reused it literally as the second instance,
  and `personalservice.personalserviceadvisor` as the third (see
  `edsupport.governor`'s ns docstring for the full ordinal
  accounting). `:background-check-not-cleared?` on the client record
  injects the failure mode the Support Services Governor must HOLD,
  un-overridably, on."
  [db {:keys [subject]}]
  (let [c (store/client db subject)]
    (cond
      (nil? c)
      {:summary "対象クライアント記録が見つかりません" :rationale "no client record"
       :cites [] :effect :background-check/set :value {:client-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:background-check-not-cleared? c))
      {:summary    (str (:client-name c) ": 身元確認が未完了")
       :rationale  "スクリーニングが身元確認未完了を検出。人手確認とホールドが必須。"
       :cites      [:background-check]
       :effect     :background-check/set
       :value      {:client-id subject :verdict :not-cleared}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:client-name c) ": 身元確認は完了")
       :rationale  "身元確認スクリーニング完了。"
       :cites      [:background-check]
       :effect     :background-check/set
       :value      {:client-id subject :verdict :cleared}
       :stake      nil
       :confidence 0.9})))

(defn- propose-placement-finalization
  "Draft the actual PLACEMENT-FINALIZATION action -- finalizing a
  real placement or referral. ALWAYS `:stake :actuation/finalize-
  placement` -- this is a REAL-WORLD act, never a draft the actor may
  auto-run. See README `Actuation`: no phase ever adds this op to a
  phase's `:auto` set (`edsupport.phase`); the governor also always
  escalates on `:actuation/finalize-placement`. Two independent layers
  agree, deliberately."
  [db {:keys [subject]}]
  (let [c (store/client db subject)
        safe? (and c (not (:assessment-administration-irregularity-unresolved? c))
                   (not (:background-check-not-cleared? c)))]
    {:summary    (str subject " 向け配置確定提案"
                      (when c (str " (client=" (:client-name c) ")")))
     :rationale  (if c
                   (str "assessment-administration-irregularity-unresolved?="
                        (:assessment-administration-irregularity-unresolved? c)
                        " background-check-not-cleared?=" (:background-check-not-cleared? c))
                   "クライアント記録が見つかりません")
     :cites      (if c [subject] [])
     :effect     :client/mark-finalized
     :value      {:client-id subject}
     :stake      :actuation/finalize-placement
     :confidence (if safe? 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :client/intake                (normalize-intake db request)
    :assessment/verify             (verify-assessment db request)
    :integrity/screen              (screen-integrity db request)
    :background-check/screen       (screen-background-check db request)
    :actuation/finalize-placement  (propose-placement-finalization db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたは教育支援サービス事業(学習評価・カウンセリング・留学配置等)の"
       "配置確定エージェントの助言者です。与えられた事実のみに基づき、提案を"
       "1つだけEDNマップで返します。説明や前置きは一切書かず、EDNだけを"
       "出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:client/upsert|:assessment/set|:integrity/set|"
       ":background-check/set|:client/mark-finalized) "
       ":stake(:actuation/finalize-placement か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :assessment/verify              {:client (store/client st subject)}
    :integrity/screen                {:client (store/client st subject)}
    :background-check/screen         {:client (store/client st subject)}
    :actuation/finalize-placement      {:client (store/client st subject)}
    {:client (store/client st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Support Services Governor
  escalates/holds -- an LLM hiccup can never auto-finalize a
  placement."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :edsupportadvisor-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
