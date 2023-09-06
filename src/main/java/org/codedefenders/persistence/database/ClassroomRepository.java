package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.Classroom;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.oneFromRS;

@Transactional
@ApplicationScoped
public class ClassroomRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClassroomRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public ClassroomRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public Optional<Integer> storeClassroom(Classroom classroom) {
        if (classroom.getId() > 0) {
            throw new IllegalArgumentException("Can't insert classroom with id > 0");
        }

        @Language("SQL") String query = """
                INSERT INTO classrooms
                (UUID, Creator_ID, Name, Password, Open, Visible, Archived)
                VALUES (?, ?, ?, ?, ?, ?, ?);
        """;

        try {
            return queryRunner.insert(query,
                    oneFromRS(rs -> rs.getInt(1)),
                    classroom.getUUID().toString(),
                    classroom.getCreatorId().orElse(null),
                    classroom.getName(),
                    classroom.getPassword().orElse(null),
                    classroom.isOpen(),
                    classroom.isVisible(),
                    classroom.isArchived()
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public void updateClassroom(Classroom classroom) {
        @Language("SQL") String query = """
                UPDATE classrooms
                SET UUID = ?,
                  Creator_ID = ?,
                  Name = ?,
                  Password = ?,
                  Open = ?,
                  Visible = ?,
                  Archived = ?
                WHERE classrooms.ID = ?
        """;
        try {
             int updatedRows = queryRunner.update(query,
                     classroom.getUUID().toString(),
                     classroom.getCreatorId().orElse(null),
                     classroom.getName(),
                     classroom.getPassword().orElse(null),
                     classroom.isOpen(),
                     classroom.isVisible(),
                     classroom.isArchived(),
                     classroom.getId()
            );
            if (updatedRows != 1) {
                throw new UncheckedSQLException("Couldn't update classroom.");
            }
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public Optional<Classroom> getClassroomById(int id) {
        @Language("SQL") String query = """
                SELECT * FROM classrooms
                WHERE ID = ?;
        """;
        try {
            return queryRunner.query(query,
                    oneFromRS(this::classroomFromRS),
                    id
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public Optional<Classroom> getClassroomByUUID(UUID uuid) {
        @Language("SQL") String query = """
                SELECT * FROM classrooms
                WHERE UUID = ?;
        """;
        try {
            return queryRunner.query(query,
                    oneFromRS(this::classroomFromRS),
                    uuid.toString()
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getAllClassrooms() {
        @Language("SQL") String query = "SELECT * FROM classrooms;";
        try {
            return queryRunner.query(query, listFromRS(this::classroomFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getAllClassroomsByMember(int userId) {
        @Language("SQL") String query = """
                SELECT classrooms.* FROM classrooms, classroom_members
                WHERE classrooms.ID = classroom_members.Classroom_ID
                AND classroom_members.User_ID = ?;
        """;
        try {
            return queryRunner.query(query,
                    listFromRS(this::classroomFromRS),
                    userId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getActiveClassrooms() {
        @Language("SQL") String query = "SELECT * FROM classrooms WHERE Archived = 0;";
        try {
            return queryRunner.query(query, listFromRS(this::classroomFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getActiveClassroomsByMember(int userId) {
        @Language("SQL") String query = """
                SELECT classrooms.* FROM classrooms, classroom_members
                WHERE classrooms.ID = classroom_members.Classroom_ID
                AND classroom_members.User_ID = ?
                AND classrooms.Archived = 0;
        """;
        try {
            return queryRunner.query(query,
                    listFromRS(this::classroomFromRS),
                    userId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getActiveClassroomsByMemberAndRole(int userId, ClassroomRole role) {
        @Language("SQL") String query = """
                SELECT classrooms.* FROM classrooms, classroom_members
                WHERE classrooms.ID = classroom_members.Classroom_ID
                AND classroom_members.User_ID = ?
                AND classroom_members.Role = ?
                AND classrooms.Archived = 0;
        """;
        try {
            return queryRunner.query(query,
                    listFromRS(this::classroomFromRS),
                    userId,
                    role.name()
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getVisibleClassrooms() {
        @Language("SQL") String query = "SELECT * FROM classrooms WHERE Archived = 0 AND Visible = 1;";
        try {
            return queryRunner.query(query, listFromRS(this::classroomFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getArchivedClassrooms() {
        @Language("SQL") String query = "SELECT * FROM classrooms WHERE Archived = 1;";
        try {
            return queryRunner.query(query, listFromRS(this::classroomFromRS));
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getArchivedClassroomsByMember(int userId) {
        @Language("SQL") String query = """
                SELECT classrooms.* FROM classrooms, classroom_members
                WHERE classrooms.ID = classroom_members.Classroom_ID
                AND classroom_members.User_ID = ?
                AND classrooms.Archived = 1;
        """;
        try {
            return queryRunner.query(query,
                    listFromRS(this::classroomFromRS),
                    userId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getArchivedClassroomsByMemberAndRole(int userId, ClassroomRole role) {
        @Language("SQL") String query = """
                SELECT classrooms.* FROM classrooms, classroom_members
                WHERE classrooms.ID = classroom_members.Classroom_ID
                AND classroom_members.User_ID = ?
                AND classroom_members.Role = ?
                AND classrooms.Archived = 1;
        """;
        try {
            return queryRunner.query(query,
                    listFromRS(this::classroomFromRS),
                    userId,
                    role.name()
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    private Classroom classroomFromRS(ResultSet rs) throws SQLException {
        int id = rs.getInt("ID");
        UUID uuid = UUID.fromString(rs.getString("UUID"));
        Integer creatorId = rs.getInt("Creator_ID");
        if (rs.wasNull()) {
            creatorId = null;
        }
        String name = rs.getString("Name");
        String password = rs.getString("Password");
        boolean open = rs.getBoolean("Open");
        boolean visible = rs.getBoolean("Visible");
        boolean archived = rs.getBoolean("Archived");
        return new Classroom(id, uuid, creatorId, name, password, open, visible, archived);
    }
}
