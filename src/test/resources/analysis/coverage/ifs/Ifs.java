import utils.Call;
import utils.MethodChain;

import static utils.Utils.doCall;
import static utils.Utils.doGet;
import static utils.Utils.doThrow;

/**
 * <p>If statements
 * <p>JaCoCo coverage: Covers the condition with branch coverage. Which line of the condition is covered varies based on
 *                     the structure of the condition. The branch coverage can also be on one of the parentheses and not
 *                     on the condition itsef.
 * <p>Extended coverage: Also covers all lines up to the then-statement and all lines between the then- and else
 *                       statement. In the case of empty branches, we try do determine which branches are taken and
 *                       which aren't, but leave them empty if we can't reliably determine it.
 */
public class Ifs {
    @Call(params = "true")
    public void thenBranchTaken1(boolean alwaysTrue) {
        if (alwaysTrue) {

            doCall();

        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void thenBranchTaken2(boolean alwaysTrue) {
        if (alwaysTrue) {

            doCall();

        } else {

            doCall();

        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void elseBranchTaken1(boolean alwaysTrue) {
        if (!alwaysTrue) {

            doCall();

        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void elseBranchTaken2(boolean alwaysTrue) {
        if (!alwaysTrue) {

            doCall();

        } else {

            doCall();

        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void elseIfBranchTaken(boolean alwaysTrue) {
        if (!alwaysTrue) {

            doCall();

        } else if (alwaysTrue) {

            doCall();

        } else {

            doCall();

        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void secondElseBranchTaken(boolean alwaysTrue) {
        if (!alwaysTrue) {

            doCall();

        } else if (!alwaysTrue) {

            doCall();

        } else {

            doCall();

        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void notCoveredJump(boolean alwaysTrue) {
        if (!alwaysTrue) {
            return;
        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void coveredJump(boolean alwaysTrue) {
        if (alwaysTrue) {
            return;
        }

        // block: ignore_end_status
    }


    @Call(params = "true")
    public void jumpInBothBranches(boolean alwaysTrue) {
        if (alwaysTrue) {
            return;
        } else {
            return;
        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void jumpFromElseIf(boolean alwaysTrue) {
        if (alwaysTrue) {
            return;
        } else if (alwaysTrue) {
            return;
        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void jumpFromSecondElse(boolean alwaysTrue) {
        if (alwaysTrue) {
            return;
        } else if (alwaysTrue) {
            return;
        } else {
            return;
        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void exceptions(boolean alwaysTrue) {
        if (alwaysTrue) {
            doCall();
            doThrow();
        }

        // block: ignore_end_status
    }

    @Call(params = {"true", "false"})
    public void exceptionWithBothBranchesTaken1(boolean trueOrFalse) {
        if (trueOrFalse) {

            doThrow();
        } else {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call(params = {"true", "false"})
    public void exceptionWithBothBranchesTaken2(boolean trueOrFalse) {
        if (trueOrFalse) {
            doCall();
        } else {

            doThrow();
        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void emptyBranches1(boolean alwaysTrue) {
        // can't determine if the then or else branch has been taken, since both are empty
        if (alwaysTrue) {

        } else {

        }

        // block: ignore_end_status
    }

    @Call(params = {"true", "false"})
    public void emptyBranches2(boolean trueOrFalse) {
        if (trueOrFalse) {

        } else {

        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void emptyBranches3(boolean alwaysTrue) {
        if (alwaysTrue) {
            doCall();
        } else {

        }

        // block: ignore_end_status
    }

    @Call(params = "true")
    public void emptyBranches4(boolean alwaysTrue) {
        // can't determine if the then or else branch has been taken, since doCall could throw an exception,
        // which would also result in a not-covered line
        if (alwaysTrue) {

        } else {
            doCall();
        }

        // block: ignore_end_status
    }

    /**
     * <p>If statements with empty conditions (optimized out)
     * <p>JaCoCo coverage: Only covers statements of the taken branch, since the rest is optimized out
     * <p>Extended coverage: Determines which branch is optimized out from the coverage and marks the other
     *                       branch as not-covered. If this can't be determined the branches and if stmt are
     *                       left EMPTY.
     */
    @Call
    public void emptyConditions1() {
        // can determine which branch was optimized out
        if (true) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void emptyConditions2() {
        if (true) {
            doCall();
        } else {
            doCall();
        }
    }
    @Call
    public void emptyConditions3() {
        if (false) {
            doCall();
        } else {
            doCall();
        }
    }
    @Call
    public void emptyConditions4() {
        if (true) {
            doCall();
        } else if (true) {
            doCall();
        } else {
            doCall();
        }
    }
    @Call
    public void emptyConditions5() {
        if (false) {
            doCall();
        } else if (false) {
            doCall();
        } else {
            doCall();
        }
    }
    @Call
    public void emptyConditions6() {
        // can't determine which branch was optimized out (could be improved later)
        if (false) {
            doCall();
        }
    }

    @Call
    public void emptyConditions7() {
        if (false) {
            doCall();
        } else {

        }
    }

    @Call
    public void emptyConditions8() {
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

    @Call
    public void exceptionFromCondition1() {
        if (
                doThrow() == 1
        ) {

        }
    }

    @Call
    public void exceptionFromCondition1OneLine() {
        if (doThrow() == 1) {

        }
    }

    @Call
    public void exceptionFromCondition2() {
        if (

                doGet(1) == doThrow()
        ) {

        }
    }

    @Call
    public void exceptionFromCondition2OneLine() {
        if (doGet(1) == doThrow()) {

        }
    }

    @Call
    public void exceptionFromCondition3() {
        if (

                doGet(1)
                ==
                doThrow()
        ) {

        }
    }

    @Call
    public void exceptionFromCondition4() {
        if (
                MethodChain.create()
                .doThrow()
                .get(1) == 1) {

        }
    }

    @Call
    public void exceptionFromCondition5OneLine() {
        if (MethodChain.create().doThrow().get(1) == 1) {

        }
    }
}
