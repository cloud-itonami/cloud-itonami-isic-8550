(ns edsupport.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300):
  this repo previously had NO demo page and no generator at all. This
  namespace drives the REAL actor stack (`edsupport.operation` ->
  `edsupport.governor` -> `edsupport.phase` -> `edsupport.store`)
  through a scenario adapted from this repo's own `edsupport.sim`
  demo driver (`clojure -M:dev:run`, confirmed BEFORE writing this
  file to produce a sensible ledger against the real seeded client
  ids `client-1`..`client-4` -- this repo's sim driver uses ids that
  DO match `edsupport.store/demo-data`, so it was safe to extend
  rather than author from scratch).

  Two deliberate extensions beyond `sim`:

    - a `:evidence-incomplete` HARD hold (finalizing a placement for a
      client with no assessment evidence checklist on file). `sim`
      exercises four of the governor's five HARD checks; adding this
      one makes ALL FIVE observable on the page.
    - a phase-gate hold (`:assessment/verify` replayed at phase 1,
      where that op is not yet an enabled write) -- so the page shows
      that the rollout gate holds independently of the governor.

  Nothing on the page is hand-typed domain data. Every client,
  jurisdiction, checklist item, verdict, placement number, rule name
  and rule detail is read back out of the store or the run's audit
  channel after the real graph ran. Even the action-gate table is
  COMPUTED by calling `edsupport.phase/gate` rather than described
  from memory. Deterministic: no timestamps in the page content, no
  map-iteration order relied on (every table sorts explicitly), so
  two consecutive runs are byte-identical.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [jp-go-dds.skin]
            [clojure.string :as str]
            [edsupport.facts :as facts]
            [edsupport.governor :as governor]
            [edsupport.operation :as op]
            [edsupport.phase :as phase]
            [edsupport.store :as store]
            [langgraph.graph :as g]))

(def ^:private operator
  "Phase 3 (supervised-auto) provider staff -- the same operator
  context `edsupport.sim` uses."
  {:actor-id "op-1" :actor-role :provider-staff :phase 3})

(def ^:private early-phase-operator
  "The SAME operator earlier in the rollout, used once at the end to
  show the phase gate holding an op the governor itself cleared."
  {:actor-id "op-1" :actor-role :provider-staff :phase 1})

;; ----------------------------- driving the real actor -----------------------------

(defn- audit-of [run] (get-in run [:state :audit] []))

(defn- exec!
  "One operation through the compiled StateGraph. Returns the run's
  audit channel so the renderer can show the human-approval trail,
  which `edsupport.operation` keeps in-run and does NOT persist to the
  store ledger (only `:committed` / `:governor-hold` facts are
  appended there)."
  [actor tid request ctx]
  (audit-of (g/run* actor {:request request :context ctx} {:thread-id tid})))

(defn- approve! [actor tid]
  (audit-of (g/run* actor {:approval {:status :approved :by "op-1"}}
                    {:thread-id tid :resume? true})))

