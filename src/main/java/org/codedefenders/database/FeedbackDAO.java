package org.codedefenders.database;

import org.apache.commons.collections.ListUtils;
import org.codedefenders.servlets.FeedbackManager;
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

	private static final Logger logger = LoggerFactory.getLogger(FeedbackManager.class);


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

	private static FeedbackManager.FeedbackType[] feedbackTypes = FeedbackManager.FeedbackType.values();

	public static boolean insertFeedback(int gid, int uid, List<Integer> ratingsList, FeedbackManager.FeedbackType[] feedbackTypes) {
		String query = "INSERT INTO ratings VALUES ";
		StringBuilder bob = new StringBuilder(query);
		String queryValues = "(?, ?, ?, ?),";

		if (ratingsList.size() > feedbackTypes.length || ratingsList.size() < 1)
			return false;

		List<DatabaseValue> allValuesList = new ArrayList<>();
		for (int i = 0; i < ratingsList.size(); i++) {
			int boundedValue = Math.min(Math.max(FeedbackManager.MIN_RATING, ratingsList.get(i)), FeedbackManager.MAX_RATING);
			List<DatabaseValue> valueList = Arrays.asList(DB.getDBV(uid),
					DB.getDBV(gid),
					DB.getDBV(feedbackTypes[i].name()),
					DB.getDBV(boundedValue));
			allValuesList = ListUtils.union(allValuesList, valueList);
			bob.append(queryValues);
		}

		DatabaseValue[] databaseValues = new DatabaseValue[allValuesList.size()];
		databaseValues = allValuesList.toArray(databaseValues);

		Connection conn = DB.getConnection();
		PreparedStatement stmt = DB.createPreparedStatement(conn, bob.substring(0, bob.length() - 1), databaseValues);
		return DB.executeUpdate(stmt, conn);
	}

	public static boolean insertFeedback(int gid, int uid, List<Integer> ratingsList) {
		return insertFeedback(gid, uid, ratingsList, feedbackTypes);
	}

	public static Integer[] getFeedbackValues(int gid, int uid, FeedbackManager.FeedbackType[] feedbackTypes) {
		DatabaseValue[] valueList = new DatabaseValue[]{DB.getDBV(gid),
				DB.getDBV(uid)};
		Integer[] values = new Integer[feedbackTypes.length];
		Arrays.fill(values, -1);

		Connection conn = DB.getConnection();
		PreparedStatement stmt = DB.createPreparedStatement(conn, GET_FEEDBACK_QUERY, valueList);
		ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
		try {
			if (rs == null || !rs.next()) {
				return null;
			}
			rs.beforeFirst();
			while (rs.next()) {
				String typeString = rs.getString(2);
				if (Arrays.stream(feedbackTypes).anyMatch(feedbackType -> typeString.equals(feedbackType.name()))) {
					values[getFeedbackIndex(typeString, feedbackTypes)] = rs.getInt(1);
				} else {
					logger.warn("No such feedback type: " + typeString);
				}
			}
		} catch (SQLException e) {
			logger.error("SQLException while parsing result set for statement\n\t", stmt);
			e.printStackTrace();
		} finally {
			DB.cleanup(conn, stmt);
		}
		return values;
	}

	public static Integer[] getFeedbackValues(int gid, int uid) {
		return getFeedbackValues(gid, uid, feedbackTypes);
	}

	public static boolean updateFeedback(int gid, int uid, List<Integer> ratingsList, FeedbackManager.FeedbackType[] feedbackTypes) {
		if (ratingsList.size() > feedbackTypes.length || ratingsList.size() < 1)
			return false;

		Connection conn = DB.getConnection();
		for (int i = 0; i < ratingsList.size(); i++) {
			int boundedValue = Math.max(FeedbackManager.MIN_RATING, ratingsList.get(i)) % (FeedbackManager.MAX_RATING + 1);
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

	public static boolean updateFeedback(int gid, int uid, List<Integer> ratingsList) {
		return updateFeedback(gid, uid, ratingsList, feedbackTypes);
	}

	public static boolean hasNotRated(int gid, int uid) {
		return getFeedbackValues(gid, uid) == null;
	}

	public static double[] getAverageGameRatings(int gid) {
		double[] values = new double[feedbackTypes.length];
		Arrays.fill(values, -1);

		Connection conn = DB.getConnection();
		PreparedStatement stmt = DB.createPreparedStatement(conn, GET_AVERAGE_GAME_RATINGS, DB.getDBV(gid));
		ResultSet rs = DB.executeQueryReturnRS(conn, stmt);
		try {
			if (rs == null || !rs.next())
				return null;
			rs.beforeFirst();
			while (rs.next()) {
				String typeString = rs.getString(2);
				if (Arrays.stream(feedbackTypes).anyMatch(feedbackType -> typeString.equals(feedbackType.name()))) {
					values[getFeedbackIndex(typeString, feedbackTypes)] = rs.getDouble(1);
				} else {
					logger.warn("No such feedback type: " + typeString);
				}
			}
		} catch (SQLException e) {
			logger.error("SQLException while parsing result set for statement\n\t", stmt);
			e.printStackTrace();
		} finally {
			DB.cleanup(conn, stmt);
		}
		return values;
	}

	private static List<Double> getAverageClassDifficultyRatings(FeedbackManager.FeedbackType feedbackType) {
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

	public static List<Double> getAverageMutationDifficulties() {
		return getAverageClassDifficultyRatings(FeedbackManager.FeedbackType.CUT_MUTATION_DIFFICULTY);
	}

	public static List<Double> getAverageTestDifficulties() {
		return getAverageClassDifficultyRatings(FeedbackManager.FeedbackType.CUT_TEST_DIFFICULTY);
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

	private static int getFeedbackIndex(String typeName, FeedbackManager.FeedbackType[] feedbackTypes) {
		int index = 0;
		for (FeedbackManager.FeedbackType feedbackType : feedbackTypes) {
			if (typeName.equals(feedbackType.name())) {
				return index;
			}
			index++;
		}
		return -1;
	}

}
