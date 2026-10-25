public class Puzzle {

    public int run(int x) {
        if (x % 3 == 0) {
            return x / 3;
        }
        return x;
    }

}
