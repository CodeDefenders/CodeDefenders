package org.codedefenders.auth.roles;

import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleRole;

public class AuthRole extends SimpleRole {
    public AuthRole(String name, Set<Permission> permissions) {
        super(name, permissions);
    }
}
