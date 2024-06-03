/*
 * Copyright (C) 2016-2019 Code Defenders contributors
 *
 * This file is part of Code Defenders.
 *
 * Code Defenders is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Code Defenders is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Code Defenders. If not, see <http://www.gnu.org/licenses/>.
 */
package org.codedefenders.execution;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.EventDAO;
import org.codedefenders.persistence.database.MutantRepository;
import org.codedefenders.persistence.database.TestRepository;
import org.codedefenders.persistence.database.UserRepository;
import org.codedefenders.util.concurrent.ExecutorServiceProvider;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class MutationTesterProducer {

    @Produces
    @ApplicationScoped
    public IMutationTester getMutationTester(@SuppressWarnings("CdiInjectionPointsInspection") Configuration config,
                                             BackendExecutorService backend, EventDAO eventDAO, UserRepository userRepo,
                                             ExecutorServiceProvider executorServiceProvider,
                                             TestRepository testRepo, MutantRepository mutantRepo) {
        if (config.isParallelize()) {
            return new ParallelMutationTester(backend, userRepo, eventDAO, testRepo, mutantRepo, config.isMutantCoverage(),
                    executorServiceProvider.createExecutorService("test-executor-parallel", config.getNumberOfParallelAntExecutions()));
        } else {
            return new MutationTester(backend, userRepo, eventDAO, testRepo, mutantRepo, config.isMutantCoverage());
        }
    }
}
