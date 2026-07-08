(ns recreation.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean venue through
  intake -> jurisdiction verification -> emergency-egress screening ->
  operation-resumption proposal (always escalates) -> human approval
  -> commit, then shows four HARD holds (a jurisdiction with no
  spec-basis, a venue whose own recorded current occupancy exceeds its
  own recorded maximum capacity, a venue whose own recorded emergency-
  egress path has NOT been cleared [screened directly via `:egress/
  screen` -- never via an actuation op against an unscreened venue --
  see this actor's own governor ns docstring / the lesson
  `parksafety`'s ADR-2607071922 Decision 5, `eldercare`'s, `museum`'s,
  `conservation`'s, `salon`'s, `entertainment`'s, `casework`'s,
  `hospital`'s, `facility`'s, `school`'s, `association`'s, `leasing`'s,
  `behavioral`'s, `secondary`'s, `card`'s, `water`'s, `telecom`'s,
  `aerospace`'s, `recovery`'s, `consulting`'s, `union`'s,
  `congregation`'s, `fab`'s, `energy`'s, `care`'s, `navigator`'s,
  `learning`'s, `banking`'s, `advertising`'s, `polling`'s,
  `research`'s, `design`'s, `nursing`'s, `sports`'s, `alliedhealth`'s,
  `laundry`'s, `holdco`'s, `photo`'s, `personalservice`'s,
  `edsupport`'s, `headoffice`'s, `residential`'s, `cultural`'s,
  `reserve`'s, `proserv`'s and `sportsevent`'s ADR-0001s already
  recorded], and a double resumption of an already-processed venue)
  that never reach a human at all, and prints the audit ledger + the
  draft operation-resumption records."
  (:require [langgraph.graph :as g]
            [recreation.store :as store]
            [recreation.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :licensed-operator :phase 3})

(defn- exec! [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== venue/intake venue-1 (JPN, clean; egress clear, occupancy within capacity) ==")
    (println (exec! actor "t1" {:op :venue/intake :subject "venue-1"
                                :patch {:id "venue-1" :venue-name "Sakura Arcade"}} operator))

    (println "== venue/verify venue-1 (escalates -- human approves) ==")
    (println (exec! actor "t2" {:op :venue/verify :subject "venue-1"} operator))
    (println (approve! actor "t2"))

    (println "== egress/screen venue-1 (clean; escalates -- human approves) ==")
    (println (exec! actor "t3" {:op :egress/screen :subject "venue-1"} operator))
    (println (approve! actor "t3"))

    (println "== actuation/resume-operation venue-1 (always escalates -- actuation/resume-operation) ==")
    (let [r (exec! actor "t4" {:op :actuation/resume-operation :subject "venue-1"} operator)]
      (println r)
      (println "-- human licensed venue operator approves --")
      (println (approve! actor "t4")))

    (println "== venue/verify venue-2 (no spec-basis -> HARD hold) ==")
    (println (exec! actor "t5" {:op :venue/verify :subject "venue-2" :no-spec? true} operator))

    (println "== venue/verify venue-3 (escalates -- human approves; sets up the occupancy test) ==")
    (println (exec! actor "t6" {:op :venue/verify :subject "venue-3"} operator))
    (println (approve! actor "t6"))

    (println "== actuation/resume-operation venue-3 (occupancy 85 > capacity 60 -> HARD hold) ==")
    (println (exec! actor "t7" {:op :actuation/resume-operation :subject "venue-3"} operator))

    (println "== egress/screen venue-4 (obstructed -> HARD hold, never reaches a human) ==")
    (println (exec! actor "t8" {:op :egress/screen :subject "venue-4"} operator))

    (println "== actuation/resume-operation venue-1 AGAIN (double-resumption -> HARD hold) ==")
    (println (exec! actor "t9" {:op :actuation/resume-operation :subject "venue-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft operation-resumption records ==")
    (doseq [r (store/resumption-history db)] (println r))))
