public class Puzzle {
    public int foo(int x) {
        return bar(x, -x);
    }
    
    private int bar(int x, int y) {
        if (x < y) {
            return y;
        }
        return x;
    }
}
