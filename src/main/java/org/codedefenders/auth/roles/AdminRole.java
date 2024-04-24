package org.codedefenders.auth.roles;

import java.util.Set;

import org.apache.shiro.authz.permission.AllPermission;

public class AdminRole extends AuthRole {
    public static final String name = "admin";

    public AdminRole() {
        super(name, Set.of(new AllPermission()));
    }
}
