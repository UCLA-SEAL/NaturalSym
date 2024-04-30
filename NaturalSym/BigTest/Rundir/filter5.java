public class filter5 {
 static final boolean apply(String row_t1, int row_t2){
  String name=row_t1;
  int total=row_t2;
  return total < 60;
}
	public static void main(String[] args) { 
		apply("1",1);
	}
}