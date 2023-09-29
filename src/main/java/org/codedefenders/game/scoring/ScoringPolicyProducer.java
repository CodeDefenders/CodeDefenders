package org.codedefenders.game.scoring;

import javax.enterprise.inject.Produces;
import javax.inject.Named;

import org.codedefenders.database.EventDAO;
import org.codedefenders.database.GameRepository;
import org.codedefenders.database.MutantRepository;
import org.codedefenders.database.PlayerRepository;

public class ScoringPolicyProducer {

    @Produces
    @Named("basic")
    public IScoringPolicy getTheBasicPolicy(EventDAO eventDAO, MutantRepository mutantRepo, GameRepository gameRepo,
                                            PlayerRepository playerRepo) {
        return new DefaultScoringPolicy(eventDAO, mutantRepo, gameRepo, playerRepo);
    }
}
