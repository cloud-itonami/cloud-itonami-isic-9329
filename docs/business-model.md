# Business Model: Other amusement and recreation activities n.e.c.

## Classification

- Repository: `cloud-itonami-isic-9329`
- ISIC Rev.5: `9329`
- Activity: other amusement and recreation activities not elsewhere classified (e.g. arcades, escape rooms, recreational fishing/hunting operations)
- Social impact: cultural/recreational access, data sovereignty, transparent audit

## Customer

- independent recreation-venue operators
- cooperative outdoor-recreation collectives
- community leisure-facility programs

## Offer

- booking/admission intake
- activity-schedule/maintenance proposal
- operation-resumption-after-hold proposal
- immutable audit ledger

## Revenue

- self-host setup: one-time implementation fee
- managed hosting: monthly subscription per venue
- support: monthly retainer with SLA
- migration: import from an incumbent venue-booking system
- per-booking fee

## Trust Controls

- no operation resumes after a safety-flagged condition without human sign-off
- a fabricated safety-inspection record forces a hold, not an override
- every resumption path is auditable
- emergency manual override paths remain outside LLM control
- an obstructed emergency-egress path, or a venue whose own recorded
  occupancy exceeds its own recorded maximum capacity, forces a hold,
  not an override
- operation resumption is logged and escalated, and cannot be resumed
  twice for the same venue: a double-resumption attempt is held off
  this actor's own venue facts alone, with no upstream comparison
  needed

## Recreation Safety Governor: decision rule

`blueprint.edn` fixes `:itonami.blueprint/governor` to `:recreation-
safety-governor` -- this is not a generic "review step," it is the
one gate the ONE real-world act this business performs (resuming
operation at a venue after a safety-flagged condition) must pass. The
governor sits between the RecOps-LLM and execution, per the README's
Core Contract:

```text
RecOps-LLM -> Recreation Safety Governor -> hold, proceed, or human approval
```

**Approves**: routine recreation-venue actions proposed against a
venue that already has a consented jurisdiction evidence checklist on
file, satisfied required evidence, a clear emergency-egress path, and
occupancy within its own recorded capacity. These proceed straight to
the venue ledger.

**Rejects or escalates**: the governor refuses to let the advisor
resume operation on its own authority when any of the following hold
-- a fabricated jurisdiction spec-basis; incomplete evidence; an
obstructed emergency-egress path; an over-capacity venue; a double-
resumption attempt. A clean resumption proposal still always routes
to a human -- `:actuation/resume-operation` is never auto-committed,
at any rollout phase.
