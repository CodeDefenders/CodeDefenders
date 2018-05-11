package org.codedefenders.game.multiplayer;

import org.codedefenders.game.GameClass;
import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.analysis.ILine;
import org.jacoco.core.tools.ExecFileLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
        // Use heuristics to include uncoverable lines
        matchNonCoverableCode(clazz);

    }

	private void matchNonCoverableCode(GameClass clazz) {
		Set<Integer> linesToAdd = new HashSet<>();
		// If there's at least one line covered, then static field initializer and compile time constants are covered
		if( linesCovered.size() > 0 ){
			linesToAdd.addAll(clazz.getLinesOfCompileTimeConstants());
			linesToAdd.addAll(clazz.getLinesOfNonInitializedFields());
		}

		// Now we need to map lines covered with methods and then-branches of ifstatements in the class
		for( Integer coveredLine : linesCovered ){
			linesToAdd.addAll( clazz.getLinesOfMethodSignaturesFor(coveredLine));
			linesToAdd.addAll( clazz.getLineOfClosingBracketFor(coveredLine));
			// If covered line belongs to method, add the method signature
		}
		//
		linesCovered.addAll( linesToAdd );
		linesUncovered.removeAll( linesToAdd );
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