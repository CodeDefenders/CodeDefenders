package org.codedefenders.auth.roles;

import java.util.Set;

import org.apache.shiro.authz.permission.AllPermission;

public class AdminRole extends Role {
    public AdminRole() {
        super("admin", Set.of(new AllPermission()));
    }
}
