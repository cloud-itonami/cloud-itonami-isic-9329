(ns recreation.recreationopsllm
  "RecOps-LLM client -- the *contained intelligence node* for the
  other-amusement/recreation-safety actor.

  It normalizes venue intake, drafts a per-jurisdiction recreation-
  venue-safety evidence checklist, screens venues for an obstructed
  emergency-egress path, and drafts the operation-resumption action.
  CRITICAL: it is a smart-but-untrusted advisor. It returns a
  *proposal* (with a rationale + the fields it cited), never a
  committed record or a real operation resumption. Every output is
  censored downstream by `recreation.governor` before anything touches
  the SSoT, and `:actuation/resume-operation` proposals NEVER auto-
  commit at any phase -- see README `Actuation`.

  Like every sibling actor's advisor, this is a deterministic mock so
  the actor graph runs offline and the governor contract is exercised
  end-to-end. In production this calls a real LLM (kotoba-llm or
  equivalent) with the same proposal shape.

  Proposal shape (all kinds):
    {:summary    str            ; human-facing draft / finding
     :rationale  str            ; why -- SCANNED by the spec-basis gate
     :cites      [kw|str ..]    ; facts/sources the LLM used -- SCANNED too
     :effect     kw             ; how a commit would mutate the SSoT
     :stake      kw|nil         ; :actuation/resume-operation | nil
     :confidence 0..1}"
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [kotoba.lang.text :as str]
            [recreation.facts :as facts]
            [recreation.registry :as registry]
            [recreation.store :as store]
            [langchain.model :as model]))

(defn- normalize-intake
  "Directory upsert -- the LLM only normalizes/validates the patch; it
  does not invent the venue, hold reason/egress status or
  jurisdiction. High confidence, low stakes."
  [_db {:keys [patch]}]
  {:summary    (str "施設記録更新: " (pr-str (keys patch)))
   :rationale  "入力 patch の正規化のみ。新規事実の生成なし。"
   :cites      (vec (keys patch))
   :effect     :venue/upsert
   :value      patch
   :stake      nil
   :confidence 0.97})

