(ns recreation.phase-test
  "The phase table as executable tests. The invariant this repo cannot
  regress on: `:actuation/resume-operation` must NEVER be a member of
  any phase's `:auto` set."
  (:require [clojure.test :refer [deftest is testing]]
            [recreation.phase :as phase]))

(deftest resume-operation-never-auto-at-any-phase
  (testing "structural invariant: no phase, now or in the future entries, auto-commits a real operation resumption"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :actuation/resume-operation))
          (str "phase " n " must not auto-commit :actuation/resume-operation")))))

(deftest egress-screen-never-auto-at-any-phase
  (testing "screening carries no direct capital risk, but is still never auto-eligible, matching every sibling screening op in this fleet"
    (doseq [[n {:keys [auto]}] phase/phases]
      (is (not (contains? auto :egress/screen))
          (str "phase " n " must not auto-commit :egress/screen")))))

(deftest phase-0-is-fully-read-only
  (is (empty? (:writes (get phase/phases 0)))))

(deftest phase-3-auto-commits-only-no-capital-risk-ops
  (testing ":venue/intake carries no direct capital risk -- auto-eligible; it is the ONLY auto-eligible op in this domain"
    (is (= #{:venue/intake} (:auto (get phase/phases 3))))))

(deftest gate-hold-always-wins
  (is (= :hold (:disposition (phase/gate 3 {:op :venue/intake} :hold)))))

(deftest gate-escalates-a-clean-non-auto-write
  (is (= :escalate (:disposition (phase/gate 3 {:op :actuation/resume-operation} :commit)))))

(deftest gate-holds-a-write-disabled-in-this-phase
  (is (= :hold (:disposition (phase/gate 0 {:op :venue/intake} :commit)))))
