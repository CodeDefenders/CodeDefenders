package org.codedefenders.multiplayer;

import org.jacoco.core.analysis.*;

import org.jacoco.core.tools.ExecFileLoader;

import java.io.*;
import java.util.ArrayList;

/**
 * Created by thoma on 16/06/2016.
 */
public class CoverageGenerator {

    private final File executionDataFile;
    private final File classesDirectory;

    private ArrayList<Integer> linesCovered;
    private ArrayList<Integer> linesUncovered;

    private ExecFileLoader execFileLoader;

    public CoverageGenerator(File reportDirectory, File classDirectory) {
        this.executionDataFile = new File(reportDirectory, "jacoco.exec");
        this.classesDirectory = classDirectory;
        this.linesCovered = new ArrayList<>();
        this.linesUncovered = new ArrayList<>();
    }

    /**
     * Create the report.
     *
     * @throws IOException
     */
    public void create(String clazz) throws IOException {
        loadExecutionData();
        analyzeStructure(clazz);

    }

    private void loadExecutionData() throws IOException {
        execFileLoader = new ExecFileLoader();
        execFileLoader.load(executionDataFile);
    }

    private void analyzeStructure(String clazz) throws IOException {
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(
                execFileLoader.getExecutionDataStore(), coverageBuilder);

        analyzer.analyzeAll(classesDirectory);

        for (IClassCoverage cc : coverageBuilder.getClasses()){
            if (cc.getName() != null && cc.getName().startsWith(clazz)) {
                for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
                    ILine line = cc.getLine(i);
                    if (line.getInstructionCounter().getStatus() == ICounter.FULLY_COVERED) {
                        linesCovered.add(i);
                    } else if (line.getInstructionCounter().getStatus() == ICounter.NOT_COVERED){
                        linesUncovered.add(i);
                    }
                }
            }
        }
    }
    public ArrayList<Integer> getLinesCovered(){
        return linesCovered;
    }

    public ArrayList<Integer> getLinesUncovered(){
        return linesUncovered;
    }

}