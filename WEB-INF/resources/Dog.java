public class Dog {

	protected int bark = 4;
	protected int bite = 3;

	public Dog() {
		bite--;
	}

	public boolean isDogDangerous() {
		if (bark > bite) {
			return false;
		}
		else {
			return true;
		}
	}
}