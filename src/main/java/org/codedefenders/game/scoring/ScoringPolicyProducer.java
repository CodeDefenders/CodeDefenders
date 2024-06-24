package org.codedefenders.game.scoring;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import org.codedefenders.database.EventDAO;
import org.codedefenders.persistence.database.GameRepository;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.PlayerRepository;

public class ScoringPolicyProducer {

    @Produces
    @Named("basic")
    public IScoringPolicy getTheBasicPolicy(EventDAO eventDAO, MutantRepository mutantRepo, GameRepository gameRepo,
                                            PlayerRepository playerRepo) {
        return new DefaultScoringPolicy(eventDAO, mutantRepo, gameRepo, playerRepo);
    }
}
