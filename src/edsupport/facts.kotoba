(ns edsupport.facts
  "Per-jurisdiction educational-support-services regulatory catalog --
  the G2-style spec-basis table the Support Services Governor checks
  every `:assessment/verify` proposal against ('did the advisor cite
  an OFFICIAL public source for this jurisdiction's educational-
  testing/counseling/student-record framework, or did it invent one?').

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.

  Seed values are drawn from each jurisdiction's official student-
  records/educational-testing authority (see `:provenance`); they are
  a STARTING catalog, not a from-scratch survey of all ~194
  jurisdictions. Extending coverage is additive: add one map to
  `catalog`, cite a real source, done -- never invent a jurisdiction's
  requirements to make coverage look bigger.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` mirrors the client-
  consent/assessment-record/counselor-qualification-verification/
  placement-completion evidence set this blueprint's own Offer names;
  `:legal-basis` / `:owner-authority` / `:provenance` are the G2
  citation the governor requires before any `:actuation/finalize-
  placement` proposal can commit."
  {"JPN" {:name "Japan"
          :owner-authority "文部科学省 (Ministry of Education, Culture, Sports, Science and Technology)"
          :legal-basis "学校教育法施行規則 (School Education Act Enforcement Regulations) -- スクールカウンセラー等活用事業実施要領"
          :national-spec "スクールカウンセラー等の学校配置およびカウンセリング記録の取扱いに関する実施要領"
          :provenance "https://www.mext.go.jp/a_menu/shotou/seitoshidou/1362275.htm"
          :required-evidence ["保護者同意記録 (client-consent-record)"
                              "評価記録 (assessment-record)"
                              "カウンセラー資格確認記録 (counselor-qualification-verification-record)"
                              "配置完了記録 (placement-completion-record)"]}
   "USA" {:name "United States"
          :owner-authority "U.S. Department of Education"
          :legal-basis "Family Educational Rights and Privacy Act (FERPA), 20 U.S.C. § 1232g -- American School Counselor Association (ASCA) Ethical Standards for School Counselors"
          :national-spec "Educational record confidentiality and counselor ethical-practice requirements for assessment and referral"
          :provenance "https://studentprivacy.ed.gov/ferpa"
          :required-evidence ["Client consent record"
                              "Assessment record"
                              "Counselor-qualification-verification record"
                              "Placement-completion record"]}
   "GBR" {:name "United Kingdom"
          :owner-authority "Department for Education (DfE)"
          :legal-basis "Keeping Children Safe in Education (statutory guidance) -- UK GDPR / Data Protection Act 2018"
          :national-spec "Mandatory DBS (Disclosure and Barring Service) checks for staff working with children, and student-data handling requirements"
          :provenance "https://www.gov.uk/government/publications/keeping-children-safe-in-education--2"
          :required-evidence ["Client consent record"
                              "Assessment record"
                              "Counselor-qualification-verification record"
                              "Placement-completion record"]}
   "DEU" {:name "Germany"
          :owner-authority "Kultusministerium (state education ministry)"
          :legal-basis "Datenschutz-Grundverordnung (DSGVO) -- erweitertes Führungszeugnis (extended certificate of good conduct) requirement for staff working with minors"
          :national-spec "Schülerdatenschutz und Nachweispflicht der fachlichen Qualifikation für Beratungslehrkräfte"
          :provenance "https://www.kmk.org/"
          :required-evidence ["Einwilligungsprotokoll (client-consent-record)"
                              "Bewertungsprotokoll (assessment-record)"
                              "Qualifikationsnachweis (counselor-qualification-verification-record)"
                              "Vermittlungsabschlussprotokoll (placement-completion-record)"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to finalize a
  placement on it."
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
      :note (str "cloud-itonami-isic-8550 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `edsupport.facts/catalog`, "
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
