import utils.Call;
import utils.TestRuntimeException;
import utils.TestEnum;
import utils.MethodChain;

import static utils.Utils.consume;
import static utils.Utils.doCall;
import static utils.Utils.doThrow;

/**
 * <p>Switch Expressions
 * <p>JaCoCo coverage:
 * <ul>
 *     <li>Covers the selector (branch coverage).</li>
 *     <li>Expression-type switch entries: Covers a line of the expression if it's not usually coverable.</li>
 *     <li>
 *         Switch without default case: Covers a line of the last switch entry, even if it should not be covered.
 *         This seems to only be the case if the switch expr is part of an assignment.
 *     </li>
 * </ul>
 * <p>Extended coverage: Covers all lines of switch entries.
 */
public class SwitchExprs {

    // ----------------- expression-type entries

    // expression-type case entries
    // no default case
    // inside local variable decl
    @Call(params = "A")
    public void expression1VariableDecl(TestEnum arg) {
        int i = switch(arg) {
            case A ->
                    1;
            case B ->
                    2;
        };
    }

    // expression-type case entries
    // no default case
    // inside assignment
    @Call(params = "A")
    public void expression1Assignment(TestEnum arg) {
        int i = 1;

        i = switch(arg) {
            case A ->
                    1;
            case B ->
                    2;
        };
    }

    // expression-type case entries
    // no default case
    // inside mehtod argument
    @Call(params = "A")
    public void expression1MethodArg(TestEnum arg) {
        consume(switch(arg) {
            case A ->
                    1;
            case B ->
                    2;
        });
    }

    // expression-type case entries
    // no default case
    @Call(params = "B")
    public void expression2(TestEnum arg) {
        int i = switch(arg) {
            case A -> 1;
            case B -> 2;
        };
        consume(switch(arg) {
            case A -> 1;
            case B -> 2;
        });
    }

    // expression-type case entries
    // exhaustive branches with default case
    @Call(params = "A")
    public void expression3(TestEnum arg) {
        int i = switch(arg) {
            case A -> 1;
            case B -> 2;
            default -> 3;
        };
        consume(switch(arg) {
            case A -> 1;
            case B -> 2;
            default -> 3;
        });
    }

    // expression-type case entries
    // exhaustive branches with default case
    @Call(params = "B")
    public void expression4(TestEnum arg) {
        int i = switch(arg) {
            case A -> 1;
            case B -> 2;
            default -> 3;
        };
        consume(switch(arg) {
            case A -> 1;
            case B -> 2;
            default -> 3;
        });
    }

    // expression-type case entries
    // non-exhaustive branches with default case
    @Call(params = "A")
    public void expression5(TestEnum arg) {
        int i = switch(arg) {
            case A -> 1;
            default -> 3;
        };
        consume(switch(arg) {
            case A -> 1;
            default -> 3;
        });
    }

    // expression-type case entries
    // non-exhaustive branches with default case
    @Call(params = "B")
    public void expression6(TestEnum arg) {
        int i = switch(arg) {
            case A -> 1;
            default -> 3;
        };
        consume(switch(arg) {
            case A -> 1;
            default -> 3;
        });
    }

    // ----------------- block-type entries

    // block-type case entries
    // no default case
    @Call(params = "A")
    public void block1(TestEnum arg) {
        int i = switch(arg) {
            case A -> {
                yield 1;
            }
            case B -> {
                yield 2;
            }
        };
        consume(switch(arg) {
            case A -> {
                yield 1;
            }
            case B -> {
                yield 2;
            }
        });
    }

    // block-type case entries
    // no default case
    @Call(params = "B")
    public void block2(TestEnum arg) {
        int i = switch(arg) {
            case A -> {
                yield 1;
            }
            case B -> {
                yield 2;
            }
        };
        consume(switch(arg) {
            case A -> {
                yield 1;
            }
            case B -> {
                yield 2;
            }
        });
    }

    // block-type case entries
    // exhaustive branches with default case
    @Call(params = "A")
    public void block3(TestEnum arg) {
        int i = switch(arg) {
            case A -> {
                yield 1;
            }
            case B -> {
                yield 2;
            }
            default -> {
                doCall();
                yield 3;
            }
        };
        consume(switch(arg) {
            case A -> {
                yield 1;
            }
            case B -> {
                yield 2;
            }
            default -> {
                doCall();
                yield 3;
            }
        });
    }

    // block-type case entries
    // non-exhaustive branches with default case
    @Call(params = "A")
    public void block4(TestEnum arg) {
        int i = switch(arg) {
            case A -> {
                yield 1;
            }
            default -> {
                doCall();
                yield 3;
            }
        };
        consume(switch(arg) {
            case A -> {
                yield 1;
            }
            default -> {
                doCall();
                yield 3;
            }
        });
    }

    // block-type case entries
    // non-exhaustive branches with default case
    @Call(params = "B")
    public void block5(TestEnum arg) {
        int i = switch(arg) {
            case A -> {
                yield 1;
            }
            default -> {
                doCall();
                yield 3;
            }
        };
        consume(switch(arg) {
            case A -> {
                yield 1;
            }
            default -> {
                doCall();
                yield 3;
            }
        });
    }

    // ----------------- throw-type entries

