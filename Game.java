package gammut;

import java.util.ArrayList;
import java.sql.*;

public class Game {

	private int id;
	private int classId;
	private int attackerId;
	private int defenderId;
	private int currentRound;
	private int finalRound;
	private String activePlayer;
	private String state;

	public Game(int id, int attackerId, int defenderId, int classId, int currentRound, int finalRound, String activePlayer, String state) {
		this.id = id;
		this.attackerId = attackerId;
		this.defenderId = defenderId;
		this.classId = classId;
		this.currentRound = currentRound;
		this.finalRound = finalRound;
		this.activePlayer = activePlayer;
		this.state = state;
	}

	public int getId() {return id;}
	
	public int getClassId() {return classId;}
	public String getClassName() {return GameSelectionManager.getNameForClass(classId);}

	public int getAttackerId() {System.out.println(attackerId); return attackerId;}
	public int getDefenderId() {return defenderId;}

	public int getCurrentRound() {return currentRound;}
	public int getFinalRound() {return finalRound;}

	public String getActivePlayer() {return activePlayer;}
	// ATTACKER, DEFENDER, NEITHER

	public String getState() {return state;}
	// CREATED, IN PROGRESS, FINISHED

	public ArrayList<Mutant> getMutants() {return GameManager.getMutantsForGame(id);}
	public ArrayList<Mutant> getAliveMutants() {
		ArrayList<Mutant> aliveMutants = new ArrayList<Mutant>();
		for (Mutant m : getMutants()) {
			if (m.isAlive()) {
				aliveMutants.add(m);
			}
		}
		return aliveMutants;
	}

	public ArrayList<Test> getTests() {return GameManager.getTestsForGame(id);}

	public int getAttackerScore() {
		return 0;
	}

	public int getDefenderScore() {
		return 0;
	}

	public void endTurn() {
		if (activePlayer.equals("ATTACKER")) {activePlayer = "DEFENDER";}
		else if (activePlayer.equals("DEFENDER")) {activePlayer = "ATTACKER";}

		if (currentRound < finalRound) {currentRound++;}
		else if ((currentRound == finalRound)&&(state.equals("IN PROGRESS"))) {state = "FINISHED";}
	}

	public boolean update() {

		Connection conn = null;
        Statement stmt = null;
        String sql = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(DatabaseAccess.DB_URL,DatabaseAccess.USER,DatabaseAccess.PASS);

            // Get all rows from the database which have the chosen username
            stmt = conn.createStatement();
            sql = String.format("UPDATE games SET CurrentRound='%d', FinalRound='%d', ActivePlayer='%s', State='%s'",
            					currentRound, finalRound, activePlayer, state);
            stmt.execute(sql);  
            return true;          

        } catch(SQLException se) {
            System.out.println(se);
            //Handle errors for JDBC
            se.printStackTrace();
        } catch(Exception e) {
            System.out.println(e);
            //Handle errors for Class.forName
            e.printStackTrace();
        } finally{
            //finally block used to close resources
            try {
                if(stmt!=null)
                   stmt.close();
            } catch(SQLException se2) {}// nothing we can do

            try {
                if(conn!=null)
                conn.close();
            } catch(SQLException se) {
                se.printStackTrace();
            }//end finally try
        } //end try

        return false;
	}
}