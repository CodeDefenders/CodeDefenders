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

        queryRunner.insert(query,
                rs -> null,
                member.getUserId(),
                member.getClassroomId(),
                member.getRole().name()
        );
    }

    public void updateMember(ClassroomMember member) {
        @Language("SQL") String query = """
                UPDATE classroom_members
                SET Role = ?
                WHERE User_ID = ?
                  AND Classroom_ID = ?;
        """;

        int updatedRows = queryRunner.update(query,
                member.getRole().name(),
                member.getUserId(),
                member.getClassroomId()
        );
        if (updatedRows != 1) {
            throw new UncheckedSQLException("Couldn't update classroom.");
        }
    }

    public void deleteMember(int classroomId, int userId) {
        @Language("SQL") String query = """
                DELETE FROM classroom_members
                WHERE User_ID = ?
                  AND Classroom_ID = ?;
        """;

        int updatedRows = queryRunner.update(query,
                userId,
                classroomId
        );
        if (updatedRows != 1) {
            throw new UncheckedSQLException("Couldn't update classroom member.");
        }
    }

    public List<ClassroomMember> getMembersForClassroom(int id) {
        @Language("SQL") String query = """
                SELECT * FROM classroom_members
                WHERE Classroom_ID = ?;
        """;

        return queryRunner.query(query,
                listFromRS(this::classroomMemberFromRS),
                id
        );
    }

    public Optional<ClassroomMember> getMemberForClassroomAndUser(int classroomId, int userId) {
        @Language("SQL") String query = """
                SELECT * FROM classroom_members
                WHERE Classroom_ID = ?
                AND User_ID = ?;
        """;

        return queryRunner.query(query,
                nextFromRS(this::classroomMemberFromRS),
                classroomId,
                userId
        );
    }

    private ClassroomMember classroomMemberFromRS(ResultSet rs) throws SQLException {
        int userId = rs.getInt("User_ID");
        int classroomId  = rs.getInt("Classroom_ID");
        ClassroomRole role = ClassroomRole.valueOf(rs.getString("Role"));
        return new ClassroomMember(userId, classroomId, role);
    }
}
