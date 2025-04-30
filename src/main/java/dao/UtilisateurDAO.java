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
    /**
     * Recherche un utilisateur dans la base de données par email, mot de passe et rôle.
     * Renvoie l'utilisateur si trouvé, sinon renvoie null.
     */
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
                
                // Essayer de récupérer le nom s'il existe, sinon utiliser le nom par défaut
                try {
                    String nom = rs.getString("nom");
                    if (nom != null && !nom.isEmpty()) {
                        user.setNom(nom);
                    }
                } catch (SQLException e) {
                    // La colonne 'nom' n'existe peut-être pas, on utilise le nom par défaut
                }
                
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Récupère un utilisateur par son identifiant.
     */
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
                
                // Essayer de récupérer le nom s'il est disponible
                try {
                    String nom = rs.getString("nom");
                    if (nom != null && !nom.isEmpty()) {
                        user.setNom(nom);
                    }
                } catch (SQLException e) {
                    // La colonne 'nom' n'existe pas, utiliser le nom par défaut
                }
                
                return user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Ajoute un nouvel utilisateur dans la base de données.
     * Cette méthode tente d'ajouter avec le champ 'nom' si disponible.
     */
    public boolean addUtilisateur(Utilisateur user) {
        // Vérifier si la colonne 'nom' existe
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
     * Méthode de secours pour ajouter un utilisateur sans utiliser la colonne 'nom'.
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
     * Vérifie si la colonne 'nom' existe dans la table 'utilisateur'.
     */
    private boolean hasNomColumn() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet columns = metaData.getColumns(null, null, "utilisateur", "nom");
            return columns.next(); // Renvoie true si la colonne existe
        } catch (SQLException e) {
            e.printStackTrace();
            return false; // En cas d'erreur, on suppose que la colonne n'existe pas
        }
    }
    
    /**
     * Vérifie si un utilisateur avec l'email donné existe déjà.
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
     * Récupère tous les utilisateurs (pour utilisation admin).
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
                
                // On tente de récupérer le nom s'il est disponible
                try {
                    String nom = rs.getString("nom");
                    if (nom != null && !nom.isEmpty()) {
                        user.setNom(nom);
                    }
                } catch (SQLException e) {
                    // La colonne 'nom' n'existe pas
                }
                
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
    
    /**
     * Supprime un utilisateur par son identifiant.
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
