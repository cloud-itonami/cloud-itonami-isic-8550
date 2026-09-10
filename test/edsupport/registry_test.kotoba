(ns edsupport.registry-test
  (:require [clojure.test :refer [deftest is]]
            [edsupport.registry :as r]))

;; ----------------------------- register-placement-finalization -----------------------------

(deftest finalization-is-a-draft-not-a-real-finalization
  (let [result (r/register-placement-finalization "client-1" "JPN" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest finalization-assigns-placement-number
  (let [result (r/register-placement-finalization "client-1" "JPN" 7)]
    (is (= (get result "placement_number") "JPN-PLC-000007"))
    (is (= (get-in result ["record" "client_id"]) "client-1"))
    (is (= (get-in result ["record" "kind"]) "placement-finalization-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest finalization-validation-rules
  (is (thrown? Exception (r/register-placement-finalization "" "JPN" 0)))
  (is (thrown? Exception (r/register-placement-finalization "client-1" "" 0)))
  (is (thrown? Exception (r/register-placement-finalization "client-1" "JPN" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-placement-finalization "client-1" "JPN" 0)
        hist (r/append [] c1)
        c2 (r/register-placement-finalization "client-2" "JPN" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "JPN-PLC-000000" (get-in hist2 [0 "record_id"])))
    (is (= "JPN-PLC-000001" (get-in hist2 [1 "record_id"])))))
