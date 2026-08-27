/*
 * Copyright (C) 2016-2025 Code Defenders contributors
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
package org.codedefenders.persistence.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.ResultSetUtils;
import org.codedefenders.util.Constants;
import org.codedefenders.util.FileUtils;
import org.codedefenders.validation.code.CodeValidationResult;
import org.intellij.lang.annotations.Language;

import static org.codedefenders.validation.code.CodeValidationResult.Type.MUTANT;

/**
 * Saves rejected tests or mutants, so researchers can gather information on how often submissions are rejected.
 * Only for statistical purposes, they are not used by CodeDefenders itself.
 * Information on the violated rules and, if applicable, the offending statements is saved to
 * {@code rejection_reasons}, with general information saved in {@code rejected_submissions}. The
 * code itself is saved in {@code data.dir/rejects/($ID).txt}, where ID is the ID in {@code rejected_submissions}.
 *
 * <br>
 * Submissions that are rejected because they cannot compile are not saved through this system, they can be
 * found by searching for tests/mutants with {@code ClassFile = NULL} in the DB.
 */
@ApplicationScoped
public class ValidationRepository {

    @Inject
    QueryRunner queryRunner;
    @Inject
    private PlayerRepository playerRepository;

    /**
     * Save information on a test or mutant that has been rejected by the validation system.
     * @param code The code of the test or mutant
     * @param userId The ID of the user who tried to submit
     * @param gameId The ID of the game in which the submission was attempted
     * @param result The validation result that contains the rejection reasons. Must be invalid.
     */
    public void saveRejectedSubmission(String code, int userId, int gameId, CodeValidationResult result) {
        if (result.isValid()) {
            throw new IllegalArgumentException("Valid submissions must not be saved here.");
        }
        int playerId = playerRepository.getPlayerIdForUserAndGame(userId, gameId);
        int id = saveSubmission(playerId, result.getType(), code);
        for (CodeValidationResult.RuleViolation<?> rule : result.getRuleViolations()) {
            @Language("SQL")
            String sql = """
                    INSERT INTO rejection_reasons(Reject_ID, General_description, Detailed_description, Validation_message, Reason)
                        VALUE (?, ?, ?, ?, ?)
                    """;
            queryRunner.update(sql,
                    id,
                    rule.rule().getGeneralDescription(),
                    rule.rule().getDetailedDescription(),
                    rule.rule().getValidationMessage(),
                    rule.getReasonDescription());
        }

    }

    /**
     * Saves a mutant that has been rejected because an identical mutant already exists.
     * @param originalMutantId The ID of the original mutant. Will be saved in the {@code Reason} column.
     */
    public void saveDuplicateMutant(String code, int userId, int gameId, int originalMutantId) {
        int playerId = playerRepository.getPlayerIdForUserAndGame(userId, gameId);
        int id = saveSubmission(playerId, MUTANT, code);
        queryRunner.update("insert into rejection_reasons(Reject_ID, General_description, Detailed_description, Validation_message, Reason) VALUE (?, ?, ?, ?, ?);",
                id, "NO DUPLICATE MUTANTS", "NO DUPLICATE MUTANTS", Constants.MUTANT_DUPLICATED_MESSAGE, "" + originalMutantId);
    }

    /**
     * Saves a test that fails on the CuT. No further information is saved.
     */
    public void saveTestsThatFailOnCut(String code, int userId, int gameId) {
        int playerId = playerRepository.getPlayerIdForUserAndGame(userId, gameId);
        int id = saveSubmission(playerId, MUTANT, code);
        queryRunner.update("insert into rejection_reasons(Reject_ID, General_description, Detailed_description, Validation_message, Reason) VALUE (?, ?, ?, ?, ?);",
                id, "FAILED ON CUT", "FAILED ON CUT", "FAILED ON CUT", "");
    }

    private int saveSubmission(int playerId, CodeValidationResult.Type submissionType, String code) {
        int id = queryRunner.insert("insert into rejected_submissions(Player_ID, Submission_type) VALUE (?,?);",
                ResultSetUtils.generatedKeyFromRS(),
                playerId,
                submissionType.toString()).orElseThrow();
        try {
            Path filepath = Files.createDirectories(FileUtils.getAbsoluteDataPath("rejects")).resolve(id + ".txt");
            Files.writeString(filepath, code);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return id;
    }
}
