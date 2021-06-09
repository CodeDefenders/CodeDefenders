package org.codedefenders.game.scoring;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.codedefenders.database.EventDAO;

public class ScoringPolicyProducer {

    @Produces
    @Named("basic")
    public IScoringPolicy getTheBasicPolicy(EventDAO eventDAO) {
        return new DefaultScoringPolicy(eventDAO);
    }
}
