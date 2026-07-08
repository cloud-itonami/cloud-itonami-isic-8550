(ns edsupport.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [edsupport.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "Sato Kenji" (:client-name (store/client s "client-1"))))
      (is (= "JPN" (:jurisdiction (store/client s "client-1"))))
      (is (false? (:assessment-administration-irregularity-unresolved? (store/client s "client-1"))))
      (is (false? (:background-check-not-cleared? (store/client s "client-1"))))
      (is (true? (:assessment-administration-irregularity-unresolved? (store/client s "client-3"))))
      (is (true? (:background-check-not-cleared? (store/client s "client-4"))))
      (is (false? (:placement-finalized? (store/client s "client-1"))))
      (is (= ["client-1" "client-2" "client-3" "client-4"]
             (mapv :id (store/all-clients s))))
      (is (nil? (store/integrity-of s "client-1")))
      (is (nil? (store/background-check-of s "client-1")))
      (is (nil? (store/assessment-of s "client-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/placement-history s)))
      (is (zero? (store/next-sequence s "JPN")))
      (is (false? (store/client-already-finalized? s "client-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :client/upsert
                                 :value {:id "client-1" :client-name "Sato Kenji"}})
        (is (= "Sato Kenji" (:client-name (store/client s "client-1"))))
        (is (false? (:background-check-not-cleared? (store/client s "client-1"))) "unrelated field preserved"))
      (testing "assessment / integrity / background-check payloads commit and read back"
        (store/commit-record! s {:effect :assessment/set :path ["client-1"]
                                 :payload {:jurisdiction "JPN" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "JPN" :checklist ["a" "b"]} (store/assessment-of s "client-1")))
        (store/commit-record! s {:effect :integrity/set :path ["client-1"]
                                 :payload {:client-id "client-1" :assessment-administration-irregularity-unresolved? false}})
        (is (= {:client-id "client-1" :assessment-administration-irregularity-unresolved? false} (store/integrity-of s "client-1")))
        (store/commit-record! s {:effect :background-check/set :path ["client-1"]
                                 :payload {:client-id "client-1" :verdict :cleared}})
        (is (= {:client-id "client-1" :verdict :cleared} (store/background-check-of s "client-1"))))
      (testing "placement finalization drafts a record and advances the sequence"
        (store/commit-record! s {:effect :client/mark-finalized :path ["client-1"]})
        (is (= "JPN-PLC-000000" (get (first (store/placement-history s)) "record_id")))
        (is (= "placement-finalization-draft" (get (first (store/placement-history s)) "kind")))
        (is (true? (:placement-finalized? (store/client s "client-1"))))
        (is (= 1 (count (store/placement-history s))))
        (is (= 1 (store/next-sequence s "JPN")))
        (is (true? (store/client-already-finalized? s "client-1")))
        (is (false? (store/client-already-finalized? s "client-2"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/client s "nope")))
    (is (= [] (store/all-clients s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/placement-history s)))
    (is (zero? (store/next-sequence s "JPN")))
    (store/with-clients s {"x" {:id "x" :client-name "n"
                                :assessment-administration-irregularity-unresolved? false
                                :background-check-not-cleared? false
                                :placement-finalized? false :jurisdiction "JPN" :status :intake}})
    (is (= "n" (:client-name (store/client s "x"))))))
