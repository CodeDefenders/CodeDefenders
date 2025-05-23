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
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.codedefenders.database.UncheckedSQLException;
import org.codedefenders.model.Classroom;
import org.codedefenders.model.ClassroomRole;
import org.codedefenders.persistence.database.util.QueryRunner;
import org.codedefenders.transaction.Transactional;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.codedefenders.persistence.database.util.ResultSetUtils.generatedKeyFromRS;
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

        return queryRunner.insert(query,
                generatedKeyFromRS(),
                classroom.getUUID().toString(),
                classroom.getCreatorId().orElse(null),
                classroom.getName(),
                classroom.getPassword().orElse(null),
                classroom.isOpen(),
                classroom.isVisible(),
                classroom.isArchived()
        );
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
    }

    public Optional<Classroom> getClassroomById(int id) {
        @Language("SQL") String query = """
                SELECT * FROM classrooms
                WHERE ID = ?;
        """;

        return queryRunner.query(query,
                oneFromRS(this::classroomFromRS),
                id
        );
    }

    public Optional<Classroom> getClassroomByUUID(UUID uuid) {
        @Language("SQL") String query = """
                SELECT * FROM classrooms
                WHERE UUID = ?;
        """;

        return queryRunner.query(query,
                oneFromRS(this::classroomFromRS),
                uuid.toString()
        );
    }

    public List<Classroom> getAllClassrooms() {
        @Language("SQL") String query = "SELECT * FROM classrooms;";
        return queryRunner.query(query, listFromRS(this::classroomFromRS));
    }

    public List<Classroom> getAllClassroomsByMember(int userId) {
        @Language("SQL") String query = """
                SELECT classrooms.* FROM classrooms, classroom_members
                WHERE classrooms.ID = classroom_members.Classroom_ID
                AND classroom_members.User_ID = ?;
        """;

        return queryRunner.query(query,
                listFromRS(this::classroomFromRS),
                userId
        );
    }

    public List<Classroom> getActiveClassrooms() {
        @Language("SQL") String query = "SELECT * FROM classrooms WHERE Archived = 0;";
        return queryRunner.query(query, listFromRS(this::classroomFromRS));
    }

    public List<Classroom> getActiveClassroomsByMember(int userId) {
        @Language("SQL") String query = """
                SELECT classrooms.* FROM classrooms, classroom_members
                WHERE classrooms.ID = classroom_members.Classroom_ID
                AND classroom_members.User_ID = ?
                AND classrooms.Archived = 0;
        """;

        return queryRunner.query(query,
                listFromRS(this::classroomFromRS),
                userId
        );
    }

    public List<Classroom> getActiveClassroomsByMemberAndRole(int userId, ClassroomRole role) {
        @Language("SQL") String query = """
                SELECT classrooms.* FROM classrooms, classroom_members
                WHERE classrooms.ID = classroom_members.Classroom_ID
                AND classroom_members.User_ID = ?
                AND classroom_members.Role = ?
                AND classrooms.Archived = 0;
        """;

        return queryRunner.query(query,
                listFromRS(this::classroomFromRS),
                userId,
                role.name()
        );
    }

    public List<Classroom> getVisibleClassrooms() {
        @Language("SQL") String query = "SELECT * FROM classrooms WHERE Archived = 0 AND Visible = 1;";
        return queryRunner.query(query, listFromRS(this::classroomFromRS));
    }

    public List<Classroom> getArchivedClassrooms() {
        @Language("SQL") String query = "SELECT * FROM classrooms WHERE Archived = 1;";
        return queryRunner.query(query, listFromRS(this::classroomFromRS));
    }

    public List<Classroom> getArchivedClassroomsByMember(int userId) {
        @Language("SQL") String query = """
                SELECT classrooms.* FROM classrooms, classroom_members
                WHERE classrooms.ID = classroom_members.Classroom_ID
                AND classroom_members.User_ID = ?
                AND classrooms.Archived = 1;
        """;

        return queryRunner.query(query,
                listFromRS(this::classroomFromRS),
                userId
        );
    }

    public List<Classroom> getArchivedClassroomsByMemberAndRole(int userId, ClassroomRole role) {
        @Language("SQL") String query = """
                SELECT classrooms.* FROM classrooms, classroom_members
                WHERE classrooms.ID = classroom_members.Classroom_ID
                AND classroom_members.User_ID = ?
                AND classroom_members.Role = ?
                AND classrooms.Archived = 1;
        """;

        return queryRunner.query(query,
                listFromRS(this::classroomFromRS),
                userId,
                role.name()
        );
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
