package org.codedefenders.model.creategames.teamassignment;

public enum TeamAssignmentMethod {
    /**
     * Teams are assigned randomly.
     */
    RANDOM,

    /**
     * Teams are assigned based on the total score of users,
     * putting users with similar total scores in the same team.
     */
    SCORE_DESCENDING
}
