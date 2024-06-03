package org.codedefenders.auth.permissions;

import org.apache.shiro.authz.Permission;

/**
 * Permission to create classrooms.
 */
public class CreateClassroomPermission implements Permission {
    public final static String name = "classroom:create";

    @Override
    public boolean implies(Permission p) {
        return p instanceof CreateClassroomPermission;
    }
}
