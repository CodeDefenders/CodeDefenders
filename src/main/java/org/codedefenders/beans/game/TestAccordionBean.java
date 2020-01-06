package org.codedefenders.beans.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.codedefenders.game.GameClass;
import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.game.TestAccordionDTO;
import org.codedefenders.game.TestAccordionDTO.TestAccordionCategory;
import org.codedefenders.util.JSONUtils;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import java.util.List;
import java.util.Map;

/**
 * <p>Provides data for the test accordion game component.</p>
 * <p>Bean Name: {@code testAccordion}</p>
 */
@ManagedBean
@RequestScoped
// TODO: Move code out of TestAccordionDTO and in here?
public class TestAccordionBean {
    private TestAccordionDTO testAccordionData;

    public TestAccordionBean() {
        testAccordionData = null;
    }

    public void setTestAccordionData(GameClass cut, List<Test> testsList, List<Mutant> mutantsList) {
        testAccordionData = new TestAccordionDTO(cut, testsList, mutantsList);
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
}
