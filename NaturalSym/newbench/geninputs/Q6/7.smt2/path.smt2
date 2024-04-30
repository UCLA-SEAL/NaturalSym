
(set-logic QF_ASNIA)
(set-option :produce-models true)
(set-option :strings-exp true)


(define-fun isinteger ((x!1 String)) Bool (str.in_re x!1 (re.+ (re.range "0" "9")))  )
(define-fun notinteger ((x!1 String)) Bool (not (isinteger x!1)) )
(define-fun real.str.from_int ((i Int)) String (ite (< i 0) (str.++ "-" (str.from_int (- i))) (str.from_int i)))
(define-fun real.str.to_int ((i String)) Int (ite (= (str.substr i 0 1) "-") (- (str.to_int (str.substr i 1 (- (str.len i) 1)))) (str.to_int i)))

(declare-fun input4_d1 () String)
(declare-fun input4_d2 () String)
(declare-fun input4_d8 () String)
(declare-fun input4_d4 () String)
(declare-fun input4_d7 () String)
(declare-fun input4_d3 () String)
(declare-fun input4_d5 () String)
(declare-fun input4_d0 () String)
(declare-fun input4_d6 () String)
(declare-fun input4 () String)




     
(declare-fun input4_d9 () String)
(declare-fun input4_d10 () String)
(declare-fun input4_d11 () String)
(declare-fun input4_d12 () String)
(declare-fun input4_d13 () String)
(declare-fun input4_d14 () String)
(declare-fun input4_d15 () String)
(declare-fun input4_d16 () String)
(declare-fun input4_d17 () String)
(declare-fun input4_d18 () String)
(declare-fun input4_d19 () String)
(declare-fun input4_d20 () String)
(declare-fun input4_d21 () String)
(declare-fun input4_d22 () String)
(declare-fun input4_d23 () String)
(declare-fun input4_d24 () String)
(declare-fun input4_d25 () String)
(declare-fun input4_d26 () String)
(declare-fun input4_d27 () String)
(assert (= input4 (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ input4_d0 (str.++ "," input4_d1)) (str.++ "," input4_d2)) (str.++ "," input4_d3)) (str.++ "," input4_d4)) (str.++ "," input4_d5)) (str.++ "," input4_d6)) (str.++ "," input4_d7)) (str.++ "," input4_d8)) (str.++ "," input4_d9)) (str.++ "," input4_d10)) (str.++ "," input4_d11)) (str.++ "," input4_d12)) (str.++ "," input4_d13)) (str.++ "," input4_d14)) (str.++ "," input4_d15)) (str.++ "," input4_d16)) (str.++ "," input4_d17)) (str.++ "," input4_d18)) (str.++ "," input4_d19)) (str.++ "," input4_d20)) (str.++ "," input4_d21)) (str.++ "," input4_d22)) (str.++ "," input4_d23)) (str.++ "," input4_d24)) (str.++ "," input4_d25)) (str.++ "," input4_d26)) (str.++ "," input4_d27)))); splitHandler input4 28
(assert  (and  (not (=  input4_d8  "2001"  )) (=  input4_d6  "1"  ) ) )
(check-sat)
(get-model)
