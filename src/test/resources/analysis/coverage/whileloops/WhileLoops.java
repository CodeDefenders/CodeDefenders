import utils.Call;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

public class WhileLoops {
    @Call(params = "0")
    public void fullyCoveredCondition(int i) {
        while (i < 5) {
            i++;
        }

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void partlyCoveredConditionBodyEntered(int i) {
        while (i < 5) {
            if (i == 0) {
                break;
            }

        }

        // block: ignore_end_status
    }

    @Call(params = "10")
    public void partlyCoveredConditionBodySkipped(int i) {
        while (i < 5) {
            i++;
        }

        // block: ignore_end_status
    }

    @Call(params = "10")
    public void partlyCoveredConditionBodySkippedWithEmptyBody(int i) {
        while (i < 5) {

        }

        // block: ignore_end_status
    }


    @Call(params = "0")
    public void bodyThrows(int i) {
        while (i < 5) {
            doThrow();

        }

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void bodyCoveredAndThrows(int i) {
        while (i == 0) {
            doCall();
            doThrow();

        }

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void bodySometimesThrows(int i) {
        while (i < 5) {
            i++;
            if (i == 2) {
                doThrow();
            }

        }

        // block: ignore_end_status
    }


    // while(false) doesn't compile: 'error: unreachable statement'
    @Call(params = "0")
    public void emptyCondition(int i) {
        while (true) {
            i++;
            if (i > 2) {
                break;
            }

        }

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void emptyConditionWithCoveredBody(int i) {
        while (true) {
            break;

        }

        // block: ignore_end_status
    }

    // space after loop is empty, since loops always jumps
    @Call(params = "0")
    public void bodyAlwaysJumpsWithReturn(int i) {
        while (i == 0) {
            return;

        }

        // block: ignore_end_status
    }

    @Call(params = "0")
    public void bodyAlwaysJumpsWithBreak(int i) {
        while (i == 0) {
            break;

        }

        // block: ignore_end_status
    }
}
