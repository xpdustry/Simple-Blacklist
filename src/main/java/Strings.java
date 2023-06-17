import arc.struct.Seq;


public class Strings extends arc.util.Strings {
  public static String rJust(String str, int newLenght) { return rJust(str, newLenght, " "); }
  public static String rJust(String str, int newLenght, String filler) {
    if (filler.length() == 0) return str; // Cannot fill, so return initial string
    return repeat(filler, (newLenght-str.length())/filler.length())+filler.substring(0, (newLenght-str.length())%filler.length())+str;
  }
  public static Seq<String> rJust(Seq<String> list, int newLenght) { return rJust(list, newLenght, " "); }
  public static Seq<String> rJust(Seq<String> list, int newLenght, String filler) {
    return list.map(str -> rJust(str, newLenght, filler));
  }

  public static String lJust(String str, int newLenght) { return lJust(str, newLenght, " "); }
  public static String lJust(String str, int newLenght, String filler) {
    if (filler.length() == 0) return str; // Cannot fill, so return initial string
    return str+repeat(filler, (newLenght-str.length())/filler.length())+filler.substring(0, (newLenght-str.length())%filler.length());
  }
  public static Seq<String> lJust(Seq<String> list, int newLenght) { return lJust(list, newLenght, " "); }
  public static Seq<String> lJust(Seq<String> list, int newLenght, String filler) {
    return list.map(str -> lJust(str, newLenght, filler));
  }

  public static String mJust(String left, String right, int newLenght) { return mJust(left, right, newLenght, " "); }
  public static String mJust(String left, String right, int newLenght, String filler) {
    if (filler.length() == 0) return left+right; // Cannot fill, so return concatened sides
    int s = newLenght-left.length()-right.length();
    return left+repeat(filler, s/filler.length())+filler.substring(0, s%filler.length())+right;
  }
  public static Seq<String> mJust(Seq<String> left, Seq<String> right, int newLenght) { return mJust(left, right, newLenght, " "); }
  public static Seq<String> mJust(Seq<String> left, Seq<String> right, int newLenght, String filler) {
    Seq<String> arr = new Seq<>(Integer.max(left.size, right.size));
    int i = 0;

    for (; i<Integer.min(left.size, right.size); i++) arr.add(mJust(left.get(i), right.get(i), newLenght, filler));
    // Fill the rest
    for (; i<left.size; i++) arr.add(lJust(left.get(i), newLenght, filler));
    for (; i<right.size; i++) arr.add(rJust(right.get(i), newLenght, filler));
    
    return arr;
  }

  public static String repeat(String str, int count) {
    String result = "";
    while (count-- > 0) result+=str;
    return result;
  }

  public static int bestLength(Iterable<? extends String> list) {
    int best = 0;
  
    for (String i : list) {
      if (i.length() > best) best = i.length();
    }

    return best;
  }

  public static String normalise(String str) {
    return stripColors(stripGlyphs(str));
  }

  public static int binary2integer(boolean... list) {
    int out = 0;

    for (int i=0; i<list.length; i++) {
      out |= list[i] ? 1 : 0;
      out <<= 1;
    }

    return out >> 1;
  }

  public static boolean[] integer2binary(int number) {
    // Check value because 0 have a negative size  
    if (number == 0) return new boolean[]{false};
      
    int size = (int) (Math.log(number)/Math.log(2)+1);
    boolean[] out = new boolean[size];
    
    while (size-- > 0) {
      out[size] = (number & 1) != 0;
      number >>= 1;
    }

    return out;
  }
}
