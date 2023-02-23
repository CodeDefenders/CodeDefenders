public class CaseInsensitiveStringTest {
    public static void main(String[] args) {
        CaseInsensitiveString str = new CaseInsensitiveString();

        char[] buffer = "abcd".toCharArray();
        str.setCharBuffer(buffer, 0, buffer.length);

        str.equals("ABCD");
        str.equals("ABCDE");

        CaseInsensitiveString str2 = new CaseInsensitiveString("ABCD");
        str.compareTo(str2);
    }
}
