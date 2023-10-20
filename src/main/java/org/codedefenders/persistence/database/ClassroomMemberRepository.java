package org.codedefenders.persistence.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.ClassroomMember;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.listFromRS;
import static org.codedefenders.persistence.database.util.ResultSetUtils.nextFromRS;

@Transactional
@ApplicationScoped
public class ClassroomMemberRepository {
    private static final Logger logger = LoggerFactory.getLogger(ClassroomMemberRepository.class);

    private final QueryRunner queryRunner;

    @Inject
    public ClassroomMemberRepository(QueryRunner queryRunner) {
        this.queryRunner = queryRunner;
    }

    public void storeMember(ClassroomMember member) {
        @Language("SQL") String query = """
                INSERT INTO classroom_members
                    (User_ID, Classroom_ID, Role)
                VALUES (?, ?, ?);
        """;

        try {
            queryRunner.insert(query,
                    rs -> null,
                    member.getUserId(),
                    member.getClassroomId(),
                    member.getRole().name()
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public void updateMember(ClassroomMember member) {
        @Language("SQL") String query = """
                UPDATE classroom_members
                SET Role = ?
                WHERE User_ID = ?
                  AND Classroom_ID = ?;
        """;
        try {
            int updatedRows = queryRunner.update(query,
                    member.getRole().name(),
                    member.getUserId(),
                    member.getClassroomId()
            );
            if (updatedRows != 1) {
                throw new UncheckedSQLException("Couldn't update classroom.");
            }
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public void deleteMember(int classroomId, int userId) {
        @Language("SQL") String query = """
                DELETE FROM classroom_members
                WHERE User_ID = ?
                  AND Classroom_ID = ?;
        """;
        try {
            int updatedRows = queryRunner.update(query,
                    userId,
                    classroomId
            );
            if (updatedRows != 1) {
                throw new UncheckedSQLException("Couldn't update classroom member.");
            }
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public List<ClassroomMember> getMembersForClassroom(int id) {
        @Language("SQL") String query = """
                SELECT * FROM classroom_members
                WHERE Classroom_ID = ?;
        """;
        try {
            return queryRunner.query(query,
                    listFromRS(this::classroomMemberFromRS),
                    id
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    public Optional<ClassroomMember> getMemberForClassroomAndUser(int classroomId, int userId) {
        @Language("SQL") String query = """
                SELECT * FROM classroom_members
                WHERE Classroom_ID = ?
                AND User_ID = ?;
        """;
        try {
            return queryRunner.query(query,
                    nextFromRS(this::classroomMemberFromRS),
                    classroomId,
                    userId
            );
        } catch (SQLException e) {
            logger.error("SQLException while executing query", e);
            throw new UncheckedSQLException("SQLException while executing query", e);
        }
    }

    private ClassroomMember classroomMemberFromRS(ResultSet rs) throws SQLException {
        int userId = rs.getInt("User_ID");
        int classroomId  = rs.getInt("Classroom_ID");
        ClassroomRole role = ClassroomRole.valueOf(rs.getString("Role"));
        return new ClassroomMember(userId, classroomId, role);
    }
}
