package org.codedefenders.multiplayer;

import javax.sound.sampled.Line;

/**
 * Created by thoma on 16/06/2016.
 */
public class LineCoverage {
    public static LineCoverage NONE = new LineCoverage();
    private Integer[] linesCovered = new Integer[0];
    private Integer[] linesUncovered = new Integer[0];

    public void setLinesCovered (Integer[] lines){ linesCovered = lines; }

    public Integer[] getLinesCovered(){
        return linesCovered;
    }

    public Integer[] getLinesUncovered(){
        return linesUncovered;
    }

    public void setLinesUncovered (Integer[] lines){
        linesUncovered = lines;
    }

    public String toString(){
        return "Covered: " + linesCovered.length + ", Uncovered: " + linesUncovered.length;
    }
}
