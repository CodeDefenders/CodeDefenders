import utils.Call;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

public class ForLoops {
    @Call
    public void headers() {
        // regular loop
        for (int i = 0; i < 10; i++) {

        }
        for (
                int i = 0;
                i < 10;
                i++
        ) {

        }

        // break in first loop
        for (int i = 0; i < 10; i++) {
            if (i == 0)
                break;
        }
        for (
                int i = 0;
                i < 10;
                i++
        ) {
            if (i == 0)
                break;
        }

        // condition always false
        for (int i = 0; i > 10; i++) {

        }
        for (
                int i = 0;
                i > 10;
                i++
        ) {

        }

        // no init
        int j = 0;
        for (; j < 10; j++) {

        }
        j = 0;
        for (
                ;
                j < 10;
                j++
        ) {

        }

        // no condition
        for (int i = 0;; i++) {
            if (i >= 10)
                break;
        }
        for (
                int i = 0;
                ;
                i++
        ) {
            if (i >= 10)
                break;
        }

        // empty header
        j = 0;
        for (;;) {
            if (j++ >= 10)
                break;
        }
    }

    @Call
    public void exceptionsInInit1() {
        for (
                int i = doThrow();
                i < 10;
                i++)
        {

        }
    }

    @Call
    public void exceptionsInInit2() {
        for (
                int i = 0,
                    j = doThrow();
                i < 10;
                i++
        ) {

        }
    }

    @Call
    public void exceptionsInUpdate1() {
        for (
                int i = 0;
                i < 10;
                i = doThrow()
        ) {

        }
    }

    @Call
    public void exceptionsInUpdate2() {
        for (
                int i = 0,
                    j = 0;
                i < 10;
                i++,
                j = doThrow()
        ) {

        }
    }

    @Call
    public void exceptionsInCondition() {
        for (
                int i = 0;
                i < doThrow();
                i++
        ) {

        }
    }

    @Call
    public void exceptions1() {
        // only exception
        for (int i = 0; i < 10; i++) {

            doThrow();

        }
    }

    @Call
    public void exceptions2() {
        // exception and regular method call
        for (int i = 0; i < 10; i++) {

            doCall();
            doThrow();

        }
    }

    @Call
    public void emptyConditions() {
        // 'for (;false;)' doesn't compile: 'error: unreachable statement'

        for (;;) {
            break;
        }
    }

    @Call
    public void jumps() {
        // loop always breaks -> space after loop is still covered
        for (int i = 0; i < 10; i++) {
            break;

        }

        // loop always returns -> space after loop shouldn't be covered, but is covred for now (could be improved later)
        for (int i = 0; i < 10; i++) {
            return;

        }

    }
}
