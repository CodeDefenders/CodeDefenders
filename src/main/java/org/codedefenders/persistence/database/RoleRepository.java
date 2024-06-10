package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.codedefenders.persistence.database.util.QueryRunner;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

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

    private int userIdFromRS(ResultSet rs) throws SQLException {
        return rs.getInt("User_ID");
    }

    public List<String> getRoleNamesForUser(int userId) {
        @Language("SQL") String query = """
                SELECT *
                FROM roles
                WHERE User_ID = ?;
        """;

        return queryRunner.query(query,
                listFromRS(this::roleNameFromRS),
                userId
        );
    }

    public void addRoleNameForUser(int userId, String roleName) {
        @Language("SQL") String query = """
                INSERT INTO roles (User_ID, Role)
                VALUES (?, ?)

                -- To avoid duplicate role entries. The key of the roles is (User_ID, Role).
                ON DUPLICATE KEY UPDATE roles.Role = Role;
        """;

        queryRunner.insert(query,
                rs -> null,
                userId,
                roleName);
    }

    public void removeRoleNameForUser(int userId, String roleName) {
        @Language("SQL") String query = """
                DELETE FROM roles
                WHERE User_ID = ?
                  AND Role = ?;
        """;

        queryRunner.update(query,
                userId,
                roleName);
    }

    public Multimap<Integer, String> getAllUserRoleNames() {
        @Language("SQL") String query = """
                SELECT *
                FROM roles;
        """;

        Multimap<Integer, String> roleNames = ArrayListMultimap.create();
        queryRunner.query(query,
            listFromRS(rs ->  roleNames.put(userIdFromRS(rs), roleNameFromRS(rs))));
        return roleNames;
    }
}
