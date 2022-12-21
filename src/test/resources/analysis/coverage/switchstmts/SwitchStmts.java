import utils.Call;
import utils.MethodChain;
import utils.TestEnum;

import static utils.Utils.doCall;
import static utils.Utils.doThrow;

public class SwitchStmts {
    @Call(params = "A")
    public void regularSwitchStmts(TestEnum arg) {
        switch (arg) {
            case A:
                doCall();
                break;
        }

        switch (arg) {
            case B:
                doCall();
                break;
        }

        switch (arg) {
            default:
                doCall();
                break;
        }

        switch (arg) {
            case A:
                doCall();
                break;
            case B:
                doCall();
                break;
        }

        switch (arg) {
            case A:
                doCall();
                break;
            default:
                doCall();
                break;
        }

        switch (arg) {
            case B:
                doCall();
                break;
            default:
                doCall();
                break;
        }

        switch (arg) {
            case A:
                doCall();
                break;
            case B:
                doCall();
                break;
            default:
                doCall();
                break;
        }
    }

    @Call(params = "A")
    public void fallthroughs(TestEnum arg) {
        switch (arg) {
            case B:
            case A:
                doCall();
                break;
        }

        switch (arg) {
            case A:
            case B:
                doCall();
                break;
        }
    }

    @Call(params = "A")
    public void emptyCases(TestEnum arg) {
        switch (arg) {
            case A:
                break;
            default:
                break;
        }

        switch (arg) {
            case B:
                break;
            default:
                break;
        }

        // no branches
        switch (arg) {
            default:
                break;
        }
    }

    @Call(params = "A")
    public void exceptionInCase1(TestEnum arg) {
        switch (arg) {
            case A:
                doThrow();
                break;
            default:
                doCall();
                break;
        }
    }

    @Call(params = "A")
    public void exceptionInCase2(TestEnum arg) {
        switch (arg) {
            case A:
                doCall();
                doThrow();
                break;
            default:
                doCall();
                break;
        }
    }

    @Call
    public void exceptionInSelector1() {
        switch (
                doThrow()
        ) {
            case 1:
                doCall();
                break;
        }
    }

    @Call
    public void exceptionInSelector2() {
        switch (
                MethodChain.create()
                        .doThrow()
                        .get(TestEnum.A)
        ) {
            case A:
                doCall();
                break;
        }
    }

    @Call(params = "A")
    public void spaceBeforeAndAfter(TestEnum arg) {
        switch (arg) {

            case A:

                doCall();

            case B:

                doCall();

                break;

            default:

                doCall();

        }
    }

    @Call(params = "A")
    public void spaceBeforeAndAfterWithBlocks(TestEnum arg) {
        switch (arg) {

            case A: {

                doCall();

            }

            case B:
            {

                doCall();

                break;

            }

            default: {

                doCall();

            }
        }
    }
}
