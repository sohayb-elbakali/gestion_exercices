package dao;

import models.Utilisateur;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UtilisateurDAO {
    public Utilisateur findByEmailAndPasswordAndRole(String email, String password, String role) {
        String sql = "SELECT * FROM utilisateur WHERE email = ? AND mot_de_passe = ? AND role = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setString(2, password);
            stmt.setString(3, role);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Utilisateur user = new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("mot_de_passe"),
                        rs.getString("role")
                );
                
                // Try to get name if it exists, otherwise use default name
                try {
                    String nom = rs.getString("nom");
                    if (nom != null && !nom.isEmpty()) {
                        user.setNom(nom);
                    }
                } catch (SQLException e) {
                    // Nom column might not exist, use default name
                }
                
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Utilisateur getById(int id) {
        String sql = "SELECT * FROM utilisateur WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Utilisateur user = new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("mot_de_passe"),
                        rs.getString("role")
                );
                
                // Try to get name if it exists, otherwise use default name
                try {
                    String nom = rs.getString("nom");
                    if (nom != null && !nom.isEmpty()) {
                        user.setNom(nom);
                    }
                } catch (SQLException e) {
                    // Nome column might not exist, use default name
                }
                
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Add a new user to the database
     */
    public boolean addUtilisateur(Utilisateur user) {
        // First check if 'nom' column exists
        if (hasNomColumn()) {
            String sql = "INSERT INTO utilisateur (email, mot_de_passe, role, nom) VALUES (?, ?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, user.getEmail());
                stmt.setString(2, user.getMotDePasse());
                stmt.setString(3, user.getRole());
                stmt.setString(4, user.getNom());
                int rowsAffected = stmt.executeUpdate();
                return rowsAffected > 0;
            } catch (SQLException e) {
                e.printStackTrace();
                return fallbackAddUser(user);
            }
        } else {
            return fallbackAddUser(user);
        }
    }
    
    /**
     * Fallback method to add a user without the nom column
     */
    private boolean fallbackAddUser(Utilisateur user) {
        String sql = "INSERT INTO utilisateur (email, mot_de_passe, role) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getMotDePasse());
            stmt.setString(3, user.getRole());
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Check if the nom column exists in the utilisateur table
     */
    private boolean hasNomColumn() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "utilisateur", "nom");
            return columns.next(); // Returns true if the column exists
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // Assume the column doesn't exist if there's an error
        }
    }
    
    /**
     * Check if a user with the given email already exists
     */
    public boolean userExists(String email) {
        String sql = "SELECT COUNT(*) FROM utilisateur WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get all users (for admin use)
     */
    public List<Utilisateur> getAllUsers() {
        List<Utilisateur> users = new ArrayList<>();
        String sql = "SELECT * FROM utilisateur";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Utilisateur user = new Utilisateur(
                        rs.getInt("id"),
                        rs.getString("email"),
                        rs.getString("mot_de_passe"),
                        rs.getString("role")
                );
                
                // Try to get name if it exists
                try {
                    String nom = rs.getString("nom");
                    if (nom != null && !nom.isEmpty()) {
                        user.setNom(nom);
                    }
                } catch (SQLException e) {
                    // Nom column might not exist
                }
                
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    /**
     * Delete a user by ID
     */
    public boolean deleteUser(int id) {
        String sql = "DELETE FROM utilisateur WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
} 
