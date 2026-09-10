(ns recreation.governor-contract-test
  "The governor contract as executable tests -- the recreation-venue
  analog of `cloud-itonami-isic-6512`'s `casualty.governor-contract-
  test`. The single invariant under test:

    RecOps-LLM never resumes operation the Recreation Safety Governor
    would reject, `:actuation/resume-operation` NEVER auto-commits at
    any phase, `:venue/intake` (no direct capital risk) MAY auto-commit
    when clean, and every decision (commit OR hold) leaves exactly one
    ledger fact."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [recreation.store :as store]
            [recreation.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :licensed-operator :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through verify -> approve, leaving a recreation-
  venue-safety evidence checklist on file. Uses distinct thread-ids
  per call site by suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :venue/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :venue/intake :subject "venue-1"
                   :patch {:id "venue-1" :venue-name "Sakura Arcade"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Sakura Arcade" (:venue-name (store/venue db "venue-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest venue-verify-always-needs-approval
  (testing "verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :venue/verify :subject "venue-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/verify-of db "venue-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a venue/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :venue/verify :subject "venue-1" :no-spec? true} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/verify-of db "venue-1")) "no verification written"))))

(deftest resume-operation-without-venue-verify-is-held
  (testing "actuation/resume-operation before any venue verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :actuation/resume-operation :subject "venue-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest occupancy-exceeds-capacity-is-held
  (testing "a venue whose own recorded current occupancy exceeds its own recorded maximum capacity -> HOLD"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "venue-3")
          res (exec-op actor "t5" {:op :actuation/resume-operation :subject "venue-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:occupancy-exceeds-capacity} (-> (store/ledger db) last :basis)))
      (is (empty? (store/resumption-history db))))))

(deftest emergency-egress-obstructed-is-held-and-unoverridable
  (testing "an obstructed emergency-egress path on a venue -> HOLD, and never reaches request-approval -- exercised via :egress/screen DIRECTLY, not via the actuation op against an unscreened venue (see this actor's governor ns docstring / parksafety's ADR-2607071922 Decision 5 / eldercare's, museum's, conservation's, salon's, entertainment's, casework's, hospital's, facility's, school's, association's, leasing's, behavioral's, secondary's, card's, water's, telecom's, aerospace's, recovery's, consulting's, union's, congregation's, fab's, energy's, care's, navigator's, learning's, banking's, advertising's, polling's, research's, design's, nursing's, sports's, alliedhealth's, laundry's, holdco's, photo's, personalservice's, edsupport's, headoffice's, residential's, cultural's, reserve's, proserv's and sportsevent's ADR-0001s)"
    (let [[db actor] (fresh)
          res (exec-op actor "t6" {:op :egress/screen :subject "venue-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:emergency-egress-obstructed} (-> (store/ledger db) first :basis)))
      (is (nil? (store/egress-screen-of db "venue-4")) "no clearance written"))))

(deftest resume-operation-always-escalates-then-human-decides
  (testing "a clean, fully-assessed venue still ALWAYS interrupts for human approval -- actuation/resume-operation is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t7pre" "venue-1")
          r1 (exec-op actor "t7" {:op :actuation/resume-operation :subject "venue-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, operation-resumption record drafted"
        (let [r2 (approve! actor "t7")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:resumed? (store/venue db "venue-1"))))
          (is (= 1 (count (store/resumption-history db))) "one draft resumption record"))))))

(deftest double-resumption-is-held
  (testing "resuming operation at the same venue twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t8pre" "venue-1")
          _ (exec-op actor "t8a" {:op :actuation/resume-operation :subject "venue-1"} operator)
          _ (approve! actor "t8a")
          res (exec-op actor "t8" {:op :actuation/resume-operation :subject "venue-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-resumed} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/resumption-history db))) "still only the one earlier resumption"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :venue/intake :subject "venue-1"
                          :patch {:id "venue-1" :venue-name "Sakura Arcade"}} operator)
      (exec-op actor "b" {:op :venue/verify :subject "venue-1" :no-spec? true} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
