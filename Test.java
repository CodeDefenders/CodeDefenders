import java.io.File;

public class Test {

	File folder;
	String className;

	private int pointsScored = 0;

	public Test(File folder, String className) {
		this.folder = folder;
		this.className = className;
	}


	public String getFolder() {
		return folder.getAbsolutePath();
	}

	public String getJava() {
		return folder.getAbsolutePath() + "Test" + className + ".java";
	}

	public String getClassFile() {
		return folder.getAbsolutePath() + "Test" + className + ".class";
	}

	public void scorePoints(int p) {
		pointsScored += p;
	}

	public int getPoints() {
		return pointsScored;
	}
}