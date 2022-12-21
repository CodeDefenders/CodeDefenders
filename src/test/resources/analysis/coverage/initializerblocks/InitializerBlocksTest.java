import static utils.Utils.doCatch;
import static utils.Utils.consume;

public class InitializerBlocksTest {
    public static void main(String[] args) {
        doCatch(InitializerBlocks::new);
        consume(InitializerBlocks.NoCoverageButStaticBlock.i);
        doCatch(InitializerBlocks.BlocksInLocalClass::method);
        doCatch(InitializerBlocks.BlocksInAnonymousClass::method);
    }
}
