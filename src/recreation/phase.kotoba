(ns recreation.phase
  "Phase 0->3 staged rollout -- the other-amusement/recreation-venue
  analog of `cloud-itonami-isic-9321`'s `parksafety.phase`.

    Phase 0  read-only        -- no writes, still governor-gated.
    Phase 1  assisted-intake  -- booking/admission intake allowed,
                                 every write needs human approval.
    Phase 2  assisted-verify  -- adds jurisdiction verification +
                                 egress screening writes, still
                                 approval.
    Phase 3  supervised auto  -- governor-clean, high-confidence
                                 `:venue/intake` (no capital risk yet)
                                 may auto-commit. `:actuation/resume-
                                 operation` NEVER auto-commits, at any
                                 phase.

  `:actuation/resume-operation` is deliberately ABSENT from every
  phase's `:auto` set, including phase 3 -- a permanent structural
  fact, not a rollout milestone still to come. Resuming operation at a
  real venue after a safety hold is the ONE real-world, safety-critical
  legal act this actor performs; it is always a human licensed venue
  operator's call. `recreation.governor`'s `:actuation/resume-
  operation` high-stakes gate enforces the same invariant
  independently -- two layers, not one, agree on this. `:egress/
  screen` is likewise never auto-eligible, at any phase -- the same
  posture every sibling's KYC/conflict/independence/surveillance/
  calibration/credential/integrity/patron/authorization/safety/anti-
  doping screening op has. Like `parksafety.phase`'s (and every prior
  sibling's) phase 3 `:auto` set, this domain has only ONE member
  (`:venue/intake`) -- no separate no-capital-risk 'file' lifecycle
  distinct from the venue itself.")

(def read-ops  #{})
(def write-ops #{:venue/intake :venue/verify :egress/screen
                 :actuation/resume-operation})

;; NOTE the invariant: `:actuation/resume-operation` is a member of
;; `write-ops` (governor-gated like any write) but is NEVER a member
;; of any phase's `:auto` set below. Do not add it there.
(def phases
  "phase -> {:label .. :writes <ops allowed to write> :auto <ops allowed to
  auto-commit when governor-clean>}."
  {0 {:label "read-only"        :writes #{}                                                              :auto #{}}
   1 {:label "assisted-intake"  :writes #{:venue/intake}                                                 :auto #{}}
   2 {:label "assisted-verify"  :writes #{:venue/intake :venue/verify :egress/screen}                    :auto #{}}
   3 {:label "supervised-auto"  :writes write-ops
      :auto #{:venue/intake}}})

(def default-phase 3)

(defn gate
  "Adjust a governor disposition for the rollout phase. Returns
  {:disposition kw :reason kw|nil}.

  - a governor HOLD always stays HOLD (compliance wins).
  - a write op not yet enabled in this phase -> HOLD (:phase-disabled).
  - a write op enabled but not auto-eligible -> ESCALATE (:phase-approval),
    even if the governor was clean.
  - `:actuation/resume-operation` is never auto-eligible at any phase,
    so it always escalates once the governor clears it (or holds if
    the governor doesn't)."
  [phase {:keys [op]} governor-disposition]
  (let [{:keys [writes auto]} (get phases phase (get phases default-phase))]
    (cond
      (= :hold governor-disposition)       {:disposition :hold :reason nil}
      (contains? read-ops op)              {:disposition governor-disposition :reason nil}
      (not (contains? writes op))          {:disposition :hold :reason :phase-disabled}
      (and (= :commit governor-disposition)
           (not (contains? auto op)))      {:disposition :escalate :reason :phase-approval}
      :else                                {:disposition governor-disposition :reason nil})))

(defn verdict->disposition
  "Map a Recreation Safety Governor verdict to a base disposition
  before the phase gate."
  [verdict]
  (cond (:hard? verdict) :hold
        (:escalate? verdict) :escalate
        :else :commit))
