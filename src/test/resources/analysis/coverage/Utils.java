package utils;

public class Utils {
    public static int doGet() {
        return 4;
    }

    public static int doThrow() {
        throw new RuntimeException();
    }

    public static void doCatch(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException ignored) {

        }
    }
}
