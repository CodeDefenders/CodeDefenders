package org.codedefenders.api.analytics;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;

import testsmell.AbstractSmell;
import testsmell.TestSmellDetector;
import testsmell.smell.AssertionRoulette;
import testsmell.smell.DuplicateAssert;
import testsmell.smell.EagerTest;
import testsmell.smell.RedundantAssertion;
import testsmell.smell.SensitiveEquality;
import testsmell.smell.UnknownTest;

public class TestSmellDetectorProducer {

    public TestSmellDetectorProducer() {
    }

    // See https://stackoverflow.com/questions/2264758/resolution-of-external-3rd-party-beans-in-weld
    public @Produces @RequestScoped TestSmellDetector createTestSmellDetector() {
        /*
         * For the moment we hardcode the configuration of the smells here to
         * allow fine tuning of smells (see #426) and allow for post processing
         * of smells TODO: Inject TestSmellDetector using CDI
         */
        List<AbstractSmell> testSmells = new ArrayList<AbstractSmell>();
        testSmells.add(new AssertionRoulette());
        testSmells.add(new DuplicateAssert());
        testSmells.add(new EagerTest());
        testSmells.add(new RedundantAssertion());
        testSmells.add(new SensitiveEquality());
        testSmells.add(new UnknownTest());
        // Those two might require some love according to #426.
        // testSmells.add(new ExceptionCatchingThrowing());
        // testSmells.add(new MagicNumberTest());
        /*
         * Those two are not mentioned on:
         * https://testsmells.github.io/pages/testsmells.html but might become
         * relevant later
         */
        // testSmells.add(new VerboseTest());
        // testSmells.add(new DependentTest());
        return new TestSmellDetector(testSmells);
      }
}