(defn- verify-jurisdiction
  "Per-jurisdiction recreation-venue-safety evidence checklist draft.
  `:no-spec?` injects the failure mode we must defend against:
  proposing a checklist for a jurisdiction with NO official spec-basis
  in `recreation.facts` -- the Recreation Safety Governor must reject
  this (never invent a jurisdiction's requirements)."
  [db {:keys [subject no-spec?]}]
  (let [v (store/venue db subject)
        iso3 (if no-spec? "ATL" (:jurisdiction v))
        sb (facts/spec-basis iso3)]
    (if (nil? sb)
      {:summary    (str iso3 " の公式spec-basisが見つかりません")
       :rationale  "recreation.facts に未登録の法域。要件を推測で作らない。"
       :cites      []
       :effect     :verification/set
       :value      {:jurisdiction iso3 :checklist [] :spec-basis nil}
       :stake      nil
       :confidence 0.9}
      {:summary    (str iso3 " (" (:owner-authority sb) ") 向け必要書類 "
                        (count (:required-evidence sb)) " 件を提案")
       :rationale  (str "公式ソース: " (:provenance sb) " / 法的根拠: " (:legal-basis sb))
       :cites      [(:legal-basis sb) (:provenance sb)]
       :effect     :verification/set
       :value      {:jurisdiction iso3
                    :checklist (:required-evidence sb)
                    :spec-basis (:provenance sb)
                    :legal-basis (:legal-basis sb)}
       :stake      nil
       :confidence 0.9})))

(defn- screen-egress
  "Emergency-egress screening draft. `:emergency-egress-obstructed?`
  on the venue record injects the failure mode: the Recreation Safety
  Governor must HOLD, un-overridably, on any obstructed egress path."
  [db {:keys [subject]}]
  (let [v (store/venue db subject)]
    (cond
      (nil? v)
      {:summary "対象施設が見つかりません" :rationale "no venue record"
       :cites [] :effect :egress-screen/set :value {:venue-id subject :verdict :unknown}
       :stake nil :confidence 0.0}

      (true? (:emergency-egress-obstructed? v))
      {:summary    (str (:venue-name v) ": 避難経路の閉塞を検出")
       :rationale  "スクリーニングが避難経路の閉塞を検出。人手確認とホールドが必須。"
       :cites      [:egress-check]
       :effect     :egress-screen/set
       :value      {:venue-id subject :egress-obstructed? true}
       :stake      nil
       :confidence 0.95}

      :else
      {:summary    (str (:venue-name v) ": 避難経路確保を確認")
       :rationale  "避難経路スクリーニング完了。"
       :cites      [:egress-check]
       :effect     :egress-screen/set
       :value      {:venue-id subject :egress-obstructed? false}
       :stake      nil
       :confidence 0.9})))

(defn- propose-operation-resumption
  "Draft the actual OPERATION-RESUMPTION action -- resuming operation
  at a real venue after a safety hold. ALWAYS `:stake :actuation/
  resume-operation` -- this is a REAL-WORLD, safety-critical act,
  never a draft the actor may auto-run. See README `Actuation`: no
  phase ever adds this op to a phase's `:auto` set (`recreation.
  phase`); the governor also always escalates on `:actuation/resume-
  operation`. Two independent layers agree, deliberately."
  [db {:keys [subject]}]
  (let [v (store/venue db subject)
        ready? (and v (not (:emergency-egress-obstructed? v))
                   (not (registry/occupancy-exceeds-capacity? v)))]
    {:summary    (str subject " 向け営業再開提案"
                      (when v (str " (venue=" (:venue-name v) ")")))
     :rationale  (if v
                   (str "emergency-egress-obstructed?=" (:emergency-egress-obstructed? v)
                        " current-occupancy=" (:current-occupancy v)
                        " maximum-capacity=" (:maximum-capacity v))
                   "施設が見つかりません")
     :cites      (if v [subject] [])
     :effect     :venue/mark-resumed
     :value      {:venue-id subject}
     :stake      :actuation/resume-operation
     :confidence (if ready? 0.9 0.3)}))

(defn infer
  "Route a request to the right proposal generator.
  request: {:op kw :subject id ...op-specific...}"
  [db {:keys [op] :as request}]
  (case op
    :venue/intake                 (normalize-intake db request)
    :venue/verify                    (verify-jurisdiction db request)
    :egress/screen                      (screen-egress db request)
    :actuation/resume-operation             (propose-operation-resumption db request)
    {:summary "未対応の操作" :rationale (str op) :cites []
     :effect :noop :stake nil :confidence 0.0}))

;; ----------------------------- Advisor protocol -----------------------------

(defprotocol Advisor
  (-advise [advisor store request] "store + request -> proposal map"))

(defn mock-advisor
  "The deterministic advisor (the `infer` logic above). Default everywhere."
  [] (reify Advisor (-advise [_ st req] (infer st req))))

(def ^:private system-prompt
  (str "あなたはレクリエーション施設の営業再開エージェントの助言者です。"
       "与えられた事実のみに基づき、提案を1つだけEDNマップで返します。説明や前置きは"
       "一切書かず、EDNだけを出力します。\n"
       "キー: :summary(人向けドラフト) :rationale(根拠/必ず事実から) "
       ":cites(使った事実キーのベクタ) "
       ":effect(:venue/upsert|:verification/set|:egress-screen/set|"
       ":venue/mark-resumed) "
       ":stake(:actuation/resume-operation か nil) :confidence(0..1)。\n"
       "重要: 登録されていない法域の要件を絶対に創作してはいけません。"
       "spec-basisが無い場合は :cites を空にし confidence を上げないこと。"))

(defn- facts-for [st {:keys [op subject]}]
  (case op
    :venue/verify              {:venue (store/venue st subject)}
    :egress/screen              {:venue (store/venue st subject)}
    :actuation/resume-operation      {:venue (store/venue st subject)}
    {:venue (store/venue st subject)}))

(defn- parse-proposal
  "Parse the model's EDN proposal defensively. Any parse/shape failure
  yields a safe low-confidence noop so the Recreation Safety Governor
  escalates/holds -- an LLM hiccup can never auto-resume operation."
  [content]
  (let [p (try (edn/read-string (str/trim (str content)))
               (catch #?(:clj Exception :cljs :default) _ nil))]
    (if (map? p)
      (-> p
          (update :cites #(vec (or % [])))
          (update :confidence #(if (number? %) (double %) 0.0))
          (update :effect #(or % :noop)))
      {:summary "LLM応答を解釈できませんでした" :rationale (str content)
       :cites [] :effect :noop :stake nil :confidence 0.0})))

(defn llm-advisor
  "An advisor backed by a `langchain.model/ChatModel` (real inference)."
  ([chat-model] (llm-advisor chat-model {}))
  ([chat-model gen-opts]
   (reify Advisor
     (-advise [_ st req]
       (let [msgs [{:role :system :content system-prompt}
                   {:role :user :content (str "操作: " (:op req)
                                              "\n対象: " (:subject req)
                                              "\n事実: " (pr-str (facts-for st req)))}]
             resp (model/-generate chat-model msgs gen-opts)]
         (parse-proposal (:content resp)))))))

(defn trace
  "Decision-grounded audit record -- persisted to the :audit channel."
  [request proposal]
  {:t          :recreationopsllm-proposal
   :op         (:op request)
   :subject    (:subject request)
   :summary    (:summary proposal)
   :rationale  (:rationale proposal)
   :cites      (:cites proposal)
   :confidence (:confidence proposal)})
