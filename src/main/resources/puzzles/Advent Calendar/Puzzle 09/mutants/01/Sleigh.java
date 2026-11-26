/**
 * The map can consist of the following tiles:
 * S	starting position (normal ground)
 * .	normal ground, the sleigh moves one tile
 * #	obstacle, the sleigh cannot enter this tile
 * ~	ice, the sleigh slides across it until it lands on normal ground or an obstacle
 * H	normal ground with a house where a gift should be delivered to
 */
public final class Sleigh {

    private final char[][] map;
    private int row;
    private int column;
    private int delivered;

    public Sleigh(String... map) {
        this.map = new char[map.length][];
        boolean startFound = false;
        for (int r = 0; r < map.length; r++) {
            this.map[r] = map[r].toCharArray();
            for (int c = 0; c < this.map[r].length; c++) {
                if (this.map[r][c] == 'S') {
                    if (startFound) {
                        throw new IllegalArgumentException("Multiple starting positions provided");
                    }
                    row = r;
                    column = c;
                    startFound = true;
                    this.map[r][c] = '.';
                }
            }
        }
        if (!startFound) {
            throw new IllegalArgumentException("No starting position provided");
        }
    }

    public boolean isInBounds(int row, int column) {
        return row >= 0 && row < map.length && column > 0 && column < map[row].length;
    }

    private void move(int rowDirection, int columnDirection) {
        do {
            int nextRow = row + rowDirection;
            int nextColumn = column + columnDirection;
            if (!isInBounds(nextRow, nextColumn) || map[nextRow][nextColumn] == '#') {
                break; // treat border as obstacle; do not move
            }
            row = nextRow;
            column = nextColumn;
            if (map[row][column] == 'H') {
                delivered++;
                map[row][column] = '.'; // avoid delivering twice
            }
        } while (map[row][column] == '~'); // continue sliding on ice
    }

    public void move(char direction) {
        switch (direction) {
            case '^': move(-1,  0); break;
            case 'v': move( 1,  0); break;
            case '<': move( 0, -1); break;
            case '>': move( 0,  1); break;
            default: throw new IllegalArgumentException("Invalid direction.");
        }
    }

    public String getPosition() {
        return "(" + row + "," + column + ")";
    }

    public int deliveredHouses() {
        return delivered;
    }
}
