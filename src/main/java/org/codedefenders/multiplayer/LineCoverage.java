package org.codedefenders.multiplayer;

import java.util.ArrayList;

/**
 * Created by thoma on 16/06/2016.
 */
public class LineCoverage {
    public static LineCoverage NONE = new LineCoverage();
    private ArrayList<Integer> linesCovered = new ArrayList<>();
    private ArrayList<Integer> linesUncovered = new ArrayList<>();

    public void setLinesCovered (ArrayList<Integer> lines){ linesCovered = lines; }

    public ArrayList<Integer> getLinesCovered(){
        return linesCovered;
    }

    public ArrayList<Integer> getLinesUncovered(){
        return linesUncovered;
    }

    public void setLinesUncovered (ArrayList<Integer> lines){
        linesUncovered = lines;
    }

    public String toString(){
        return "Covered: " + linesCovered.size() + ", Uncovered: " + linesUncovered.size();
    }
}
