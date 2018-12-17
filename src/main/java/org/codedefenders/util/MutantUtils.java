package org.codedefenders.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

/**
 * Basic utilities for mutants
 *
 * @author gambi
 *
 */
public class MutantUtils {
    public void storeMutantToFile(String mutantFileName, String mutatedCode) throws IOException {
        File mutantFile = new File(mutantFileName);
        try (FileWriter fw = new FileWriter(mutantFile); BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(mutatedCode);
        }
    }

    public String cleanUpMutatedCode(String originalCode, String mutatedCode) {
        // From https://stackoverflow.com/questions/454908/split-java-string-by-new-line
        List<String> sutLines = Arrays.asList(originalCode.split("\\r?\\n"));
        List<String> mutantLines = Arrays.asList(mutatedCode.split("\\r?\\n"));

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
}
