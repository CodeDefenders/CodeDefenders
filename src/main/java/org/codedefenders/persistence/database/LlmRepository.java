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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.model.llm.PromptType;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.QueryUtils;
import org.codedefenders.service.llm.NoSuchModelException;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class LlmRepository {
    private static final Logger logger = LoggerFactory.getLogger(LlmRepository.class);

    private static final String DEFAULT_MODEL_NAME = "DEFAULT_MODEL";

    private static final String DEFAULT_ATTACKER_PROMPT =
            """
                    Change the first class of the following java code in a significant way.
                    The behaviour of the program should change.
                    Only change existing methods and fields.
                    Write nothing but the changed java code.
                    Make sure to introduce at least one change.

                    You might see some diffs of mutants at the end of the user message.
                    Those mutants already exist, create different ones.""".trim().stripIndent();

    private static final String DEFAULT_DEFENDER_PROMPT =
            """
                    Write a single test for the following Java code using a maximum of 2 assertions.
                    Write only the content of the test method, without including formatting, comments,
                    the header or the method declaration. Use JUnit 4.
                    """.trim().stripIndent();

    private static final String DEFAULT_ATTACKER_DEPS_PROMPT =
            """
                    Change the following java code in a significant way.
                    The behaviour of the program should change.
                    Only change existing methods and fields.
                    Write nothing but the changed java code of the first class. don't change any
                    other classes.
                    Make sure to introduce at least one change.

                    You might see some diffs of mutants at the end of the user message.
                    Those mutants already exist, create different ones.""".trim().stripIndent();

    private static final String DEFAULT_RESOLVE_EQUIVALENT_PROMPT =
            """
                    You will see the code of a java class and then a git diff of a change. Write a test using
                    JUnit 4 that succeeds on the old version, but fails after the diff is applied.
                    Use a maximum of 2 assertions.
                     Write only the content of the test method, without including formatting, comments,
                    the header or the method declaration. Use JUnit 4.
                    """.trim().stripIndent();

    private static final String DEFAULT_DEFENDER_DEPS_PROMPT =
            """
                    Write a single test for the first class of the following Java code using a maximum of 2 assertions.
                    The other classes are dependencies of the first class, you don't need to test them.
                    Write only the content of the test method, without including formatting, comments,
                    the header or the method declaration. Use JUnit 4.
                    """.trim().stripIndent();

    private static final String DEFAULT_DEFENDER_FOCUS_PROMPT =
            """
                    Write a single test for the method %s of the following Java code using a maximum of 2 assertions.
                    Write only the content of the test method, without including formatting, comments,
                    the header or the method declaration. Use JUnit 4.
                    """.trim().stripIndent();

    @Inject
    Configuration config;

    @Inject
    QueryRunner queryRunner;

    /**
     * Add models from the config file to DB. Duplicates are ignored.
     */
    public void addNewModels() {
        @Language("SQL")
        String sql = "INSERT IGNORE INTO llm_models(model_name, type) VALUES (?,?)";
        Object[][] params = QueryUtils.extractBatchParams(config.getLlmOllamaModels(),
                model -> model, model -> "OLLAMA");
        queryRunner.batch(sql, params);

        params = QueryUtils.extractBatchParams(config.getLlmOpenaiModels(),
                model -> model, model -> "OPENAI");
        queryRunner.batch(sql, params);

        boolean defaultValuesExists =
                queryRunner.query("SELECT model_name FROM llm_models WHERE model_name = ?",
                        ResultSet::isBeforeFirst, DEFAULT_MODEL_NAME);
        if (!defaultValuesExists) {
            resetDefaultModel();
        }
    }

    public void resetDefaultModel() {
        LlModel defaultModel = new LlModel(DEFAULT_MODEL_NAME, LlmType.DEFAULT);
        defaultModel.setPrompt(PromptType.ATTACK_DEFAULT, DEFAULT_ATTACKER_PROMPT);
        defaultModel.setPrompt(PromptType.ATTACK_DEPENDENCIES, DEFAULT_ATTACKER_DEPS_PROMPT);
        defaultModel.setPrompt(PromptType.ATTACK_EQUIVALENCE, DEFAULT_RESOLVE_EQUIVALENT_PROMPT);
        defaultModel.setPrompt(PromptType.DEFEND_DEFAULT, DEFAULT_DEFENDER_PROMPT);
        defaultModel.setPrompt(PromptType.DEFEND_DEPENDENCIES, DEFAULT_DEFENDER_DEPS_PROMPT);
        defaultModel.setPrompt(PromptType.DEFEND_FOCUS, DEFAULT_DEFENDER_FOCUS_PROMPT);

        saveModel(defaultModel);

    }

    /**
     * Saves the values of the model in the database. If the model already exists, update its values,
     * if it doesn't exist, create a new entry.
     */
    public void saveModel(LlModel model) {
        boolean alreadyExists =
                queryRunner.query("SELECT model_name FROM llm_models WHERE model_name = ? AND type = ?",
                        ResultSet::isBeforeFirst, model.getName(), model.getType().name());

        @Language("SQL")
        String sql;
        if (alreadyExists) {
            sql = """
                    update llm_models set
                    defender_dependencies = ?,
                    defender_method_focus = ?,
                    attacker_dependencies = ?,
                    active = ?
                    WHERE model_name = ? AND type = ?;
                    """;
        } else {
            sql = """
                    insert into llm_models(
                                           defender_dependencies,
                                           defender_method_focus,
                                           attacker_dependencies,
                                           active, model_name, type)
                    values (?,?,?,?,?,?)""";
        }
        queryRunner.update(sql,
                model.isDefenderDependencies(),
                model.isDefenderMethodFocus(),
                model.isAttackerDependencies(),
                model.isActive(), model.getName(), model.getType().name());

        updatePrompts(model);
    }

    /**
     * Only changes the prompts and the prompt availabilities of an existing model.
     * Throws an exception if that model doesn't exist.
     * This also deletes prompts in the DB if they are not present in the model.
     *
     * @param model The model, identified by type and name, containing the new prompts.
     */
    public void updatePrompts(LlModel model) {
        Set<PromptType> promptTypes = model.getCustomPromptTypes();

        @Language("SQL")
        String updateSql = """
                update llm_models set attacker_dependencies = ?, defender_dependencies = ?, defender_method_focus = ?
                where type = ? and model_name = ?
                """;
        queryRunner.update(updateSql, model.isAttackerDependencies(), model.isDefenderDependencies(),
                model.isDefenderMethodFocus(), model.getType().name(), model.getName());

        @Language("SQL")
        String insertSql = """
                insert into llm_prompts(llm_prompts.model_name, llm_prompts.model_type, llm_prompts.prompt_type, prompt)
                values (?,?,?,?) on duplicate key update prompt = VALUES(prompt)""";
        Object[][] insertArguments = QueryUtils.extractBatchParams(promptTypes,
                p -> model.getName(),
                p -> model.getType().name(),
                Enum::name,
                p -> model.getPrompt(p).orElse(""));

        queryRunner.batch(insertSql, insertArguments);

        @Language("SQL")
        String purgeSql = """
                delete from llm_prompts where model_type = ? and model_name = ? and prompt_type not in (%s)
                """;
        purgeSql = String.format(purgeSql, QueryUtils.makePlaceholders(promptTypes.size()));

        List<String> purgeArguments = new ArrayList<>();
        purgeArguments.add(model.getType().name());
        purgeArguments.add(model.getName());
        purgeArguments.addAll(promptTypes.stream().map(PromptType::name).toList());
        queryRunner.update(purgeSql, purgeArguments.toArray());
    }

    public void setActive(String name, LlmType type, boolean active) {
        @Language("SQL")
        String sql = "UPDATE llm_models SET active = ? WHERE model_name = ? and type = ?";
        queryRunner.update(sql, active, name, type.name());
    }

    /**
     * Update the values of an existing {@link LlModel}. It is identified by type and name, all other values are filled
     * up from DB.
     *
     * @throws org.codedefenders.service.llm.NoSuchModelException If there is no model with this type and name
     *                                                            in the database.
     */
    public void loadModel(LlModel model) throws NoSuchModelException {
        LlModel fromDB = getModelFromName(model.getName(), model.getType(), false).orElseThrow(
                () -> new NoSuchModelException(model.getType(), model.getName())
        );
        model.copyValues(fromDB);
    }

    public Optional<LlModel> getDefaultModel() {
        @Language("SQL")
        String sql = """
                SELECT * FROM llm_models
                    LEFT JOIN llm_prompts ON
                        llm_models.model_name = llm_prompts.model_name
                        AND llm_models.type = llm_prompts.model_type
                    WHERE type = ?;
                """;
        return queryRunner.query(sql, LlmRepository::oneModelFromRs, LlmType.DEFAULT.name());
    }

    public Optional<LlModel> getModelFromName(String name, LlmType type, boolean mustBeActive) {
        @Language("SQL")
        String sql = """
                SELECT * FROM llm_models
                         LEFT JOIN llm_prompts ON
                            llm_models.model_name = llm_prompts.model_name
                            AND llm_models.type = llm_prompts.model_type
                         WHERE llm_models.model_name = ? AND llm_models.type = ?
                """
                + (mustBeActive ? " AND active = true" : ";");
        return queryRunner.query(sql, LlmRepository::oneModelFromRs, name, type.name());
    }

    public List<LlModel> getAllModels() {
        return getAllModels(false);
    }

    public List<LlModel> getAllModels(boolean mustBeActive) {
        @Language("SQL")
        String sql = """
                SELECT * FROM llm_models
                    LEFT JOIN llm_prompts
                        ON llm_models.model_name = llm_prompts.model_name
                        AND llm_models.type = llm_prompts.model_type
                WHERE type != ?""" + (mustBeActive ? " AND active = true" : "")
                + " ORDER BY llm_models.type, llm_models.model_name";

        List<LlModel> modelsInDB = queryRunner.query(
                sql, LlmRepository::fromRS, LlmType.DEFAULT.name());

        return modelsInDB.stream().filter(this::modelIsInConfig).toList();
    }

    private boolean modelIsInConfig(LlModel m) {
        return m.getType() == LlmType.OLLAMA && config.getLlmOllamaModels().contains(m.getName())
                || m.getType() == LlmType.OPENAI && config.getLlmOpenaiModels().contains(m.getName());
    }

    private static Optional<LlModel> oneModelFromRs(ResultSet rs) throws SQLException {
        List<LlModel> results = fromRS(rs);
        if (results.isEmpty()) {
            return Optional.empty();
        } else if (results.size() > 1) {
            throw new RuntimeException("Found two results, when there should be only one.");
        } else return Optional.of(results.get(0));
    }

    private static List<LlModel> fromRS(ResultSet rs) throws SQLException {
        List<LlModel> result = new ArrayList<>();
        LlModel currentModel = null;
        while (rs.next()) {
            String name = rs.getString("llm_models.model_name");
            LlmType type = LlmType.valueOf(rs.getString("llm_models.type"));
            if (currentModel == null || !currentModel.getName().equals(name) || currentModel.getType() != type) {
                currentModel = new LlModel(name, type);
                result.add(currentModel);
                currentModel.setDefenderDependencies(rs.getBoolean("defender_dependencies"));
                currentModel.setDefenderMethodFocus(rs.getBoolean("defender_method_focus"));
                currentModel.setAttackerDependencies(rs.getBoolean("attacker_dependencies"));
                currentModel.setActive(rs.getBoolean("active"));
            }

            String promptType = rs.getString("llm_prompts.prompt_type");
            String prompt = rs.getString("llm_prompts.prompt");
            if (promptType != null && prompt != null) {
                currentModel.setPrompt(PromptType.valueOf(promptType), prompt);
            }
        }

        return result;
    }


}
