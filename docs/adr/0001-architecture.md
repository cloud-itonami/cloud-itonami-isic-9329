# ADR-0001: RecOps-LLM ⊣ Recreation Safety Governor architecture

## Status

Accepted. `cloud-itonami-isic-9329` promoted from `:blueprint` to
`:implemented` in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-9329` publishes an OSS business blueprint for
other amusement and recreation activities not elsewhere classified:
arcades, escape rooms, recreational fishing/hunting operations. Like
every prior actor in this fleet, the blueprint alone is not an
implementation: this ADR records the governed-actor architecture that
promotes it to real, tested code, following the same langgraph
StateGraph + independent Governor + Phase 0→3 rollout pattern
established by `cloud-itonami-isic-6511` (life insurance) and applied
across seventy-one prior siblings, most recently `cloud-itonami-isic-
9319` (sports-event officiating/timing).

## Decision

### Decision 1: single-actuation shape, and its structural relationship to `parksafety`/9321

This blueprint's own README/business-model.md/operator-guide.md
consistently name only ONE real-world act: "resuming operation after
a safety-flagged condition." Matching `leasing`/`underwriting`/
`testlab`/`clinic`/`veterinary`/`funeral`/`parksafety`/`salon`/
`entertainment`/`facility`/`consulting`/`advertising`/`polling`/
`research`/`design`/`sports`/`alliedhealth`/`photo`/
`personalservice`/`edsupport`/`cultural`/`proserv`/`sportsevent`'s
single-actuation shape, `high-stakes` here is a one-member set,
`#{:actuation/resume-operation}`. This build is structurally the
closest sibling to `cloud-itonami-isic-9321`'s (`parksafety`) own
`:actuation/reopen-ride` -- both actors resume/reopen something after
a safety hold -- but ISIC 9321 covers fixed amusement RIDES (a
mechanical ride-inspection regime) while ISIC 9329 covers genuinely
different venue types (arcades, escape rooms, recreational fishing/
hunting) where the real, load-bearing concerns are assembly-venue
fire/life-safety (occupancy, emergency egress), not ride mechanics.
This build deliberately does NOT clone `parksafety`'s checks
verbatim -- it re-derives its own check family from ISIC 9329's own
named example activities (see Decisions 3-4).

### Decision 2: entity and op shape

The primary entity is a `venue`, matching the business-model.md's own
Offer language ("booking/admission intake"). Four ops:
`:venue/intake` (directory/booking upsert, no capital risk),
`:venue/verify` (per-jurisdiction recreation-venue-safety evidence
checklist, never auto), `:egress/screen` (emergency-egress screening,
unconditional-evaluation discipline, never auto), and `:actuation/
resume-operation` (POSITIVE, high-stakes -- resuming a real venue's
operation after a safety hold).

### Decision 3: `emergency-egress-obstructed-violations` -- the 57th unconditional-evaluation screening grounding, a genuinely new concept

