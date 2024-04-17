package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.auth.CodeDefendersRealm;
import org.codedefenders.auth.roles.Role;
import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;

/**
 * Handles system-wide roles for authorization.
 */
@Named("roleRepo")
@ApplicationScoped
public class RoleRepository {
    private static final Logger logger = LoggerFactory.getLogger(RoleRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public RoleRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    private String roleNameFromRS(ResultSet rs) throws SQLException {
        return rs.getString("Role");
    }

    public List<String> getRoleNamesForUser(int userId) {
        @Language("SQL") String query = """
                SELECT *
                FROM roles
                WHERE User_ID = ?;
        """;

        try {
            return queryRunner.query(query,
                    listFromRS(this::roleNameFromRS),
                    userId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }
}
