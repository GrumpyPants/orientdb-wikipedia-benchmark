package com.tinkerpop.graph.benchmark.orientdb;

import com.tinkerpop.graph.benchmark.GraphLoaderService;

import java.sql.*;

/**
 * A {@link GraphLoaderService} that uses OrientDB via Native APIs
 *
 * @author MAHarwood with help from Luca
 */
public class OrientDbNativeLoaderImpl implements GraphLoaderService {
	private String orientDbName;
	Connection connection;
	PreparedStatement preparedInsertArticleStatement = null;
	PreparedStatement preparedArticleExistsStatement = null;
	PreparedStatement preparedUpdateArticleStatement = null;
	ResultSet rs = null;

	public OrientDbNativeLoaderImpl() {
	}

	public void init() {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			System.out.println("Where is your PostgreSQL JDBC Driver? Include in your library path!");
			e.printStackTrace();
			return;
		}

		try {
			connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:5432/wikimap", "david", "");
		} catch (SQLException e) {
			System.out.println("Connection Failed! Check output console");
			e.printStackTrace();
			return;
		}

		if (connection != null) {
			System.out.println("You made it, take control your database now!");
		} else {
			System.out.println("Failed to make connection!");
		}

		try {
			preparedInsertArticleStatement =  connection.prepareStatement("INSERT INTO article(title) VALUES(?)");
			preparedArticleExistsStatement =  connection.prepareStatement("SELECT EXISTS(SELECT 1 FROM article WHERE title=?)");
			preparedUpdateArticleStatement =  connection.prepareStatement("UPDATE article SET links = array_append(links, ?) WHERE title = ?");
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void addLink(String fromNodeKey, String toNodeKey) {
		String fromNodeKeySanitized = fromNodeKey.replace('_', ' ');
		String toNodeKeySanitized = toNodeKey.replace('_', ' ');
		// it is likely that fromNodeKey is the same as the last call because of the way the Wikipedia content is organised
		if (!fromNodeKey.equals(lastFromNodeKey)) {
			boolean articleExists = doesArticleExist(fromNodeKey);
			if (!articleExists) {
				addArticle(fromNodeKeySanitized);
			}

			lastFromNodeKey = fromNodeKey;
		}

		// it is likely that toNodeKey is the same as the last call because of the way the Wikipedia content is organised
		if (!toNodeKey.equals(lastToNodeKey)) {
			boolean doesArticleExist = doesArticleExist(toNodeKey);
			if (!doesArticleExist) {
				addArticle(toNodeKeySanitized);
			}

			lastToNodeKey = toNodeKey;
		}

		// Create the edge
		updateArticleList(fromNodeKeySanitized, toNodeKeySanitized);
	}

	private void updateArticleList(String fromNodeKey, String toNodeKey) {
		try {
			preparedUpdateArticleStatement.setString(1, toNodeKey);
			preparedUpdateArticleStatement.setString(2, fromNodeKey);
			preparedUpdateArticleStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private boolean doesArticleExist(final String fromNodeKey) {
		try {
			preparedArticleExistsStatement.setString(1, fromNodeKey);
			rs = preparedArticleExistsStatement.executeQuery();
			return rs.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return false;
	}

	public void addArticle(String title) {
		try {
			preparedInsertArticleStatement.setString(1, title);
			preparedInsertArticleStatement.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	//Cached lookup from last call
	private String lastFromNodeKey;
	private String lastToNodeKey;

	@Override
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}