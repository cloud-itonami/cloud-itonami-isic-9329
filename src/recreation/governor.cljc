(ns recreation.governor
  "Recreation Safety Governor -- the independent compliance layer that
  earns the RecOps-LLM the right to commit. The LLM has no notion of
  jurisdictional recreation-venue-safety law, whether a venue's own
  emergency-egress path is actually clear, whether a venue's own
  measured occupancy actually stays within its own recorded capacity
  limit, or when an act stops being a draft and becomes a real-world
  operation resumption, so this MUST be a separate system able to
  *reject* a proposal and fall back to HOLD -- the recreation-venue
  analog of `cloud-itonami-isic-9321`'s `parksafety.governor` (Ride
  Safety Governor).

  Five checks, in priority order, ALL HARD violations: a human approver
  CANNOT override them (you don't get to approve your way past a
  fabricated jurisdiction spec-basis, incomplete resumption evidence,
  an obstructed emergency-egress path, an over-capacity venue, or a
  double resumption of the same venue). The confidence/actuation gate
  is SOFT: it asks a human to look (low confidence / actuation), and
  the human may approve -- but see `recreation.phase`: for `:stake
  :actuation/resume-operation` (a real public-safety act) NO phase ever
  allows auto-commit either. Two independent layers agree that
  actuation is always a human call.

  This vertical's own named example activities (arcades, escape rooms,
  recreational fishing/hunting operations) are genuinely distinct from
  `parksafety`/9321's fixed amusement-RIDE domain -- there is no
  mechanical ride-inspection regime here. The real, load-bearing
  concerns are assembly-venue fire/life-safety (emergency-egress
  obstruction, occupancy-vs-capacity), not ride mechanics.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source (`recreation.
                                       facts`), or invent one?
    2. Evidence incomplete         -- for `:actuation/resume-operation`,
                                       has the jurisdiction actually
                                       been assessed with a full
                                       resumption evidence checklist
                                       (safety-inspection/occupancy-
                                       certification/egress-plan/
                                       operating-license) on file?
    3. Emergency-egress obstructed -- reported by THIS proposal itself
                                       (an `:egress/screen` that just
                                       found an obstruction), or already
                                       on file for the venue (`:egress/
                                       screen`/`:actuation/resume-
                                       operation`). Evaluated
                                       UNCONDITIONALLY (not scoped to a
                                       specific op), the SAME discipline
                                       `casualty.governor/sanctions-
                                       violations`'s original fix
                                       establishes -- GENUINELY NEW (grep-
                                       verified absent -- zero hits for
                                       'egress' as a standalone check
                                       across every prior sibling; the
                                       fleet's one prior 'egress'
                                       reference, `facility.facts`'s own
                                       required-EVIDENCE checklist item,
                                       is a document-on-file requirement,
                                       not a live obstruction-status
                                       check), the 57th distinct
                                       application of this discipline
                                       overall (most recently
                                       `sportsevent.governor/anti-
                                       doping-control-unresolved-
                                       violations` at 56th). Grounded in
                                       real assembly-venue fire/life-
                                       safety law: NFPA 101 Life Safety
                                       Code (Means of Egress, Chapters
                                       7/12/13) and Germany's
                                       Versammlungsstättenverordnung
                                       (Rettungswege requirements) --
                                       acutely relevant to escape rooms
                                       specifically, after real-world
                                       escape-room fire incidents
                                       prompted dedicated fire-code
                                       attention to locked/obscured
                                       emergency exits in several
                                       jurisdictions.
    4. Occupancy exceeds capacity  -- for `:actuation/resume-operation`,
                                       INDEPENDENTLY recompute whether
                                       the venue's own `:current-
                                       occupancy` exceeds its own
                                       `:maximum-capacity`
                                       (`recreation.registry/occupancy-
                                       exceeds-capacity?`) -- needs no
                                       proposal inspection or stored-
                                       verdict lookup at all. An HONEST,
                                       literal reuse of `facility.
                                       registry/occupancy-exceeds-
                                       capacity?` (ISIC 9311's own FIRST
                                       non-temporal instance of this
                                       fleet's MAXIMUM-ceiling family),
                                       the 13th instance of that family
                                       overall and the FIRST literal
                                       re-application of `facility.
                                       registry`'s own specific check to
                                       a second venue type -- not
                                       claimed as a new concept.
    5. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:actuation/resume-
                                       operation` (a REAL public-safety
                                       act) -> escalate.

  One more guard, double-resumption prevention, is enforced but NOT
  listed as a numbered HARD check above because it needs no upstream
  comparison at all -- `already-resumed-violations` refuses to resume
  operation at the SAME venue twice, off a dedicated `:resumed?` fact
  (never a `:status` value) -- the SAME 'check a dedicated boolean, not
  status' discipline `parksafety.governor`'s own `:reopened?` guard (and
  every governor's guard before it) establishes, informed by
  `cloud-itonami-isic-6492`'s status-lifecycle bug (ADR-2607071320)."
  (:require [recreation.facts :as facts]
            [recreation.registry :as registry]
            [recreation.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean.
  Resuming operation at a real venue after a safety hold is the ONE
  real-world actuation event this actor performs -- a single-member
  set, matching `cloud-itonami-isic-9321`'s (and every other single-
  actuation sibling's) shape."
  #{:actuation/resume-operation})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:venue/verify` (or `:actuation/resume-operation`) proposal with no
  spec-basis citation is a HARD violation -- never invent a
  jurisdiction's recreation-venue-safety requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:venue/verify :actuation/resume-operation} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:actuation/resume-operation`, the jurisdiction's required
  safety-inspection/occupancy-certification/egress-plan/operating-
  license evidence must actually be satisfied -- do not trust the
  advisor's self-reported confidence alone."
  [{:keys [op subject]} st]
  (when (= op :actuation/resume-operation)
    (let [v (store/venue st subject)
          verification (store/verify-of st subject)]
      (when-not (and verification
                     (facts/required-evidence-satisfied?
                      (:jurisdiction v) (:checklist verification)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(防火対象物点検報告書/収容人員証明書/避難計画書/営業許可証等)が充足していない状態での再開提案"}]))))

(defn- emergency-egress-obstructed-violations
  "An obstructed emergency-egress path -- reported by THIS proposal
  (e.g. an `:egress/screen` that itself just found an obstruction), or
  already on file in the store for the venue (`:egress/screen`/
  `:actuation/resume-operation`) -- is a HARD, un-overridable hold.
  Evaluated UNCONDITIONALLY (not scoped to a specific op) so the
  screening op itself can HARD-hold on its own finding."
  [{:keys [op subject]} proposal st]
  (let [hit-in-proposal? (true? (get-in proposal [:value :egress-obstructed?]))
        venue-id (when (contains? #{:egress/screen :actuation/resume-operation} op) subject)
        hit-on-file? (and venue-id (:emergency-egress-obstructed? (store/venue st venue-id)))]
    (when (or hit-in-proposal? hit-on-file?)
      [{:rule :emergency-egress-obstructed
        :detail "避難経路の閉塞が解消されていない施設の営業再開提案は進められない"}])))

(defn- occupancy-exceeds-capacity-violations
  "For `:actuation/resume-operation`, INDEPENDENTLY recompute whether
  the venue's own current-occupancy exceeds its own maximum-capacity
  via `recreation.registry/occupancy-exceeds-capacity?` -- needs no
  proposal inspection or stored-verdict lookup at all, an honest reuse
  of `facility.registry`'s own specific check for a second venue
  type."
  [{:keys [op subject]} st]
  (when (= op :actuation/resume-operation)
    (let [v (store/venue st subject)]
      (cond
        ;; Either figure missing or non-numeric: the limit cannot be
        ;; evaluated, so it is not "within limits". This used to fall
        ;; through as "not over" and proceed.
        ;; Only when the entity EXISTS: a missing entity is a different
        ;; violation that another gate owns, and firing here would mask it.
        (and v (not (registry/occupancy-exceeds-capacity-checkable? v)))
        [{:rule :occupancy-exceeds-capacity
          :detail "上限判定に必要な値が記録されていない -- 限度内と断定できないため進めない"}]

        (registry/occupancy-exceeds-capacity? v)
        [{:rule :occupancy-exceeds-capacity
          :detail (str subject " の現在収容人員(" (:current-occupancy v)
                      ")が最大収容人員(" (:maximum-capacity v) ")を超過している")}]))))

(defn- already-resumed-violations
  "For `:actuation/resume-operation`, refuses to resume operation at
  the SAME venue twice, off a dedicated `:resumed?` fact -- see ns
  docstring for why this sidesteps the status-lifecycle risk `cloud-
  itonami-isic-6492`'s ADR-0001 documents."
  [{:keys [op subject]} st]
  (when (= op :actuation/resume-operation)
    (when (store/venue-already-resumed? st subject)
      [{:rule :already-resumed
        :detail (str subject " は既に営業再開済み")}])))

(defn check
  "Censors a RecOps-LLM proposal against the governor rules. Returns
   {:ok? bool :violations [..] :confidence c :escalate? bool :high-stakes? bool
    :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (emergency-egress-obstructed-violations request proposal st)
                           (occupancy-exceeds-capacity-violations request st)
                           (already-resumed-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
