package org.codedefenders.beans.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.codedefenders.game.AbstractGame;
import org.codedefenders.game.TestAccordionDTO;
import org.codedefenders.game.TestAccordionDTO.TestAccordionCategory;
import org.codedefenders.util.JSONUtils;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Provides data for the test accordion game component.</p>
 * <p>Bean Name: {@code testAccordion}</p>
 */
@Named(value = "testAccordion")
@RequestScoped
// TODO: Move code out of TestAccordionDTO and in here?
public class TestAccordionBean {

    @Inject
    AbstractGame game;

    /**
     * Contains the test and mutant information to be displayed in the accordion.
     */
    private TestAccordionDTO testAccordionData;

    public TestAccordionBean() {
        testAccordionData = null;
    }

    @PostConstruct
    public void setup() {
        testAccordionData = new TestAccordionDTO(game.getCUT(), game.getAllTests(), game.getMutants());
    }

    // --------------------------------------------------------------------------------

    public List<TestAccordionCategory> getCategories() {
        return testAccordionData.getCategories();
    }

    public String getJSON() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Map.class, new JSONUtils.MapSerializer())
                .create();
        return gson.toJson(testAccordionData);
    }

    public String getCategoriesAsJSON() {
        Gson gson = new GsonBuilder()
                .create();
        return gson.toJson(testAccordionData.getCategories());
    }

    public String getTestsAsJSON() {
        Gson gson = new GsonBuilder()
                // It is important that its HashMap.class, it doesn't work if I change it to Map.class …
                .registerTypeAdapter(HashMap.class, new JSONUtils.MapSerializer())
                .create();
        return gson.toJson(testAccordionData.getTests());
    }
}
