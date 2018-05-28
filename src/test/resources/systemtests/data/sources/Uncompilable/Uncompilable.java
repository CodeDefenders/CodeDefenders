public class Uncompilable {

	// Syntax error here
	private int topFloor

	// Missing field here
	// private int currentFloor = 0; // default
	private int capacity = 10; // default
	private int numRiders = 0; // default

	public Uncompilable(int highestFloor) {
		topFloor = highestFloor;
	}

	public Uncompilable(int highestFloor, int maxRiders) {
		this(highestFloor);
		capacity = maxRiders;
	}

	public int getTopFloor() {
		return topFloor;
	}

	public int getCurrentFloor() {
		return currentFloor;
	}
}
