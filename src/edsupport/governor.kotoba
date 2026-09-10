(ns edsupport.governor
  "Support Services Governor -- the independent compliance layer that
  earns the EdSupportOps-LLM the right to commit. The LLM has no
  notion of jurisdictional educational-testing/counseling law,
  whether a client's own recorded assessment-administration
  irregularity has actually stayed unresolved, whether a background
  check has actually stayed cleared, or when an act stops being a
  draft and becomes a real-world placement or referral finalization,
  so this MUST be a separate system able to *reject* a proposal and
  fall back to HOLD -- the educational-support analog of
  `cloud-itonami-isic-8620`'s ClinicGovernor.

  Five checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them (you don't get to approve your way
  past a fabricated jurisdiction spec-basis, incomplete evidence, an
  unresolved test-administration irregularity, an uncleared
  background check, or a double finalization). The confidence/
  actuation gate is SOFT: it asks a human to look (low confidence /
  actuation), and the human may approve -- but see `edsupport.phase`:
  for `:stake :actuation/finalize-placement` (a real placement/
  referral finalization) NO phase ever allows auto-commit either. Two
  independent layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the assessment proposal cite
                                       an OFFICIAL source
                                       (`edsupport.facts`), or invent
                                       one?
    2. Evidence incomplete         -- for `:actuation/finalize-
                                       placement`, has the client
                                       actually been assessed with a
                                       full client-consent-record/
                                       assessment-record/counselor-
                                       qualification-verification-
                                       record/placement-completion-
                                       record evidence checklist on
                                       file?
    3. Assessment-administration
       irregularity unresolved     -- reported by THIS proposal
                                       itself (an `:integrity/screen`
                                       that just found an unresolved
                                       irregularity), or already on
                                       file for the client
                                       (`:integrity/screen`/
                                       `:actuation/finalize-
                                       placement`). Evaluated
                                       UNCONDITIONALLY (not scoped to
                                       a specific op) so the screening
                                       op itself can HARD-hold on its
                                       own finding. A GENUINELY NEW
                                       concept in this fleet (grep-
                                       verified absent -- no proctor/
                                       exam-integrity/test-security/
                                       testing-integrity concept
                                       exists anywhere else in this
                                       fleet), the 49th
                                       distinct application of the
                                       unconditional-evaluation
                                       discipline overall (`casualty.
                                       governor/sanctions-violations`'s
                                       original fix; most recently
                                       `personalservice.governor/
                                       background-check-not-cleared-
                                       violations` at 48th). Grounded
                                       in real test-security practice
                                       (AERA/APA/NCME's Standards for
                                       Educational and Psychological
                                       Testing, and ETS's Office of
                                       Testing Integrity precedent for
                                       real-world test-security
                                       enforcement).
    4. Background check not
       cleared                        -- reported by THIS proposal
                                       itself (a `:background-check/
                                       screen` that just found an
                                       uncleared check), or already on
                                       file for the client
                                       (`:background-check/screen`/
                                       `:actuation/finalize-
                                       placement`). Evaluated
                                       UNCONDITIONALLY, the SAME
                                       discipline as check 3 above --
                                       an HONEST literal reuse of
                                       `school.governor/background-
                                       check-not-cleared-violations`'s
                                       own concept (the FIRST
                                       instance; `sports.governor`
                                       reused it literally as the
                                       SECOND, `personalservice.
                                       governor` as the THIRD) -- the
                                       FOURTH literal instance of this
                                       specific concept, and the 50th
                                       distinct application of the
                                       unconditional-evaluation
                                       discipline overall, not claimed
                                       as new. Grounded in real
                                       safeguarding law requiring
                                       background checks for staff
                                       working directly with students
                                       (UK's Keeping Children Safe in
                                       Education DBS-check mandate;
                                       Germany's erweitertes
                                       Führungszeugnis requirement).
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/
                                       finalize-placement` (a REAL
                                       placement/referral finalization)
                                       -> escalate.

  One more guard, double-finalization prevention, is enforced but NOT
  listed as a numbered HARD check above because it needs no upstream
  comparison at all -- `already-finalized-violations` refuses to
  finalize a placement for the SAME client twice, off a dedicated
  `:placement-finalized?` fact (never a `:status` value) -- the SAME
  'check a dedicated boolean, not status' discipline every prior
  sibling governor's guards establish, informed by `cloud-itonami-
  isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [edsupport.facts :as facts]
            [edsupport.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Finalizing a real placement or referral is the ONE real-world
  actuation event this actor performs -- a single-member set,
  matching `cloud-itonami-isic-6511`'s/`6621`'s/`6629`'s/`6612`'s/
  `6492`'s/`7120`'s/`8620`'s/`personalservice`'s single-actuation
  shape."
  #{:actuation/finalize-placement})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:assessment/verify` (or `:actuation/finalize-placement`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's educational-testing/counseling
  requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:assessment/verify :actuation/finalize-placement} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は評価基準として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/finalize-placement`, the jurisdiction's required
  client-consent-record/assessment-record/counselor-qualification-
  verification-record/placement-completion-record evidence must
  actually be satisfied -- do not trust the advisor's self-reported
  confidence alone."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-placement)
    (let [c (store/client st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction c) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(保護者同意記録/評価記録/カウンセラー資格確認記録/配置完了記録等)が充足していない状態での提案"}]))))

(defn- assessment-administration-irregularity-unresolved-violations
  "An unresolved test-administration irregularity -- reported by THIS
  proposal (e.g. an `:integrity/screen` that itself just found an
  unresolved irregularity), or already on file in the store for the
  client (`:integrity/screen`/`:actuation/finalize-placement`) -- is a
  HARD, un-overridable hold. Evaluated UNCONDITIONALLY (not scoped to
  a specific op) so the screening op itself can HARD-hold on its own
  finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (true? (get-in proposal [:value :assessment-administration-irregularity-unresolved?]))
        client-id (when (contains? #{:integrity/screen :actuation/finalize-placement} op) subject)
        hit-on-file? (and client-id (true? (:assessment-administration-irregularity-unresolved? (store/client st client-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :assessment-administration-irregularity-unresolved
        :detail "評価実施の不正/セキュリティ上の疑義が未解決の状態での配置確定提案は進められない"}])))

(defn- background-check-not-cleared-violations
  "An uncleared background check -- reported by THIS proposal (e.g. a
  `:background-check/screen` that itself just found an uncleared
  check), or already on file in the store for the client
  (`:background-check/screen`/`:actuation/finalize-placement`) -- is a
  HARD, un-overridable hold. Evaluated UNCONDITIONALLY (not scoped to
  a specific op) so the screening op itself can HARD-hold on its own
  finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (= :not-cleared (get-in proposal [:value :verdict]))
        client-id (when (contains? #{:background-check/screen :actuation/finalize-placement} op) subject)
        hit-on-file? (and client-id (= :not-cleared (:verdict (store/background-check-of st client-id))))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :background-check-not-cleared
        :detail "身元確認が未完了の状態での配置確定提案は進められない"}])))

(defn- already-finalized-violations
  "For `:actuation/finalize-placement`, refuses to finalize a
  placement for the SAME client twice, off a dedicated `:placement-
  finalized?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :actuation/finalize-placement)
    (when (store/client-already-finalized? st subject)
      [{:rule :already-finalized
        :detail (str subject " は既に配置確定済み")}])))

(defn check
  "Censors an EdSupportOps-LLM proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (assessment-administration-irregularity-unresolved-violations request proposal st)
                           (background-check-not-cleared-violations request proposal st)
                           (already-finalized-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
