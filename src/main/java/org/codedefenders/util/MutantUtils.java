/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.DeltaType;
import com.github.difflib.patch.Patch;
import org.codedefenders.analysis.gameclass.MethodDescription;

/**
 * Basic utilities for mutants.
 *
 * @author gambi
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
        List<String> sutLines = new ArrayList<>(Arrays.asList(originalCode.split("\\r?\\n")));
        List<String> mutantLines = new ArrayList<>(Arrays.asList(mutatedCode.split("\\r?\\n")));

        Patch<String> differences = DiffUtils.diff(sutLines, mutantLines);
        List<Integer> reversedLinesToDelete = new ArrayList<>();
        // Position are 0-indexed
        for (AbstractDelta<String> delta : differences.getDeltas()) {
            final DeltaType type = delta.getType();
            switch (type) {
                case CHANGE:
                    if (delta.getSource().size() >= delta.getTarget().size()) {
                        break;
                    }
                case INSERT:
                    for (int nestedIndex = 0; nestedIndex < delta.getTarget().getLines().size(); nestedIndex++) {
                        String line = delta.getTarget().getLines().get(nestedIndex);
                        if (line != null) {
                            if (line.trim().length() == 0) {
                                int pos = delta.getTarget().getPosition() + nestedIndex;
                                reversedLinesToDelete.add(0, pos);
                            }
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        // Now remove the lines from the last to the first
        for (Integer index : reversedLinesToDelete) {
            mutantLines.remove(index.intValue());
        }
        return String.join("\n", mutantLines);
    }

    public static boolean isOutsideOfMethods(List<MethodDescription> methodDescriptions, Patch<String> differences) { //TODO woanders hin
        methodDescriptions = new ArrayList<>(methodDescriptions);
        methodDescriptions.sort(Comparator.comparingInt(MethodDescription::getStartLine));

        List<AbstractDelta<String>> deltas = new ArrayList<>(differences.getDeltas());
        deltas.sort(Comparator.comparingInt(d -> d.getSource().getPosition()));

        for (int i = 0; i < methodDescriptions.size(); i++) {
            MethodDescription m = methodDescriptions.get(i);
            for (AbstractDelta<String> d : deltas) {
                Chunk<String> source = d.getSource();
                int startInSource = source.getPosition() + 1;
                int endInSource = startInSource + source.getLines().size() - 1;
                if (startInSource > m.getEndLine()) {
                    //No mutation inside this method.
                    if (i == methodDescriptions.size() - 1) {
                        //The mutation is located after the last method.
                        return true;
                    }
                    break;
                } else if (endInSource < m.getStartLine()) {
                    //This delta ends before the method starts.
                    if (i == 0 || methodDescriptions.get(i - 1).getEndLine() < startInSource) {
                        //The mutation is located before the first method or between two methods.
                        return true;
                    }
                    continue;
                } else if (startInSource > m.getStartLine()) {
                    //Mutation starts inside the method.
                    if (endInSource >= m.getEndLine()) {
                        //Mutation ends after the method. There must be changes outside the method.
                        return true;
                    } else {
                        //Mutation ends inside the method.
                        continue;
                    }
                } else {
                    //The first line of the method is inside the mutation. Since it is possible that the method
                    //declaration has been changed, we cannot easily determine if the mutation is outside the
                    // method without actually parsing the code. Best to assume that there is code outside the
                    // method.
                    return true;
                }
            }
        }
        //No code outside of methods has been found.
        return false;
    }
}
