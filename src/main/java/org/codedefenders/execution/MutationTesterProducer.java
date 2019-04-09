package org.codedefenders.execution;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;

import org.codedefenders.configuration.Property;

@ApplicationScoped
public class MutationTesterProducer {

    @Inject
    @Property("parallelize")
    private boolean enableParalleExecution;
    
    @Produces
    @RequestScoped
    public IMutationTester getMutationTester() {

        System.out.println("MutationTesterProducer.getMutationTester() Parellelize " + enableParalleExecution);
        // if( someCondition ) {
        // return new NewConfigurationImpl();
        // }
        // else {
        // return new OldConfigurationImpl();
        // }
        return new MutationTester();
    }

}