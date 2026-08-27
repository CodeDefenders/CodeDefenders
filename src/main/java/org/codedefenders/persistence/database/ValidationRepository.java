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

@ApplicationScoped
public class ValidationRepository {

    @Inject
    QueryRunner queryRunner;
    @Inject
    private PlayerRepository playerRepository;

    public void saveRejectedSubmission(String code, int userId, int gameId, CodeValidationResult result) {
        saveRejectedSubmission(code, playerRepository.getPlayerIdForUserAndGame(userId, gameId), result);
    }

    public void saveRejectedSubmission(String code, int playerId, CodeValidationResult result) {
        if (result.isValid()) {
            throw new IllegalArgumentException("Valid submissions must not be saved here.");
        }
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

    public void saveDuplicateMutant(String code, int userId, int gameId, int originalMutantId) {
        int playerId = playerRepository.getPlayerIdForUserAndGame(userId, gameId);
        int id = saveSubmission(playerId, MUTANT, code);
        queryRunner.update("insert into rejection_reasons(Reject_ID, General_description, Detailed_description, Validation_message, Reason) VALUE (?, ?, ?, ?, ?);",
                id, "NO DUPLICATE MUTANTS", "NO DUPLICATE MUTANTS", Constants.MUTANT_DUPLICATED_MESSAGE, "" + originalMutantId);
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
