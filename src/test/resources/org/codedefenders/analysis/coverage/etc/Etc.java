import java.util.List;
import java.util.ArrayList;

import utils.Call;

public class Etc {
    @Call(params = "(1; ); (3; 4)", exception = NumberFormatException.class)
    public void parsePoints(String in) {
        boolean foundParen = false;
        char[] chars = in.toCharArray();

        String currentMatch = null;
        List<String> currentPoint = new ArrayList<>();

        class Point {
            int x;
            int y;

            public Point(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }

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
                            Integer.parseInt(
                                    currentPoint.get(1))

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

        return;
    }
}
