package gammut;

import java.io.File;
import java.util.*;
import java.io.*;
import diff_match_patch.*;

public class Mutant {

	private int id;
	private int gameId;

	private byte[] javaFile;
	private byte[] classFile;

	private boolean alive = true;
	private boolean equivalent = false;
	private boolean suspectedEquivalent;
	private boolean declaredEquivalent;

	private int points = 0;

	private LinkedList<diff_match_patch.Diff> diffs;

	File folder;
	String className;

	public Mutant(File folder, String className) {
		this.folder = folder;
		this.className = className;
	}

	public Mutant(int gid, InputStream jStream, InputStream cStream) {
		this.gameId = gid;

		try {

			int nRead;

			ByteArrayOutputStream jBuffer = new ByteArrayOutputStream();

			while ((nRead = jStream.read()) != -1) {
				jBuffer.write(nRead);
			}

			jBuffer.flush();
			javaFile = jBuffer.toByteArray();

			ByteArrayOutputStream cBuffer = new ByteArrayOutputStream();

			while ((nRead = cStream.read()) != -1) {
				cBuffer.write(nRead);
			}

			cBuffer.flush();
			classFile = cBuffer.toByteArray();

		} 
		catch (IOException e) {System.out.println(e);}
	}

	public Mutant(int mid, int gid, InputStream jStream, InputStream cStream, boolean alive, boolean sEquiv, boolean dEquiv, int points) {
		this(mid, jStream, cStream);

		this.id = mid;
		this.alive = alive;
		this.suspectedEquivalent = sEquiv;
		this.declaredEquivalent = dEquiv;
		this.points = points;
	}


	public String getFolder() {return folder.getAbsolutePath();}
	public String getJava() {return folder.getAbsolutePath() + className + ".java";}
	public String getClassFile() {return folder.getAbsolutePath() + className + ".class";}

	public void setEquivalent(boolean e) {equivalent = e;}
	public boolean isEquivalent() {return equivalent;}

	public void setAlive(boolean a) {alive = a;}
	public boolean isAlive() {return alive;}

	public void scorePoints(int p) {points += p;}
	public int getPoints() {return points;}
	public void removePoints() {points = 0;}

	public void setDifferences(LinkedList<diff_match_patch.Diff> diffs) {this.diffs = diffs;}
	public ArrayList<diff_match_patch.Diff> getDifferences() {
		ArrayList<diff_match_patch.Diff> diffArray = new ArrayList<diff_match_patch.Diff>();

		for (diff_match_patch.Diff d : diffs) {
			if (d.operation != diff_match_patch.Operation.EQUAL) {
				diffArray.add(d);
			}
		}
		return diffArray;
	}

	public String getHTMLReadout() {
		String html = "";

        for (diff_match_patch.Diff d : getDifferences()) {
            if (d.operation == diff_match_patch.Operation.INSERT) {
            		html += "<p> +: " + d.text;
            }
            else {
            	html += "<p> -: " + d.text;
            }
        }
        html += "<br>";
        return html;
	}
}