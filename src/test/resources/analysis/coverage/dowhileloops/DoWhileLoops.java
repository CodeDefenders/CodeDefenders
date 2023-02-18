import utils.Call;
import utils.MethodChain;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

/**
 * <p>Do-while loops
 * <p>JaCoCo coverage: Only covers the condition.
 * <p>Extended coverage: Covers space from 'do' keyword up to body based on wheter the start of the body has been
 *                       executed. Covers space from body to end based on wheter the condition has been executed.
 */
public class DoWhileLoops {
    @Call
    public void regularLoops() {
        int i = 0;

        // fully covered condition, non-empty body
        do {
            i++;
        } while(i < 5);

        // partly covered condition, non-empty body
        do {
             i++;
        } while(i > 10);

        // partly covered condition, empty body
        do {

        } while(i > 10);
    }

    @Call
    public void exceptions1() {
        int i = 0;

        // only exception
        do

        {
            doThrow();

        } while(i == 0);

    }

    @Call
    public void exceptions2() {
        int i = 0;

        // exception and regular method call
        do {
            doCall();
            doThrow();

        } while(i == 0);
    }

    @Call
    public void emptyConditions() {
        int i = 0;

        // condition always the same
        do {
            doCall();
        } while(false);

        // loop body always jumps
        do {
            break;
        } while(i < 5);
    }

    // space after loop is empty, since loops always jumps
    @Call
    public void jumps1() {
        int i = 0;

        do {
            return;

        } while(i == 0);

    }

    // space after loop with jump is covered, we detect that the method runs until the end
    @Call
    public void jumps2() {
        int i = 0;

        do {
            doCall();
            break;

        } while (i == 0);

    }

    @Call
    public void exceptionInCondition() {
        do {

        }

        while (doThrow() == 0);
    }

    @Call
    public void exceptionFromCoveredExprInCondition() {
        do {

        }

        while (MethodChain.create()
                .doThrow()
                .get(0) == 0);
    }
}
