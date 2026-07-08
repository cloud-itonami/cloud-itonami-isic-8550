# ADR-0001: EdSupportOps-LLM ⊣ Support Services Governor architecture

## Status

Accepted. `cloud-itonami-isic-8550` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-8550` publishes an OSS business blueprint for
educational support activities: non-instructional support for
education such as educational testing, guidance counseling, and
student-exchange placement services. Like every prior actor in this
fleet, the blueprint alone is not an implementation: this ADR records
the governed-actor architecture that promotes it to real, tested
code, following the same langgraph StateGraph + independent Governor
+ Phase 0→3 rollout pattern established by `cloud-itonami-isic-6511`
(life insurance) and applied across sixty-four prior siblings, most
recently `cloud-itonami-isic-9609` (other personal service activities
n.e.c.).

## Decision

### Decision 1: single-actuation shape

This blueprint's own README/business-model.md/operator-guide.md
consistently name only ONE real-world act: "finalizing a placement or
referral." Matching `leasing`/`underwriting`/`testlab`/`clinic`/
`veterinary`/`funeral`/`parksafety`/`salon`/`entertainment`/
`facility`/`consulting`/`advertising`/`polling`/`research`/`design`/
`sports`/`alliedhealth`/`photo`/`personalservice`'s single-actuation
shape, `high-stakes` here is a one-member set, `#{:actuation/
finalize-placement}`.

### Decision 2: entity and op shape

The primary entity is a `client`, matching the README's own Core
Contract language ("intake + identity + academic records"). Five
ops: `:client/intake` (directory upsert, no capital risk),
`:assessment/verify` (per-jurisdiction evidence checklist, never
auto), `:integrity/screen` (assessment-administration-irregularity
screening, unconditional-evaluation discipline, never auto),
`:background-check/screen` (background-check screening,
unconditional-evaluation discipline, never auto), and `:actuation/
finalize-placement` (POSITIVE, high-stakes -- finalizing a real
placement or referral). Two screening ops (rather than the more
common one) because this build introduces two independent
unconditional-evaluation concerns.

### Decision 3: `assessment-administration-irregularity-unresolved-violations` -- the 49th unconditional-evaluation screening grounding, a genuinely new concept

Before writing this check, every prior sibling's governor/registry
namespaces were grepped for "proctor", "exam-integrity", "test-
security" and "testing-integrity" -- zero hits, confirming this is a
genuinely new unconditional-evaluation concept, avoiding the false-
precedent-claim risk `leasing`'s ADR-0001 documents.
`assessment-administration-irregularity-unresolved-violations` reuses
the unconditional-evaluation DISCIPLINE (`casualty.governor/
sanctions-violations`'s original fix) for the 49th distinct
application overall, continuing the count established across this
fleet's builds (most recently `personalservice.governor/background-
check-not-cleared-violations` at 48th). Grounded in real test-
security practice: AERA/APA/NCME's Standards for Educational and
Psychological Testing (test-security and administration-integrity
provisions), and the real-world precedent of ETS's Office of Testing
Integrity. Gates `:integrity/screen` and `:actuation/finalize-
placement`.

### Decision 4: `background-check-not-cleared-violations` -- an honest FOURTH literal reuse, not claimed as new

`school.governor` established this concept FIRST; `sports.governor`
reused it literally as the SECOND instance; `personalservice.
governor` as the THIRD. `edsupport.governor/background-check-not-
cleared-violations` is the FOURTH literal instance of this specific
concept, and the 50th distinct application of the unconditional-
evaluation discipline overall -- not claimed as new. Grounded in real
safeguarding law requiring background checks for staff working
directly with students (UK's Keeping Children Safe in Education DBS-
check mandate; Germany's erweitertes Führungszeugnis requirement).
Gates `:background-check/screen` and `:actuation/finalize-placement`.

### Decision 5: dedicated double-actuation-guard boolean

`:placement-finalized?` is a dedicated boolean on the `client`
record, never a single `:status` value -- the same discipline every
prior sibling governor's guards establish, informed by `cloud-
itonami-isic-6492`'s real status-lifecycle bug (ADR-2607071320).

### Decision 6: Store protocol, MemStore + DatomicStore parity

`edsupport.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in `test/edsupport/
store_contract_test.clj` -- the same seam every sibling actor uses so
swapping the SSoT backend is a configuration change, not a rewrite.
The protocol's per-entity accessor is named `client` directly -- not
a Clojure special form, so no `-of` suffix workaround was needed.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:client/intake` (no
capital risk). `:assessment/verify`, `:integrity/screen` and
`:background-check/screen` are never auto-eligible at any phase
(matching every sibling's screening-op posture), and `:actuation/
finalize-placement` is permanently excluded from every phase's
`:auto` set -- a structural fact, not a rollout milestone, enforced
by BOTH `edsupport.phase` and `edsupport.governor`'s `high-stakes`
set independently.

### Decision 8: no bespoke domain capability lib

This blueprint's own `:itonami.blueprint/required-technologies` names
no domain-specific capability beyond the generic robotics/identity/
forms/dmn/bpmn/audit-ledger stack -- there was no capability-lib
decision to make at all.

### Decision 9: mock + LLM advisor pair

`edsupport.edsupportadvisor` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-
finalizing a placement).

### Decision 10: no `blueprint.edn` field-sync fixes needed

Matching `photo`/7420's and `personalservice`/9609's own experience,
this repo's `blueprint.edn` already had the correct `isic-` prefixed
`:id` and correctly populated `:required-technologies`/`:optional-
technologies` matching the `kotoba-lang/industry` registry's own
entry for `"8550"` exactly -- only the `:maturity` field itself
needed adding.

## Alternatives considered

- **A dual-actuation shape** (e.g. splitting "placement" and
  "referral" into two acts). Rejected: the blueprint's own text
  consistently names only ONE real-world act ("finalizing a
  placement or referral"); inventing a second would not be grounded
  in the blueprint's own text.
- **A single combined screening op** covering both the integrity and
  background-check concerns. Rejected: the two concerns are
  independently groundable in different real-world regulatory
  regimes (test-security standards vs. student-safeguarding law), so
  two separate dedicated ops (each gated by its own HARD check) more
  precisely match the "screen the screening op directly" discipline
  this fleet's ADRs already establish, rather than conflating two
  distinct real-world concerns into one screening proposal.
- **Reusing `care.registry/caregiver-workload-exceeds-maximum?`'s
  caseload-ratio shape** for a "counselor-caseload-exceeds-maximum?"
  check. Considered, then rejected in favor of the genuinely new
  assessment-administration-irregularity concept: reusing the
  caseload shape again would have added no new check-family instance
  this build could not already get from an honest reuse elsewhere,
  whereas the testing-integrity concept was grep-verified absent
  fleet-wide.

## Consequences

- Sixty-fifth actor in this fleet (64 implemented before this build).
- Establishes a genuinely NEW unconditional-evaluation-screening
  concept (assessment-administration-irregularity-unresolved),
  grep-verified absent from every prior sibling before the claim was
  finalized.
- Documents an honest FOURTH literal reuse of the background-check-
  not-cleared concept (school 1st, sports 2nd, personalservice 3rd,
  edsupport 4th), not claimed as new.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/edsupport/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- `blueprint.edn` required no field-sync fixes this time (already
  correct) -- only the `:maturity` flip itself.

## References

- `orgs/cloud-itonami/cloud-itonami-isic-8550/README.md`
- `orgs/cloud-itonami/cloud-itonami-isic-8550/docs/business-model.md`
- `orgs/kotoba-lang/industry/resources/kotoba/industry/registry.edn` (entry `"8550"`)
