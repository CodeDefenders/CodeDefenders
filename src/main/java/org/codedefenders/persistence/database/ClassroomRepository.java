package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

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

        @Language("SQL") String query = String.join("\n",
                "INSERT INTO classrooms"
                + "(Name, Room_Code, Password, Open)"
                + "VALUES (?, ?, ?, ?);");

        try {
            return queryRunner.insert(query,
                    oneFromRS(rs -> rs.getInt(1)),
                    classroom.getName(),
                    classroom.getRoomCode(),
                    classroom.getPassword().orElse(null),
                    classroom.isOpen()
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public boolean updateClassroom(Classroom classroom) {
        @Language("SQL") String query = String.join("\n",
                "UPDATE classrooms",
                "SET Name = ?,",
                "  Room_Code = ?,",
                "  Password = ?,",
                "  Open = ?",
                "WHERE classrooms.ID = ?");
        try {
            return 1 == queryRunner.update(query,
                    classroom.getName(),
                    classroom.getRoomCode(),
                    classroom.getPassword().orElse(null),
                    classroom.isOpen(),
                    classroom.getId()
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public Optional<Classroom> getClassroomById(int id) {
        @Language("SQL") String query = String.join("\n",
                "SELECT * FROM classrooms",
                "WHERE ID = ?;"
        );
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

    public Optional<Classroom> getClassroomByRoomCode(String roomCode) {
        @Language("SQL") String query = String.join("\n",
                "SELECT * FROM classrooms",
                "WHERE Room_Code = ?;"
        );
        try {
            return queryRunner.query(query,
                    oneFromRS(this::classroomFromRS),
                    roomCode
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getClassroomsByMemberAndRole(int userId, ClassroomRole role) {
        @Language("SQL") String query = String.join("\n",
                "SELECT classrooms.* FROM classrooms, classroom_members",
                "WHERE classrooms.ID = classroom_members.Classroom_ID",
                "AND classroom_members.User_ID = ?",
                "AND classroom_members.Role = ?;"
        );
        try {
            return queryRunner.query(query,
                    listFromRS(this::classroomFromRS),
                    userId,
                    role
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<Classroom> getClassroomsByMember(int userId) {
        @Language("SQL") String query = String.join("\n",
                "SELECT classrooms.* FROM classrooms, classroom_members",
                "WHERE classrooms.ID = classroom_members.Classroom_ID",
                "AND classroom_members.User_ID = ?;"
        );
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


    private Classroom classroomFromRS(ResultSet rs) throws SQLException {
        int id = rs.getInt("ID");
        String name = rs.getString("Name");
        String roomCode = rs.getString("Room_Code");
        String password = rs.getString("Password");
        boolean open = rs.getBoolean("Open");
        return new Classroom(id, name, roomCode, password, open);
    }
}
