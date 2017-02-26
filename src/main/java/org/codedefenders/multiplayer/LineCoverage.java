package org.codedefenders.multiplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thoma on 16/06/2016.
 */
public class LineCoverage {

    private List<Integer> linesCovered = new ArrayList<>();

    private List<Integer> linesUncovered = new ArrayList<>();

    public void setLinesCovered (List<Integer> lines) {
        linesCovered.clear();
        linesCovered.addAll(lines);
    }

    public List<Integer> getLinesCovered(){
        return linesCovered;
    }

    public List<Integer> getLinesUncovered(){
        return linesUncovered;
    }

    public void setLinesUncovered (List<Integer> lines){
        linesUncovered.clear();
        linesUncovered.addAll(lines);
    }

    public String toString(){
        return "Covered: " + linesCovered.size() + ", Uncovered: " + linesUncovered.size();
    }
}
