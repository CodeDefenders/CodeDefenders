import static utils.Utils.doCatch;

public class ConstructorsTest {
    public static void main(String[] args) {
        new Constructors();
        new Constructors(0);
        doCatch(() -> new Constructors(0, 0));
        new Constructors.CompactConstructorsEmpty();
        new Constructors.CompactConstructors(0);
    }
}
