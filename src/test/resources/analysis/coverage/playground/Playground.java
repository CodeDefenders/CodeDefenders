import java.util.ArrayList;
import java.util.List;

import utils.Call;

public class Playground {
    class Point {
        int x;
        int y;

        public Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    // @Call(params = "(1; 2); (3; 44a); (5; 6)")
    // @Call(params = "(1; 2); (3; 44); (5; 6)")
    // @Call(params = "(1; 2); (3; 4); (5; 6)")
    @Call(params = "(1; 2); (3; ); (5; 6)")
    public List<Point> parsePoints(String in) {
        boolean foundParen = false;
        char[] chars = in.toCharArray();

        String currentMatch = null;
        List<String> currentPoint = new ArrayList<>();

        List<Point> result = new ArrayList<>();

        for (char c : chars) {
            if (!foundParen) {
                if (c == '(') {
                    foundParen = true;
                }
            } else {
                if (c == ')') {
                    currentPoint.add(currentMatch);
                    currentMatch = null;

                    result.add(new Point(
                            Integer.parseInt(currentPoint.get(0)),
                            Integer.parseInt(currentPoint.get(1))
                    ));
                    currentPoint.clear();
                    foundParen = false;
                } else if (c == ';') {
                    currentPoint.add(currentMatch);
                    currentMatch = null;
                } else if (c >= '0' && c <= '9') {
                    currentMatch = currentMatch == null
                            ? "" + c
                            : currentMatch + c;
                }
            }
        }

        return result;
    }


    // TODO: check exceptions thrown in loop and if conditions
    // TODO: check if EMPTY cases are handled everywhere (any statement can be optimized out)

}
