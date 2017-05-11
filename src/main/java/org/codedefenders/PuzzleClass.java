package org.codedefenders;

/**
 * Created by joe on 28/03/2017.
 */

import org.codedefenders.duel.DuelGame;
import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Statement;

public class PuzzleClass {

    private static final Logger logger = LoggerFactory.getLogger(PuzzleClass.class);

    private int id, classId, levelNum, puzzleNum, points;
    private String puzzleName, hint, desc, mode;

    public PuzzleClass(int classId, int points, String hint, String desc, String mode) {

        this.classId = classId;
        this.points = points;
        this.hint = hint;
        this.desc = desc;
        this.mode = mode;

    }

    public PuzzleClass(int classId, String hint, String desc, String mode) {

        this.classId = classId;
        this.hint = hint;
        this.desc = desc;
        this.mode = mode;

    }

    public PuzzleClass(int classId, String hint, String desc) {

        this.classId = classId;
        this.hint = hint;
        this.desc = desc;

    }

    public PuzzleClass (int levelNum, int puzzleNum) {

        this.levelNum = levelNum;
        this.puzzleNum = puzzleNum;

    }

    public PuzzleClass (int levelNum) {

        this.levelNum = levelNum;

    }

    public int getId() { return id; }

    public int getPoints() { return points; }

    public int getLevelNum() { return levelNum; }

    public int getPuzzleNum() { return puzzleNum; }

    public String getPuzzleName() { return puzzleName; }

    public String getPuzzleMode() { return mode; }

    public String getHint() { return hint; }

    public String getDesc() { return desc; }

}
