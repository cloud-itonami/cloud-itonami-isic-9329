(ns recreation.store
  "SSoT for the other-amusement/recreation-safety actor, behind a
  `Store` protocol so the backend is a swap, not a rewrite -- the same
  seam every prior `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/recreation/store_contract_test.clj), which is the whole point:
  the actor, the Recreation Safety Governor and the audit ledger never
  know which SSoT they run on.

  Like `parksafety.store`'s simpler entities, a VENUE is acted on
  directly by the ONE actuation op -- no dynamically-filed sub-record,
  and the double-resumption guard checks a dedicated `:resumed?`
  boolean rather than a `:status` value, the same discipline
  `parksafety.governor`'s own `:reopened?` guard (and every governor's
  guard before it) establishes.

  The ledger stays append-only on every backend: 'which venue was
  screened for emergency-egress obstruction, which venue resumed
  operation, on what jurisdictional basis, approved by whom' is always
  a query over an immutable log -- the audit trail a patron trusting a
  venue needs, and the evidence an operator needs if a resumption is
  later disputed."
  (:require [recreation.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (venue [s id])
  (all-venues [s])
  (egress-screen-of [s venue-id] "committed emergency-egress screening verdict for a venue, or nil")
  (verify-of [s venue-id] "committed jurisdiction verification, or nil")
  (ledger [s])
  (resumption-history [s] "the append-only operation-resumption history (recreation.registry drafts)")
  (next-sequence [s jurisdiction] "next resumption-number sequence for a jurisdiction")
  (venue-already-resumed? [s venue-id] "has this venue's hold already been resumed?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-venues [s venues] "replace/seed the venue directory (map id->venue)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained venue set so the actor + tests run offline."
  []
  {:venues
   {"venue-1" {:id "venue-1" :venue-name "Sakura Arcade" :hold-reason "fire-code follow-up inspection"
               :emergency-egress-obstructed? false :current-occupancy 40 :maximum-capacity 60
               :resumed? false :jurisdiction "JPN" :status :intake}
    "venue-2" {:id "venue-2" :venue-name "Atlantis Escape Room" :hold-reason "scheduled follow-up review"
               :emergency-egress-obstructed? false :current-occupancy 8 :maximum-capacity 10
               :resumed? false :jurisdiction "ATL" :status :intake}
    "venue-3" {:id "venue-3" :venue-name "鈴木釣り堀" :hold-reason "assembly-occupancy follow-up inspection"
               :emergency-egress-obstructed? false :current-occupancy 85 :maximum-capacity 60
               :resumed? false :jurisdiction "JPN" :status :intake}
    "venue-4" {:id "venue-4" :venue-name "田中脱出ゲーム" :hold-reason "emergency-egress follow-up inspection"
               :emergency-egress-obstructed? true :current-occupancy 12 :maximum-capacity 20
               :resumed? false :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- resume-venue!
  "Backend-agnostic `:venue/mark-resumed` -- looks up the venue via the
  protocol and drafts the operation-resumption record, and returns
  {:result .. :venue-patch ..} for the caller to persist."
  [s venue-id]
  (let [v (venue s venue-id)
        seq-n (next-sequence s (:jurisdiction v))
        result (registry/register-operation-resumption venue-id (:jurisdiction v) seq-n)]
    {:result result
     :venue-patch {:resumed? true
                   :resumption-number (get result "resumption_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (venue [_ id] (get-in @a [:venues id]))
  (all-venues [_] (sort-by :id (vals (:venues @a))))
  (egress-screen-of [_ id] (get-in @a [:egress-screens id]))
  (verify-of [_ venue-id] (get-in @a [:verifications venue-id]))
  (ledger [_] (:ledger @a))
  (resumption-history [_] (:resumptions @a))
  (next-sequence [_ jurisdiction] (get-in @a [:sequences jurisdiction] 0))
  (venue-already-resumed? [_ venue-id] (boolean (get-in @a [:venues venue-id :resumed?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :venue/upsert
      (swap! a update-in [:venues (:id value)] merge value)

      :verification/set
      (swap! a assoc-in [:verifications (first path)] payload)

      :egress-screen/set
      (swap! a assoc-in [:egress-screens (first path)] payload)

      :venue/mark-resumed
      (let [venue-id (first path)
            {:keys [result venue-patch]} (resume-venue! s venue-id)
            jurisdiction (:jurisdiction (venue s venue-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:sequences jurisdiction] (fnil inc 0))
                       (update-in [:venues venue-id] merge venue-patch)
                       (update :resumptions registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-venues [s venues] (when (seq venues) (swap! a assoc :venues venues)) s))

(defn seed-db
  "A MemStore seeded with the demo venue set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :verifications {} :egress-screens {} :ledger [] :sequences {}
                           :resumptions []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (verification/egress-screen payloads, ledger
  facts, resumption records) are stored as EDN strings so
  `langchain.db` doesn't expand them into sub-entities -- the same
  convention every sibling actor's store uses. The identity-schema
  builder, EDN-blob codec and seq-keyed event-log read/append are the
  shared kotoba-lang/langchain-store machinery (ADR-2607141600) -- the
  seam ~190 actors hand-roll; this store keeps only its domain wiring."
  (ls/identity-schema
   [:venue/id :verification/venue-id :egress-screen/venue-id
    :ledger/seq :resumption/seq :sequence/jurisdiction]))

(defn- venue->tx [{:keys [id venue-name hold-reason emergency-egress-obstructed? current-occupancy
                          maximum-capacity resumed? jurisdiction status resumption-number]}]
  (cond-> {:venue/id id}
    venue-name                             (assoc :venue/venue-name venue-name)
    hold-reason                            (assoc :venue/hold-reason hold-reason)
    (some? emergency-egress-obstructed?)   (assoc :venue/emergency-egress-obstructed? emergency-egress-obstructed?)
    current-occupancy                      (assoc :venue/current-occupancy current-occupancy)
    maximum-capacity                       (assoc :venue/maximum-capacity maximum-capacity)
    (some? resumed?)                       (assoc :venue/resumed? resumed?)
    jurisdiction                           (assoc :venue/jurisdiction jurisdiction)
    status                                 (assoc :venue/status status)
    resumption-number                      (assoc :venue/resumption-number resumption-number)))

(def ^:private venue-pull
  [:venue/id :venue/venue-name :venue/hold-reason :venue/emergency-egress-obstructed?
   :venue/current-occupancy :venue/maximum-capacity :venue/resumed?
   :venue/jurisdiction :venue/status :venue/resumption-number])

(defn- pull->venue [m]
  (when (:venue/id m)
    {:id (:venue/id m) :venue-name (:venue/venue-name m) :hold-reason (:venue/hold-reason m)
     :emergency-egress-obstructed? (boolean (:venue/emergency-egress-obstructed? m))
     :current-occupancy (:venue/current-occupancy m)
     :maximum-capacity (:venue/maximum-capacity m)
     :resumed? (boolean (:venue/resumed? m))
     :jurisdiction (:venue/jurisdiction m) :status (:venue/status m)
     :resumption-number (:venue/resumption-number m)}))

(defrecord DatomicStore [conn]
  Store
  (venue [_ id]
    (pull->venue (d/pull (d/db conn) venue-pull [:venue/id id])))
  (all-venues [_]
    (->> (d/q '[:find [?id ...] :where [?e :venue/id ?id]] (d/db conn))
         (map #(pull->venue (d/pull (d/db conn) venue-pull [:venue/id %])))
         (sort-by :id)))
  (egress-screen-of [_ id]
    (ls/dec* (d/q '[:find ?p . :in $ ?vid
                :where [?k :egress-screen/venue-id ?vid] [?k :egress-screen/payload ?p]]
              (d/db conn) id)))
  (verify-of [_ venue-id]
    (ls/dec* (d/q '[:find ?p . :in $ ?vid
                :where [?a :verification/venue-id ?vid] [?a :verification/payload ?p]]
              (d/db conn) venue-id)))
  (ledger [_] (ls/read-stream conn :ledger/seq :ledger/fact))
  (resumption-history [_] (ls/read-stream conn :resumption/seq :resumption/record))
  (next-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :sequence/jurisdiction ?j] [?e :sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (venue-already-resumed? [s venue-id]
    (boolean (:resumed? (venue s venue-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :venue/upsert
      (d/transact! conn [(venue->tx value)])

      :verification/set
      (d/transact! conn [{:verification/venue-id (first path) :verification/payload (ls/enc payload)}])

      :egress-screen/set
      (d/transact! conn [{:egress-screen/venue-id (first path) :egress-screen/payload (ls/enc payload)}])

      :venue/mark-resumed
      (let [venue-id (first path)
            {:keys [result venue-patch]} (resume-venue! s venue-id)
            jurisdiction (:jurisdiction (venue s venue-id))
            next-n (inc (next-sequence s jurisdiction))]
        (d/transact! conn
                     [(venue->tx (assoc venue-patch :id venue-id))
                      {:sequence/jurisdiction jurisdiction :sequence/next next-n}
                      {:resumption/seq (count (resumption-history s)) :resumption/record (ls/enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (ls/append-blob! conn :ledger/seq :ledger/fact (count (ledger s)) fact)
    fact)
  (with-venues [s venues]
    (when (seq venues) (d/transact! conn (mapv venue->tx (vals venues)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:venues ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [venues]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-venues s venues))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo venue set -- the Datomic-backed
  analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
