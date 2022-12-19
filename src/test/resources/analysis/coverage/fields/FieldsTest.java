import static utils.Utils.doCatch;

public class FieldsTest {
    public static void main(String[] args) {
        doCatch(Fields.RegularFields::new);
        new Fields.TrickyVariableDeclarators();
        Fields.StaticFieldWithoutInitializer.method();
    }
}
