import utils.Call;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

public class WhileLoops {
    @Call
    public void regularLoops() {
        int i = 0;

        // fully covered condition
        while (i < 5) {
            i++;

        }

        // partly covered condition, body entered
        while (i < 10) {
            i++;
            if (i > 7) {
                break;
            }

        }

        // partly covered condition, body always jumps
        while (i < 10) {
            i++;
            break;

        }

        // partly covered condition, body not entered
        while (i > 15) {
            i++;

        }
    }

    @Call
    public void exceptions1() {
        int i = 0;

        // only exception
        while (i == 0) {
            doThrow();
        }
    }

    @Call
    public void exceptions2() {
        int i = 0;

        // exception and regular method call
        while (i == 0) {
            doCall();
            doThrow();
        }
    }

    @Call
    public void emptyConditions() {
        int i = 0;

        while (true) {
            i++;
            if (i > 2) {
                break;
            }

        }

        while (true) {
            break;

        }

        // while(false) doesn't compile: 'error: unreachable statement'
    }


    // space after loop is empty, since loops always jumps
    @Call
    public void jumps1() {
        int i = 0;
        while (i == 0) {
            return;

        }

    }

    // space after loop with jump is covered, we detect that the method runs until the end
    @Call
    public void jumps2() {
        int i = 0;
        while (i == 0) {
            break;

        }

    }
}
