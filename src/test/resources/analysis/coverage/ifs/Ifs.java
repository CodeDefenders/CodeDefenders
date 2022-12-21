import utils.Call;
import utils.MethodChain;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

/**
 * <p>if statements
 * <p><b>JaCoCo coverage</b>: covers a line of the condition to indicate how many branches were taken.
 *                            which line of the condition is covered varies based on the structure of the condition.
 * <p><b>extended coverage</b>: covers all lines up to the then-statement and all lines between the then- and else
 *                              statement. if the condition is partly covered, then the partly-covered line in the
 *                              condition is inherited from JaCoCo. in the case of empty branches, we try do determine
 *                              which branches are taken and which aren't, but leave them empty if we can't determine
 *                              it with 100% confidence.
 */
public class Ifs {
    // test if the correct branches are taken under normal circumstances
    @Call(params = "true")
    public void branches(boolean alwaysTrue) {
        // then-branch taken
        if (alwaysTrue) {
            doCall();
        }
        if (alwaysTrue) {
            doCall();
        } else {
            doCall();
        }

        // else-branch taken
        if (!alwaysTrue) {
            doCall();
        }
        if (!alwaysTrue){
            doCall();
        } else {
            doCall();
        }

        // else-if-branch taken
        if (!alwaysTrue) {
            doCall();
        } else if (alwaysTrue) {
            doCall();
        } else {
            doCall();
        }

        // second else-branch taken
        if (!alwaysTrue) {
            doCall();
        } else if (!alwaysTrue) {
            doCall();
        } else {
            doCall();
        }
    }

    // test if the space after jumps is correctly left empty
    @Call(params = "true")
    public void jumps1(boolean alwaysTrue) {
        // jump in one branch (not covered)
        if (!alwaysTrue) {
            return;

        }

        // jump in one branch (covered)
        if (alwaysTrue) {
            return;

        }

        // jump in both branches
        if (alwaysTrue) {
            return;

        } else {
            return;

        }

    }

    // test if the space after jumps is correctly left empty
    @Call(params = "true")
    public void jumps2(boolean alwaysTrue) {
        // second jump behind a second if
        if (alwaysTrue) {
            return;
        } else if (alwaysTrue) {
            return;
        }

        // jump in three branches
        if (alwaysTrue) {
            return;
        } else if (alwaysTrue) {
            return;
        } else {
            return;
        }

    }

    @Call(params = "true")
    public void exceptions(boolean alwaysTrue) {
        if (alwaysTrue) {
            doCall();
            doThrow();
        }

    }

    // test if the space up to the exception is covered
    @Call(params = {"true", "false"})
    public void exceptionWithBothBranchesTaken(boolean trueOrFalse) {
        // for some reason, swapping doThrow() and doCall() here doesn't work,
        // as it leads to partial coverage in the condition
        if (trueOrFalse) {

            doThrow();
        } else {
            doCall();
        }
    }

    @Call(params = {"true, true", "true, false"})
    public void emptyBranches(boolean alwaysTrue, boolean trueOrFalse) {
        // can't determine if the then or else branch has been taken, since both are empty
        if (alwaysTrue) {

        } else {

        }

        if (trueOrFalse) {

        } else {

        }

        if (alwaysTrue) {
            doCall();
        } else {

        }

        // can't determine if the then or else branch has been taken, since doCall could throw an exception,
        // which would also result in a not-covered line
        if (alwaysTrue) {

        } else {
            doCall();
        }
    }

    /**
     * <p>if statements with empty conditions (optimized out)
     * <p><b>JaCoCo coverage</b>: only covers statements of the taken branch, since the rest is optimized out
     * <p><b>extended coverage</b>: determines which branch is optimized out from the coverage and marks the other
     *                              branch as not-covered. if this can't be determined the branches and if stmt are
     *                              left EMPTY.
     */
    @Call
    public void emptyConditions() {
        // can determine which branch was optimized out
        if (true) {
            doCall();
        }

        if (true) {
            doCall();
        } else {
            doCall();
        }

        if (false) {
            doCall();
        } else {
            doCall();
        }

        if (true) {
            doCall();
        } else if (true) {
            doCall();
        } else {
            doCall();
        }

        if (false) {
            doCall();
        } else if (false) {
            doCall();
        } else {
            doCall();
        }

        // can't determine which branch was optimized out (could be improved later)
        if (false) {
            doCall();
        }

        if (false) {
            doCall();
        } else {

        }

        if (true) {

        } else {

        }
    }

    @Call(params = "true")
    public void withoutBraces(boolean alwaysTrue) {
        // then-branch taken
        if (alwaysTrue)
            doCall();
        if (alwaysTrue)
            doCall();
        else
            doCall();

        // else-branch taken
        if (!alwaysTrue)
            doCall();
        if (!alwaysTrue)
            doCall();
        else
            doCall();
    }

    // test detection of which branch has been taken with multiple branches inside the condition
    @Call(params = {"true", "false"})
    public void conditions(boolean trueOrFalse) {
        // not-covered instruction in condition
        if (
                MethodChain.create()
                        .dontCallLambda(() -> {})
                        .get(trueOrFalse)
        ) {

        } else {

        }

        // extra not-covered condition in condition
        if (
                MethodChain.create()
                        .dontCallLambda(() -> {
                            int i = System.currentTimeMillis() < 0 ? 0 : 1;
                        })
                        .get(trueOrFalse)
        ) {

        } else {

        }

        // extra partly-covered condition in condition
        if (
                MethodChain.create()
                        .callLambda(() -> {
                            int i = System.currentTimeMillis() < 0 ? 0 : 1;
                        })
                        .get(trueOrFalse)
        ) {

        } else {

        }
        if (
                MethodChain.create()
                        .callLambda(() -> {
                            int i = System.currentTimeMillis() < 0 ? 0 : 1;
                        })
                        .get(trueOrFalse)
        ) {
            doCall();
        } else {

        }

        // extra fully-covered condition in condition (multiple actually)
        if (
                MethodChain.create()
                        .callLambda(() -> {
                            for (int i = 0; i < 2; i++) {
                                int j = i == 0 ? 0 : 1;
                            }
                        })
                        .get(trueOrFalse)
        ) {

        } else {

        }
    }
}
