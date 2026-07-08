(ns recreation.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [recreation.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sakura Arcade" (:venue-name (store/venue s "venue-1"))))
      (is (= "JPN" (:jurisdiction (store/venue s "venue-1"))))
      (is (= 40 (:current-occupancy (store/venue s "venue-1"))))
      (is (false? (:emergency-egress-obstructed? (store/venue s "venue-1"))))
      (is (= 85 (:current-occupancy (store/venue s "venue-3"))))
      (is (true? (:emergency-egress-obstructed? (store/venue s "venue-4"))))
      (is (false? (:resumed? (store/venue s "venue-1"))))
      (is (= ["venue-1" "venue-2" "venue-3" "venue-4"]
             (mapv :id (store/all-venues s))))
      (is (nil? (store/egress-screen-of s "venue-1")))
      (is (nil? (store/verify-of s "venue-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/resumption-history s)))
      (is (zero? (store/next-sequence s "JPN")))
      (is (false? (store/venue-already-resumed? s "venue-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :venue/upsert
                                 :value {:id "venue-1" :venue-name "Sakura Arcade"}})
        (is (= "Sakura Arcade" (:venue-name (store/venue s "venue-1"))))
        (is (= 40 (:current-occupancy (store/venue s "venue-1"))) "unrelated field preserved"))
      (testing "verification / egress-screen payloads commit and read back"
        (store/commit-record! s {:effect :verification/set :path ["venue-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/verify-of s "venue-1")))
        (store/commit-record! s {:effect :egress-screen/set :path ["venue-1"]
                                 :payload {:venue-id "venue-1" :egress-obstructed? false}})
        (is (= {:venue-id "venue-1" :egress-obstructed? false} (store/egress-screen-of s "venue-1"))))
      (testing "operation resumption drafts a record and advances the sequence"
        (store/commit-record! s {:effect :venue/mark-resumed :path ["venue-1"]})
        (is (= "JPN-RSM-000000" (get (first (store/resumption-history s)) "record_id")))
        (is (= "operation-resumption-draft" (get (first (store/resumption-history s)) "kind")))
        (is (true? (:resumed? (store/venue s "venue-1"))))
        (is (= 1 (count (store/resumption-history s))))
        (is (= 1 (store/next-sequence s "JPN")))
        (is (true? (store/venue-already-resumed? s "venue-1")))
        (is (false? (store/venue-already-resumed? s "venue-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/venue s "nope")))
    (is (= [] (store/all-venues s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/resumption-history s)))
    (is (zero? (store/next-sequence s "JPN")))
    (store/with-venues s {"x" {:id "x" :venue-name "n"
                               :current-occupancy 10 :maximum-capacity 30
                               :emergency-egress-obstructed? false
                               :resumed? false :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:venue-name (store/venue s "x"))))))
