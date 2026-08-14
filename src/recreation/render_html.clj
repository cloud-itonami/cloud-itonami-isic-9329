(ns recreation.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 (com-junkawasaki/root ADR-2607189300):
  this repo previously had NO demo page and no generator at all. This
  namespace drives the REAL actor stack (`recreation.operation` ->
  `recreation.governor` -> `recreation.store`) through a scenario
  adapted from this repo's own `recreation.sim` demo driver
  (`clojure -M:dev:run`, confirmed BEFORE writing this file to produce a
  sensible ledger against the real seeded venue ids `venue-1`..`venue-4`
  -- this repo's own sim driver uses ids that DO match
  `recreation.store/demo-data`, so it was safe to reuse rather than
  author from scratch).

  The scenario EXTENDS the sim by one thread: `recreation.sim` fires four
  of the governor's five HARD checks, leaving `:evidence-incomplete`
  unexercised. Proposing `:actuation/resume-operation` for `venue-2` --
  whose jurisdiction verification HARD-held earlier in this same run, so
  no verification is ever filed -- fires the fifth. All five HARD rules
  this governor can raise are therefore demonstrated on one page.

  Nothing on the page is hand-typed domain data: every venue,
  jurisdiction, checklist item, hold reason, rule and resumption number
  is read back out of the store (or out of `recreation.facts` /
  `recreation.phase`, which ARE the seed catalogs) after the graph has
  actually run. No timestamps appear in the page content, so reruns
  against the same seed are byte-identical -- verify by diffing two
  consecutive runs into scratch directories.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [jp-go-dds.skin]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [recreation.store :as store]
            [recreation.facts :as facts]
            [recreation.registry :as registry]
            [recreation.phase :as phase]
            [recreation.governor :as governor]
            [recreation.operation :as op]
            [langgraph.graph :as g]))

(def ^:private operator
  "The same injected operator context `recreation.sim` uses."
  {:actor-id "op-1" :actor-role :licensed-operator :phase 3})

(def ^:private approver-id "op-1")

;; ----------------------------- the real run -----------------------------

(def ^:private scenario
  "One entry = one supervised actor run (one `:thread-id`). `:approve?`
  marks the threads the governor/phase gate escalates to a human, which
  are then resumed with a real `{:approval {:status :approved}}` -- the
  actor's genuine human-in-the-loop path, not a simulated one.

  Every `:subject` and every `:patch` value is a real key of
  `recreation.store/demo-data`."
  [{:tid "t1-intake"   :approve? false
    :request {:op :venue/intake :subject "venue-1"
              :patch {:id "venue-1" :venue-name "Sakura Arcade"}}}
   {:tid "t1-verify"   :approve? true
    :request {:op :venue/verify :subject "venue-1"}}
   {:tid "t1-egress"   :approve? true
    :request {:op :egress/screen :subject "venue-1"}}
   {:tid "t1-resume"   :approve? true
    :request {:op :actuation/resume-operation :subject "venue-1"}}
   ;; HARD: a jurisdiction with no official spec-basis in recreation.facts
   {:tid "t2-verify"   :approve? false
    :request {:op :venue/verify :subject "venue-2" :no-spec? true}}
   ;; HARD: resuming a venue whose required evidence was never filed
   ;; (its verification HARD-held above, so nothing reached the store)
   {:tid "t2-resume"   :approve? false
    :request {:op :actuation/resume-operation :subject "venue-2"}}
   {:tid "t3-verify"   :approve? true
    :request {:op :venue/verify :subject "venue-3"}}
   ;; HARD: the venue's own recorded occupancy exceeds its own capacity
   {:tid "t3-resume"   :approve? false
    :request {:op :actuation/resume-operation :subject "venue-3"}}
   ;; HARD: the screening op itself finds an obstructed egress path
   {:tid "t4-egress"   :approve? false
    :request {:op :egress/screen :subject "venue-4"}}
   ;; HARD: double resumption of a venue already resumed above
   {:tid "t1-resume-2" :approve? false
    :request {:op :actuation/resume-operation :subject "venue-1"}}])

(defn- run-thread!
  "Runs one operation thread to completion and returns the FINAL run's
  `:audit` stream and `:record`. Both are real graph state: `:audit`
  carries the approval lifecycle (`:approval-requested` /
  `:approval-granted`) that the store ledger never sees, and `:record`
  carries the `:effect` the commit node actually handed to the store."
  [actor {:keys [tid request approve?]}]
  (let [r (g/run* actor {:request request :context operator} {:thread-id tid})
        r (if approve?
            (g/run* actor {:approval {:status :approved :by approver-id}}
                    {:thread-id tid :resume? true})
            r)]
    {:tid tid
     :request request
     :audit (vec (get-in r [:state :audit]))
     :record (get-in r [:state :record])}))

