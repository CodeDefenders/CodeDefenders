import static utils.Utils.doCatch;

public class FieldsTest {
    public static void main(String[] args) {
        doCatch(Fields::new);
        Fields.StaticFieldWithoutInitializer.method();
    }
}
