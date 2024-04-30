
(set-logic QF_ASNIA)
(set-option :produce-models true)
(set-option :strings-exp true)


(define-fun isinteger ((x!1 String)) Bool (str.in_re x!1 (re.+ (re.range "0" "9")))  )
(define-fun notinteger ((x!1 String)) Bool (not (isinteger x!1)) )
(define-fun real.str.from_int ((i Int)) String (ite (< i 0) (str.++ "-" (str.from_int (- i))) (str.from_int i)))
(define-fun real.str.to_int ((i String)) Int (ite (= (str.substr i 0 1) "-") (- (str.to_int (str.substr i 1 (- (str.len i) 1)))) (str.to_int i)))

(declare-fun input1_d1 () String)
(declare-fun input1_d4 () String)
(declare-fun x2 () Int)
(declare-fun x1 () String)
(declare-fun input1_d3 () String)
(declare-fun input1 () String)
(declare-fun input1_d0 () String)
(declare-fun input1_d2 () String)



     
(assert (= input1 (str.++ (str.++ (str.++ (str.++ input1_d0 (str.++ "," input1_d1)) (str.++ "," input1_d2)) (str.++ "," input1_d3)) (str.++ "," input1_d4)))); splitHandler input1 5
(assert (>= (str.len input1_d2) 5)) 
(assert (>= (str.len input1_d3) 5)) 
(assert  (and (=  x1 ( str.++ ( str.++  ""  input1_d4 ) ( str.substr input1_d2  0 (- 2 0) ) ) ) (=  x2 (-  (+  ( str.to_int ( str.substr input1_d3  3 (- 5 3) )  ) (*  ( str.to_int ( str.substr input1_d3  0 (- 2 0) )  ) 60 ) ) (+  ( str.to_int ( str.substr input1_d2  3 (- 5 3) )  ) (*  ( str.to_int ( str.substr input1_d2  0 (- 2 0) )  ) 60 ) ) ) ) ) )
(assert  (and (>=  x2 45 )  (and (>=  (-  (+  ( str.to_int ( str.substr input1_d3  3 (- 5 3) )  ) (*  ( str.to_int ( str.substr input1_d3  0 (- 2 0) )  ) 60 ) ) (+  ( str.to_int ( str.substr input1_d2  3 (- 5 3) )  ) (*  ( str.to_int ( str.substr input1_d2  0 (- 2 0) )  ) 60 ) ) ) 0 )  (and (isinteger  ( str.substr input1_d3  0 (- 2 0) ) )  (and (isinteger  ( str.substr input1_d3  3 (- 5 3) ) )  (and (isinteger  ( str.substr input1_d2  0 (- 2 0) ) ) (isinteger  ( str.substr input1_d2  3 (- 5 3) ) ) ) ) ) ) ) )
(check-sat)
(get-model)
