public class FailingTests {
    static class TrickyVariableDeclarators {
        private
        Runnable s = () -> {};

        int i; // should be COVERED but is NOT_COVERED
    }

    public void notCoveredAtAllForSomeReason() {
        doCall();
        Object a = null;
        synchronized (a) {

        }
    }
}
