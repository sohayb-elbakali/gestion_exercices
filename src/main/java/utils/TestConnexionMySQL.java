package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestConnexionMySQL {

    // Paramètres de connexion (à adapter selon votre configuration)
    private static final String URL = "jdbc:mysql://localhost:3306/gestion_exercices";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public static void main(String[] args) {
        testConnexion();
    }

    public static void testConnexion() {
        System.out.println("Tentative de connexion à la base de données...");

        try (Connection connection = DriverManager.getConnection(URL, USER, PASSWORD)) {
            System.out.println("Connexion réussie !");
            System.out.println("Informations sur la connexion :");
            System.out.println("Base de données : " + connection.getCatalog());
            System.out.println("Version MySQL : " + connection.getMetaData().getDatabaseProductVersion());
        } catch (SQLException e) {
            System.err.println("Échec de la connexion : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
