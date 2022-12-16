import static utils.Utils.doCatch;

public class MethodsTest {
    public static void main(String[] args) {
        Methods.empty();
        Methods.explicitReturn();
        doCatch(Methods::throwsException);
        new Methods.InterfaceDefaultMethods(){}.empty();
        new Methods.InterfaceDefaultMethods(){}.explicitReturn();
        doCatch(new Methods.InterfaceDefaultMethods(){}::throwsException);
    }
}
