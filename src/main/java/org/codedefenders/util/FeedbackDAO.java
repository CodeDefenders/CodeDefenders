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

	private static final String UPDATE_FEEDBACK_QUERY = "UPDATE ratings\n" +
			"SET value = ?\n" +
			"WHERE Game_ID = ? AND User_ID = ? AND type = ?;";

	private static final String GET_AVERAGE_CLASS_DIFFICULTIES = "SELECT\n" +
			"  IFNULL(AVG(value), -1)   AS 'average',\n" +
			"  c.Class_ID,\n" +
			"  COUNT(value) AS 'votes'\n" +
			"FROM\n" +
			"  (SELECT * FROM ratings WHERE type = ? AND value > 0) as filteredRatings\n" +
			"  RIGHT JOIN games g ON filteredRatings.Game_ID = g.ID\n" +
			"  RIGHT JOIN classes c ON g.Class_ID = c.Class_ID\n" +
			"GROUP BY c.Class_ID ORDER BY c.Class_ID;";

	private static final String GET_AVERAGE_GAME_RATINGS = "SELECT\n" +
			"  AVG(value) AS 'average',\n" +
			"  type\n" +
			"FROM ratings\n" +
			"WHERE Game_ID = ? AND value > 0\n" +
			"GROUP BY type;";

	private static final String GET_NB_FEEDBACKS_FOR_GAME = "SELECT COUNT(DISTINCT User_ID) AS 'nb_feedbacks'\n" +
			"FROM\n" +
			"  ratings\n" +
			"WHERE Game_ID = ?;";

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
		try {
			if (rs == null || !rs.next())
				return null;
			rs.beforeFirst();
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

	public static boolean updateFeedback(int gid, int uid, List<Integer> ratingsList) {
		Feedback.FeedbackType[] feedbackTypes = Feedback.FeedbackType.values();

		if (ratingsList.size() > feedbackTypes.length || ratingsList.size() < 1)
			return false;

		Connection conn = DB.getConnection();
		for (int i = 0; i < ratingsList.size(); i++) {
			int boundedValue = Math.max(Feedback.MIN_RATING, ratingsList.get(i)) % (Feedback.MAX_RATING + 1);
			DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(boundedValue),
					DB.getDBV(gid),
					DB.getDBV(uid),
					DB.getDBV(feedbackTypes[i].name())};
			PreparedStatement stmt = DB.createPreparedStatement(conn, UPDATE_FEEDBACK_QUERY, valueList);
			if (!DB.executeUpdate(stmt, conn))
				return false;
		}
		return true;
	}

	public static boolean hasNotRated(int gid, int uid) {
		return getFeedbackValues(gid, uid) == null;
	}

	public static double[] getAverageGameRatings (int gid) {
		double[] values = new double[Feedback.FeedbackType.values().length];
		Arrays.fill(values, -1);

		Connection conn = DB.getConnection();
		PreparedStatement stmt = DB.createPreparedStatement(conn, GET_AVERAGE_GAME_RATINGS, DB.getDBV(gid));
		ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
		try {
			if (rs == null || !rs.next())
				return null;
			rs.beforeFirst();
			while (rs.next()) {
				int typeIndex = Feedback.FeedbackType.valueOf(rs.getString(2)).ordinal();
				values[typeIndex] = rs.getDouble(1);
			}
		} catch (SQLException e) {
			logger.error("SQLException while parsing result set for statement\n\t", stmt);
			e.printStackTrace();
		} finally {
			DB.cleanup(conn, stmt);
		}
		return values;
	}

	private static List<Double> getAverageClassDifficultyRatings (Feedback.FeedbackType feedbackType) {
		List<Double> values = new ArrayList<>();

		Connection conn = DB.getConnection();
		PreparedStatement stmt = DB.createPreparedStatement(conn, GET_AVERAGE_CLASS_DIFFICULTIES, DB.getDBV(feedbackType.name()));
		ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
		try {
			if (rs == null || !rs.next())
				return null;
			rs.beforeFirst();
			while (rs.next()) {
				values.add(rs.getDouble(1));
			}
		} catch (SQLException e) {
			logger.error("SQLException while parsing result set for statement\n\t", stmt);
			e.printStackTrace();
		} finally {
			DB.cleanup(conn, stmt);
		}
		return values;
	}

	public static List<Double> getAverageMutationDifficulties () {
		return getAverageClassDifficultyRatings(Feedback.FeedbackType.CUT_MUTATION_DIFFICULTY);
	}

	public static List<Double> getAverageTestDifficulties () {
		return getAverageClassDifficultyRatings(Feedback.FeedbackType.CUT_TEST_DIFFICULTY);
	}

	public static int getNBFeedbacksForGame(int gid) {
		Connection conn = DB.getConnection();
		PreparedStatement stmt = DB.createPreparedStatement(conn, GET_NB_FEEDBACKS_FOR_GAME, DB.getDBV(gid));
		ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
		try {
			if (rs == null)
				return 0;
			if (rs.next()) {
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			logger.error("SQLException while parsing result set for statement\n\t", stmt);
			e.printStackTrace();
		} finally {
			DB.cleanup(conn, stmt);
		}
		return 0;
	}

}
