import static utils.Utils.doCatch;

public class BlocksTest {
    public static void main(String[] args) {
        Blocks.coveredToEnd();
        Blocks.earlyReturn();
        doCatch(Blocks::earlyException);
        doCatch(Blocks::earlyIndirectException);
        Blocks.independentNodes(true);
        Blocks.nestedBlocks();
    }
}
