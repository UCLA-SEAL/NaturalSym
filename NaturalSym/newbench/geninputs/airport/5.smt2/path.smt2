
(set-logic QF_ASNIA)
(set-option :produce-models true)
(set-option :strings-exp true)


(define-fun isinteger ((x!1 String)) Bool (str.in_re x!1 (re.+ (re.range "0" "9")))  )
(define-fun notinteger ((x!1 String)) Bool (not (isinteger x!1)) )
(define-fun real.str.from_int ((i Int)) String (ite (< i 0) (str.++ "-" (str.from_int (- i))) (str.from_int i)))
(define-fun real.str.to_int ((i String)) Int (ite (= (str.substr i 0 1) "-") (- (str.to_int (str.substr i 1 (- (str.len i) 1)))) (str.to_int i)))

(declare-fun x8 () String)
(declare-fun input1_d1 () String)
(declare-fun x3 () String)
(declare-fun input1_d4 () String)
(declare-fun x9 () Int)
(declare-fun input1_d3 () String)
(declare-fun input1 () String)
(declare-fun input2 () String)
(declare-fun input2_d1 () String)
(declare-fun input2_d0 () String)
(declare-fun input1_d0 () String)
(declare-fun x4 () String)
(declare-fun input1_d2 () String)



     
(assert (= input2 (str.++ input2_d0 (str.++ "," input2_d1)))); splitHandler input2 2
(assert (= input1 (str.++ (str.++ (str.++ (str.++ input1_d0 (str.++ "," input1_d1)) (str.++ "," input1_d2)) (str.++ "," input1_d3)) (str.++ "," input1_d4)))); splitHandler input1 5
(assert (>= (str.len input1_d2) 5)) 
(assert  (and (=  x3 input1_d0 )  (and (=  x4 ( str.++ ( str.++ ( str.++  ""  input1_d3 )  ","  ) input1_d4 ) )  (and (=  x8 input2_d0 ) (=  x9 1 ) ) ) ) )
(assert  (and (>  ( str.to_int input1_d1  ) 1000 )  (and (=  ( str.substr input1_d2  3 (- 5 3) )  "CA"  )  (and (isinteger  input1_d1 )  (and  (not (=  input2_d1  "closed"  ))  (not (=  x3 x8 )) ) ) ) ) )
(check-sat)
(get-model)
