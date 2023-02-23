import utils.Call;
import utils.MethodChain;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

/**
 * <p>For loops
 * <p>JaCoCo coverage: Covers initializer, condition and update expression.
 * <p>Extended coverage: Also covers space between the header elements and the space between header and body.
 */
public class ForLoops {
    @Call
    public void regularLoop() {
        for (int i = 0; i < 10; i++) {
            doCall();
        }

        for (
                int i = 0;

                i < 10;

                i++
        ) {
            doCall();
        }
    }

    @Call
    public void breakInFirstLoop() {
        for (
                int i = 0;

                i < 10;

                i++
        ) {
            if (i == 0)
                break;
        }

        // block: ignore_end_status
    }

    @Call
    public void breakInFirstLoopOneLine() {
        for (int i = 0; i < 10; i++) {
            if (i == 0)
                break;
        }

        // block: ignore_end_status
    }

    @Call
    public void conditionFalse() {
        for (
                int i = 0;

                i > 10;

                i++
        ) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void conditionFalseOneLine() {
        for (int i = 0; i > 10; i++) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void noInit() {
        int j = 0;
        for (
                ;

                j < 10;

                j++
        ) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void noInitOneLine() {
        int j = 0;
        for (; j < 10; j++) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void noCondition() {
        for (
                int i = 0;

                ;

                i++
        ) {
            if (i >= 10)
                break;
        }

        // block: ignore_end_status
    }

    @Call
    public void noConditionOneLine() {
        for (int i = 0;; i++) {
            if (i >= 10)
                break;
        }

        // block: ignore_end_status
    }

    @Call
    public void emptyHeader() {
        int j = 0;
        for (;;) {
            if (j++ >= 10)
                break;
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionsInInit() {
        for (
                int i = doThrow();

                i < 10;

                i++)
        {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionsInInitOneLine() {
        for (int i = doThrow(); i < 10; i++) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionsInSecondInit() {
        for (
                int i = 0,

                    j = doThrow();

                i < 10;

                i++
        ) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionsInCoveredInit() {
        for (
                int i = MethodChain.create()
                        .doThrow()
                        .get(1);

                i < 10;

                i++
        ) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionsInUpdate() {
        for (
                int i = 0;

                i < 10;

                i = doThrow()
        ) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionsInUpdateOneLine() {
        for (int i = 0; i < 10; i = doThrow()) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionsInSecondUpdate() {
        for (
                int i = 0,
                    j = 0;

                i < 10;

                i++,

                j = doThrow()
        ) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionsInCondition() {
        for (
                int i = 0;

                i < doThrow();

                i++
        ) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionsInConditionOneLine() {
        for (int i = 0; i < doThrow(); i++) {
            doCall();
        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionInBody() {
        for (int i = 0; i < 10; i++) {

            doThrow();

        }

        // block: ignore_end_status
    }

    @Call
    public void exceptionAndCallInBody() {
        for (int i = 0; i < 10; i++) {

            doCall();
            doThrow();

        }

        // block: ignore_end_status
    }

    @Call
    public void emptyLoop() {
        for (;;) {
            break;
        }

        // block: ignore_end_status
    }

    @Call
    public void jumps1() {
        // loop always breaks -> space after loop is still covered
        for (int i = 0; i < 10; i++) {
            break;

        }

        // block: ignore_end_status
    }

    @Call
    public void jumps2() {
        // loop always returns -> space after loop shouldn't be covered, but is covred for now (could be improved later)
        for (int i = 0; i < 10; i++) {
            return;

        }

        // block: ignore_end_status
    }
}
