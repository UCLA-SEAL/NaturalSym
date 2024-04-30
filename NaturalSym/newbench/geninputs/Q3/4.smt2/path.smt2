
(set-logic QF_ASNIA)
(set-option :produce-models true)
(set-option :strings-exp true)


(define-fun isinteger ((x!1 String)) Bool (str.in_re x!1 (re.+ (re.range "0" "9")))  )
(define-fun notinteger ((x!1 String)) Bool (not (isinteger x!1)) )
(define-fun real.str.from_int ((i Int)) String (ite (< i 0) (str.++ "-" (str.from_int (- i))) (str.from_int i)))
(define-fun real.str.to_int ((i String)) Int (ite (= (str.substr i 0 1) "-") (- (str.to_int (str.substr i 1 (- (str.len i) 1)))) (str.to_int i)))

(declare-fun input3_d7 () String)
(declare-fun input3_d12 () String)
(declare-fun input3_d13 () String)
(declare-fun input3_d10 () String)
(declare-fun input3_d1 () String)
(declare-fun input3_d4 () String)
(declare-fun input3_d6 () String)
(declare-fun input3_d5 () String)
(declare-fun input3_d11 () String)
(declare-fun input3_d8 () String)
(declare-fun input3_d9 () String)
(declare-fun input3_d0 () String)
(declare-fun input3 () String)
(declare-fun input3_d3 () String)
(declare-fun input3_d2 () String)




     
(declare-fun input3_d14 () String)
(declare-fun input3_d15 () String)
(declare-fun input3_d16 () String)
(declare-fun input3_d17 () String)
(declare-fun input3_d18 () String)
(declare-fun input3_d19 () String)
(declare-fun input3_d20 () String)
(declare-fun input3_d21 () String)
(assert (= input3 (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ (str.++ input3_d0 (str.++ "," input3_d1)) (str.++ "," input3_d2)) (str.++ "," input3_d3)) (str.++ "," input3_d4)) (str.++ "," input3_d5)) (str.++ "," input3_d6)) (str.++ "," input3_d7)) (str.++ "," input3_d8)) (str.++ "," input3_d9)) (str.++ "," input3_d10)) (str.++ "," input3_d11)) (str.++ "," input3_d12)) (str.++ "," input3_d13)) (str.++ "," input3_d14)) (str.++ "," input3_d15)) (str.++ "," input3_d16)) (str.++ "," input3_d17)) (str.++ "," input3_d18)) (str.++ "," input3_d19)) (str.++ "," input3_d20)) (str.++ "," input3_d21)))); splitHandler input3 22
(assert  (not (=  input3_d13  "1"  )) )
(check-sat)
(get-model)