(defn run-demo!
  "Runs a fresh seeded store through a scenario that reaches every
  disposition this actor can produce.

  client-1 (JPN, integrity resolved, background check cleared) clears
  a full lifecycle: intake (auto-commit -- the ONLY op in phase 3's
  `:auto` set), an assessment verification, an assessment-integrity
  screening and a background-check screening (each phase-gated to a
  human approval), then a placement finalization (which ALWAYS
  escalates -- `:actuation/finalize-placement` is absent from every
  phase's `:auto` set AND is in `edsupport.governor/high-stakes`, two
  independent layers agreeing) -- all approved by op-1.

  Then five HARD holds, none of which ever reaches a human:
    - client-2: an assessment verification for a jurisdiction with no
      official spec-basis in `edsupport.facts`   -> :no-spec-basis
    - client-3: a screening that itself detects an unresolved
      assessment-administration irregularity
                       -> :assessment-administration-irregularity-unresolved
    - client-4: a screening that itself detects an uncleared
      background check                          -> :background-check-not-cleared
    - client-2: a placement finalization with no assessment evidence
      checklist on file                          -> :evidence-incomplete
    - client-1: a second placement finalization  -> :already-finalized

  And one phase-gate hold: client-1's assessment verification replayed
  at phase 1, where `:assessment/verify` is not yet an enabled write.

  Returns {:db store :run-audit [..]} -- every field the renderer
  reads is real governor/phase/store output, not a hand-typed copy."
  []
  (let [db (store/seed-db)
        actor (op/build db)
        trail (volatile! [])
        step! (fn [audit] (vswap! trail into audit) audit)]

    (step! (exec! actor "t1-intake" {:op :client/intake :subject "client-1"
                                     :patch {:id "client-1" :client-name "Sato Kenji"}}
                  operator))

    (step! (exec! actor "t2-assess" {:op :assessment/verify :subject "client-1"} operator))
    (step! (approve! actor "t2-assess"))

    (step! (exec! actor "t3-integrity" {:op :integrity/screen :subject "client-1"} operator))
    (step! (approve! actor "t3-integrity"))

    (step! (exec! actor "t4-background" {:op :background-check/screen :subject "client-1"} operator))
    (step! (approve! actor "t4-background"))

    (step! (exec! actor "t5-finalize" {:op :actuation/finalize-placement :subject "client-1"} operator))
    (step! (approve! actor "t5-finalize"))

    (step! (exec! actor "t6-assess" {:op :assessment/verify :subject "client-2" :no-spec? true} operator))
    (step! (exec! actor "t7-integrity" {:op :integrity/screen :subject "client-3"} operator))
    (step! (exec! actor "t8-background" {:op :background-check/screen :subject "client-4"} operator))
    (step! (exec! actor "t9-finalize" {:op :actuation/finalize-placement :subject "client-2"} operator))

    (step! (exec! actor "t10-phase" {:op :assessment/verify :subject "client-1"} early-phase-operator))

    (step! (exec! actor "t11-finalize" {:op :actuation/finalize-placement :subject "client-1"} operator))

    {:db db :run-audit @trail}))

;; ----------------------------- rendering helpers -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- kw->s [v] (if (keyword? v) (name v) (str v)))

(defn- join-basis [basis]
  (str/join ", " (map kw->s basis)))

(defn- yes-no [flag bad-label good-label]
  (if flag
    (str "<span class=\"critical\">" bad-label "</span>")
    (str "<span class=\"ok\">" good-label "</span>")))

(defn- hard-hold? [fact]
  (and (= :governor-hold (:t fact)) (seq (:basis fact))))

(defn- phase-hold? [fact]
  (and (= :governor-hold (:t fact)) (empty? (:basis fact))))

