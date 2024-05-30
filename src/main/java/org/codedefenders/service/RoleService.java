package org.codedefenders.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringJoiner;
import java.util.function.Consumer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.catalina.Role;
import org.apache.catalina.UserDatabase;
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
                     You configured auth.admin.users to promote the following users to admin: '%s'. Please remember to \
                     disable this setting in the config after you finish setting up Code Defenders."""
                .stripIndent().formatted(String.join(", ", userNames)));

        boolean missingUser = promoteUsersToAdmin(userNames, messages::add);

        if (missingUser) {
        messages.add("""
                    Some users were not found. If you want to add these users as administrators, please create the \
                    accounts and restart the application.""".stripIndent());
        }

        logger.warn(messages.toString());
    }

    /**
     * Promotes all users in the Tomcat UserDatabase with the given role to admin. Unknown users are ignored.
     * This should only be used to migrate admin users from the Tomcat user database.
     * @param roleName The name of the admin role in the Tomcat user database.
     */
    @RequiresPermission(AdminPermission.name)
    public void migrateAdminUsers(String roleName) {
        if (roleName == null) {
            return;
        }

        List<String> userNames = getUsersWithRoleFromUserDatabase(roleName);
        if (userNames == null) {
            return;
        }

        if (userNames.isEmpty()) {
            logger.warn("""
                        Your configuration still contains the deprecated auth.admin.role setting, but no users \
                        matching the role were found in your Tomcat user database. You can safely remove \
                        auth.admin.role from your config.""");
            return;
        }

        StringJoiner messages = new StringJoiner("\n");
        messages.add("""
                     Your configuration still contains the deprecated auth.admin.role setting. Code Defenders will now \
                     migrate your existing admin users from the Tomcat user database to our new system. You can safely \
                     remove auth.admin.role after this is done."""
                .stripIndent());

        boolean missingUser = promoteUsersToAdmin(userNames, messages::add);

        if (missingUser) {
            messages.add("""
                    Some users were not found. If you want to add these users as administrators, please create the \
                    accounts and restart the application.""".stripIndent());
        }

        logger.warn(messages.toString());
    }

    /**
     * Promotes the given users to admin.
     * @param userNames User names to promote.
     * @param logMessage A callback for log messages.
     * @return {@code true} if any of the given users wasn't found.
     */
    private boolean promoteUsersToAdmin(List<String> userNames, Consumer<String> logMessage) {
        boolean missingUser = false;
        for (String userName : userNames) {
            var user = userRepo.getUserByName(userName);
            if (user.isPresent()) {
                roleRepo.addRoleNameForUser(user.get().getId(), AdminRole.name);
                logMessage.accept("Promoted user to admin: '%s'".formatted(userName));
            } else {
                missingUser = true;
                logMessage.accept("Could not promote user to admin: '%s'. User not found.".formatted(userName));
            }
        }
        return missingUser;
    }

    /**
     * Retrieves users with the given role from the Tomcat UserDatabase.
     * @param roleName The role to look up users for.
     * @return A list of found usernames with the role, or {@code null} if there was a problem with the lookup.
     */
    private List<String> getUsersWithRoleFromUserDatabase(String roleName) {
        UserDatabase userDatabase;
        try {
            userDatabase = (UserDatabase) new InitialContext().lookup("java:comp/env/auth/UserDatabase");
        } catch (NamingException e) {
            logger.error("Exception while looking up Tomcat UserDatabase.", e);
            return null;
        }

        Role adminRole = userDatabase.findRole(roleName);
        if (adminRole == null) {
            logger.error("Couldn't role '{}' in Tomcat UserDatabase.", roleName);
            return null;
        }

        List<String> adminUserNames = new ArrayList<>();
        userDatabase.getUsers().forEachRemaining(user -> {
            if (user.isInRole(adminRole)) {
                adminUserNames.add(user.getUsername());
            }
        });
        return adminUserNames;
    }
}
