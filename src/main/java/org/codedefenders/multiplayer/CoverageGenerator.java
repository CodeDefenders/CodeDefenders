package org.codedefenders.multiplayer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map.Entry;

import org.codedefenders.GameClass;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.tools.ExecFileLoader;

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
    public void create(GameClass clazz) throws IOException {
        loadExecutionData();
        // Original Code
        analyzeStructure(clazz);
        // Include Uncovered lines corresponding to Non initialized fields
        includeNonInitializedFields(clazz);

    }

	private void includeNonInitializedFields(GameClass clazz) {
		for (Entry<Integer, Integer> nonInitializedField : clazz.getLinesOfNonInitializedFields()) {
			for (int i = nonInitializedField.getKey(); i <= nonInitializedField.getValue(); i++) {
				linesCovered.add(i);
				if (linesUncovered.contains(i)) {
					linesUncovered.remove(i);
				}
			}
		}
	}

	private void loadExecutionData() throws IOException {
        execFileLoader = new ExecFileLoader();
        execFileLoader.load(executionDataFile);
    }

    private void analyzeStructure(GameClass clazz) throws IOException {
        final CoverageBuilder coverageBuilder = new CoverageBuilder();
        final Analyzer analyzer = new Analyzer(
                execFileLoader.getExecutionDataStore(), coverageBuilder);

        analyzer.analyzeAll(classesDirectory);

        for (IClassCoverage cc : coverageBuilder.getClasses()){
            String fullyQualifiedName = cc.getName().replace("/",".");
            if (fullyQualifiedName != null && fullyQualifiedName.startsWith(clazz.getName())) {
                for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {

					ILine line = cc.getLine(i);

                    if (line.getInstructionCounter().getStatus() == ICounter.FULLY_COVERED ||
                            line.getInstructionCounter().getStatus() == ICounter.PARTLY_COVERED) {
                        linesCovered.add(i);
                    }
                        else if (line.getInstructionCounter().getStatus() == ICounter.NOT_COVERED){
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