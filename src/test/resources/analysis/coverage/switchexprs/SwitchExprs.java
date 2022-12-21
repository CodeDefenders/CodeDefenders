import utils.Call;
import utils.TestRuntimeException;
import utils.TestEnum;
import utils.MethodChain;

import static utils.Utils.consume;
import static utils.Utils.doCall;
import static utils.Utils.doThrow;

public class SwitchExprs {

    // ----------------- regular expression-type entries

    // expression-type case entries
    // no default case
    @Call(params = "A")
    public void expression1(TestEnum arg) {
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

    // ----------------- regular block-type entries

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

    // ----------------- regular throw-type entries

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
        int i = switch(arg) {

            case A ->
                    1
                    + 1;

            case B ->
                    2
                    + 1;

        };

        consume(switch(arg) {

            case A ->
                    1
                    + 1;

            case B ->
                    2
                    + 1;

        });
    }
}
