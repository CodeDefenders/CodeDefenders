package org.codedefenders.auth.roles;

import java.util.Set;

import org.codedefenders.auth.permissions.CreateClassroomPermission;

public class TeacherRole extends Role {
    public TeacherRole() {
        super("teacher", Set.of(new CreateClassroomPermission()));
    }
}
