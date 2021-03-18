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

import java.util.concurrent.ExecutorService;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.codedefenders.configuration.Configuration;
import org.codedefenders.database.EventDAO;
import org.codedefenders.persistence.database.UserRepository;

public class MutationTesterProducer {

    @Inject
    private Configuration config;

    @Inject
    private BackendExecutorService backend;

    @Inject
    @ThreadPool("test-executor")
    private ExecutorService testExecutorThreadPool;

    @Inject
    private EventDAO eventDAO;

    @Inject
    private UserRepository userRepo;

    @Produces
    @RequestScoped
    public IMutationTester getMutationTester() {
        if (config.isParallelize()) {
            return new ParallelMutationTester(backend, userRepo, eventDAO, config.isMutantCoverage(), testExecutorThreadPool);
        } else {
            return new MutationTester(backend, userRepo, eventDAO, config.isMutantCoverage());
        }
    }

}
