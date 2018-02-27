package org.codedefenders.util;

import org.apache.commons.collections.ListUtils;
import org.codedefenders.Feedback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FeedbackDAO {

	private static final Logger logger = LoggerFactory.getLogger(Feedback.class);


	private static final String GET_FEEDBACK_QUERY = "SELECT value, type\n" +
			"FROM ratings\n" +
			"WHERE Game_ID = ? AND User_ID = ?;";

	public static boolean insertFeedback(int gid, int uid, List<Integer> ratingsList) {
		Feedback.FeedbackType[] feedbackTypes = Feedback.FeedbackType.values();
		String query = "INSERT INTO ratings VALUES ";
		String queryValues = "(?, ?, ?, ?),";

		if (ratingsList.size() > feedbackTypes.length || ratingsList.size() < 1)
			return false;

		List<DatabaseValue> allValuesList = new ArrayList<>();
		for (int i = 0; i < ratingsList.size(); i++) {
			int boundedValue = Math.max(Feedback.MIN_RATING, ratingsList.get(i)) % (Feedback.MAX_RATING + 1);
			List<DatabaseValue> valueList = Arrays.asList(DB.getDBV(uid),
					DB.getDBV(gid),
					DB.getDBV(feedbackTypes[i].name()),
					DB.getDBV(boundedValue));
			allValuesList = ListUtils.union(allValuesList, valueList);
			query += queryValues;
		}

		DatabaseValue[] databaseValues = new DatabaseValue[allValuesList.size()];
		databaseValues = allValuesList.toArray(databaseValues);

		Connection conn = DB.getConnection();
		PreparedStatement stmt = DB.createPreparedStatement(conn, query.substring(0, query.length() - 1), databaseValues);
		return DB.executeUpdate(stmt, conn);
	}

	public static int[] getFeedbackValues(int gid, int uid) {
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(gid),
				DB.getDBV(uid)};
		int[] values = new int[Feedback.FeedbackType.values().length];
		Arrays.fill(values, -1);

		Connection conn = DB.getConnection();
		PreparedStatement stmt = DB.createPreparedStatement(conn, GET_FEEDBACK_QUERY, valueList);
		ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
		if (rs == null)
			return null;
		try {
			while (rs.next()) {
				int typeIndex = Feedback.FeedbackType.valueOf(rs.getString(2)).ordinal();
				values[typeIndex] = rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.error("SQLException while parsing result set for statement\n\t", stmt);
			e.printStackTrace();
		} finally {
			DB.cleanup(conn, stmt);
		}
		return values;
	}

}
