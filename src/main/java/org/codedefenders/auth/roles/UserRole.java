package org.codedefenders.auth.roles;

import java.util.Set;

public class UserRole extends Role {
    public static final String name = "user";

    public UserRole() {
        super(name, Set.of());
    }
}
