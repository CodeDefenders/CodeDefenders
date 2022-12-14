import static utils.Utils.doCatch;

public class CoverageDemoTest {
    public static void main(String[] args) {
        // Classes
        new CoverageDemo.ClassWithoutConstructor();
        new CoverageDemo.ClassWithConstructor();

        // Interfaces
        new CoverageDemo.InterfaceWithDefaultMethod(){}.method();
        new CoverageDemo.Interface(){
            @Override
            public void method() {

            }
        }.method();

        // Records
        new CoverageDemo.EmptyRecordWithoutConstructor();
        new CoverageDemo.EmptyRecordWithConstructor();
        new CoverageDemo.RecordWithoutConstructor(0);
        new CoverageDemo.RecordWithConstructor(0);

        // Fields
        doCatch(CoverageDemo.Fields::new);
        CoverageDemo.StaticFieldWithoutInitializer.method();

        // Constructors
        new CoverageDemo.Constructors();
        new CoverageDemo.Constructors(0);
        doCatch(() -> new CoverageDemo.Constructors(0, 0));
        new CoverageDemo.CompactConstructorsEmpty();
        new CoverageDemo.CompactConstructors(0);

        // Methods
        CoverageDemo.Methods.empty();
        CoverageDemo.Methods.explicitReturn();
        doCatch(CoverageDemo.Methods::throwsException);
        new CoverageDemo.InterfaceDefaultMethods(){}.empty();
        new CoverageDemo.InterfaceDefaultMethods(){}.explicitReturn();
        doCatch(new CoverageDemo.InterfaceDefaultMethods(){}::throwsException);


        // Local Variables
        doCatch(CoverageDemo::localVariables);

        // Blocks
        CoverageDemo.Blocks.coveredToEnd();
        CoverageDemo.Blocks.earlyReturn();
        doCatch(CoverageDemo.Blocks::earlyException);
        doCatch(CoverageDemo.Blocks::earlyIndirectException);
        CoverageDemo.Blocks.independentNodes(true);
        CoverageDemo.Blocks.nestedBlocks();

        // Ifs
        CoverageDemo.Ifs.normalIf(true);
        CoverageDemo.Ifs.returnFromIf(true);
        doCatch(() -> CoverageDemo.Ifs.exceptionFromIf(true));
    }
}
