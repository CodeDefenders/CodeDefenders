package gammut;

import java.io.*;
import javax.servlet.*;

public class Test {

	File folder;
	String className;
	String text;

	boolean validTest = true;

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

	public void setText(String t) {text = t;}
	public String getText() {return text;}

	public void scorePoints(int p) {pointsScored += p;}
	public int getPoints() {return pointsScored;}

	public void setValidTest(boolean b) {validTest = b;}
	public boolean isValidTest() {return validTest;}

	public String getHTMLReadout() throws IOException {

        return "<code>" + getText().replace("\n", "<br>") + "</code>";
	}
}