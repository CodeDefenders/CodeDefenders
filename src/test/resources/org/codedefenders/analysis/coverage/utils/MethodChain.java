package utils;

import utils.Utils;

public class MethodChain {
    public int field = 0;

    public static MethodChain create() {
        return new MethodChain();
    }

    public MethodChain consume(int i) {
        return this;
    }

    public MethodChain callLambda(Runnable r) {
        r.run();
        return this;
    }

    public MethodChain dontCallLambda(Runnable r) {
        return this;
    }

    public MethodChain call() {
        return this;
    }

    public MethodChain doThrow() {
        Utils.doThrow();
        return this;
    }

    public <T> T get(T obj) {
        return obj;
    }
}
