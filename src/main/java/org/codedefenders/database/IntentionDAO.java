package org.codedefenders.database;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.codedefenders.game.Mutant;
import org.codedefenders.game.Test;
import org.codedefenders.model.AttackerIntention;
import org.codedefenders.model.DefenderIntention;

public class IntentionDAO {
	
	final private static String defender_query = 
			"INSERT INTO intention (Test_ID, Game_ID, Target_Mutants, Target_Lines) "
			+ "VALUES (?, ?, ?, ?);";
	
	final private static String attacker_query = 
			"INSERT INTO intention (Mutant_ID, Game_ID, Target_Mutant_Type) "
			+ "VALUES (?, ?, ?);";
	
	public static int storeIntentionForTest(Test test, DefenderIntention intention) throws Exception {

		// Contextual Information Test and Game
        Integer testId = test.getId();
        Integer gameId = test.getGameId();

        StringBuffer targetMutantsAsCSV = new StringBuffer();
        for(Integer integer : intention.getMutants()){
        	targetMutantsAsCSV.append( integer ).append(",");
        }
        if( targetMutantsAsCSV.length() > 0 ){
        	targetMutantsAsCSV.reverse().deleteCharAt(0).reverse();
        } 
        String targetMutants = targetMutantsAsCSV.toString();
        
        StringBuffer targetLinesAsCSV = new StringBuffer();
        for(Integer integer : intention.getLines()){
        	targetLinesAsCSV.append( integer ).append(",");
        }
        if( targetLinesAsCSV.length() > 0 ){
        	targetLinesAsCSV.reverse().deleteCharAt(0).reverse();
        }
        String targetLines = targetLinesAsCSV.toString();
        
        // Target DefenderIntention
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(testId),
                DB.getDBV(gameId),
                DB.getDBV(targetMutants),
                DB.getDBV(targetLines),
        };
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, defender_query, valueList);

        final int result = DB.executeUpdateGetKeys(stmt, conn);
        if (result != -1) {
            return result;
        } else {
            throw new Exception("Could not store defender intention to database.");
        }
    }

	public static int storeIntentionForMutant(Mutant newMutant, AttackerIntention intention) throws Exception {
		System.out.println("IntentionDAO.storeIntentionForMutant() ");
		// Target DefenderIntention
        DatabaseValue[] valueList = new DatabaseValue[]{
                DB.getDBV(newMutant.getId()),
                DB.getDBV(newMutant.getGameId()),
                DB.getDBV(intention.toString()),
        };
        Connection conn = DB.getConnection();
        PreparedStatement stmt = DB.createPreparedStatement(conn, attacker_query, valueList);

        final int result = DB.executeUpdateGetKeys(stmt, conn);
        if (result != -1) {
            return result;
        } else {
            throw new Exception("Could not store attacker intention to database.");
        }
	}

}
