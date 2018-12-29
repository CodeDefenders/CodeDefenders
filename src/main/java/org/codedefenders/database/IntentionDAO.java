/**
 * Copyright (C) 2016-2018 Code Defenders contributors
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
package org.codedefenders.database;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.model.AttackerIntention;
import org.codedefenders.model.DefenderIntention;

import java.util.stream.Collectors;

/**
 * This class handles the database logic for player intentions.
 *
 * @see AttackerIntention
 * @see DefenderIntention
 */
public class IntentionDAO {

    /**
     * Stores a given {@link DefenderIntention} in the database.
     * <p>
     * For context information, the associated {@link Test} of the intention
     * is provided as well.
     *
     * @param test      the given test as a {@link Test}.
     * @param intention the given intention as an {@link DefenderIntention}.
     * @return the generated identifier of the intention as an {@code int}.
     * @throws Exception If storing the intention was not successful.
     */
    public static int storeIntentionForTest(Test test, DefenderIntention intention) throws Exception {
        int testId = test.getId();
        int gameId = test.getGameId();

        final String targetLines = intention.getLines().stream().map(String::valueOf).collect(Collectors.joining(","));

        final String query = String.join("\n",
                "INSERT INTO intention (Test_ID, Game_ID, Target_Lines)",
                "VALUES (?,?,?);"
        );

        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(testId),
                DatabaseValue.of(gameId),
                DatabaseValue.of(targetLines),
        };

        final int result = DB.executeUpdateQueryGetKeys(query, values);
        if (result != -1) {
            return result;
        } else {
            throw new Exception("Could not store defender intention to database.");
        }
    }

    /**
     * Stores a given {@link AttackerIntention} in the database.
     * <p>
     * For context information, the associated {@link Test} of the intention
     * is provided as well.
     *
     * @param mutant      the given mutant as a {@link Mutant}.
     * @param intention the given intention as an {@link AttackerIntention}.
     * @return the generated identifier of the intention as an {@code int}.
     * @throws Exception If storing the intention was not successful.
     */
    public static int storeIntentionForMutant(Mutant mutant, AttackerIntention intention) throws Exception {
        final String query = String.join("\n",
                "INSERT INTO intention (Mutant_ID, Game_ID, Target_Mutant_Type)",
                "VALUES (?,?,?);"
        );

        DatabaseValue[] values = new DatabaseValue[]{
                DatabaseValue.of(mutant.getId()),
                DatabaseValue.of(mutant.getGameId()),
                DatabaseValue.of(intention.toString()),
        };

        final int result = DB.executeUpdateQueryGetKeys(query, values);
        if (result != -1) {
            return result;
        } else {
            throw new Exception("Could not store attacker intention to database.");
        }
    }
}
