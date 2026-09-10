(ns edsupport.store
  "SSoT for the educational-support-services actor, behind a `Store`
  protocol so the backend is a swap, not a rewrite -- the same seam
  every prior `cloud-itonami-isic-*` actor in this fleet uses:

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/edsupport/store_contract_test.clj), which is the whole
  point: the actor, the Support Services Governor and the audit
  ledger never know which SSoT they run on.

  Like `clinic.store`'s/`personalservice.store`'s simpler entities, a
  CLIENT is acted on directly by the ONE actuation op -- no
  dynamically-filed sub-record, and the double-finalization guard
  checks a dedicated `:placement-finalized?` boolean rather than a
  `:status` value, the same discipline `clinic.governor`'s/
  `personalservice.governor`'s guards establish.

  NOTE on naming: the protocol's per-entity accessor is `client`
  directly -- not a Clojure special form, so no `-of` suffix
  workaround was needed.

  The ledger stays append-only on every backend: 'which client was
  screened for an assessment-integrity irregularity, which client was
  screened for a cleared background check, which placement was
  finalized, on what jurisdictional basis, approved by whom' is
  always a query over an immutable log -- the audit trail a student/
  institution trusting an educational-support provider needs, and the
  evidence an operator needs if a placement decision is later
  disputed."
  (:require [edsupport.registry :as registry]
            [langchain.db :as d]
            [langchain-store.core :as ls]))