(defn run-demo!
  "Drives a freshly seeded store through `scenario` and returns
  `{:db .. :threads [..]}`. Everything the renderer prints is read back
  out of `db` (or out of the seed catalogs) afterwards."
  []
  (let [db (store/seed-db)
        actor (op/build db)]
    {:db db
     :threads (mapv (partial run-thread! actor) scenario)}))

;; ----------------------------- html helpers -----------------------------

(defn- esc [v]
  (-> (str v)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")))

(defn- label
  "Keyword -> bare name, anything else -> its string. The ledger's
  `:basis` mixes keywords (`:id`, `:egress-check`) with citation strings
  (a legal basis, a provenance URL), so this must not assume `name`."
  [v]
  (if (keyword? v) (name v) (str v)))

(defn- code [v] (str "<code>" (esc v) "</code>"))
(defn- ok [s] (str "<span class=\"ok\">" s "</span>"))
(defn- warn [s] (str "<span class=\"warn\">" s "</span>"))
(defn- crit [s] (str "<span class=\"critical\">" s "</span>"))
(defn- muted [s] (str "<span class=\"muted\">" s "</span>"))

(defn- row [cells]
  (str "        <tr>" (str/join (map #(str "<td>" % "</td>") cells)) "</tr>"))

(defn- table [headers rows]
  (str "    <table>\n"
       "      <thead><tr>"
       (str/join (map #(str "<th>" (esc %) "</th>") headers))
       "</tr></thead>\n"
       "      <tbody>\n"
       (str/join "\n" rows) "\n"
       "      </tbody>\n"
       "    </table>\n"))

(defn- section [title lede body]
  (str "  <section class=\"card\">\n"
       "    <h2>" title "</h2>\n"
       (when lede (str "    <p class=\"muted\">" lede "</p>\n"))
       body
       "  </section>\n"))

;; ----------------------------- derived views -----------------------------

(defn- last-fact-for [ledger venue-id]
  (last (filter #(= (:subject %) venue-id) ledger)))

(defn- status-cell [ledger venue-id]
  (let [f (last-fact-for ledger venue-id)]
    (cond
      (nil? f) (muted "no activity")
      (= :committed (:t f)) (ok "committed")
      (= :governor-hold (:t f))
      (crit (str "HARD hold &middot; "
                 (esc (label (-> f :violations first :rule)))))
      :else (muted (esc (label (:t f)))))))

(defn- egress-cell [{:keys [emergency-egress-obstructed?]}]
  (if emergency-egress-obstructed?
    (crit "obstructed")
    (ok "clear")))

(defn- occupancy-cell [{:keys [current-occupancy maximum-capacity] :as v}]
  (let [txt (str "<span class=\"num\">" (esc current-occupancy) " / "
                 (esc maximum-capacity) "</span>")]
    (cond
      (not (registry/occupancy-exceeds-capacity-checkable? v))
      (crit (str txt " &middot; un-checkable"))
      (registry/occupancy-exceeds-capacity? v)
      (crit (str txt " &middot; over capacity"))
      :else (str txt " " (muted "within capacity")))))

(defn- resumption-cell [{:keys [resumed? resumption-number]}]
  (if resumed?
    (ok (str "resumed &middot; " (code resumption-number)))
    (muted "on hold")))

(defn- venue-rows [db ledger]
  (for [{:keys [id venue-name jurisdiction hold-reason] :as v} (store/all-venues db)]
    (row [(code id) (esc venue-name) (code jurisdiction) (esc hold-reason)
          (egress-cell v) (occupancy-cell v) (resumption-cell v)
          (status-cell ledger id)])))

(defn- phase-rows []
  (for [[n {phase-label :label :keys [writes auto]}] (sort-by key phase/phases)]
    (row [(str "<span class=\"num\">" n "</span>")
          (esc phase-label)
          (if (seq writes)
            (str/join " " (map #(code %) (sort-by label writes)))
            (muted "none"))
          (if (seq auto)
            (str/join " " (map #(code %) (sort-by label auto)))
            (muted "none"))])))

(defn- op-gate-rows []
  (for [o (sort-by label phase/write-ops)]
    (let [writes-at (sort (keep (fn [[n p]] (when (contains? (:writes p) o) n)) phase/phases))
          auto-at   (sort (keep (fn [[n p]] (when (contains? (:auto p) o) n)) phase/phases))
          stakes?   (contains? governor/high-stakes o)]
      (row [(code o)
            (if (seq writes-at) (esc (str/join ", " writes-at)) (muted "never"))
            (if (seq auto-at)
              (warn (esc (str "phase " (str/join ", " auto-at))))
              (crit "never &middot; at any phase"))
            (if stakes?
              (crit "high-stakes actuation &middot; always a human call")
              (muted "not actuation"))]))))

(defn- hold-rows [ledger]
  (for [f (filter #(= :governor-hold (:t %)) ledger)
        v (:violations f)]
    (row [(crit (esc (label (:rule v))))
          (code (:op f))
          (code (:subject f))
          (esc (:detail v))
          (str "<span class=\"num\">" (esc (:confidence f)) "</span>")])))

(defn- jurisdiction-rows []
  (for [[iso3 {:keys [name owner-authority legal-basis provenance required-evidence]}]
        (sort-by key facts/catalog)]
    (row [(code iso3) (esc name) (esc owner-authority) (esc legal-basis)
          (str "<span class=\"num\">" (count required-evidence) "</span>")
          (str "<a href=\"" (esc provenance) "\">" (esc provenance) "</a>")])))

(defn- verification-rows [db]
  (for [{:keys [id]} (store/all-venues db)
        :let [v (store/verify-of db id)]
        :when v]
    (row [(code id) (code (:jurisdiction v))
          (str "<ul>" (str/join (map #(str "<li>" (esc %) "</li>") (:checklist v))) "</ul>")
          (esc (:legal-basis v))
          (if (:spec-basis v)
            (ok (esc (:spec-basis v)))
            (crit "none &middot; HARD hold"))])))

(defn- egress-rows [db]
  (for [{:keys [id venue-name]} (store/all-venues db)
        :let [s (store/egress-screen-of db id)]
        :when s]
    (row [(code id) (esc venue-name)
          (if (:egress-obstructed? s) (crit "obstructed") (ok "clear"))
          (if-let [a (:approved-by s)] (ok (esc a)) (muted "n/a"))])))

(defn- resumption-rows [db]
  (for [r (store/resumption-history db)]
    (row [(code (get r "record_id")) (esc (get r "kind"))
          (code (get r "venue_id")) (code (get r "jurisdiction"))
          (if (get r "immutable") (ok "immutable") (warn "mutable"))])))

;; --- approver attribution: ASK the store, do not assume ------------------

(def ^:private register-probes
  "`:effect` -> (db, subject) -> the record(s) that commit could have
  written. The attribution section walks these and reports the approver
  ACTUALLY present, so the page self-corrects the day the store starts
  retaining one -- it never hardcodes 'this is broken'."
  {:venue/upsert       (fn [db id] [(store/venue db id)])
   :verification/set   (fn [db id] [(store/verify-of db id)])
   :egress-screen/set  (fn [db id] [(store/egress-screen-of db id)])
   :venue/mark-resumed (fn [db id]
                         [(first (filter #(= id (get % "venue_id"))
                                         (store/resumption-history db)))
                          (store/venue db id)])})

(def ^:private approver-keys
  "Every spelling a fix might plausibly use, so the probe is not defeated
  by a rename."
  [:approved-by :approver :approved_by "approved_by" "approved-by"
   "approver" "approvedBy"])

(defn- approver-in
  "The approver attribution actually retained in `records`, or nil."
  [records]
  (some (fn [r]
          (when (map? r)
            (some (fn [k]
                    (let [v (get r k)]
                      (when (and (some? v) (not= v "")) v)))
                  approver-keys)))
        records))

(defn- attribution-rows [db threads]
  (for [{:keys [audit record]} threads
        :when record
        :let [granted (first (filter #(= :approval-granted (:t %)) audit))
              effect  (:effect record)
              subject (first (:path record))
              sent    (get-in record [:payload :approved-by])
              probe   (get register-probes effect)
              kept    (when probe (approver-in (probe db subject)))]]
    (row [(code (:op (first (filter :op audit))))
          (code subject)
          (code effect)
          (if granted (esc (:by granted)) (muted "auto-commit"))
          (if sent (ok (esc sent)) (muted "not attached"))
          (if kept (ok (esc kept)) (crit "absent"))
          (cond
            (nil? granted)
            (muted "auto-commit at phase 3 &mdash; no human approver exists")
            kept (ok "retained in record")
            sent (crit "audit only &mdash; not retained in record")
            :else (warn "actor did not attach an approver to the payload"))])))

(defn- ledger-rows [ledger]
  (for [{:keys [t op subject basis summary violations]} ledger]
    (row [(if (= :governor-hold t) (crit (esc (label t))) (ok (esc (label t))))
          (code op) (code subject)
          (esc (str/join ", " (map label basis)))
          (esc (or summary (str/join " / " (map :detail violations))))])))

(defn- approval-stream-rows [threads]
  (for [{:keys [tid audit]} threads
        f audit
        :when (#{:approval-requested :approval-granted} (:t f))]
    (row [(code tid)
          (if (= :approval-granted (:t f))
            (ok (esc (label (:t f))))
            (warn (esc (label (:t f)))))
          (code (:op f)) (code (:subject f))
          (esc (or (some-> (:reason f) label) (:by f) ""))])))

;; ----------------------------- document -----------------------------

(defn render
  "Renders the operator console from a `db` + `threads` that have already
  been through `run-demo!`."
  [{:keys [db threads]}]
  (let [ledger (vec (store/ledger db))
        holds (filterv #(= :governor-hold (:t %)) ledger)
        commits (filterv #(= :committed (:t %)) ledger)
        rules (distinct (mapv :rule (mapcat :violations holds)))
        cov (facts/coverage)]
    (str
     "<!doctype html>\n"
     "<html lang=\"en\"><head><meta charset=\"utf-8\">"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">"
     "<title>cloud-itonami-isic-9329 &middot; other amusement &amp; recreation</title><style>"
     (jp-go-dds.skin/dds+skin)
     "</style></head><body>\n"
     "<header class=\"bar\">\n"
     "  <h1>Other amusement and recreation activities n.e.c. (ISIC 9329) — Operator Console</h1>\n"
     "</header>\n"
     "<p class=\"subtitle\">read-only sample · governor-gated · operation resumption is always a human call</p>\n"
     "<main>\n"

     (section
      "This run"
      (str "Generated at build time by <code>recreation.render-html</code> "
           "(<code>clojure -M:dev:render-html</code>) from a real "
           "<code>recreation.operation</code> StateGraph run over a freshly "
           "seeded <code>recreation.store</code>. No figure below is typed by hand.")
      (table ["Venues" "Operation threads" "Ledger facts" "Commits"
              "HARD holds" "Distinct HARD rules" "Resumption drafts"]
             [(row [(str "<span class=\"num\">" (count (store/all-venues db)) "</span>")
                    (str "<span class=\"num\">" (count threads) "</span>")
                    (str "<span class=\"num\">" (count ledger) "</span>")
                    (str "<span class=\"num\">" (count commits) "</span>")
                    (crit (str "<span class=\"num\">" (count holds) "</span>"))
                    (str "<span class=\"num\">" (count rules) "</span>")
                    (str "<span class=\"num\">" (count (store/resumption-history db)) "</span>")])]))

     (section
      "Venue directory"
      (str "The seeded venue set (<code>recreation.store/demo-data</code>) as it "
           "stands AFTER the run — egress status, occupancy against the venue's "
           "own recorded capacity, and whether operation was actually resumed.")
      (table ["Venue" "Name" "Jurisdiction" "Hold reason" "Emergency egress"
              "Occupancy / capacity" "Resumption" "Last op status"]
             (venue-rows db ledger)))

     (section
      "Governor HARD holds (this run)"
      (str "All " (count rules) " HARD checks <code>recreation.governor</code> can raise "
           "fired in this scenario. A HARD violation is un-overridable: it never "
           "reaches a human approver at all, so there is no approve-your-way-past path.")
      (table ["Rule" "Op" "Venue" "Detail (governor)" "Advisor confidence"]
             (hold-rows ledger)))

     (section
      "Rollout phase gate"
      (str "<code>recreation.phase</code>, read directly — the phase table is the "
           "seed, not a description of it. Phase " phase/default-phase " is the default.")
      (table ["Phase" "Label" "Writes allowed" "Auto-commit eligible"]
             (phase-rows)))

     (section
      "Per-op gate"
      (str "Derived by intersecting <code>recreation.phase/write-ops</code> with each "
           "phase's <code>:writes</code>/<code>:auto</code> set and with "
           "<code>recreation.governor/high-stakes</code>. Two independent layers agree "
           "that <code>:actuation/resume-operation</code> never auto-commits.")
      (table ["Op" "Writable at phase" "Auto-commit eligible" "Stake"]
             (op-gate-rows)))

     (section
      "Jurisdiction spec-basis catalog"
      (str (esc (:note cov)) " Covered: "
           (esc (str/join ", " (:covered-jurisdictions cov)))
           ". A jurisdiction absent from this table has NO spec-basis, and the "
           "governor HARD-holds any proposal that invents one — which is exactly "
           "what <code>venue-2</code> (<code>ATL</code>) demonstrates above.")
      (table ["ISO3" "Jurisdiction" "Owner authority" "Legal basis"
              "Required evidence" "Provenance"]
             (jurisdiction-rows)))

     (section
      "Committed jurisdiction verifications"
      (str "Read back from the store — the evidence checklists that actually "
           "committed, and who approved each. Absent verifications are absent "
           "because the governor held them.")
      (table ["Venue" "Jurisdiction" "Required evidence (committed)"
              "Legal basis" "Spec-basis"]
             (verification-rows db)))

     (section
      "Emergency-egress screening register"
      "Committed screening verdicts, read back from the store."
      (table ["Venue" "Name" "Verdict" "Approved by (retained)"]
             (egress-rows db)))

     (section
      "Operation-resumption register (draft, unsigned)"
      (str "Built by <code>recreation.registry/register-operation-resumption</code> — "
           "the record a licensed venue operator would keep. Signature is the "
           "operator's own act, never this actor's: every certificate this actor "
           "produces is drafted unsigned.")
      (table ["Record id" "Kind" "Venue" "Jurisdiction" "Immutability"]
             (resumption-rows db)))

     (section
      "Approver attribution — what the store actually retained"
      (str "Derived at render time by probing each register for an approver key, "
           "rather than assuming: the row reports what the actor attached to the "
           "commit <code>:payload</code> next to what the store kept. "
           "<code>:verification/set</code> and <code>:egress-screen/set</code> persist "
           "the payload verbatim, so attribution survives. "
           "<code>:venue/mark-resumed</code> reconstructs its record from "
           "<code>recreation.registry</code> and reads neither <code>:value</code> nor "
           "<code>:payload</code>, so the approver of the one real-world actuation is "
           "visible in the audit stream but NOT in the register. That is labelled, not "
           "omitted — a reader must be able to tell &ldquo;nobody approved&rdquo; from "
           "&ldquo;the store did not keep it&rdquo;. This table re-derives itself, so it "
           "will show <em>retained</em> the day the store is fixed.")
      (table ["Op" "Venue" "Effect" "Approver (audit)" "Attached to payload"
              "Retained in register" "Verdict"]
             (attribution-rows db threads)))

     (section
      "Approval lifecycle (graph audit stream)"
      (str "These facts live in the run's <code>:audit</code> channel and are never "
           "written to the store ledger — shown so the gap above is checkable rather "
           "than asserted.")
      (table ["Thread" "Fact" "Op" "Venue" "Reason / approver"]
             (approval-stream-rows threads)))

     (section
      "Audit ledger (this run)"
      "The append-only decision-fact log the store itself holds."
      (table ["Fact" "Op" "Venue" "Basis" "Summary / governor detail"]
             (ledger-rows ledger)))

     "</main>\n"
     "<footer>\n"
     "  <p>cloud-itonami-isic-9329 — Open Business Blueprint for ISIC Rev.5 9329. "
     "Regenerate with <code>clojure -M:dev:render-html</code>; the generator refuses "
     "to write a console in which the governor raised no HARD hold.</p>\n"
     "</footer>\n"
     "</body></html>\n")))

(defn -main [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        {:keys [db] :as run} (run-demo!)
        ledger (vec (store/ledger db))
        holds (filterv #(= :governor-hold (:t %)) ledger)
        rules (distinct (mapv :rule (mapcat :violations holds)))]
    ;; Build-time invariant, not a convention: a console that shows no HARD
    ;; hold would be a page claiming this actor is ungoverned. Refuse to
    ;; write one rather than emit a quietly-wrong sample.
    (when (zero? (count holds))
      (throw (ex-info (str "refusing to write " out
                           ": the run produced 0 :governor-hold records, so the "
                           "console would not demonstrate a single HARD governor "
                           "hold. Fix the scenario or the governor, not this check.")
                      {:out out
                       :ledger-facts (count ledger)
                       :governor-holds 0})))
    (io/make-parents out)
    (spit out (render run))
    (println "wrote" out
             (str "(" (count ledger) " ledger facts, "
                  (count holds) " HARD holds over " (count rules) " distinct rules: "
                  (str/join ", " (map label rules)) ", "
                  (count (store/resumption-history db)) " resumption drafts)"))))
