package org.codedefenders;

/**
 * Created by joe on 13/04/2017.
 */

import org.codedefenders.story.StoryGame;
import org.codedefenders.util.DatabaseAccess;
import org.codedefenders.util.FileManager;
import org.codedefenders.validation.CodeValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.crypto.Data;
import java.io.*;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.codedefenders.Constants.*;

public class ResultManager extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(ResultManager.class);

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        HttpSession session = request.getSession();
        ArrayList<String> messages = (ArrayList<String>) session.getAttribute("messages");
        int uid = (Integer) session.getAttribute("uid");

        StoryGame thisPuzzle = (StoryGame) session.getAttribute("puzzle");

        if (thisPuzzle == null) {
            response.sendRedirect("/story/view");
            return;
        }

        // Achievements
        List<Achievements> firstLevel = DatabaseAccess.firstLevelAchv(uid);
        // if it's the first time, first level and completed
        if (thisPuzzle.getLevelNum() == 1 && thisPuzzle.getPuzzle() == 1 && thisPuzzle.getStoryState() == StoryState.COMPLETED && firstLevel == null) {
            if (!DatabaseAccess.alreadyAchieved(uid, 1)) { // check if user already has achievement
                logger.info("Adding First Level Achievement");
                Achievements firstLevelAchv = new Achievements(uid, 1); // 1 is for First Level Achivement
                firstLevelAchv.insert(); // add achievement
                messages.add("Achievement unlocked: " + DatabaseAccess.getAchvNameById(1).getAchvName());
            }
        }

        // temporary solution (for full level achievement, aID is +1 of level Number)
        // TODO: if manually adding more achievements, need to change this method
        List<StoryGame> fullLevelList = DatabaseAccess.getPuzzlesForLevel(thisPuzzle.getLevelNum());
        if (fullLevelList != null) {
            int levelSize = fullLevelList.size();
            int count = 0;
            for (StoryGame sg : fullLevelList) {
                if (sg.getStoryState().equals(StoryState.COMPLETED)) {
                    count++;
                }
            }
            if (levelSize == count) { // if number of completed puzzles is equal to number of puzzles for that level..
                if (!DatabaseAccess.alreadyAchieved(uid, 2)) {
                    logger.info("Adding Achievement for level:" + thisPuzzle.getLevelNum());
                    Achievements fullLevelAchv = new Achievements(uid, thisPuzzle.getLevelNum() + 1);
                    fullLevelAchv.insert(); // add achievement
                    messages.add("Achievement unlocked: " + DatabaseAccess.getAchvNameById(thisPuzzle.getLevelNum() + 1).getAchvName());
                }
            }
        }
        // End of achievements

        Object testsPassed = session.getAttribute("testsPassed");
        Object numberMutants = session.getAttribute("numberMutants");
        Object score = session.getAttribute("score");
        request.setAttribute("numberMutants", numberMutants);
        request.setAttribute("testsPassed", testsPassed);
        request.setAttribute("score", score);

        if (thisPuzzle.getStoryMode().equals(PuzzleMode.ATTACKER)) {
            logger.info("Redirecting to puzzle attacker results page");

            RequestDispatcher dispatcher = request.getRequestDispatcher(STORY_ATT_RES_VIEW_JSP);
            dispatcher.forward(request, response);
        } else {
            logger.info("Redirecting to puzzle defender results page");

            RequestDispatcher dispatcher = request.getRequestDispatcher(STORY_DEF_RES_VIEW_JSP);
            dispatcher.forward(request, response);
        }

    }

    // retry level
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {

        ArrayList<String> messages = new ArrayList<String>();
        HttpSession session = request.getSession();
        int uid = (Integer) session.getAttribute("uid");

        session.setAttribute("messages", messages);

        StoryGame thisPuzzle = (StoryGame) session.getAttribute("puzzle");

        // send back to Puzzle Manager
        request.setAttribute("puzzle", thisPuzzle);
        response.sendRedirect("/puzzles");

    }

}
