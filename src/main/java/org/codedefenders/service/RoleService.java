package org.codedefenders.service;

import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.codedefenders.auth.CodeDefendersRealm;
import org.codedefenders.auth.annotation.RequiresPermission;
import org.codedefenders.auth.permissions.AdminPermission;
import org.codedefenders.auth.roles.AdminRole;
import org.codedefenders.auth.roles.AuthRole;
import org.codedefenders.persistence.database.RoleRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

@ApplicationScoped
public class RoleService {
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

    private final CodeDefendersRealm realm;
    private final RoleRepository roleRepo;
    private final UserRepository userRepo;

    @Inject
    public RoleService(CodeDefendersRealm realm, RoleRepository roleRepo, UserRepository userRepo) {
        this.realm = realm;
        this.roleRepo = roleRepo;
        this.userRepo = userRepo;
    }

    /**
     * Retrieves the roles for a user. This includes only roles that are saved in the database.
     * Implicit roles are not included.
     * @param userId The user to fetch the roles for.
     * @return A list of roles that are stored in the database for the given user.
     */
    public List<AuthRole> getRolesForUser(int userId) {
        return roleRepo.getRoleNamesForUser(userId).stream()
                .map(realm::resolveRole)
                .toList();

    }

    /**
     * Retrieves the roles for all users. This includes only roles that are saved in the database.
     * Implicit roles are not included.
     * @return A map from user ids to lists of roles. The lists only contain roles that are stored in the database.
     */
    public Multimap<Integer, AuthRole> getAllUserRoles() {
        Multimap<Integer, String> roleNames = roleRepo.getAllUserRoleNames();
        Multimap<Integer, AuthRole> roles = ArrayListMultimap.create();
        for (var entry : roleNames.entries()) {
            roles.put(entry.getKey(), realm.resolveRole(entry.getValue()));
        }
        return roles;
    }

    /**
     * Adds a role for a given user.
     * @param userId The user id to add the role for.
     * @param role The role to add. Must be a role that is stored in the database. Implicit roles are not allowed.
     */
    @RequiresPermission(AdminPermission.name)
    public void addRoleForUser(int userId, AuthRole role) {
        roleRepo.addRoleNameForUser(userId, role.getName());
        realm.invalidate(userId);
    }

    /**
     * Removes a role for a given user.
     * @param userId The user id to remove the role for.
     * @param role The role to remove. Must be a role that is stored in the database. Implicit roles are not allowed.
     */
    @RequiresPermission(AdminPermission.name)
    public void removeRoleForUser(int userId, AuthRole role) {
        roleRepo.removeRoleNameForUser(userId, role.getName());
        realm.invalidate(userId);
    }

    /**
     * Sets the roles for a given user.
     * @param userId The user id to set the roles for.
     * @param newRoles The roles to set. Must a collection of roles that are stored in the database.
     *                 Implicit roles are not allowed.
     */
    @RequiresPermission(AdminPermission.name)
    public void setRolesForUser(int userId, Collection<AuthRole> newRoles) {
        Collection<AuthRole> existingRoles = getRolesForUser(userId);

        for (AuthRole role : existingRoles) {
            if (!newRoles.contains(role)) {
                roleRepo.removeRoleNameForUser(userId, role.getName());
            }
        }

        for (AuthRole role : newRoles) {
            if (!existingRoles.contains(role)) {
                roleRepo.addRoleNameForUser(userId, role.getName());
            }
        }

        realm.invalidate(userId);
    }

    /**
     * Promotes the users with the given names to admin. Unknown users are ignored.
     * This should only be used to for the initial role setup.
     * @param userNames The usernames of the users.
     */
    @RequiresPermission(AdminPermission.name)
    public void doInitialAdminSetup(List<String> userNames) {
        if (userNames.isEmpty()) {
            return;
        }

        StringJoiner messages = new StringJoiner("\n");
        messages.add("""
You are seeing the following message because you configured auth.admin.users to promote the following users to admin: \
'%s'. Please remember to disable this setting after the setup is done."""
                .formatted(String.join(", ", userNames)));

        boolean missingUser = false;
        for (String userName : userNames) {
            var user = userRepo.getUserByName(userName);
            if (user.isPresent()) {
                roleRepo.addRoleNameForUser(user.get().getId(), AdminRole.name);
                messages.add("Promoted user to admin: '%s'".formatted(userName));
            } else {
                missingUser = true;
                messages.add("Could not promote user to admin: '%s'. User not found.".formatted(userName));
            }
        }

        if (missingUser) {
        messages.add("""
Some users were not found. If you want to add these users as administrators, please create the accounts and restart \
the application.""");
        }

        logger.warn(messages.toString());
    }
}
