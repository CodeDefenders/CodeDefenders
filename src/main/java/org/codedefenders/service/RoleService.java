package org.codedefenders.service;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.auth.CodeDefendersRealm;
import org.codedefenders.auth.annotation.RequiresPermission;
import org.codedefenders.auth.permissions.AdminPermission;
import org.codedefenders.auth.roles.Role;
import org.codedefenders.persistence.database.RoleRepository;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

@ApplicationScoped
public class RoleService {
    private final CodeDefendersRealm realm;
    private final RoleRepository roleRepo;

    @Inject
    public RoleService(CodeDefendersRealm realm, RoleRepository roleRepo) {
        this.realm = realm;
        this.roleRepo = roleRepo;
    }

    public List<Role> getRolesForUser(int userId) {
        return roleRepo.getRoleNamesForUser(userId).stream()
                .map(realm::resolveRole)
                .toList();

    }

    public Multimap<Integer, Role> getAllUserRoles() {
        Multimap<Integer, String> roleNames = roleRepo.getAllUserRoleNames();
        Multimap<Integer, Role> roles = ArrayListMultimap.create();
        for (var entry : roleNames.entries()) {
            roles.put(entry.getKey(), realm.resolveRole(entry.getValue()));
        }
        return roles;
    }

    @RequiresPermission(AdminPermission.name)
    public void addRoleForUser(int userId, Role role) {
        roleRepo.addRoleNameForUser(userId, role.getName());
    }

    @RequiresPermission(AdminPermission.name)
    public void deleteRoleForUser(int userId, Role role) {
        roleRepo.deleteRoleNameForUser(userId, role.getName());
    }
}
