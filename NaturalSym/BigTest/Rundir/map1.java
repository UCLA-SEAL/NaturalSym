public class map1 {
 static final map1 apply(String row){
  return new map1(row.split(",")[0],(Integer.parseInt(row.split(",")[1])));
}
    String s1; int i1; map1 o1;
    String s2; int i2; map1 o2;
    String s3, s4, s5, s6;
    public map1(String x1, String x2) {s1 = x1; s2 = x2;}
    public map1(String x1, String x2, String x3) {s1 = x1; s2 = x2; s3 = x3;}
    public map1(String x1, String x2, String x3, String x4) {s1 = x1; s2 = x2; s3 = x3; s4 = x4;}
    public map1(String x1, String x2, String x3, String x4, String x5) {s1 = x1; s2 = x2; s3 = x3; s4 = x4; s5 = x5;}
    public map1(int x1, String x2) {i1 = x1; s2 = x2;}
    public map1(int x1, int x2) {i1 = x1; i2 = x2;}
    public map1(int x1, map1 x2) {i1 = x1; o2 = x2;}
    public map1(String x1, int x2) {s1 = x1; i2 = x2;}
    public map1(String x1, map1 x2) {s1 = x1; o2 = x2;}
    public map1(map1 x1, String x2) {o1 = x1; s2 = x2;}
    public map1(map1 x1, int x2) {o1 = x1; i2 = x2;}
    public map1(map1 x1, map1 x2) {o1 = x1; o2 = x2;}
	public static void main(String[] args) { 
		apply("1");
	}
}