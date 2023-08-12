package org.codedefenders.model.creategames.roleassignment;

public enum RoleAssignmentMethod {
    /**
     * Users are assigned roles randomly, trying to assign the correct number of attackers and defenders.
     */
    RANDOM,

    /**
     * Users are assigned the role opposite of the last role they played as.
     */
    OPPOSITE
}
