import static utils.Utils.doCatch;

public class ByteVectorTest1 {
    public static void main(String[] args) {
        ByteVector byteVector = new ByteVector();
        byteVector.putUTF8("" + '\200');
    }
}
