package org.codedefenders.multiplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by thoma on 03/06/2016.
 */
public class ClassStatus {
    public List<LineCovered> lineCoverage;
    public List<LineMutant> lineMutants;

    public ClassStatus (){
        lineCoverage = new ArrayList<>();
        lineMutants = new ArrayList<>();
    }
}
