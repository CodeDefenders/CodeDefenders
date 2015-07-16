package gammut;

import java.io.File;
import java.util.*;
import diff_match_patch.*;

public class Mutant {

	File folder;
	String className;
	private boolean alive = true;
	private int pointsScored = 0;

	private LinkedList<diff_match_patch.Diff> diffs;

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

	public void setDifferences(LinkedList<diff_match_patch.Diff> diffs) {
		this.diffs = diffs;
	}

	public ArrayList<diff_match_patch.Diff> getDifferences() {
		ArrayList<diff_match_patch.Diff> diffArray = new ArrayList<diff_match_patch.Diff>();

		for (diff_match_patch.Diff d : diffs) {
			if (d.operation != diff_match_patch.Operation.EQUAL) {
				diffArray.add(d);
			}
		}
		return diffArray;
	}

}