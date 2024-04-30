
(set-logic QF_ASNIA)
(set-option :produce-models true)
(set-option :strings-exp true)


(define-fun isinteger ((x!1 String)) Bool (str.in_re x!1 (re.+ (re.range "0" "9")))  )
(define-fun notinteger ((x!1 String)) Bool (not (isinteger x!1)) )
(define-fun real.str.from_int ((i Int)) String (ite (< i 0) (str.++ "-" (str.from_int (- i))) (str.from_int i)))
(define-fun real.str.to_int ((i String)) Int (ite (= (str.substr i 0 1) "-") (- (str.to_int (str.substr i 1 (- (str.len i) 1)))) (str.to_int i)))

(declare-fun input2 () String)
(declare-fun input1_d1 () String)
(declare-fun input2_d1 () String)
(declare-fun x2 () Int)
(declare-fun x1 () String)
(declare-fun input2_d0 () String)
(declare-fun x5 () Int)
(declare-fun x10 () Int)
(declare-fun input1 () String)
(declare-fun input1_d0 () String)
(declare-fun x4 () String)


(assert (= input2 (str.++ (str.++ input2_d0 ","  )  input2_d1))) ; splitHandler input2
(assert (= input1 (str.++ (str.++ input1_d0 ","  )  input1_d1))) ; splitHandler input1

(assert  (and (=  x1 input1_d0 )  (and (=  x2 ( str.to_int input1_d1  ) )  (and (=  x4 input2_d0 )  (and (=  x5 ( str.to_int input2_d1  ) ) (=  x10 (+  x5 x2 ) ) ) ) ) ) )
(assert  (and (>=  x10 60 )  (and (isinteger  input1_d1 )  (and (isinteger  input2_d1 ) (=  x1 x4 ) ) ) ) )
(check-sat)
(get-model)
     