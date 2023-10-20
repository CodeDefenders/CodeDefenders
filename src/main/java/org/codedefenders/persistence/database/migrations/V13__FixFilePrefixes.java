/*
 * Copyright (C) 2021 Code Defenders contributors
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

package org.codedefenders.persistence.database.migrations;

import java.sql.Connection;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.dbutils.QueryRunner;
import org.codedefenders.configuration.Configuration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.intellij.lang.annotations.Language;

// Using @Singleton should lead to only the name of this class showing up in the flyway_schema_history_table script
// column instead of also including some WeldClientProxy foo.
@Singleton
public class V13__FixFilePrefixes extends BaseJavaMigration {

    private final QueryRunner queryRunner = new QueryRunner();
    private final String dataDir;

    @Inject
    public V13__FixFilePrefixes(@SuppressWarnings("CdiInjectionPointsInspection") Configuration config) {
        String dataDirPath = config.getDataDir().toString();
        if (!dataDirPath.endsWith("/")) {
            dataDir = dataDirPath + "/";
        } else {
            dataDir = dataDirPath;
        }
    }

    @Override
    public void migrate(Context context) throws Exception {
        // Note it is important to NOT close this connection!!
        Connection conn = context.getConnection();

        var tables = List.of(
                "classes",
                "dependencies",
                "mutants",
                "tests"
        );
        for (String table : tables) {
            // We explicitly want to update all rows in the table
            //noinspection SqlWithoutWhere
            @Language("SQL") String query = """
                    UPDATE %s
                    SET JavaFile = REGEXP_REPLACE(JavaFile, CONCAT('^', ?), ''),
                        ClassFile = REGEXP_REPLACE(ClassFile, CONCAT('^', ?), '');
            """.formatted(table);
            queryRunner.update(conn, query, Pattern.quote(dataDir), Pattern.quote(dataDir));
        }
    }
}
