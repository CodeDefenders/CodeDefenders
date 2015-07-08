import java.io.File;

public class Mutant {

	File folder;
	String className;
	private boolean alive = true;
	private int pointsScored = 0;

	public Mutant(File folder, String className) {
		this.folder = folder;
		this.className = className;
	}


	public String getFolder() {
		return folder.getAbsolutePath();
	}

	public String getJava() {
		return folder.getAbsolutePath() + className + ".java";
	}

	public String getClassFile() {
		return folder.getAbsolutePath() + className + ".class";
	}

	public boolean isAlive() {return alive;}
	public void setAlive(boolean a) {alive = a;}

	public void scorePoints(int p) {
		pointsScored += p;
	}

	public int getPoints() {
		return pointsScored;
	}

}