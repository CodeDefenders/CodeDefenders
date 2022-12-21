import static utils.Utils.doCatch;

public class FieldsTest {
    public static void main(String[] args) {
        doCatch(Fields.RegularFields::new);
        doCatch(Fields.ExceptionWithMultipleDelcarators::new);
        doCatch(Fields.ExceptionWithMultipleDelcaratorsAndEmpty::new);
        doCatch(Fields.ExceptionWithMultipleDelcaratorsOnSameLine::new);
        new Fields.TrickyVariableDeclarators();
        Fields.StaticFieldWithoutInitializer.method();
        doCatch(Fields.FieldsInLocalClass::method);
        doCatch(Fields.FieldsInAnonymousClass::method);
    }
}
