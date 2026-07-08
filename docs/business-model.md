# Business Model: Educational support activities

## Classification

- Repository: `cloud-itonami-isic-8550`
- ISIC Rev.5: `8550`
- Activity: educational support activities -- non-instructional support for education such as educational testing, guidance counseling, and student-exchange placement services
- Social impact: education access, data sovereignty, transparent audit

## Customer

- independent testing/counseling services
- cooperative educational-support collectives
- community student-services programs

## Offer

- client (student/institution) intake
- assessment/placement proposal
- report/referral proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per provider
- support: monthly retainer with SLA
- migration: import from an incumbent student-services system
- per-assessment fee

## Trust Controls

- no placement or referral is finalized without human sign-off
- a fabricated assessment forces a hold, not an override
- every record path is auditable
- student data stays outside Git
- emergency manual override paths remain outside LLM control
- an unresolved assessment-administration irregularity, or an
  uncleared background check, forces a hold, not an override
- placement finalization is logged and escalated, and cannot be
  finalized twice for the same client: a double-finalization attempt
  is held off this actor's own client facts alone, with no upstream
  comparison needed

## Support Services Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:support-
services-governor` -- this is not a generic "review step," it is the
one gate the ONE real-world act this business performs (finalizing a
real placement or referral) must pass. The governor sits between the
EdSupportOps-LLM and execution, per the README's Core Contract:

```text
EdSupportOps-LLM -> Support Services Governor -> hold, proceed, or human approval
```

**Approves**: routine educational-support actions proposed against a
client that already has a consented assessment plan on file,
satisfied required evidence, a resolved assessment-administration
integrity status, and a cleared background check. These proceed
straight to the client ledger.

**Rejects or escalates**: the governor refuses to let the advisor
finalize a placement on its own authority when any of the following
hold -- a fabricated jurisdiction spec-basis; incomplete evidence; an
unresolved assessment-administration irregularity; an uncleared
background check; a double-finalization attempt. A clean finalization
proposal still always routes to a human -- `:actuation/finalize-
placement` is never auto-committed, at any rollout phase.
