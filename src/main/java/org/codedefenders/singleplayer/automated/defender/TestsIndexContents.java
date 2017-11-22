package org.codedefenders.singleplayer.automated.defender;

import org.codedefenders.GameClass;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;

import static org.codedefenders.Constants.AI_DIR;
import static org.codedefenders.Constants.F_SEP;
import static org.codedefenders.Constants.TEST_INFO_EXT;

/**
 * @author Ben Clegg
 */
class TestsIndexContents {

    private ArrayList<Integer> testIds;
    private int dummyGameId;
    private int numTests;

    public TestsIndexContents(GameClass cut) {
        testIds = new ArrayList<Integer>();
        dummyGameId = -1;
        numTests = -1;
        //Parse the test index file of a given class.
        try {
            File f = new File(AI_DIR + F_SEP + "tests" + F_SEP +
                    cut.getAlias() + F_SEP + "TestsIndex" + TEST_INFO_EXT);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuild = dbFactory.newDocumentBuilder();
            Document d = dBuild.parse(f);

            d.getDocumentElement().normalize();

            NodeList tNodes = d.getElementsByTagName("test");
            for (int i = 0; i < tNodes.getLength(); i++) {
                Node tNode = tNodes.item(i);
                Node id = tNode.getAttributes().getNamedItem("id");
                testIds.add(Integer.parseInt(id.getTextContent()));
            }
            NodeList q = d.getElementsByTagName("quantity");
            numTests = Integer.parseInt(q.item(0).getTextContent());
            NodeList g = d.getElementsByTagName("dummygame");
            dummyGameId = Integer.parseInt(g.item(0).getTextContent());

        } catch (Exception e) {
            e.printStackTrace();
            //TODO: Handle errors.
        }


    }

    public ArrayList<Integer> getTestIds() {
        return testIds;
    }

    public int getNumTests() {
        return numTests;
    }

    public int getDummyGameId() {
        return dummyGameId;
    }

}
