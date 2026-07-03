# cloud-itonami-8550

Open Business Blueprint for **ISIC Rev.5 8550**: Educational support activities.

This repository designs a forkable OSS business for educational support activities -- non-instructional support for education such as educational testing, guidance counseling, and student-exchange placement services -- run by a qualified, licensed operator so a community or
independent educator never surrenders student data and ledgers to a
closed SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a document-courier robot handles physical record handoff where used,
under an actor that proposes actions and an independent **Support Services Governor**
that gates them. The governor never dispatches hardware itself;
`:high`/`:safety-critical` actions require human sign-off.

## Core Contract

```text
intake + identity + academic records
        |
        v
EdSupportOps-LLM -> Support Services Governor -> hold, proceed, or human approval
        |
        v
academic ledger + evidence record + audit
```

No automated proposal, by itself, can complete the following without governor
approval and audit evidence: finalizing a placement or referral.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`8550`). This vertical's academic/case records are practice-specific
rather than a shared cross-operator data contract, so it runs on the generic
identity/forms/dmn/bpmn/audit-ledger stack -- no bespoke domain capability lib.

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## Maturity

`:blueprint` -- this repository is the published business/operator design.
The governed actor implementation (`EdSupportOps-LLM` + `Support Services Governor` as
running code) is a follow-up, same as any other `:blueprint`-tier
`cloud-itonami-*` entry in `kotoba-lang/industry`'s registry.

## License

Code and implementation templates are AGPL-3.0-or-later.
