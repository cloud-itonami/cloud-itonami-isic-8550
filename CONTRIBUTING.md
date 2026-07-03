# Contributing

`cloud-itonami-8550` accepts contributions to the OSS blueprint, capability
bindings, policy tests, documentation and operator model.

## Development

This repo holds the business blueprint and operator contracts. See
`kotoba-lang/industry` for the technology-stack resolution.

```bash
clojure -M:test
clojure -M:lint
```

Keep changes small and include tests for any capability-layer change.

## Rules

- Do not commit real student/participant records, credentials, or personal data.
- Keep finalizing a placement or referral behind the Support Services Governor.
- Treat this vertical as high-risk: add tests for assessment integrity,
  record-action gating and audit logging.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests

PRs should describe:

- what behavior changed
- which policy invariant is affected
- how it was tested
- whether operator or certification docs need updates
