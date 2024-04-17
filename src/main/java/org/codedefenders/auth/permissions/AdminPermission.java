package org.codedefenders.auth.permissions;

import org.apache.shiro.authz.Permission;

/**
 * Permission for the admin management pages and admin controls on other pages.
 */
public class AdminPermission implements Permission {
    public final static String name = "admin";

    @Override
    public boolean implies(Permission p) {
        return p instanceof AdminPermission;
    }
}
