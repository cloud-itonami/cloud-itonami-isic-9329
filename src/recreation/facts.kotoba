(ns recreation.facts
  "Per-jurisdiction recreation-venue-safety regulatory catalog -- the
  G2-style spec-basis table the Recreation Safety Governor checks
  every venue/verify proposal against ('did the advisor cite an
  OFFICIAL public source for this jurisdiction's fire/life-safety and
  recreation-licensing requirements, or did it invent one?').

  This blueprint's own named example activities -- arcades, escape
  rooms, recreational fishing/hunting operations -- are genuinely
  distinct from `parksafety`/9321's fixed amusement-RIDE domain (no
  mechanical ride-inspection regime applies here); the real, load-
  bearing regulatory concerns for THIS vertical are assembly-venue
  fire/life-safety (occupancy limits, means-of-egress -- acutely
  relevant to escape rooms specifically, after real-world escape-room
  fire incidents prompted dedicated fire-code attention in several
  jurisdictions) and, for recreational fishing/hunting operations,
  angling/game licensing compliance.

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official fire/life-
  safety and recreation-licensing regulators (see `:provenance`); they
  are a STARTING catalog, not a from-scratch survey of all ~194
  jurisdictions.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the generic
  safety-inspection/occupancy-certification/egress-plan/license-
  verification evidence set submitted in some form; `:legal-basis` /
  `:owner-authority` / `:provenance` are the G2 citation the governor
  requires before any :venue/verify proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "総務省消防庁 (Fire and Disaster Management Agency, FDMA)"
          :legal-basis "消防法 (Fire Service Act) 第8条 -- 防火対象物の防火管理; 風俗営業等の規制及び業務の適正化等に関する法律 (game-arcade licensing)"
          :national-spec "特定防火対象物の収容人員/避難経路に関する規制基準"
          :provenance "https://www.fdma.go.jp/"
          :required-evidence ["防火対象物点検報告書 (safety-inspection record)"
                              "収容人員証明書 (occupancy-certification record)"
                              "避難計画書 (egress-plan documentation)"
                              "営業許可証 (operating-license verification)"]}
   "USA" {:name "United States"
          :owner-authority "National Fire Protection Association (NFPA) / State Fire Marshal"
          :legal-basis "NFPA 101 Life Safety Code (Assembly Occupancy, Chapter 12/13 Means of Egress)"
          :national-spec "State Fire Marshal assembly-occupancy permit and means-of-egress inspection requirements"
          :provenance "https://www.nfpa.org/codes-and-standards/nfpa-101-standard-development/101"
          :required-evidence ["Safety-inspection record"
                              "Occupancy-certification record"
                              "Egress-plan documentation"
                              "Operating-license verification"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "Fire and Rescue Authority (local), under the Regulatory Reform (Fire Safety) Order 2005"
          :legal-basis "Regulatory Reform (Fire Safety) Order 2005; Health and Safety at Work etc. Act 1974"
          :national-spec "Fire risk assessment and means-of-escape requirements for premises open to the public"
          :provenance "https://www.gov.uk/workplace-fire-safety-your-responsibilities"
          :required-evidence ["Safety-inspection record"
                              "Occupancy-certification record"
                              "Egress-plan documentation"
                              "Operating-license verification"]}
   "DEU" {:name "Germany"
          :owner-authority "Bauaufsichtsbehörden der Länder (state building-supervisory authorities)"
          :legal-basis "Versammlungsstättenverordnung (VStättVO, Assembly Venue Ordinance)"
          :national-spec "VStättVO Anforderungen an Rettungswege und zulässige Besucherzahl"
          :provenance "https://www.bauministerkonferenz.de/"
          :required-evidence ["Prüfbericht (safety-inspection record)"
                              "Besucherzahlnachweis (occupancy-certification record)"
                              "Rettungswegplan (egress-plan documentation)"
                              "Betriebserlaubnis (operating-license verification)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to resume
  operation on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions actually
  have a spec-basis entry. Never report a missing jurisdiction as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-9329 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `recreation.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
