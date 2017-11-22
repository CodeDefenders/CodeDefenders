import org.junit.*;

import static org.junit.Assert.*;

public class TestWithSystemCall2 {
    @Test(timeout = 4000)
    public void test() throws Throwable {
        //PrintStream o = System.out;
        //o.println("Hello world");
        try {
            java.io.File dir = new java.io.File(".");
            java.io.File[] filesList = dir.listFiles();
            for (java.io.File file : filesList) {
                if (file.isFile()) {
                    System.out.println(file.getName());
                }
            }
        } catch (Throwable e) {
        }

    }
}