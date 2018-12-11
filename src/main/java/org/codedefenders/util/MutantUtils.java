package org.codedefenders.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.codedefenders.game.Mutant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Basic utilities for mutants
 * 
 * @author gambi
 *
 */
public class MutantUtils {

    private static final Logger logger = LoggerFactory.getLogger(MutantUtils.class);

    public void storeMutantToFile(String mutantFileName, String mutatedCode) throws IOException {
        File mutantFile = new File(mutantFileName);
        try (FileWriter fw = new FileWriter(mutantFile); BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(mutatedCode);
        }
    }

    public String cleanUpMutatedCode(String originalCode, String mutatedCode) {
        // From https://stackoverflow.com/questions/454908/split-java-string-by-new-line
            List<String> sutLines = Arrays.asList( originalCode.split("\\r?\\n") );
            List<String> mutantLines = new ArrayList(Arrays.asList( mutatedCode.split("\\r?\\n") ));

            Patch differences = DiffUtils.diff(sutLines, mutantLines);
            List<Integer> reversedLinesToDelete = new ArrayList<>();
            // Position are 0-indexed
            for(Delta delta : differences.getDeltas() ){
                if ( Delta.TYPE.INSERT.equals( delta.getType() ) ) {
                    // Position is the start of the modification, nestedIndex is relative to that position
                    for(int nestedIndex = 0; nestedIndex < delta.getRevised().getLines().size(); nestedIndex++ ){
                        Object o = delta.getRevised().getLines().get( nestedIndex );
                        if( o instanceof String ){
                            String line = (String) o;
                            if(line.trim().length() == 0 ){
                                int pos = delta.getRevised().getPosition() + nestedIndex;
                                reversedLinesToDelete.add(0, pos);
                            }
                        }
                    }
                }  else if (Delta.TYPE.CHANGE.equals( delta.getType() ) ) {
                    // Inserting blank lines in between code results in a change  and not an insert
                    // However, the size of the change must be larger 
                    if( delta.getOriginal().size() < delta.getRevised().size() ){
                        // At this point we can remove from the change those lines which where blank
                        for(int nestedIndex = 0; nestedIndex < delta.getRevised().getLines().size(); nestedIndex++ ){
                            Object o = delta.getRevised().getLines().get( nestedIndex );
                            if( o instanceof String ){
                                String line = (String) o;
                                if(line.trim().length() == 0 ){
                                    int pos = delta.getRevised().getPosition() + nestedIndex;
                                    reversedLinesToDelete.add(0, pos);
                                }
                            }
                        }   
                    }
                }
            }
            // Now remove the lines from the last to the first
            for( Integer index : reversedLinesToDelete ){
                mutantLines.remove( index.intValue());
            }
        return String.join("\n", mutantLines);
    }

    //  Is this specifi to Mutant or can this be moved to MutantUtils instead?
    public List<String> readLinesIfFileExist(Path path) {
    List<String> lines = new ArrayList<>();
    try {
        if (Files.exists(path))
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        else {
            logger.error("File not found {}", path);
        }
    } catch (IOException e) {
        e.printStackTrace();  // TODO handle properly
        return null;
    }
    return lines;
}
}
