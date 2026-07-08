# cloud-itonami-isic-9329

Open Business Blueprint for **ISIC Rev.5 9329**: Other amusement and
recreation activities n.e.c..

This repository publishes a recreation-venue-safety actor -- booking/
admission intake, per-jurisdiction recreation-venue-safety regulatory
assessment, emergency-egress screening and operation-resumption-
after-hold finalization -- as an OSS business that any qualified,
licensed operator can fork, deploy, run, improve and sell, so a
community or independent operator never surrenders patron/collection
data and ledgers to a closed SaaS.

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
[`9609`](https://github.com/cloud-itonami/cloud-itonami-isic-9609),
[`8550`](https://github.com/cloud-itonami/cloud-itonami-isic-8550),
[`7010`](https://github.com/cloud-itonami/cloud-itonami-isic-7010),
[`8790`](https://github.com/cloud-itonami/cloud-itonami-isic-8790),
[`8542`](https://github.com/cloud-itonami/cloud-itonami-isic-8542),
[`6411`](https://github.com/cloud-itonami/cloud-itonami-isic-6411),
[`7490`](https://github.com/cloud-itonami/cloud-itonami-isic-7490),
[`9319`](https://github.com/cloud-itonami/cloud-itonami-isic-9319)) --
here it is **RecOps-LLM ⊣ Recreation Safety Governor**.

> **Why an actor layer at all?** An LLM is great at drafting a venue-
> intake summary, normalizing records, and checking whether a venue's
> own recorded occupancy actually stays within its own recorded
> maximum capacity -- but it has **no notion of which jurisdiction's
> fire/life-safety and recreation-licensing law is official, no
> license to resume real operation at a venue, and no way to know on
> its own whether a venue's emergency-egress path has actually stayed
> clear**. Letting it resume operation directly invites fabricated
> regulatory citations, patrons being let back into an over-capacity
> venue, and an obstructed emergency exit being quietly overlooked --
> and liability, and public-safety risk, for whoever runs it. This
> project seals the RecOps-LLM into a single node and wraps it with an
> independent **Recreation Safety Governor**, a human **approval
> workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers booking/admission intake through recreation-venue-
safety regulatory assessment, emergency-egress screening and
operation-resumption finalization. It does **not**, by itself, hold
any license required to operate an arcade, escape room or recreational
fishing/hunting service in a given jurisdiction, and it does not claim
to. It also does not perform the actual venue-safety engineering
review itself -- `recreation.registry/occupancy-exceeds-capacity?` is
a pure ground-truth recompute against the venue's own recorded
fields, not a full fire-code/life-safety engineering review (the same
honest scope limit `facility.registry/occupancy-exceeds-capacity?`,
the check this reuses, already documents). Whoever deploys and
operates a live instance (a licensed venue operator) supplies any
jurisdiction-specific license, the real safety-inspection delivery
and the real venue-management-system integrations, and bears that
jurisdiction's liability -- the software supplies the governed, spec-
cited, audited execution scaffold so that operator does not have to
build the compliance layer from scratch.

### Actuation

**Resuming operation at a venue after a safety-flagged condition is
never autonomous, at any phase, by construction.** Two independent
layers enforce this (`recreation.governor`'s `:actuation/resume-
operation` high-stakes gate and `recreation.phase`'s phase table,
which never puts `:actuation/resume-operation` in any phase's `:auto`
set) -- see `recreation.phase`'s docstring and
`test/recreation/phase_test.clj`'s
`resume-operation-never-auto-at-any-phase`. The actor may draft, check
and recommend; a human licensed venue operator is always the one who
actually resumes operation. Matching `leasing`'s/`underwriting`'s/
`testlab`'s/`clinic`'s/`veterinary`'s/`funeral`'s/`parksafety`'s/
`salon`'s/`entertainment`'s/`facility`'s/`consulting`'s/
`advertising`'s/`polling`'s/`research`'s/`design`'s/`sports`'s/
`alliedhealth`'s/`photo`'s/`personalservice`'s/`edsupport`'s/
`cultural`'s/`proserv`'s/`sportsevent`'s single-actuation shape,
grounded directly in this blueprint's own README text ("No automated
proposal, by itself, can complete the following without governor
approval and audit evidence: resuming operation after a safety-
flagged condition") -- a POSITIVE actuation (committing a real
resumption record), matching this fleet's majority actuation shape
(`3600`/`6190` are the fleet's two NEGATIVE-actuation exceptions), and
structurally the closest sibling to `parksafety`/9321's own
`:actuation/reopen-ride` (same "resume after a safety hold" shape,
genuinely different venue domain -- arcades/escape-rooms/recreational-
fishing-hunting rather than fixed amusement rides).

## The core contract

```
venue intake + jurisdiction facts (recreation.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌───────────────────────┐
   │ RecOps-LLM            │ ─────────────▶ │ Recreation Safety              │  (independent system)
   │ (sealed)              │  + citations    │ Governor:                    │
   └───────────────────────┘                 │ spec-basis · evidence-       │
          │                 commit ◀┼ incomplete · emergency-           │
          │                         │ egress-obstructed                     │
    record + ledger        escalate ┼ (unconditional, NEW) · occupancy-      │
          │              (ALWAYS for│ exceeds-capacity (MAXIMUM-              │
          │               :actuation│ ceiling, honest reuse) ·                 │
          │               /resume-  │ already-resumed                            │
          ▼               operation)└───────────────────────┘
      human approval
```

**The RecOps-LLM never resumes operation the Recreation Safety
Governor would reject, and never does so without a human sign-off.**
Hard violations (fabricated regulatory requirements; unsupported
evidence; an obstructed emergency-egress path; an over-capacity venue;
a double resumption) force **hold** and *cannot* be approved past; a
clean resumption proposal still always routes to a human.

## Run

```bash
clojure -M:dev:run     # walk one clean single-actuation lifecycle + four HARD-hold cases through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot
performs the physical domain work**. Here a facility-safety monitoring
robot supports physical hazard detection, under the actor, gated by
the independent **Recreation Safety Governor**. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions require
human sign-off.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Recreation Safety Governor, operation-resumption draft records, audit ledger |
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
`9329`). This vertical's venue/operational records are practice-
specific rather than a shared cross-operator data contract, so
`recreation.*` runs on the generic robotics/identity/forms/dmn/bpmn/
audit-ledger stack only -- no bespoke domain capability lib to
reference at all.

## Layout

| File | Role |
|---|---|
| `src/recreation/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + operation-resumption history. No dynamically-filed sub-record -- the actuation op acts directly on a pre-seeded venue, and the double-actuation guard checks a dedicated `:resumed?` boolean rather than a `:status` value |
| `src/recreation/registry.cljc` | Operation-resumption draft records, plus `occupancy-exceeds-capacity?` -- an HONEST, literal reuse of `facility.registry`'s own specific MAXIMUM-ceiling check (the 13th instance of that family overall), applied to a second venue type, not claimed as new |
| `src/recreation/facts.cljc` | Per-jurisdiction recreation-venue fire/life-safety and licensing catalog with an official spec-basis citation per entry, honest coverage reporting |
| `src/recreation/recreationopsllm.cljc` | **RecOps-LLM** -- `mock-advisor` ‖ `llm-advisor`; intake/venue-verification/egress-screening/resumption proposals |
| `src/recreation/governor.cljc` | **Recreation Safety Governor** -- 5 HARD checks (spec-basis · evidence-incomplete · emergency-egress-obstructed, unconditional evaluation, GENUINELY NEW, the 57th grounding of this discipline · occupancy-exceeds-capacity, MAXIMUM-ceiling reuse, the 13th instance, not claimed as new · already-resumed guard) + 1 soft (confidence/actuation gate) |
| `src/recreation/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (operation resumption always human; venue intake is the ONLY auto-eligible op, no direct capital risk) |
| `src/recreation/operation.cljc` | **OperationActor** -- langgraph-clj StateGraph |
| `src/recreation/sim.cljc` | demo driver |
| `test/recreation/*_test.clj` | governor contract · phase invariants · store parity · registry conformance · facts coverage |

## Business-process coverage (honest)

This actor covers booking/admission intake through recreation-venue-
safety regulatory assessment, emergency-egress screening and
operation-resumption finalization -- the core governed lifecycle this
blueprint's own `docs/business-model.md` names as its Offer:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Venue intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:venue/intake`/`:venue/verify`) | Real venue-management-system integration, real fire/life-safety engineering review itself (see `recreation.facts`'s docstring) |
| Emergency-egress screening, evaluated unconditionally so the screening op itself can HARD-hold on its own finding (`:egress/screen`) | Recreational fishing/hunting license verification against a live game-and-wildlife registry -- deliberately outside this actor's R0 scope |
| Operation-resumption finalization, HARD-gated on full evidence, a clear emergency-egress path and occupancy within capacity, plus a double-resumption guard (`:actuation/resume-operation`) | |
| Immutable audit ledger for every intake/verification/screening/resumption decision | |

Extending coverage is additive: add the next gate (e.g. a recreational-
fishing/hunting license-verification check) as its own governed op
with its own HARD checks and tests, following the SAME "an independent
governor re-verifies against the actor's own records before any
real-world act" pattern this repo's flagship op already establishes.

## Jurisdiction coverage (honest)

`recreation.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `recreation.facts/catalog` --
currently 4 seeded (JPN, USA, GBR, DEU) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `recreation.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

## Maturity

`:implemented` -- `RecOps-LLM` + `Recreation Safety Governor` run as
real, tested code (see `Run` above), promoted from the originally-
published `:blueprint`-tier scaffold, modeled closely on the seventy-
one prior actors' architecture. See `docs/adr/0001-architecture.md`
for the history and design.

## License

Code and implementation templates are AGPL-3.0-or-later.
