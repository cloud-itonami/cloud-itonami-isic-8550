(ns edsupport.registry
  "Pure-function placement/referral-finalization record construction --
  an append-only educational-support-services book-of-record draft.

  Like every sibling actor's registry, there is no single
  international check-digit standard for a placement/referral
  reference number -- every provider/jurisdiction assigns its own
  reference format. This namespace does NOT invent one; it builds a
  jurisdiction-scoped sequence number and validates the record's
  required fields, the same honest, non-fabricating discipline
  `edsupport.facts` uses.

  Unlike siblings whose distinctive check is a numeric/temporal
  ground-truth recompute (a registry-level pure predicate), this
  build's TWO distinctive checks (`assessment-administration-
  irregularity-unresolved?` and `background-check-not-cleared?`) are
  both BOOLEAN flags read directly off the client's own record by
  `edsupport.governor` -- the same shape `photo.governor`'s `minor-
  subject-guardian-consent-unresolved-violations` and `personalservice.
  governor`'s `background-check-not-cleared-violations` use, neither
  of which needed a dedicated registry-level predicate either. This
  namespace is therefore intentionally 'plain': record construction
  only, no distinctive check function.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real student-services system. It builds the RECORD an
  educational-support provider would keep, not the act of finalizing
  the placement itself (that is `edsupport.operation`'s `:actuation/
  finalize-placement`, always human-gated -- see README `Actuation`)."
  (:require [kotoba.lang.text :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  provider's own act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn register-placement-finalization
  "Validate + construct the PLACEMENT-FINALIZATION registration DRAFT
  -- the educational-support provider's own act of finalizing a real
  placement or referral. Pure function -- does not touch any real
  student-services system; it builds the RECORD a provider would
  keep. `edsupport.governor` independently re-verifies the client's
  own assessment-integrity and background-check status, and blocks a
  double-finalization for the same client, before this is ever
  allowed to commit."
  [client-id jurisdiction sequence]
  (when-not (and client-id (not= client-id ""))
    (throw (ex-info "placement-finalization: client_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "placement-finalization: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "placement-finalization: sequence must be >= 0" {})))
  (let [placement-number (str (str/upper jurisdiction) "-PLC-" (zero-pad sequence 6))
        record {"record_id" placement-number
                "kind" "placement-finalization-draft"
                "client_id" client-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "placement_number" placement-number
     "certificate" (unsigned-certificate "PlacementFinalization" placement-number placement-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
