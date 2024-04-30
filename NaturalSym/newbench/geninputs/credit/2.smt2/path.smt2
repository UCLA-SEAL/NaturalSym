
(set-logic QF_ASNIA)
(set-option :produce-models true)
(set-option :strings-exp true)


(define-fun isinteger ((x!1 String)) Bool (str.in_re x!1 (re.+ (re.range "0" "9")))  )
(define-fun notinteger ((x!1 String)) Bool (not (isinteger x!1)) )
(define-fun real.str.from_int ((i Int)) String (ite (< i 0) (str.++ "-" (str.from_int (- i))) (str.from_int i)))
(define-fun real.str.to_int ((i String)) Int (ite (= (str.substr i 0 1) "-") (- (str.to_int (str.substr i 1 (- (str.len i) 1)))) (str.to_int i)))

(declare-fun input1_P1_d0 () String)
(declare-fun x4_P1 () Int)
(declare-fun x8 () Int)
(declare-fun input1_P2 () String)
(declare-fun input1_P2_d2 () String)
(declare-fun x15 () String)
(declare-fun x3_P2 () String)
(declare-fun x5 () Int)
(declare-fun x6 () Int)
(declare-fun input1_P2_d0 () String)
(declare-fun input1_P2_d1 () String)
(declare-fun input1_P1_d3 () String)
(declare-fun x3_P1 () String)
(declare-fun input1_P1_d1 () String)
(declare-fun input1_P2_d3 () String)
(declare-fun x4_P2 () Int)
(declare-fun input1_P1 () String)
(declare-fun input1_P1_d2 () String)
(declare-fun x10 () (Array Int Int))



     
(assert (= input1_P1 (str.++ (str.++ (str.++ input1_P1_d0 (str.++ "," input1_P1_d1)) (str.++ "," input1_P1_d2)) (str.++ "," input1_P1_d3)))); splitHandler input1_P1 4
(assert (= input1_P2 (str.++ (str.++ (str.++ input1_P2_d0 (str.++ "," input1_P2_d1)) (str.++ "," input1_P2_d2)) (str.++ "," input1_P2_d3)))); splitHandler input1_P2 4
(assert  (and (=  x4_P1 ( select  x10 0 ) )  (and (=  x4_P2 ( select  x10 1 ) )  (and (=  x3_P1 input1_P1_d3 )  (and (=  x4_P1 ( str.to_int input1_P1_d1  ) )  (and (=  x3_P2 input1_P2_d3 )  (and (=  x4_P2 ( str.to_int input1_P2_d1  ) ) (=  x15 ( str.++ ( str.++ ( str.++  ""  x3_P1 )  ":"  ) ( str.from_int x5  ) ) ) ) ) ) ) ) ) )
(assert  (and (=  input1_P1_d2  "new car"  )  (and (isinteger  input1_P1_d1 )  (and (=  input1_P2_d2  "new car"  )  (and (isinteger  input1_P2_d1 )  (and (=  x3_P2 x3_P1 )  (and (>  x5 x6 )  (and (=  ( select  x10 1 ) x6 )  (and (>=  1 0 )  (and (<  1 x8 )  (and (=  ( select  x10 0 ) x5 )  (and (>=  0 0 )  (and (<  0 x8 )  (and (>=  x8 0 ) (>  x5 1500 ) ) ) ) ) ) ) ) ) ) ) ) ) ) )
(check-sat)
(get-model)
