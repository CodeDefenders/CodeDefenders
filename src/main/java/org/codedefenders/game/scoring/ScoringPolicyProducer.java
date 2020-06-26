package org.codedefenders.game.scoring;

import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Named;

import org.codedefenders.database.EventDAO;

public class ScoringPolicyProducer {

    @Inject
    private EventDAO eventDAO;
    
    @Produces
    @Named("basic")
    public IScoringPolicy getTheBasicPolicy() {
        DefaultScoringPolicy basicScoringPolicy = new DefaultScoringPolicy(eventDAO);
        return basicScoringPolicy;
    }
}
