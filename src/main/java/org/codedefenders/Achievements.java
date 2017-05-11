package org.codedefenders;

/**
 * Created by joe on 03/04/2017.
 */

import org.codedefenders.util.DatabaseAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Achievements {

    private int userId;
    private int achvId;
    private int achieved;
    private int levelId;
    private int puzzle;
    private String achvName;
    private String achvDesc;
    private StoryState state;

    private static Logger logger = LoggerFactory.getLogger(Achievements.class);

    public Achievements (int userId, int achvId, int achieved, String achvName, String achvDesc) {

        this.userId = userId;
        this.achvId = achvId;
        this.achieved = achieved;
        this.achvName = achvName;
        this.achvDesc = achvDesc;

    }

    // Level completion achievements
    public Achievements (int userId, int achvId, int achieved, String achvName) {

        this.userId = userId;
        this.achvId = achvId;
        this.achieved = achieved;
        this.achvName = achvName;

    }

    public Achievements (int userId, int achvId) {

        this.userId = userId;
        this.achvId = achvId;

    }

    public Achievements (String achvName) {

        this.achvName = achvName;

    }

    public int getUserId() { return userId; }

    public int getAchvId() { return achvId; }

    public int getAchieved() { return achieved; }

    public int getLevelId() { return levelId; }

    public int getPuzzle() { return puzzle; }

    public StoryState getStoryState() { return state; }

    public String getAchvName() { return achvName; }

    public String getAchvDesc() { return achvDesc; }

    // insert if they have achieved it
    public boolean insert() {

        logger.info("Inserting user achievement: " + achvId + " for User ID: " + userId);

        Connection conn = null;
        Statement stmt = null;

        try {
            conn = DatabaseAccess.getConnection();

            stmt = conn.createStatement();
            String sql = String.format("INSERT INTO user_achievements (Achievement_ID, User_ID, Achieved) VALUES (%d, %d, 1);",
                    achvId, userId);
            stmt.execute(sql);

            conn.close();
            stmt.close();
            return true;
        } catch (SQLException se) {
            System.out.println(se);
        } catch (Exception e) {
            System.out.println(e);
        } finally {
            DatabaseAccess.cleanup(conn, stmt);
        }

        return false;

    }

}