    // throw-type case entries
    // no default case
    @Call(params = "A")
    public void throw1(TestEnum arg) {
        int i = switch(arg) {
            case A -> throw new TestRuntimeException();
            case B -> 2;
        };
    }

    // throw-type case entries
    // no default case
    @Call(params = "A")
    public void throw2(TestEnum arg) {
        int i = switch(arg) {
            case A -> 2;
            case B -> throw new TestRuntimeException();
        };
        consume(switch(arg) {
            case A -> 2;
            case B -> throw new TestRuntimeException();
        });
    }

    // throw-type case entries
    // exhaustive branches with default case
    @Call(params = "A")
    public void throw3(TestEnum arg) {
        int i = switch(arg) {
            case A -> throw new TestRuntimeException();
            case B -> throw new TestRuntimeException();
            default -> 3;
        };
    }

    // throw-type case entries
    // non-exhaustive branches with default case
    @Call(params = "A")
    public void throw4(TestEnum arg) {
        int i = switch(arg) {
            case A -> 1;
            default -> throw new TestRuntimeException();
        };
    }

    // throw-type case entries
    // non-exhaustive branches with default case
    @Call(params = "A")
    public void throw5(TestEnum arg) {
        int i = switch(arg) {
            case A -> throw new TestRuntimeException();
            default -> 1;
        };
    }

    // throw-type case entries
    // non-exhaustive branches with default case
    @Call(params = "B")
    public void throw6(TestEnum arg) {
        int i = switch(arg) {
            case A -> 1;
            default -> throw new TestRuntimeException();
        };
    }

    // ------------------ indirect exceptions

    // expression-type case entries
    // indirect exception
    @Call(params = "A")
    public void exception1(TestEnum arg) {
        doCall();
        int i = switch(arg) {
            case A -> doThrow();
            case B -> 2;
        };
    }

    // expression-type case entries
    // indirect exception
    @Call(params = "B")
    public void exception2(TestEnum arg) {
        doCall();
        int i = switch(arg) {
            case A -> 1;
            case B -> doThrow();
        };
    }

    // expression-type case entries
    // indirect exception
    @Call(params = "A")
    public void exception3(TestEnum arg) {
        doCall();
        int i = switch(arg) {
            case A -> MethodChain.create()
                        .doThrow()
                        .get(1);

            case B -> 2;
        };
    }

    // block-type case entries
    // indirect exception
    @Call(params = "A")
    public void exception4(TestEnum arg) {
        doCall();
        int i = switch(arg) {
            case A -> {
                doThrow();
                yield 1;
            }
            case B -> 2;
        };
    }

    // block-type case entries
    // indirect exception
    @Call(params = "A")
    public void excpetion5(TestEnum arg) {
        doCall();
        int i = switch(arg) {
            case A -> {
                yield doThrow();
            }
            case B -> 2;
        };
    }

    // block-type case entries
    // indirect exception
    @Call(params = "A")
    public void excpetion6(TestEnum arg) {
        doCall();
        int i = switch(arg) {
            case A -> {
                MethodChain.create()
                        .doThrow()
                        .get(1);

                yield 1;
            }
            case B -> 2;
        };
    }

    // block-type case entries
    // indirect exception
    @Call(params = "A")
    public void exception7(TestEnum arg) {
        doCall();
        int i = switch(arg) {
            case A -> {
                yield MethodChain.create()
                        .doThrow()
                        .get(1);

            }
            case B -> 2;
        };
    }

    // exception in selector
    @Call
    public void exception8() {
        int i = switch(doThrow()) {
            case 1 -> 1;
            default -> 2;
        };
    }

    // exception from covered expr in selector
    @Call
    public void exception9() {
        int i = switch(

                MethodChain.create()
                    .doThrow()
                    .get(1)

                ) {
            case 1 -> 1;
            default -> 2;
        };
    }

    // ------------------ other

    @Call(params = "B")
    public void switchExprIntoMethodChain(TestEnum arg) {
        (switch (arg) {
            case A -> MethodChain.create();
            case B -> {
                MethodChain m = MethodChain.create();
                yield m;
            }
        }).call();
    }

    @Call(params = "A")
    public void multilineExpressionsAndSpacing(TestEnum arg) {
        int i =
                switch(
                        arg
                        )

                        {

            case A ->
                    1
                    + 1;

            case B ->
                    2
                    + 1;

        };

        consume(
                switch(
                        arg
                        )

                        {

            case A ->
                    1
                    + 1;

            case B ->
                    2
                    + 1;

        });
    }

    @Call(params = "B")
    public void testStatusAfterExpr1(TestEnum arg) {
        int i = switch(arg) {
            case A -> 1;
            default -> 2;
        };

        // block: ignore_end_status
    }

    @Call(params = "B")
    public void testStatusAfterExpr2(TestEnum arg) {
        int i = switch(arg) {
            case A -> 1;
            default -> doThrow();
        };

        // block: ignore_end_status
    }

    @Call(params = "B")
    public void testStatusAfterExpr3(TestEnum arg) {
        int i = switch(arg) {
            case A -> 1;
            default -> MethodChain.create()
                    .doThrow()
                    .get(1);
        };

        // block: ignore_end_status
    }
}
