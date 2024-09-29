public class Puzzle {

    public String run(int x) {
        String result = "";
        for (int i = 0; i < x; i++) {
            if (i % 10 == 0) {
                result += "X";
            } else {
                result += "O";
            }
        }
        return result;
    }

}
