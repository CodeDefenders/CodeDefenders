import static utils.Utils.consume;

public class ClassesTest {
    public static void main(String[] args) {
        // Classes
        new Classes.ClassWithoutConstructor();
        new Classes.ClassWithConstructor();

        // Abstract classes
        new Classes.AbstractClassWithoutConstructor(){};
        new Classes.AbstractClassWithConstructor(){};

        // Interfaces
        // implement the interfaces and invoke their methods to make sure to cover them if they were coverable
        new Classes.InterfaceWithDefaultMethod(){}.method();
        new Classes.Interface(){
            @Override
            public void method() {

            }
        }.method();

        // Records
        new Classes.EmptyRecordWithoutConstructor();
        new Classes.EmptyRecordWithConstructor();
        new Classes.RecordWithoutConstructor(0);
        new Classes.RecordWithConstructor(0);

        // Enums
        consume(Classes.EnumWithoutConstructor.CONSTANT);
        consume(Classes.EnumWithConstructor.CONSTANT);
    }
}
