(ns periodic.core-test
  (:require [clojure.test :refer :all]
            [periodic.core :refer :all]
            [clojure.core.async :as async]
            [clojure.math.numeric-tower :as math]))

(deftest accuracy
  (let [source-chan (async/to-chan (repeat 1000 :value))
        error-chan (async/chan)
        errors (async/into [] error-chan)]

    (async/pipe (periodic< 10 source-chan :units :ms :error error-chan)
                (async/chan (async/dropping-buffer 1)))

    (let [error-coll (async/<!! errors)
          n (count error-coll)
          {succeeded true} (group-by #(< (math/abs %) 2/1000) error-coll)
          pass-proportion (/ (count succeeded) n)
          average-error (/ (reduce + error-coll) n)]

      (is (> pass-proportion 0.95)
          "greater than 95% of timing errors are less than 2 ms")

      (is (< average-error 1/1000000)
          "the average timing error is less than 1 µs"))))

(deftest test-periodic<
  (let [source (range 10)
        source-chan (async/to-chan source)
        target-chan (periodic< 10 source-chan :units :ms)
        target (async/into [] target-chan)]

    (is (= source (async/<!! target)))))

(deftest test-periodic>
  (let [source (range 10)
        source-chan (async/to-chan source)
        target-chan (async/chan)
        target (async/into [] target-chan)]

    (async/pipe source-chan (periodic> 10 target-chan :units :ms))

    (is (= source (async/<!! target)))))
