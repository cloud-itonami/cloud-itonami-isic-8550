# cloud-itonami-isic-8550

Open Business Blueprint for **ISIC Rev.5 8550**: Educational support
activities.

This repository publishes an educational-support-services actor --
client intake, per-jurisdiction educational-testing/counseling
regulatory assessment, assessment-integrity screening,
background-check screening and placement/referral finalization -- as
an OSS business that any qualified, licensed operator can fork,
deploy, run, improve and sell, so a community or independent educator
never surrenders student data and ledgers to a closed SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet
([`cloud-itonami-isic-6511`](https://github.com/cloud-itonami/cloud-itonami-isic-6511),
[`6512`](https://github.com/cloud-itonami/cloud-itonami-isic-6512),
[`6621`](https://github.com/cloud-itonami/cloud-itonami-isic-6621),
[`6622`](https://github.com/cloud-itonami/cloud-itonami-isic-6622),
[`6629`](https://github.com/cloud-itonami/cloud-itonami-isic-6629),
[`6520`](https://github.com/cloud-itonami/cloud-itonami-isic-6520),
[`6530`](https://github.com/cloud-itonami/cloud-itonami-isic-6530),
[`6820`](https://github.com/cloud-itonami/cloud-itonami-isic-6820),
[`6612`](https://github.com/cloud-itonami/cloud-itonami-isic-6612),
[`6492`](https://github.com/cloud-itonami/cloud-itonami-isic-6492),
[`6920`](https://github.com/cloud-itonami/cloud-itonami-isic-6920),
[`6611`](https://github.com/cloud-itonami/cloud-itonami-isic-6611),
[`7120`](https://github.com/cloud-itonami/cloud-itonami-isic-7120),
[`8620`](https://github.com/cloud-itonami/cloud-itonami-isic-8620),
[`8530`](https://github.com/cloud-itonami/cloud-itonami-isic-8530),
[`9200`](https://github.com/cloud-itonami/cloud-itonami-isic-9200),
[`7500`](https://github.com/cloud-itonami/cloud-itonami-isic-7500),
[`9603`](https://github.com/cloud-itonami/cloud-itonami-isic-9603),
[`9521`](https://github.com/cloud-itonami/cloud-itonami-isic-9521),
[`9321`](https://github.com/cloud-itonami/cloud-itonami-isic-9321),
[`8730`](https://github.com/cloud-itonami/cloud-itonami-isic-8730),
[`9102`](https://github.com/cloud-itonami/cloud-itonami-isic-9102),
[`9103`](https://github.com/cloud-itonami/cloud-itonami-isic-9103),
[`9602`](https://github.com/cloud-itonami/cloud-itonami-isic-9602),
[`9000`](https://github.com/cloud-itonami/cloud-itonami-isic-9000),
[`8890`](https://github.com/cloud-itonami/cloud-itonami-isic-8890),
[`8610`](https://github.com/cloud-itonami/cloud-itonami-isic-8610),
[`9311`](https://github.com/cloud-itonami/cloud-itonami-isic-9311),
[`8510`](https://github.com/cloud-itonami/cloud-itonami-isic-8510),
[`9412`](https://github.com/cloud-itonami/cloud-itonami-isic-9412),
[`6491`](https://github.com/cloud-itonami/cloud-itonami-isic-6491),
[`8720`](https://github.com/cloud-itonami/cloud-itonami-isic-8720),
[`8521`](https://github.com/cloud-itonami/cloud-itonami-isic-8521),
[`6619`](https://github.com/cloud-itonami/cloud-itonami-isic-6619),
[`3600`](https://github.com/cloud-itonami/cloud-itonami-isic-3600),
[`6190`](https://github.com/cloud-itonami/cloud-itonami-isic-6190),
[`3030`](https://github.com/cloud-itonami/cloud-itonami-isic-3030),
[`3830`](https://github.com/cloud-itonami/cloud-itonami-isic-3830),
[`7020`](https://github.com/cloud-itonami/cloud-itonami-isic-7020),
[`9420`](https://github.com/cloud-itonami/cloud-itonami-isic-9420),
[`9491`](https://github.com/cloud-itonami/cloud-itonami-isic-9491),
[`2610`](https://github.com/cloud-itonami/cloud-itonami-isic-2610),
[`3512`](https://github.com/cloud-itonami/cloud-itonami-isic-3512),
[`8810`](https://github.com/cloud-itonami/cloud-itonami-isic-8810),
[`8691`](https://github.com/cloud-itonami/cloud-itonami-isic-8691),
[`8569`](https://github.com/cloud-itonami/cloud-itonami-isic-8569),
[`6419`](https://github.com/cloud-itonami/cloud-itonami-isic-6419),
[`7310`](https://github.com/cloud-itonami/cloud-itonami-isic-7310),
[`7320`](https://github.com/cloud-itonami/cloud-itonami-isic-7320),
[`7210`](https://github.com/cloud-itonami/cloud-itonami-isic-7210),
[`7410`](https://github.com/cloud-itonami/cloud-itonami-isic-7410),
[`8710`](https://github.com/cloud-itonami/cloud-itonami-isic-8710),
[`8541`](https://github.com/cloud-itonami/cloud-itonami-isic-8541),
[`8690`](https://github.com/cloud-itonami/cloud-itonami-isic-8690),
[`9601`](https://github.com/cloud-itonami/cloud-itonami-isic-9601),
[`6420`](https://github.com/cloud-itonami/cloud-itonami-isic-6420),
[`7420`](https://github.com/cloud-itonami/cloud-itonami-isic-7420),
[`9609`](https://github.com/cloud-itonami/cloud-itonami-isic-9609)) --
here it is **EdSupportOps-LLM ⊣ Support Services Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a client-
> intake summary, normalizing records, and checking whether a
> jurisdiction's own required educational-testing/counseling evidence
> checklist has been satisfied -- but it has **no notion of which
> jurisdiction's student-records law is official, no license to
> finalize a real placement or referral, and no way to know on its
> own whether a test-administration irregularity or a background
> check has actually stayed resolved/cleared**. Letting it finalize a
> placement directly invites fabricated regulatory citations, a
> placement being finalized on top of an unresolved test-security
> concern, and an uncleared background check being quietly overlooked
> -- and liability, and student-safeguarding risk, for whoever runs
> it. This project seals the EdSupportOps-LLM into a single node and
> wraps it with an independent **Support Services Governor**, a human
> **approval workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers client intake through educational-testing/
counseling regulatory assessment, assessment-integrity screening,
background-check screening and placement/referral finalization. It
does **not**, by itself, hold any license required to operate as an
educational-support provider in a given jurisdiction, and it does not
claim to. It also does not perform the actual testing/counseling work
itself, or judge its quality -- `edsupport.governor`'s checks read
the client's own recorded boolean fields directly, not a service-
quality review. Whoever deploys and operates a live instance (a
licensed educational-support provider) supplies any jurisdiction-
specific license, the real testing/counseling delivery and the real
student-information-system integrations, and bears that
jurisdiction's liability -- the software supplies the governed,
spec-cited, audited execution scaffold so that provider does not have
to build the compliance layer from scratch.

### Actuation

**Finalizing a real placement or referral is never autonomous, at any
phase, by construction.** Two independent layers enforce this
(`edsupport.governor`'s `:actuation/finalize-placement` high-stakes
gate and `edsupport.phase`'s phase table, which never puts
`:actuation/finalize-placement` in any phase's `:auto` set) -- see
`edsupport.phase`'s docstring and `test/edsupport/phase_test.kotoba`'s
`finalize-placement-never-auto-at-any-phase`. The actor may draft,
check and recommend; a human provider staff member is always the one
who actually finalizes a placement. Matching `leasing`'s/
`underwriting`'s/`testlab`'s/`clinic`'s/`veterinary`'s/`funeral`'s/
`parksafety`'s/`salon`'s/`entertainment`'s/`facility`'s/`consulting`'s/
`advertising`'s/`polling`'s/`research`'s/`design`'s/`sports`'s/
`alliedhealth`'s/`photo`'s/`personalservice`'s single-actuation shape,
grounded directly in this blueprint's own README text ("No automated
proposal, by itself, can complete the following without governor
approval and audit evidence: finalizing a placement or referral") --
a POSITIVE actuation (finalizing a real record), matching this
fleet's majority actuation shape (`3600`/`6190` are the fleet's two
NEGATIVE-actuation exceptions).

## The core contract

```
client intake + jurisdiction facts (edsupport.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ EdSupportOps-LLM      │ ─────────────▶ │ Support Services              │  (independent system)
   │ (sealed)              │  + citations    │ Governor:                    │
   └───────────────────────┘                 │ spec-basis · evidence-       │
          │                 commit ◀┼ incomplete · assessment-          │
          │                         │ administration-irregularity-       │
    record + ledger        escalate ┼ unresolved (unconditional, NEW)     │
          │              (ALWAYS for│ · background-check-not-cleared       │
          │               :actuation│ (unconditional, honest reuse) ·       │
          │               /finalize-│ already-finalized                      │
          ▼               placement)└───────────────────────┘
      human approval
```

**The EdSupportOps-LLM never finalizes a placement the Support
Services Governor would reject, and never does so without a human
sign-off.** Hard violations (fabricated regulatory requirements;
unsupported evidence; an unresolved assessment-administration
irregularity; an uncleared background check; a double finalization)
force **hold** and *cannot* be approved past; a clean finalization
proposal still always routes to a human.

## Run

```bash
kbb -M:dev:run     # walk one clean single-actuation lifecycle + four HARD-hold cases through the actor
kbb -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
kbb -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a document-courier robot
handles physical record handoff where used, under the actor, gated by
the independent **Support Services Governor**. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions require
human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Support Services Governor, placement-finalization draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`8550`). This vertical's client records are practice-specific rather
than a shared cross-operator data contract, so `edsupport.*` runs on
the generic robotics/identity/forms/dmn/bpmn/audit-ledger stack only
-- no bespoke domain capability lib to reference at all.

## Layout

| File | Role |
|---|---|
| `src/edsupport/store.kotoba` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + placement-finalization history. No dynamically-filed sub-record -- the actuation op acts directly on a pre-seeded client, and the double-actuation guard checks a dedicated `:placement-finalized?` boolean rather than a `:status` value |
| `src/edsupport/registry.kotoba` | Placement-finalization draft records. Intentionally 'plain': this build's two distinctive checks are both boolean flags evaluated directly by the governor, not registry-level numeric/temporal predicates |
| `src/edsupport/facts.kotoba` | Per-jurisdiction educational-testing/counseling catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/edsupport/edsupportadvisor.kotoba` | **EdSupportOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/assessment-verification/integrity-screening/background-check-screening/placement-finalization proposals |
| `src/edsupport/governor.kotoba` | **Support Services Governor** -- 5 HARD checks (spec-basis · evidence-incomplete · assessment-administration-irregularity-unresolved, unconditional evaluation, GENUINELY NEW, the 49th grounding of this discipline · background-check-not-cleared, unconditional evaluation, the FOURTH literal instance of `school`'s/`sports`'s/`personalservice`'s concept, the 50th grounding overall, not claimed as new · already-finalized guard) + 1 soft (confidence/actuation gate) |
| `src/edsupport/phase.kotoba` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (placement finalization always human; client intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/edsupport/operation.kotoba` | **OperationActor** -- langgraph-clj StateGraph |
| `src/edsupport/sim.kotoba` | demo driver |
| `test/edsupport/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers client intake through educational-testing/
counseling regulatory assessment, assessment-integrity screening,
background-check screening and placement/referral finalization -- the
core governed lifecycle this blueprint's own `docs/business-model.md`
names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Client intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:client/intake`/`:assessment/verify`) | Real student-information-system integration, real testing/counseling/exchange-placement work itself (see `edsupport.facts`'s docstring) |
| Assessment-integrity screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:integrity/screen`) | Any service-quality judgment itself -- deliberately outside this actor's competence |
| Background-check screening, evaluated unconditionally (`:background-check/screen`) | |
| Placement/referral finalization, HARD-gated on full evidence, a resolved assessment-integrity status and a cleared background check, plus a double-finalization guard (`:actuation/finalize-placement`) | |
| Immutable audit ledger for every intake/verification/screening/finalization decision | |

Extending coverage is additive: add the next gate (e.g. a records-
retention-period check) as its own governed op with its own HARD
checks and tests, following the SAME "an independent governor
re-verifies against the actor's own records before any real-world
act" pattern this repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`edsupport.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `edsupport.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `edsupport.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `EdSupportOps-LLM` + `Support Services Governor` run
as real, tested code (see `Run` above), promoted from the originally-
published `:blueprint`-tier scaffold, modeled closely on the sixty-
four prior actors' architecture. See `docs/adr/0001-architecture.md`
for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
