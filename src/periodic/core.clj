(ns periodic.core
  (:require [clojure.core.async :as async
             :refer [chan go-loop put! >! <! timeout close! pipe]]
            [clojure.core.async.impl.protocols :as p]))

(def ^:private ->s
  {:nHz #(/ 1 1000000000 %)
   :µHz #(/ 1 1000000 %)
   :uHz #(/ 1 1000000 %)
   :mHz #(/ 1 1000 %)
   :Hz  #(/ 1 %)
   :kHz #(/ 1 1/1000 %)
   :MHz #(/ 1 1/1000000 %)
   :GHz #(/ 1 1/1000000000 %)

   :ns #(* 1/1000000000 %)
   :µs #(* 1/1000000 %)
   :us #(* 1/1000000 %)
   :ms #(* 1/1000 %)
   :s  identity
   :ks #(* 1000 %)
   :Ms #(* 1000000 %)
   :Gs #(* 1000000000 %)})

(def ^:private default
  {:units :s
   :kp 1/100
   :ki 1/1000
   :kd 1/10000
   :error (chan)
   :buf-or-n 1
   :period-error-limit 2})

(defn periodic<
  "Takes a period and a source channel, and returns a channel which is
  periodically supplied with values from the source channel. Returned
  channel closes when source channel closes. Uses a PID controller to
  adapt to blocking delays.

  Options are passed as :key val. Supported options:

    :units val - period unit (:nHz :µHz :uHz :mHz :Hz :kHz :MHz :GHz :ns
        :µs :us :ms :s :ks :Ms :Gs); defaults to :s

    :kp val - PID controller proportional gain; defaults to 1/100

    :ki val - PID controller integral gain; defaults to 1/1000

    :kd val - PID controller derivative gain; defaults to 1/10000

    :error val - timing error channel

    :buf-or-n val - buffer or buffer size for returned channel

    :period-error-limit - reset error to zero when timing error exceeds
        this many periods. Useful for preventing overcorrection when source or
        target channels block for an extended time; defaults to 2 periods"

  [period in & {:keys [units kp ki kd error buf-or-n period-error-limit]
                :or {units (default :units)
                     kp (default :kp)
                     ki (default :ki)
                     kd (default :kd)
                     error (default :error)
                     buf-or-n (default :buf-or-n)
                     period-error-limit (default :period-error-limit)}}]

  (let [out (chan buf-or-n)
        period (* ((units ->s) period) 1000000000)]

    (go-loop [start (System/nanoTime)
              offset 0
              sum 0
              i0 0
              i1 0]

      (if-let [v (<! in)]
        (do
          (>! out v)

          (let [P (* kp i0)
                I (* ki sum)
                D (* kd (- i0 i1))
                offset (+ offset P I D)
                interval (/ (- period offset) 1000000)]

            (when (pos? interval) (<! (timeout interval)))

            (let [end (System/nanoTime)
                  actual (- end start)
                  residual (- actual period)]
              (put! error ((:ns ->s) residual))
              (if (> residual (* period period-error-limit))
                (recur (System/nanoTime) 0 0 0 0)
                (recur (System/nanoTime) offset
                       (+ sum residual) i1 residual)))))

        (do (close! out)
            (close! error))))

    out))

(defn periodic>
  "Like periodic<, but takes a period and a target channel and returns
  a channel which periodically supplies values to the target
  channel. Takes the same options as periodic<"
  
  [period out & {:keys [units kp ki kd error buf-or-n period-error-limit]
                 :or {units (default :units)
                      kp (default :kp)
                      ki (default :ki)
                      kd (default :kd)
                      error (default :error)
                      buf-or-n (default :buf-or-n)
                      period-error-limit (default :period-error-limit)}}]

  (let [in (chan buf-or-n)]
    (pipe (periodic< period in
                     :units units
                     :kp kp :ki ki :kd kd
                     :error error
                     :period-error-limit period-error-limit)
          out)
    in))