Before writing this check, every prior sibling's governor/registry/
facts namespaces were grepped for "egress" -- the only hits were
`facility.facts`'s own required-EVIDENCE checklist items ("emergency-
egress plan" as a document-on-file requirement, not a live
obstruction-status check) and unrelated substring matches on
"regresses". This confirms `emergency-egress-obstructed-violations`
is a genuinely new unconditional-evaluation concept -- a live,
binary safety-hold status check, distinct from any prior sibling's
evidence-checklist item. It reuses the unconditional-evaluation
DISCIPLINE (`casualty.governor/sanctions-violations`'s original fix)
for the 57th distinct application overall, continuing the count
established across this fleet's builds (most recently `sportsevent.
governor/anti-doping-control-unresolved-violations` at 56th).
Grounded in real assembly-venue fire/life-safety law: NFPA 101 Life
Safety Code (Means of Egress, Chapters 7/12/13) and Germany's
Versammlungsstättenverordnung (Rettungswege requirements) -- acutely
relevant to escape rooms specifically, after real-world escape-room
fire incidents prompted dedicated fire-code attention to locked or
obscured emergency exits in several jurisdictions. Gates `:egress/
screen` and the actuation.

### Decision 4: `occupancy-exceeds-capacity?` -- an honest, literal reuse of `facility.registry`'s own specific check, the 13th MAXIMUM-ceiling instance

`facility.registry/occupancy-exceeds-capacity?` (ISIC 9311, sports-
facility venues) established the FIRST non-temporal instance of this
fleet's MAXIMUM-ceiling check family -- comparing a venue's own
`:current-occupancy` against its own `:maximum-capacity`, two
permanent fields on the SAME entity. `recreation.registry/occupancy-
exceeds-capacity?` is a LITERAL re-application of that exact check
(same field names, same comparison, same real fire-code/life-safety
occupancy-limit concept) to a genuinely different venue type
(arcades/escape-rooms/recreational-facility venues). Grep-verified:
no other sibling has reused this specific function by name since
`facility` established it (many cite it in family docstrings as
precedent for their own differently-named MAXIMUM-ceiling checks, but
none had reapplied the identical concept to a second venue type until
this build). This is the 13th MAXIMUM-ceiling instance overall,
honestly characterized as a reuse, not a new concept. Gates only the
actuation (a pure ground-truth recompute, no dedicated screening op
needed beyond `:venue/verify`'s evidence-checklist gate).

### Decision 5: dedicated double-actuation-guard boolean

`:resumed?` is a dedicated boolean on the `venue` record, never a
single `:status` value -- the same discipline every prior sibling
governor's guards establish (most directly, `parksafety.governor`'s
own `:reopened?` guard for the structurally closest sibling),
informed by `cloud-itonami-isic-6492`'s status-lifecycle bug
(ADR-2607071320).

### Decision 6: Store protocol, MemStore + DatomicStore parity

`recreation.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.
db`-backed), proven to satisfy the same contract in `test/
recreation/store_contract_test.clj` -- the same seam every sibling
actor uses so swapping the SSoT backend is a configuration change,
not a rewrite. The protocol's per-entity accessor is named `venue`
directly -- not a Clojure special form, so no `-of` suffix workaround
was needed.

### Decision 7: Phase 0→3 rollout

Phase 3's `:auto` set has exactly one member, `:venue/intake` (no
capital risk). `:venue/verify` and `:egress/screen` are never auto-
eligible at any phase (matching every sibling's screening/
verification-op posture), and `:actuation/resume-operation` is
permanently excluded from every phase's `:auto` set -- a structural
fact, not a rollout milestone, enforced by BOTH `recreation.phase`
and `recreation.governor`'s `high-stakes` set independently.

### Decision 8: no bespoke domain capability lib

This blueprint's own `:itonami.blueprint/required-technologies`
names no domain-specific capability beyond the generic robotics/
identity/forms/dmn/bpmn/audit-ledger stack -- there was no
capability-lib decision to make at all.

### Decision 9: mock + LLM advisor pair

`recreation.recreationopsllm` provides `mock-advisor` (deterministic,
default everywhere -- the actor graph and governor contract run
offline) and `llm-advisor` (backed by `langchain.model/ChatModel`,
with a defensive EDN-proposal parser so a malformed LLM response
degrades to a safe low-confidence noop rather than ever auto-resuming
operation).

### Decision 10: no `blueprint.edn` field-sync fixes needed

Matching `photo`/7420's, `personalservice`/9609's, `edsupport`/8550's,
`headoffice`/7010's, `residential`/8790's, `cultural`/8542's,
`reserve`/6411's, `proserv`/7490's and `sportsevent`/9319's own
experience, this repo's `blueprint.edn` already had the correct
`isic-` prefixed `:id` and correctly populated `:required-
technologies`/`:optional-technologies` matching the `kotoba-lang/
industry` registry's own entry for `"9329"` exactly -- only the
`:maturity` field itself needed adding.

## Alternatives considered

- **Cloning `parksafety.governor`'s checks verbatim** (inspection-not-
  passed, operators-insufficient). Rejected: ISIC 9329's named example
  activities (arcades, escape rooms, recreational fishing/hunting)
  have no mechanical ride-inspection regime and no per-ride staffing
  minimum -- the real concerns are assembly-venue fire/life-safety,
  warranting a re-derived, domain-appropriate check family instead of
  a mechanical port.
- **Framing `occupancy-exceeds-capacity?` as a new concept for this
  venue type.** Rejected: it is the exact same real-world concept and
  field comparison `facility.registry` already established; honest
  reuse characterization matches this fleet's precedent-verification
  discipline.
- **A dual-actuation shape** (splitting "resuming operation" into a
  separate per-hazard-type actuation). Rejected: the blueprint's own
  text consistently names only ONE real-world act.

## Consequences

- Seventy-second actor in this fleet (71 implemented before this
  build, per `kotoba-lang/industry`'s registry at build time).
- Establishes a genuinely NEW unconditional-evaluation-screening
  concept (emergency-egress-obstructed), grep-verified absent from
  every prior sibling before the claim was finalized.
- Documents an honest, literal reuse of `facility.registry`'s own
  specific MAXIMUM-ceiling check (occupancy-exceeds-capacity), the
  13th instance of that family, applied to a second venue type for
  the first time.
- `MemStore` ‖ `DatomicStore` parity is proven by `test/recreation/
  store_contract_test.clj`, the same `:db-api`-driven swap pattern
  every sibling actor uses.
- `blueprint.edn` required no field-sync fixes this time (already
  correct) -- only the `:maturity` flip itself.

## References

- `orgs/cloud-itonami/cloud-itonami-isic-9329/README.md`
- `orgs/cloud-itonami/cloud-itonami-isic-9329/docs/business-model.md`
- `orgs/cloud-itonami/cloud-itonami-isic-9321/src/parksafety/governor.cljc` (structurally closest sibling)
- `orgs/cloud-itonami/cloud-itonami-isic-9311/src/facility/registry.cljc` (`occupancy-exceeds-capacity?` origin)
- `orgs/kotoba-lang/industry/resources/kotoba/industry/registry.edn` (entry `"9329"`)
