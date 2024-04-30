
(set-logic QF_ASNIA)
(set-option :produce-models true)
(set-option :strings-exp true)


(define-fun isinteger ((x!1 String)) Bool (str.in_re x!1 (re.+ (re.range "0" "9")))  )
(define-fun notinteger ((x!1 String)) Bool (not (isinteger x!1)) )
(define-fun real.str.from_int ((i Int)) String (ite (< i 0) (str.++ "-" (str.from_int (- i))) (str.from_int i)))
(define-fun real.str.to_int ((i String)) Int (ite (= (str.substr i 0 1) "-") (- (str.to_int (str.substr i 1 (- (str.len i) 1)))) (str.to_int i)))

(declare-fun input2 () String)
(declare-fun input2_d1 () String)
(declare-fun input2_d0 () String)




     
(assert (= input2 (str.++ input2_d0 (str.++ "," input2_d1)))); splitHandler input2 2
(assert (=  input2_d1  "closed"  ) )
(check-sat)
(get-model)
