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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.commons.lang3.ArrayUtils;
import org.codedefenders.configuration.Configuration;
import org.codedefenders.game.Role;
import org.codedefenders.model.llm.LlModel;
import org.codedefenders.model.llm.LlmDefaultStrategy;
import org.codedefenders.model.llm.LlmStrategy;
import org.codedefenders.model.llm.LlmType;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.persistence.database.util.QueryUtils;
import org.codedefenders.service.llm.NoSuchModelException;
import org.intellij.lang.annotations.Language;

@ApplicationScoped
public class LlmRepository {

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
    }

    /**
     * Set the model as active or inactive in the DB.
     */
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

    public Optional<LlModel> getModelFromName(String name, LlmType type, boolean mustBeActive) {
        @Language("SQL")
        String sql = """
                SELECT * FROM llm_models
                         WHERE llm_models.model_name = ? AND llm_models.type = ?
                """
                + (mustBeActive ? " AND active = true;" : ";");
        return queryRunner.query(sql, LlmRepository::oneModelFromRs, name, type.name());
    }

    public List<LlModel> getAllModels() {
        return getAllModels(false);
    }

    public List<LlModel> getAllModels(boolean mustBeActive) {
        @Language("SQL")
        String sql = """
                SELECT * FROM llm_models
                WHERE type != ?""" + (mustBeActive ? " AND active = true" : "")
                + " ORDER BY llm_models.type, llm_models.model_name";

        List<LlModel> modelsInDB = queryRunner.query(
                sql, LlmRepository::fromRS, LlmType.DEFAULT.name());

        return modelsInDB.stream().filter(this::modelIsInConfig).toList();
    }

    public Optional<LlmStrategy> getStrategyByName(String name) {
        if (ArrayUtils.contains(LlmDefaultStrategy.values(), name)) {
            return Optional.of(LlmStrategy.of(LlmDefaultStrategy.valueOf(name)));
        } else {
            //TODO Custom strategies!!
            throw new RuntimeException("NOT IMPLEMENTED");
        }
    }

    public List<LlmStrategy> getAllStrategies() {
        List<LlmStrategy> result = new ArrayList<>();
        for (LlmDefaultStrategy s : LlmDefaultStrategy.values()) {
            result.add(LlmStrategy.of(s));
        }
        //TODO Custom strategies!!
        return result;
    }

    public List<LlmStrategy> getStrategiesForRole(Role role) {
        String prefix = switch (role) {
            case ATTACKER -> "MUTANT";
            case DEFENDER -> "TEST";
            default -> throw new IllegalArgumentException("This role is not supported:" + role);
        };
        return getAllStrategies()
                .stream()
                .filter(s -> s.getBase().name().startsWith(prefix))
                .toList();
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
        } else {
            return Optional.of(results.get(0));
        }
    }

    private static List<LlModel> fromRS(ResultSet rs) throws SQLException {
        List<LlModel> result = new ArrayList<>();
        while (rs.next()) {
            String name = rs.getString("llm_models.model_name");
            LlmType type = LlmType.valueOf(rs.getString("llm_models.type"));
            boolean active = rs.getBoolean("llm_models.active");
            result.add(new LlModel(name, type, active));
        }

        return result;
    }


}
