public final class ChristmasLights {

    private final boolean[] lights;

    public ChristmasLights(int numberOfLights) {
        lights = new boolean[numberOfLights];
    }

    public void switchOn() {
        switchOff();

        int position = 0;
        int step = 1;
        int round = 0;

        while (round < lights.length * 2) {
            if ((position + round) % 3 == 0) {
                lights[position] = !lights[position];
            } else if (position % 2 == 0) {
                lights[position] = true;
            } else if (round % 4 == 0) {
                lights[position] = false;
            }

            if (lights[position]) {
                step = step % 3 + 1;
            } else {
                step = 1;
            }

            position = (position + step) % lights.length;
            round++;
        }
    }

    public void switchOff() {
        for (int i = 0; i < lights.length; i++) {
            lights[i] = false;
        }
    }

    public int countLightsOn() {
        int count = 0;
        for (int i = 0; i < lights.length; i++) {
            if (lights[i]) {
                count++;
            }
        }
        return count;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lights.length; i++) {
            if (lights[i]) {
                sb.append('o');
            } else {
                sb.append('-');
            }
        }
        return sb.toString();
    }
}
