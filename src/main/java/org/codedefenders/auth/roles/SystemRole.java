package org.codedefenders.auth.roles;

import java.util.Set;

import org.apache.shiro.authz.permission.AllPermission;

/**
 * A role for system tasks. E.g. cron jobs or startup tasks.
 */
public class SystemRole extends AuthRole {
    public static final String name = "system";

    public SystemRole() {
        super(name, Set.of(new AllPermission()));
    }
}
