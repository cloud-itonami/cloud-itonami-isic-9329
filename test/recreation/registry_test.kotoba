(ns recreation.registry-test
  (:require [clojure.test :refer [deftest is]]
            [recreation.registry :as r]))

;; ----------------------------- occupancy-exceeds-capacity? -----------------------------

(deftest not-exceeded-when-within-capacity
  (is (not (r/occupancy-exceeds-capacity?
            {:current-occupancy 40 :maximum-capacity 60})))
  (is (not (r/occupancy-exceeds-capacity?
            {:current-occupancy 60 :maximum-capacity 60}))))

(deftest exceeded-when-past-capacity
  (is (r/occupancy-exceeds-capacity?
       {:current-occupancy 85 :maximum-capacity 60})))

(deftest missing-fields-are-not-treated-as-exceeded
  (is (not (r/occupancy-exceeds-capacity? {})))
  (is (not (r/occupancy-exceeds-capacity? {:current-occupancy 85}))))

;; ----------------------------- register-operation-resumption -----------------------------

(deftest resumption-is-a-draft-not-a-real-resumption
  (let [result (r/register-operation-resumption "venue-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest resumption-assigns-resumption-number
  (let [result (r/register-operation-resumption "venue-1" "JPN" 7)]
    (is (= (get result "resumption_number") "JPN-RSM-000007"))
    (is (= (get-in result ["record" "venue_id"]) "venue-1"))
    (is (= (get-in result ["record" "kind"]) "operation-resumption-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest resumption-validation-rules
  (is (thrown? Exception (r/register-operation-resumption "" "JPN" 0)))
  (is (thrown? Exception (r/register-operation-resumption "venue-1" "" 0)))
  (is (thrown? Exception (r/register-operation-resumption "venue-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-operation-resumption "venue-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-operation-resumption "venue-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-RSM-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-RSM-000001" (get-in hist2 [1 "record_id"])))))
