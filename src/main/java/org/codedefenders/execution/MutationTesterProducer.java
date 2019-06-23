package org.codedefenders.execution;

import java.util.concurrent.ExecutorService;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.codedefenders.configuration.Property;

public class MutationTesterProducer {

    @Inject
    @Property("parallelize")
    private boolean enableParalleExecution;

    @Inject
    private BackendExecutorService backend;

    @Inject
    @ThreadPool("test-executor")
    private ExecutorService testExecutorThreadPool;

    @Inject
    @Property("mutant.coverage")
    private boolean useMutantCoverage;

    @Produces
    @RequestScoped
    public IMutationTester getMutationTester() {
        if( enableParalleExecution ){
            return new ParallelMutationTester(backend, useMutantCoverage, testExecutorThreadPool);
        } else{
            return new MutationTester(backend, useMutantCoverage);
        }
    }

}
