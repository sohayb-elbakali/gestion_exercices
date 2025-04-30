package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String URL = "jdbc:mysql://localhost:3306/gestion_exercices";
    private static final String USER = "root"; // par défaut
    private static final String PASSWORD = ""; // vide par défaut dans XAMPP
    
    // For better performance in a real app, this would be a connection pool
    private static Connection sharedConnection;
    
    /**
     * Get a database connection. In a simple implementation, this returns a direct connection.
     * In a production environment, this would return a connection from a connection pool.
     */
    public static Connection getConnection() throws SQLException {
        if (sharedConnection == null || sharedConnection.isClosed()) {
            try {
                // Ensure driver is loaded (modern JDBC drivers don't require this,
                // but it's good practice for compatibility)
                Class.forName("com.mysql.cj.jdbc.Driver");
                
                // Create a new connection
                sharedConnection = DriverManager.getConnection(URL, USER, PASSWORD);
                LOGGER.info("Database connection established");
            } catch (ClassNotFoundException e) {
                LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found", e);
                throw new SQLException("MySQL JDBC Driver not found", e);
            }
        }
        return sharedConnection;
    }
    
    /**
     * Close the shared database connection if it's open
     */
    public static void closeConnection() {
        if (sharedConnection != null) {
            try {
                if (!sharedConnection.isClosed()) {
                    sharedConnection.close();
                    LOGGER.info("Database connection closed");
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error closing database connection", e);
            } finally {
                sharedConnection = null;
            }
        }
    }
    
    /**
     * Shutdown hook to ensure connection is closed when the application exits
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Application shutdown detected, closing database connection");
            closeConnection();
        }));
    }
}
