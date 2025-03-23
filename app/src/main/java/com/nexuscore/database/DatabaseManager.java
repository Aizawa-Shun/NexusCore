package com.nexuscore.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Class that manages database operations
 */
public class DatabaseManager {
    private Connection connection;
    private static final String DB_URL = "jdbc:sqlite:nexus_core.db";

    /**
     * Constructor - initializes database connection
     */
    public DatabaseManager() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");

            // Connect to the database
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to database");

            // Create tables if they don't exist
            initializeTables();
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
        }
    }

    /**
     * Method to initialize necessary tables
     */
    private void initializeTables() throws SQLException {
        String createConversationsTable = "CREATE TABLE IF NOT EXISTS conversations (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "title TEXT" +
                ");";

        String createMessagesTable = "CREATE TABLE IF NOT EXISTS messages (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "conversation_id INTEGER, " +
                "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "sender TEXT, " +
                "content TEXT, " +
                "FOREIGN KEY (conversation_id) REFERENCES conversations(id)" +
                ");";

        try (Statement statement = connection.createStatement()) {
            statement.execute(createConversationsTable);
            statement.execute(createMessagesTable);
            System.out.println("Tables initialized successfully");
        }
    }

    /**
     * Method to create a new conversation and return its ID
     */
    public int createConversation(String title) {
        String sql = "INSERT INTO conversations (title) VALUES (?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.executeUpdate();

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to create conversation: " + e.getMessage());
        }
        return -1;
    }

    /**
     * Method to save a message
     */
    public void saveMessage(int conversationId, String sender, String content) {
        String sql = "INSERT INTO messages (conversation_id, sender, content) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, conversationId);
            pstmt.setString(2, sender);
            pstmt.setString(3, content);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Failed to save message: " + e.getMessage());
        }
    }

    /**
     * Method to close the database connection
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed");
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}