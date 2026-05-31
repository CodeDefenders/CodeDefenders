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
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.dto.TextSetting;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;

@ApplicationScoped
public class TextSettingsRepository {
    private static final Logger logger = LoggerFactory.getLogger(TextSettingsRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public TextSettingsRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public List<TextSetting> getTextSettings(String language) {
        @Language("SQL")
        String query = """
        SELECT *
        FROM text_settings
        WHERE Language = ?
        """;

        return queryRunner.query(
                query,
                listFromRS(TextSettingsRepository::textSettingFromRS),
                language
        );
    }

    private static TextSetting textSettingFromRS(ResultSet rs) throws SQLException {
        return new TextSetting(
                TextSetting.SETTING_NAME.valueOf(rs.getString("Name")),
                rs.getString("Language"),
                rs.getString("Value")
        );
    }

    public boolean updateTextSetting(TextSetting setting) {
        @Language("SQL")
        String query = """
        INSERT INTO text_settings (Name, Language, Value)
        VALUES (?, ?, ?)
        ON DUPLICATE KEY UPDATE Value = VALUES(Value)
        """;

        int rowsAffected = queryRunner.update(
                query,
                setting.name().name(),
                setting.language(),
                setting.value()
        );
        return rowsAffected > 0;
    }

    public Optional<TextSetting> getTextSetting(String language, TextSetting.SETTING_NAME settingName) {
        @Language("SQL")
        String query = """
        SELECT *
        FROM text_settings
        WHERE Language = ? AND Name = ?
        """;

        return Optional.ofNullable(queryRunner.query(
                query,
                rs -> {
                    if (rs.next()) {
                        return textSettingFromRS(rs);
                    } else {
                        return null;
                    }
                },
                language,
                settingName.name()
        ));
    }
}