(defprotocol Store
  (client [s id])
  (all-clients [s])
  (integrity-of [s client-id] "committed assessment-integrity screening verdict for a client, or nil")
  (background-check-of [s client-id] "committed background-check screening verdict for a client, or nil")
  (assessment-of [s client-id] "committed assessment evidence checklist, or nil")
  (ledger [s])
  (placement-history [s] "the append-only placement-finalization history (edsupport.registry drafts)")
  (next-sequence [s jurisdiction] "next placement-number sequence for a jurisdiction")
  (client-already-finalized? [s client-id] "has this client's placement already been finalized?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-clients [s clients] "replace/seed the client directory (map id->client)"))

;; ----------------------------- demo data -----------------------------

(defn demo-data
  "A small, self-contained client set so the actor + tests run
  offline."
  []
  {:clients
   {"client-1" {:id "client-1" :client-name "Sato Kenji"
               :assessment-administration-irregularity-unresolved? false
               :background-check-not-cleared? false
               :placement-finalized? false :jurisdiction "JPN" :status :intake}
    "client-2" {:id "client-2" :client-name "Atlantis Doe"
               :assessment-administration-irregularity-unresolved? false
               :background-check-not-cleared? false
               :placement-finalized? false :jurisdiction "ATL" :status :intake}
    "client-3" {:id "client-3" :client-name "鈴木花子"
               :assessment-administration-irregularity-unresolved? true
               :background-check-not-cleared? false
               :placement-finalized? false :jurisdiction "JPN" :status :intake}
    "client-4" {:id "client-4" :client-name "田中一郎"
               :assessment-administration-irregularity-unresolved? false
               :background-check-not-cleared? true
               :placement-finalized? false :jurisdiction "JPN" :status :intake}}})

;; ----------------------------- shared commit logic -----------------------------

(defn- finalize-placement!
  "Backend-agnostic `:client/mark-finalized` -- looks up the client via
  the protocol and drafts the placement-finalization record, and
  returns {:result .. :client-patch ..} for the caller to persist."
  [s client-id]
  (let [c (client s client-id)
        seq-n (next-sequence s (:jurisdiction c))
        result (registry/register-placement-finalization client-id (:jurisdiction c) seq-n)]
    {:result result
     :client-patch {:placement-finalized? true
                    :placement-number (get result "placement_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (client [_ id] (get-in @a [:clients id]))
  (all-clients [_] (sort-by :id (vals (:clients @a))))
  (integrity-of [_ id] (get-in @a [:integrity id]))
  (background-check-of [_ id] (get-in @a [:background-checks id]))
  (assessment-of [_ client-id] (get-in @a [:assessments client-id]))
  (ledger [_] (:ledger @a))
  (placement-history [_] (:placements @a))
  (next-sequence [_ jurisdiction] (get-in @a [:sequences jurisdiction] 0))
  (client-already-finalized? [_ client-id] (boolean (get-in @a [:clients client-id :placement-finalized?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :client/upsert
      (swap! a update-in [:clients (:id value)] merge value)

      :assessment/set
      (swap! a assoc-in [:assessments (first path)] payload)

      :integrity/set
      (swap! a assoc-in [:integrity (first path)] payload)

      :background-check/set
      (swap! a assoc-in [:background-checks (first path)] payload)

      :client/mark-finalized
      (let [client-id (first path)
            {:keys [result client-patch]} (finalize-placement! s client-id)
            jurisdiction (:jurisdiction (client s client-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:sequences jurisdiction] (fnil inc 0))
                       (update-in [:clients client-id] merge client-patch)
                       (update :placements registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-clients [s clients] (when (seq clients) (swap! a assoc :clients clients)) s))

(defn seed-db
  "A MemStore seeded with the demo client set. The deterministic
  default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {} :integrity {} :background-checks {} :ledger [] :sequences {}
                           :placements []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Compound values (assessment/integrity/background-check payloads,
  ledger facts, placement records) are stored as EDN strings so
  `langchain.db` doesn't expand them into sub-entities -- the same
  convention every sibling actor's store uses."
  {:client/id                    {:db/unique :db.unique/identity}
   :assessment/client-id          {:db/unique :db.unique/identity}
   :integrity/client-id            {:db/unique :db.unique/identity}
   :background-check/client-id      {:db/unique :db.unique/identity}
   :ledger/seq                       {:db/unique :db.unique/identity}
   :placement/seq                     {:db/unique :db.unique/identity}
   :sequence/jurisdiction               {:db/unique :db.unique/identity}})

(defn- client->tx [{:keys [id client-name
                          assessment-administration-irregularity-unresolved?
                          background-check-not-cleared? placement-finalized?
                          jurisdiction status placement-number]}]
  (cond-> {:client/id id}
    client-name                                       (assoc :client/client-name client-name)
    (some? assessment-administration-irregularity-unresolved?)
    (assoc :client/assessment-administration-irregularity-unresolved? assessment-administration-irregularity-unresolved?)
    (some? background-check-not-cleared?)               (assoc :client/background-check-not-cleared? background-check-not-cleared?)
    (some? placement-finalized?)                          (assoc :client/placement-finalized? placement-finalized?)
    jurisdiction                                            (assoc :client/jurisdiction jurisdiction)
    status                                                   (assoc :client/status status)
    placement-number                                          (assoc :client/placement-number placement-number)))

(def ^:private client-pull
  [:client/id :client/client-name
   :client/assessment-administration-irregularity-unresolved?
   :client/background-check-not-cleared? :client/placement-finalized?
   :client/jurisdiction :client/status :client/placement-number])

(defn- pull->client [m]
  (when (:client/id m)
    {:id (:client/id m) :client-name (:client/client-name m)
     :assessment-administration-irregularity-unresolved?
     (boolean (:client/assessment-administration-irregularity-unresolved? m))
     :background-check-not-cleared? (boolean (:client/background-check-not-cleared? m))
     :placement-finalized? (boolean (:client/placement-finalized? m))
     :jurisdiction (:client/jurisdiction m) :status (:client/status m)
     :placement-number (:client/placement-number m)}))

(defrecord DatomicStore [conn]
  Store
  (client [_ id]
    (pull->client (d/pull (d/db conn) client-pull [:client/id id])))
  (all-clients [_]
    (->> (d/q '[:find [?id ...] :where [?e :client/id ?id]] (d/db conn))
         (map #(pull->client (d/pull (d/db conn) client-pull [:client/id %])))
         (sort-by :id)))
  (integrity-of [_ id]
    (ls/dec* (d/q '[:find ?p . :in $ ?cid
                :where [?k :integrity/client-id ?cid] [?k :integrity/payload ?p]]
              (d/db conn) id)))
  (background-check-of [_ id]
    (ls/dec* (d/q '[:find ?p . :in $ ?cid
                :where [?k :background-check/client-id ?cid] [?k :background-check/payload ?p]]
              (d/db conn) id)))
  (assessment-of [_ client-id]
    (ls/dec* (d/q '[:find ?p . :in $ ?cid
                :where [?a :assessment/client-id ?cid] [?a :assessment/payload ?p]]
              (d/db conn) client-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp ls/dec* second))))
  (placement-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :placement/seq ?s] [?e :placement/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp ls/dec* second))))
  (next-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :sequence/jurisdiction ?j] [?e :sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (client-already-finalized? [s client-id]
    (boolean (:placement-finalized? (client s client-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :client/upsert
      (d/transact! conn [(client->tx value)])

      :assessment/set
      (d/transact! conn [{:assessment/client-id (first path) :assessment/payload (ls/enc payload)}])

      :integrity/set
      (d/transact! conn [{:integrity/client-id (first path) :integrity/payload (ls/enc payload)}])

      :background-check/set
      (d/transact! conn [{:background-check/client-id (first path) :background-check/payload (ls/enc payload)}])

      :client/mark-finalized
      (let [client-id (first path)
            {:keys [result client-patch]} (finalize-placement! s client-id)
            jurisdiction (:jurisdiction (client s client-id))
            next-n (inc (next-sequence s jurisdiction))]
        (d/transact! conn
                     [(client->tx (assoc client-patch :id client-id))
                      {:sequence/jurisdiction jurisdiction :sequence/next next-n}
                      {:placement/seq (count (placement-history s)) :placement/record (ls/enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (ls/enc fact)}])
    fact)
  (with-clients [s clients]
    (when (seq clients) (d/transact! conn (mapv client->tx (vals clients)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:clients ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [clients]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-clients s clients))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo client set -- the Datomic-
  backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
