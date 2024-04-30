
(set-logic QF_ASNIA)
(set-option :produce-models true)
(set-option :strings-exp true)


(define-fun isinteger ((x!1 String)) Bool (str.in_re x!1 (re.+ (re.range "0" "9")))  )
(define-fun notinteger ((x!1 String)) Bool (not (isinteger x!1)) )
(define-fun real.str.from_int ((i Int)) String (ite (< i 0) (str.++ "-" (str.from_int (- i))) (str.from_int i)))
(define-fun real.str.to_int ((i String)) Int (ite (= (str.substr i 0 1) "-") (- (str.to_int (str.substr i 1 (- (str.len i) 1)))) (str.to_int i)))

(declare-fun input1_P1_d0 () String)
(declare-fun x5_P1 () String)
(declare-fun x8 () Int)
(declare-fun input1_P2 () String)
(declare-fun input1_P2_d2 () String)
(declare-fun x15 () String)
(declare-fun x9 () Int)
(declare-fun input1_P2_d0 () String)
(declare-fun input1_P2_d1 () String)
(declare-fun x11 () Int)
(declare-fun x5_P2 () String)
(declare-fun input1_P1_d3 () String)
(declare-fun input1_P1_d1 () String)
(declare-fun input1_P2_d3 () String)
(declare-fun x6_P2 () Int)
(declare-fun x12 () (Array Int Int))
(declare-fun input1_P1 () String)
(declare-fun input1_P1_d2 () String)
(declare-fun x6_P1 () Int)
(declare-fun x10 () Int)



     
(declare-fun input1_P1_d4 () String)
(declare-fun input1_P2_d4 () String)
(assert (= input1_P1 (str.++ (str.++ (str.++ (str.++ input1_P1_d0 (str.++ "," input1_P1_d1)) (str.++ "," input1_P1_d2)) (str.++ "," input1_P1_d3)) (str.++ "," input1_P1_d4)))); splitHandler input1_P1 5
(assert (= input1_P2 (str.++ (str.++ (str.++ (str.++ input1_P2_d0 (str.++ "," input1_P2_d1)) (str.++ "," input1_P2_d2)) (str.++ "," input1_P2_d3)) (str.++ "," input1_P2_d4)))); splitHandler input1_P2 5
(assert  (and (=  x6_P1 ( select  x12 0 ) )  (and (=  x6_P2 ( select  x12 1 ) )  (and (=  x5_P1 input1_P1_d2 )  (and (=  x6_P1 1 )  (and (=  x5_P2 input1_P2_d2 )  (and (=  x6_P2 1 )  (and (=  x11 (+  x8 x10 ) ) (=  x15 ( str.++ ( str.++ ( str.++  ""  x5_P1 )  ":"  ) ( str.from_int x11  ) ) ) ) ) ) ) ) ) ) )
(assert  (and (<  ( str.to_int input1_P1_d1  ) 1960 )  (and (>  ( str.to_int input1_P1_d1  ) 1900 )  (and (isinteger  input1_P1_d1 )  (and (>=  ( str.to_int input1_P1_d3  ) 4 )  (and (isinteger  input1_P1_d3 )  (and (<  ( str.to_int input1_P2_d1  ) 1960 )  (and (>  ( str.to_int input1_P2_d1  ) 1900 )  (and (isinteger  input1_P2_d1 )  (and (>=  ( str.to_int input1_P2_d3  ) 4 )  (and (isinteger  input1_P2_d3 )  (and (=  x5_P2 x5_P1 )  (and (=  ( select  x12 1 ) x8 )  (and (>=  1 0 )  (and (<  1 x9 )  (and (=  ( select  x12 0 ) x10 )  (and (>=  0 0 )  (and (<  0 x9 ) (>=  x9 0 ) ) ) ) ) ) ) ) ) ) ) ) ) ) ) ) ) ) )
(check-sat)
(get-model)
