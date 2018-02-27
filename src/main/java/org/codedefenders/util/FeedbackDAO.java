package org.codedefenders.util;

import org.apache.commons.collections.ListUtils;
import org.codedefenders.Feedback;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeedbackDAO {
	public static boolean updateFeedback(int gid, int uid, List<Integer> ratingsList) {
		Feedback.FeedbackType[] feedbackTypes = Feedback.FeedbackType.values();
		String query = "INSERT INTO ratings VALUES ";
		String queryValues = "(?, ?, ?, ?),";
		System.out.println("Updating " + uid + "'s rating for game " + gid + ": " + ratingsList);

		if (ratingsList.size() > feedbackTypes.length || ratingsList.size() < 1)
			return false;

		List<DatabaseValue> allValuesList = new ArrayList<>();
		for (int i = 0; i < ratingsList.size(); i++) {
			List<DatabaseValue> valueList = Arrays.asList(DB.getDBV(uid),
					DB.getDBV(gid),
					DB.getDBV(feedbackTypes[i].name()),
					DB.getDBV(ratingsList.get(i)));
			allValuesList = ListUtils.union(allValuesList, valueList);
			query += queryValues;
		}

		DatabaseValue[] databaseValues = new DatabaseValue[allValuesList.size()];
		databaseValues = allValuesList.toArray(databaseValues);
		System.out.println("allValuesList: " + allValuesList);

		Connection conn = DB.getConnection();
		PreparedStatement stmt = DB.createPreparedStatement(conn, query.substring(0, query.length()-1), databaseValues);
		System.out.println("pstmt: " + stmt);
		return DB.executeUpdate(stmt, conn);
	}

}
