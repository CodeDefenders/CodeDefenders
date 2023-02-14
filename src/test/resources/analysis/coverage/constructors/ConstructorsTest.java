import static utils.Utils.doCatch;

public class ConstructorsTest {
    public static void main(String[] args) {
        new Constructors();
        new Constructors(0);
        doCatch(() -> new Constructors(0, 0));
        new Constructors.CompactConstructorsEmpty();
        doCatch(Constructors.CompactConstructorsEmptyException1::new);
        doCatch(Constructors.CompactConstructorsEmptyException2::new);
        new Constructors.CompactConstructors(0);
        doCatch(() -> new Constructors.CompactConstructorsException1(0));
        doCatch(() -> new Constructors.CompactConstructorsException2(0));
        doCatch(Constructors.ThrowingBaseClass::new);
        doCatch(Constructors.ThrowingBaseClassWithSuper::new);
        doCatch(Constructors.ThrowingBaseClassWithSuperAndCoveredArg1::new);
        doCatch(Constructors.ThrowingBaseClassWithSuperAndCoveredArg2::new);
    }
}
