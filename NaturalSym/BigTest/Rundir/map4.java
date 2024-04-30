public class map4 {
 static final map4 apply(String row_t1, int row_t2, int row_t3){
  String name=row_t1;
  int math=row_t2;
  int physics=row_t3;
  return new map4(name,(math + physics));
}
    String s1; int i1; map4 o1;
    String s2; int i2; map4 o2;
    String s3, s4, s5, s6;
    public map4(String x1, String x2) {s1 = x1; s2 = x2;}
    public map4(String x1, String x2, String x3) {s1 = x1; s2 = x2; s3 = x3;}
    public map4(String x1, String x2, String x3, String x4) {s1 = x1; s2 = x2; s3 = x3; s4 = x4;}
    public map4(String x1, String x2, String x3, String x4, String x5) {s1 = x1; s2 = x2; s3 = x3; s4 = x4; s5 = x5;}
    public map4(int x1, String x2) {i1 = x1; s2 = x2;}
    public map4(int x1, int x2) {i1 = x1; i2 = x2;}
    public map4(int x1, map4 x2) {i1 = x1; o2 = x2;}
    public map4(String x1, int x2) {s1 = x1; i2 = x2;}
    public map4(String x1, map4 x2) {s1 = x1; o2 = x2;}
    public map4(map4 x1, String x2) {o1 = x1; s2 = x2;}
    public map4(map4 x1, int x2) {o1 = x1; i2 = x2;}
    public map4(map4 x1, map4 x2) {o1 = x1; o2 = x2;}
	public static void main(String[] args) { 
		apply("1",1,1);
	}
}