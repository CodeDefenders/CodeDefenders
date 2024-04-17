package org.codedefenders.auth.roles;

import java.util.Set;

public class UserRole extends Role {
    public UserRole() {
        super("user", Set.of());
    }
}
