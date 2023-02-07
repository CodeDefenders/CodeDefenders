package utils;

import java.util.List;

import utils.TestRuntimeException;

public class Utils {
    public static void doCall() {

    }

    public static <T> T doGet(T obj) {
        return obj;
    }

    public static int doThrow() {
        throw new TestRuntimeException();
    }

    public static List<Integer> doThrowList() {
        throw new TestRuntimeException();
    }

    public static Object doThrowObject() {
        throw new TestRuntimeException();
    }

    public static void doCatch(Runnable r) {
        try {
            r.run();
        } catch (TestRuntimeException ignored) {

        }
    }

    public static void consume(int i) {

    }

    public static void consume(boolean b) {

    }

    public static void consume(int i, int j) {

    }

    public static void consume(Object o) {

    }

    public static void callLambda(Runnable r) {
        r.run();
    }

    public static void dontCallLambda(Runnable r) {

    }
}
