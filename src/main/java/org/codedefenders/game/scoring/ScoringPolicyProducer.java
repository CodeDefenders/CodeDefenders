package org.codedefenders.game.scoring;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameRepository;
import org.codedefenders.database.MutantRepository;

public class ScoringPolicyProducer {

    @Produces
    @Named("basic")
    public IScoringPolicy getTheBasicPolicy(EventDAO eventDAO, MutantRepository mutantRepo, GameRepository gameRepo) {
        return new DefaultScoringPolicy(eventDAO, mutantRepo, gameRepo);
    }
}
