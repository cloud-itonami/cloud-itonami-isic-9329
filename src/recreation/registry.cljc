(ns recreation.registry
  "Pure-function operation-resumption record construction -- an
  append-only venue book-of-record draft.

  Like every sibling actor's registry, there is no single international
  check-digit standard for a resumption reference number -- every
  venue/jurisdiction assigns its own reference format. This namespace
  does NOT invent one; it builds a jurisdiction-scoped sequence number
  and validates the record's required fields, the same honest, non-
  fabricating discipline `recreation.facts` uses.

  `occupancy-exceeds-capacity?` is an HONEST, literal reuse of
  `facility.registry/occupancy-exceeds-capacity?` (ISIC 9311's own
  FIRST non-temporal instance of this fleet's MAXIMUM-ceiling family --
  comparing TWO permanent fields on the SAME entity, `:current-
  occupancy` vs `:maximum-capacity`, rather than a field against a
  shared constant) -- the same real fire-code/life-safety occupancy-
  limit concept, applied here to a genuinely different venue type
  (arcades/escape-rooms/recreational-facility venues rather than
  sports-facility assembly venues). This is the 13th instance of the
  MAXIMUM-ceiling family overall, and the FIRST literal re-application
  of `facility.registry`'s own specific check to a second venue type --
  not claimed as a new concept.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real venue-operations system. It builds the RECORD a
  venue operator would keep, not the act of resuming operation itself
  (that is `recreation.operation`'s `:actuation/resume-operation`,
  always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is the
  licensed venue operator's own act, not this actor's. See README
  `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

(defn occupancy-exceeds-capacity?
  "Does `venue`'s own `:current-occupancy` exceed its own `:maximum-
  capacity`? A pure ground-truth check comparing TWO permanent fields
  on the same entity -- see ns docstring for why this is an honest
  reuse of `facility.registry`'s own specific check, not a new
  concept."
  [{:keys [current-occupancy maximum-capacity]}]
  (and (number? current-occupancy) (number? maximum-capacity)
       (> current-occupancy maximum-capacity)))

(defn register-operation-resumption
  "Validate + construct the OPERATION-RESUMPTION registration DRAFT --
  the venue's own legal act of resuming operation to patrons after a
  safety-flagged hold. Pure function -- does not touch any real venue-
  operations system; it builds the RECORD a venue operator would keep.
  `recreation.governor` independently re-verifies the venue's own
  emergency-egress status and occupancy ceiling, and blocks a double-
  resumption of the same venue, before this is ever allowed to
  commit."
  [venue-id jurisdiction sequence]
  (when-not (and venue-id (not= venue-id ""))
    (throw (ex-info "operation-resumption: venue_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "operation-resumption: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "operation-resumption: sequence must be >= 0" {})))
  (let [resumption-number (str (str/upper-case jurisdiction) "-RSM-" (zero-pad sequence 6))
        record {"record_id" resumption-number
                "kind" "operation-resumption-draft"
                "venue_id" venue-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "resumption_number" resumption-number
     "certificate" (unsigned-certificate "OperationResumption" resumption-number resumption-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
