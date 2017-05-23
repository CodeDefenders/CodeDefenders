package org.codedefenders;

/**
 * Created by joe on 28/03/2017.
 * Holds all form actions to do with adding and editing puzzles
 */

import jdk.nashorn.internal.ir.RuntimeNode;
import org.codedefenders.story.StoryGame;
import org.codedefenders.story.StoryPuzzle;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PuzzleEditor extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        res.sendRedirect("story/view");

    }

    public void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException, ServletException {

        HttpSession session = req.getSession();

        int levelNumber;
        int classId;
        int uid;
        int puzzleId;
        String puzzleName, puzzleHint, puzzleDesc, puzzleMode;
        String formType = req.getParameter("formType");

        ArrayList<String> messages = new ArrayList<String>();
        session.setAttribute("messages", messages);
        uid = (Integer) session.getAttribute("uid");

        // Takes form value and has different actions for each type of form taken in
        if (formType.equals("reqEdit")) {

            puzzleName = req.getParameter("classAlias");
            classId = Integer.parseInt(req.getParameter("editClassId"));
            req.setAttribute("editClassId", classId);
            req.setAttribute("classAlias", puzzleName);
            RequestDispatcher view = getServletContext().getRequestDispatcher("/puzzle/edit");
            view.forward(req, res);

        } else if (formType.equals("sendEdit")) {

            levelNumber = Integer.parseInt(req.getParameter("levelNumber"));
            String tempNum = req.getParameter("puzzleNumber");
            classId = Integer.parseInt(req.getParameter("editClassId"));
            puzzleName = req.getParameter("classAlias");
            puzzleHint = req.getParameter("puzzleHint");
            puzzleDesc = req.getParameter("puzzleDesc");
            puzzleMode = req.getParameter("puzzleMode");
            puzzleId = Integer.parseInt(req.getParameter("puzzleId"));

            int puzzleNumber = 0;

            try { // see if input can be converted to valid type: int
                puzzleNumber = Integer.parseInt(tempNum);
            } catch (Exception e) {
                e.printStackTrace();
                messages.add("Please enter a valid puzzle number");
                req.setAttribute("editClassId", classId);
                req.setAttribute("classAlias", puzzleName);
                RequestDispatcher view = getServletContext().getRequestDispatcher("/puzzle/edit");
                view.forward(req, res);
                return;
            }

            List<StoryGame> levelList = DatabaseAccess.getPuzzlesForLevel(levelNumber);

            for (StoryGame l : levelList) {
                if (l.getPuzzle() == puzzleNumber && puzzleId != l.getPuzzleId()) {
                    messages.add(levelNumber + "-" + puzzleNumber + " has already been taken. Please try another one.");
                    req.setAttribute("editClassId", classId);
                    req.setAttribute("classAlias", puzzleName);
                    RequestDispatcher view = getServletContext().getRequestDispatcher("/puzzle/edit");
                    view.forward(req, res);
                    return;
                }
            }

            if (puzzleDesc == null || puzzleName == null || puzzleDesc == "" || puzzleName == "") {

                messages.add("Please enter a puzzle name and/or a description");
                req.setAttribute("editClassId", classId);
                req.setAttribute("classAlias", puzzleName);
                RequestDispatcher view = getServletContext().getRequestDispatcher("/puzzle/edit");
                view.forward(req, res);

            } else {

                EditPuzzle ep = new EditPuzzle(uid, levelNumber, puzzleNumber, classId, puzzleName, puzzleHint, puzzleDesc, puzzleMode);

                if (ep.update()) {
                    messages.add("Edit successful!");
                    res.sendRedirect("story/mypuzzles");
                } else {
                    messages.add("There was an error, please try again.");
                    res.sendRedirect(req.getHeader("referer"));
                }

            }

        } else if (formType.equals("deletePuzzle")) {

            classId = Integer.parseInt(req.getParameter("delClassId"));
            uid = (Integer) session.getAttribute("uid");

            StoryPuzzle temp = DatabaseAccess.getPuzzleId(classId);
            int pid = temp.getPuzzleId();

            EditPuzzle ep = new EditPuzzle(classId, pid);

            if (ep.delete()) { // TODO: foreign key constraint error
                messages.add("Delete successful!");
                res.sendRedirect("story/mypuzzles");
            } else {
                messages.add("Error when deleting");
                res.sendRedirect(req.getHeader("referer"));
            }

        }

    }

}
