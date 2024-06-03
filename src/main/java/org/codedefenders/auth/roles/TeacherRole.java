package org.codedefenders.auth.roles;

import java.util.Set;

import org.codedefenders.auth.permissions.CreateClassroomPermission;

public class TeacherRole extends AuthRole {
    public static final String name = "teacher";

    public TeacherRole() {
        super(name, Set.of(new CreateClassroomPermission()));
    }
}
