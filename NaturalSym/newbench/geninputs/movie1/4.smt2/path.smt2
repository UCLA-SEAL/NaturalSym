
(set-logic QF_ASNIA)
(set-option :produce-models true)
(set-option :strings-exp true)


(define-fun isinteger ((x!1 String)) Bool (str.in_re x!1 (re.+ (re.range "0" "9")))  )
(define-fun notinteger ((x!1 String)) Bool (not (isinteger x!1)) )
(define-fun real.str.from_int ((i Int)) String (ite (< i 0) (str.++ "-" (str.from_int (- i))) (str.from_int i)))
(define-fun real.str.to_int ((i String)) Int (ite (= (str.substr i 0 1) "-") (- (str.to_int (str.substr i 1 (- (str.len i) 1)))) (str.to_int i)))

(declare-fun input1_d1 () String)
(declare-fun input1_d3 () String)
(declare-fun input1 () String)
(declare-fun input1_d0 () String)
(declare-fun input1_d2 () String)




     
(declare-fun input1_d4 () String)
(assert (= input1 (str.++ (str.++ (str.++ (str.++ input1_d0 (str.++ "," input1_d1)) (str.++ "," input1_d2)) (str.++ "," input1_d3)) (str.++ "," input1_d4)))); splitHandler input1 5
(assert  (and (<  ( str.to_int input1_d3  ) 4 )  (and (isinteger  input1_d3 )  (and (<  ( str.to_int input1_d1  ) 1960 )  (and (>  ( str.to_int input1_d1  ) 1900 ) (isinteger  input1_d1 ) ) ) ) ) )
(check-sat)
(get-model)
