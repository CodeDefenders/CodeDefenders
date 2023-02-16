import utils.Call;
import utils.MethodChain;

import static utils.Utils.doThrow;

public class EnclosedExprs {
    @Call
    public void exception1() {
        int i =
                (

                        MethodChain.create()
                                .doThrow()
                                .get(1)

                );
    }

    @Call
    public void exception2() {
        int i
                =
                (

                        doThrow()

                );
    }

    @Call
    public void exception3() {
        int i
                =
                (

                        1
                        +
                        doThrow()

                );
    }

    @Call
    public void nestedExprException() {
        int i
                =
                (

                    (

                            1
                            +
                            doThrow()

                    )

                );
    }
}