(defn- last-fact-for [ledger id]
  (last (filter #(= (:subject %) id) ledger)))

(defn- status-cell [ledger id]
  (let [f (last-fact-for ledger id)]
    (cond
      (nil? f) "<span class=\"muted\">no activity</span>"
      (hard-hold? f) (str "<span class=\"critical\">HARD hold &middot; "
                          (esc (join-basis (:basis f))) "</span>")
      (phase-hold? f) (str "<span class=\"warn\">phase gate hold &middot; "
                           (esc (kw->s (:phase-reason f))) "</span>")
      (= :committed (:t f)) (str "<span class=\"ok\">committed &middot; "
                                 (esc (kw->s (:op f))) "</span>")
      :else "<span class=\"muted\">in progress</span>")))

;; ----------------------------- approver attribution -----------------------------
;;
;; MEASURED, not assumed. `edsupport.operation`'s :request-approval node
;; puts the approver on the record's `:payload`; whether that survives
;; depends on which `:effect` branch of `edsupport.store/commit-record!`
;; handles it -- the payload-carrying branches (`:assessment/set`,
;; `:integrity/set`, `:background-check/set`) keep it, while
;; `:client/mark-finalized` recomputes its record from
;; `edsupport.registry` and therefore cannot. Rather than hardcode that
;; split, the page DERIVES it per record by looking for an approver key
;; and falling back to the audit fact -- so the disclosure disappears by
;; itself the day the store starts retaining it.

(defn- retained-approver
  "The approver actually persisted with a committed record, whatever
  key shape the register uses -- or nil if the store dropped it."
  [record]
  (when (map? record)
    (or (get record :approved-by) (get record "approved_by") (get record "approved-by"))))

(defn- audited-approver
  "The human approver recorded in the run's audit channel for this
  op+subject (`:approval-granted`), which the store ledger does not
  persist."
  [run-audit op subject]
  (some (fn [f]
          (when (and (= :approval-granted (:t f)) (= op (:op f)) (= subject (:subject f)))
            (:by f)))
        run-audit))

(defn- approver-cell [record run-audit op subject]
  (let [retained (retained-approver record)
        audited (audited-approver run-audit op subject)]
    (cond
      retained (format "<span class=\"ok\">%s</span> <span class=\"muted\">(retained in record)</span>"
                       (esc retained))
      audited (format "<span class=\"warn\">%s</span> <span class=\"muted\">(audit only &mdash; not retained in record)</span>"
                      (esc audited))
      :else "<span class=\"muted\">no human approval in this run</span>")))

;; ----------------------------- rows -----------------------------

(defn- client-row [ledger {:keys [id client-name jurisdiction placement-number
                                  assessment-administration-irregularity-unresolved?
                                  background-check-not-cleared? placement-finalized?]}]
  (format "        <tr><td><code>%s</code></td><td>%s</td><td><code>%s</code></td><td>%s</td><td>%s</td><td>%s</td><td>%s</td></tr>"
          (esc id) (esc client-name) (esc jurisdiction)
          (yes-no assessment-administration-irregularity-unresolved?
                  "irregularity unresolved" "resolved")
          (yes-no background-check-not-cleared? "not cleared" "cleared")
          (if placement-finalized?
            (str "<span class=\"ok\">finalized &middot; <span class=\"num\">"
                 (esc placement-number) "</span></span>")
            "<span class=\"muted\">not finalized</span>")
          (status-cell ledger id)))

(defn- rule-row [[rule facts]]
  (format "        <tr><td><code>%s</code></td><td class=\"num\">%d</td><td>%s</td><td>%s</td></tr>"
          (esc (kw->s rule))
          (count facts)
          (esc (str/join ", " (map :subject facts)))
          (esc (or (->> facts
                        (mapcat :violations)
                        (filter #(= rule (:rule %)))
                        first
                        :detail)
                   ""))))

(defn- gate-row
  "COMPUTED from `edsupport.phase`, not described from memory: asks the
  real gate what it does with a governor-clean proposal at phase 3, and
  reads the phase table for which phases permit the write at all and
  which permit an unattended commit."
  [op]
  (let [{:keys [disposition reason]} (phase/gate phase/default-phase {:op op} :commit)
        permits (fn [k] (->> (keys phase/phases)
                             sort
                             (filter #(contains? (get-in phase/phases [% k]) op))
                             (map str)
                             (str/join ", ")))
        writes-in (permits :writes)
        auto-in (permits :auto)]
    (format "        <tr><td><code>%s</code></td><td>%s</td><td class=\"num\">%s</td><td class=\"num\">%s</td></tr>"
            (esc (kw->s op))
            (case disposition
              :commit "<span class=\"ok\">auto-commit when governor-clean</span>"
              :escalate (str "<span class=\"warn\">human approval &middot; "
                             (esc (kw->s reason)) "</span>")
              :hold (str "<span class=\"critical\">hold &middot; " (esc (kw->s reason)) "</span>"))
            (if (str/blank? writes-in) "&mdash;" (esc writes-in))
            (if (str/blank? auto-in)
              "<span class=\"critical\">never</span>"
              (esc auto-in)))))

(defn- jurisdiction-row [[iso3 {:keys [name owner-authority legal-basis provenance required-evidence]}]]
  (format "        <tr><td><code>%s</code></td><td>%s</td><td>%s</td><td>%s</td><td class=\"num\">%d</td><td><a href=\"%s\">source</a></td></tr>"
          (esc iso3) (esc name) (esc owner-authority) (esc legal-basis)
          (count required-evidence) (esc provenance)))

(defn- register-rows
  "One row per committed screening/assessment record actually found in
  the store, per client."
  [db run-audit clients]
  (for [{:keys [id]} clients
        [label op record summarize]
        [["assessment evidence checklist" :assessment/verify (store/assessment-of db id)
          (fn [r] (str (count (:checklist r)) " item(s) &middot; spec-basis "
                       (if (:spec-basis r)
                         (str "<code>" (esc (:spec-basis r)) "</code>")
                         "<span class=\"critical\">none</span>")))]
         ["assessment-integrity screening" :integrity/screen (store/integrity-of db id)
          (fn [r] (yes-no (:assessment-administration-irregularity-unresolved? r)
                          "irregularity unresolved" "resolved"))]
         ["background check" :background-check/screen (store/background-check-of db id)
          (fn [r] (if (= :cleared (:verdict r))
                    "<span class=\"ok\">cleared</span>"
                    (str "<span class=\"critical\">" (esc (kw->s (:verdict r))) "</span>")))]]
        :when record]
    (format "        <tr><td><code>%s</code></td><td>%s</td><td>%s</td><td>%s</td></tr>"
            (esc id) (esc label) (summarize record)
            (approver-cell record run-audit op id))))

(defn- placement-row [record run-audit]
  (format "        <tr><td class=\"num\">%s</td><td>%s</td><td><code>%s</code></td><td><code>%s</code></td><td>%s</td><td>%s</td></tr>"
          (esc (get record "record_id"))
          (esc (get record "kind"))
          (esc (get record "client_id"))
          (esc (get record "jurisdiction"))
          (if (get record "immutable")
            "<span class=\"ok\">immutable</span>"
            "<span class=\"critical\">mutable</span>")
          (approver-cell record run-audit :actuation/finalize-placement (get record "client_id"))))

(defn- ledger-row [{:keys [t op subject basis phase-reason confidence] :as f}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td><code>%s</code></td><td>%s</td><td class=\"num\">%s</td></tr>"
          (cond
            (hard-hold? f) "<span class=\"critical\">governor-hold (HARD)</span>"
            (phase-hold? f) "<span class=\"warn\">governor-hold (phase gate)</span>"
            :else (str "<span class=\"ok\">" (esc (kw->s t)) "</span>"))
          (esc (kw->s op)) (esc subject)
          (esc (cond
                 (seq basis) (join-basis basis)
                 phase-reason (str (kw->s phase-reason) " (phase " (:phase f) ")")
                 :else ""))
          (esc (or confidence ""))))

(defn- approval-row [{:keys [t op subject reason phase confidence by]}]
  (format "        <tr><td>%s</td><td><code>%s</code></td><td><code>%s</code></td><td>%s</td><td class=\"num\">%s</td></tr>"
          (if (= :approval-granted t)
            "<span class=\"ok\">approval-granted</span>"
            "<span class=\"warn\">approval-requested</span>")
          (esc (kw->s op)) (esc subject)
          (esc (if by (str "approved by " by) (str (kw->s reason) " (phase " phase ")")))
          (esc (or confidence ""))))

(defn- proposal-row [{:keys [op subject summary rationale confidence]}]
  (format "        <tr><td><code>%s</code></td><td><code>%s</code></td><td>%s</td><td>%s</td><td class=\"num\">%s</td></tr>"
          (esc (kw->s op)) (esc subject) (esc summary) (esc rationale) (esc confidence)))

;; ----------------------------- document -----------------------------

(defn- section [title lead headers rows]
  (str "  <section class=\"card\">\n"
       "    <h2>" title "</h2>\n"
       "    <p class=\"muted\">" lead "</p>\n"
       "    <table>\n"
       "      <thead><tr>" (str/join "" (map #(str "<th>" % "</th>") headers)) "</tr></thead>\n"
       "      <tbody>\n"
       (str/join "\n" rows) "\n"
       "      </tbody>\n"
       "    </table>\n"
       "  </section>\n"))

(defn render
  "Renders the full operator-console.html document from the result of
  `run-demo!` (or any other real scenario)."
  [{:keys [db run-audit]}]
  (let [ledger (vec (store/ledger db))
        clients (store/all-clients db)
        placements (vec (store/placement-history db))
        holds (filter #(= :governor-hold (:t %)) ledger)
        hard-holds (filter hard-hold? ledger)
        by-rule (sort-by (comp kw->s key)
                         (group-by #(-> % :basis first) hard-holds))
        approvals (filter #(#{:approval-requested :approval-granted} (:t %)) run-audit)
        proposals (filter #(= :edsupportadvisor-proposal (:t %)) run-audit)
        seen-jurisdictions (sort (distinct (keep :jurisdiction clients)))
        cov (facts/coverage seen-jurisdictions)]
    (str
     "<html lang=\"en\"><head><meta charset=\"utf-8\">"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
     "<title>cloud-itonami-isic-8550 &middot; educational-support-services</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Educational support services (ISIC 8550) — Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample · governor-gated · placement finalization always human-approved</span>\n"
     "</header>\n"
     "<main>\n"
     "  <section class=\"banner\">\n"
     "    <p>Build-time-generated by <code>edsupport.render-html</code> (<code>clojure -M:dev:render-html</code>) by running the real actor —\n"
     "       <code>edsupport.operation</code> → <code>edsupport.governor</code> → <code>edsupport.phase</code> → <code>edsupport.store</code>.\n"
     "       Every identifier below is read back out of the store or the run's audit channel after the graph ran; nothing on this page is hand-typed.</p>\n"
     "    <p class=\"muted\">This run: <span class=\"num\">" (count clients) "</span> seeded clients ·\n"
     "       <span class=\"num\">" (count ledger) "</span> persisted ledger facts ·\n"
     "       <span class=\"critical num\">" (count hard-holds) "</span> HARD governor holds across\n"
     "       <span class=\"num\">" (count by-rule) "</span> distinct rules ·\n"
     "       <span class=\"num\">" (- (count holds) (count hard-holds)) "</span> phase-gate hold ·\n"
     "       <span class=\"num\">" (count (filter #(= :approval-granted (:t %)) approvals)) "</span> human approvals ·\n"
     "       <span class=\"num\">" (count placements) "</span> placement finalization record.</p>\n"
     "  </section>\n"

     (section "Client directory"
              (str "Seeded client set (<code>edsupport.store/demo-data</code>) as it stands after the run. "
                   "The two screening flags are read straight off each client record — they are the facts "
                   "<code>edsupport.governor</code> re-checks independently of anything the advisor claims.")
              ["Client" "Name" "Jurisdiction" "Assessment integrity" "Background check" "Placement" "Last op status"]
              (map (partial client-row ledger) clients))

     (section "HARD governor holds observed in this run"
              (str "Un-overridable. A human approver cannot approve past any of these — the graph routes them "
                   "straight to <code>:hold</code> and they never reach the <code>:request-approval</code> node. "
                   "Rule names and details below are read out of the persisted violation records, not restated.")
              ["Rule" "Holds" "Subject(s)" "Detail (as recorded)"]
              (map rule-row by-rule))

     (section "Action gate — computed from edsupport.phase"
              (str "Each row is produced by actually calling <code>edsupport.phase/gate</code> at the default phase "
                   "(<span class=\"num\">" phase/default-phase "</span>) with a governor-clean proposal, and by reading the phase table — "
                   "not transcribed from documentation. "
                   "<code>:actuation/finalize-placement</code> is absent from every phase's auto set; "
                   "<code>edsupport.governor/high-stakes</code> escalates it independently, so two layers agree.")
              ["Op" "At phase 3, governor-clean" "Phases permitting the write" "Phases permitting auto-commit"]
              (map gate-row (sort phase/write-ops)))

     (section "Jurisdiction spec-basis catalog"
              (str "<code>edsupport.facts/catalog</code> — the official sources the governor requires a proposal to cite "
                   "before any placement can be finalized. Coverage over the jurisdictions actually present in the client "
                   "directory is reported honestly: requested <span class=\"num\">" (:requested cov) "</span>, "
                   "covered <span class=\"num\">" (:covered cov) "</span>, missing "
                   "<span class=\"critical\">" (esc (str/join ", " (:missing-jurisdictions cov))) "</span> — "
                   "a jurisdiction not in this table has NO spec-basis, and the governor holds any proposal that invents one.")
              ["ISO3" "Jurisdiction" "Owner authority" "Legal basis" "Required evidence" "Provenance"]
              (map jurisdiction-row (sort-by key facts/catalog)))

     (section "Committed registers"
              (str "What the SSoT actually retained. The approver column is DERIVED per record — it looks for an approver "
                   "key on the stored record and falls back to the run's <code>:approval-granted</code> audit fact, "
                   "labelling that case explicitly rather than showing a blank. A blank would not let you tell "
                   "\"nobody approved\" from \"the store did not keep it\".")
              ["Client" "Register" "Committed value" "Approver"]
              (register-rows db run-audit clients))

     (section "Placement finalization register"
              (str "The append-only book of record (<code>edsupport.registry</code>). Every certificate this actor "
                   "produces is an UNSIGNED draft — signature is the provider's own act, not this actor's. "
                   "This register is rebuilt from the client id and a jurisdiction-scoped sequence at commit time, "
                   "which is why the approver shows as audit-only above.")
              ["Placement number" "Kind" "Client" "Jurisdiction" "Immutability" "Approver"]
              (map #(placement-row % run-audit) placements))

     (section "Persisted audit ledger"
              (str "Append-only decision-fact log held by <code>edsupport.store</code> — every commit and every hold "
                   "this scenario produced, in order. Holds with a rule basis are HARD; the one with a phase reason "
                   "is the rollout gate refusing a write the governor itself had cleared.")
              ["Fact" "Op" "Subject" "Basis" "Confidence"]
              (map ledger-row ledger))

     (section "Human approval trail (run audit channel)"
              (str "Escalations and their resolutions. These live in the graph's <code>:audit</code> channel for the run; "
                   "<code>edsupport.operation</code> persists only <code>:committed</code> and <code>:governor-hold</code> "
                   "facts to the store, so this is the only place the approver's identity appears for a "
                   "<code>:actuation/finalize-placement</code>.")
              ["Fact" "Op" "Subject" "Reason / approver" "Confidence"]
              (map approval-row approvals))

     (section "EdSupportOps-LLM proposals (contained intelligence node)"
              (str "Every proposal the advisor emitted, with the rationale the governor scanned. The advisor is sealed "
                   "into the <code>:advise</code> node and can only propose — it never writes the SSoT, and a low-confidence "
                   "or uncited proposal is censored downstream before anything commits.")
              ["Op" "Subject" "Summary" "Rationale" "Confidence"]
              (map proposal-row proposals))

     "</main>\n"
     "<footer>\n"
     "  <p>cloud-itonami-isic-8550 — educational support services actor. Regenerate with\n"
     "     <code>clojure -M:dev:render-html</code>. Deterministic: the page contains no timestamps and\n"
     "     two consecutive runs are byte-identical.</p>\n"
     "</footer>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        {:keys [db] :as result} (run-demo!)
        ledger (vec (store/ledger db))
        holds (filterv #(= :governor-hold (:t %)) ledger)
        hard (filterv hard-hold? ledger)]
    ;; Build-time invariant, not a convention: a console that shows no
    ;; governor hold is not evidence that the governor works. Refuse to
    ;; emit one. (Precedent: cloud-itonami-isic-2513.)
    (when (zero? (count holds))
      (throw (ex-info "render-html: the run produced 0 :governor-hold records -- refusing to emit a console that cannot demonstrate the governor"
                      {:ledger-facts (count ledger)})))
    (when (zero? (count hard))
      (throw (ex-info "render-html: the run produced no HARD governor hold (every hold had an empty :basis) -- refusing to emit a console that cannot demonstrate an un-overridable rule"
                      {:holds (count holds)})))
    (.mkdirs (java.io.File. (.getParent (java.io.File. ^String out))))
    (spit out (render result))
    (println "wrote" out
             (str "(" (count ledger) " ledger facts, "
                  (count hard) " HARD holds over "
                  (count (distinct (map (comp first :basis) hard))) " distinct rules, "
                  (- (count holds) (count hard)) " phase-gate hold, "
                  (count (store/placement-history db)) " placement record)"))))
