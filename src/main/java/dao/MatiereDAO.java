package dao;

import models.Matiere;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des matières.
 * Fournit des méthodes d'accès à la table "matiere" dans la base de données.
 */
public class MatiereDAO {

    /**
     * Récupère toutes les matières triées par nom.
     *
     * @return une liste de matières.
     */
    public List<Matiere> getAllMatieres() {
        List<Matiere> matieres = new ArrayList<>();
        String sql = "SELECT id, nom FROM matiere ORDER BY nom";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            // Parcours des résultats de la requête
            while (rs.next()) {
                matieres.add(new Matiere(
                        rs.getInt("id"),
                        rs.getString("nom")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return matieres;
    }

    /**
     * Récupère une matière à partir de son identifiant.
     *
     * @param id l'identifiant de la matière.
     * @return la matière correspondante, ou null si non trouvée.
     */
    public Matiere getMatiereById(int id) {
        String sql = "SELECT id, nom FROM matiere WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Matiere(
                        rs.getInt("id"),
                        rs.getString("nom")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Ajoute une nouvelle matière dans la base de données.
     *
     * @param matiere la matière à ajouter.
     * @return true si l'ajout réussit, false sinon.
     */
    public boolean addMatiere(Matiere matiere) {
        String sql = "INSERT INTO matiere (nom) VALUES (?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, matiere.getNom());
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Met à jour une matière existante dans la base de données.
     *
     * @param matiere la matière à mettre à jour.
     * @return true si la mise à jour réussit, false sinon.
     */
    public boolean updateMatiere(Matiere matiere) {
        String sql = "UPDATE matiere SET nom = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, matiere.getNom());
            stmt.setInt(2, matiere.getId());
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Supprime une matière de la base de données en fonction de son identifiant.
     * Avant la suppression, vérifie s'il existe des exercices dépendants.
     *
     * @param id l'identifiant de la matière à supprimer.
     * @return true si la suppression réussit, false sinon.
     */
    public boolean deleteMatiere(int id) {
        // Vérifier s'il y a des exercices liés à cette matière
        if (hasDependentExercices(id)) {
            // Ne peut pas supprimer une matière qui a des exercices associés
            return false;
        }
        
        String sql = "DELETE FROM matiere WHERE id = ?";
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
    
    /**
     * Vérifie si une matière possède des exercices associés.
     *
     * @param matiereId l'identifiant de la matière.
     * @return true si des exercices existent, false sinon.
     */
    private boolean hasDependentExercices(int matiereId) {
        String sql = "SELECT COUNT(*) FROM exercice WHERE matiere_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, matiereId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // En cas d'erreur, on part sur qu'il y a des dépendances pour éviter une suppression accidentelle
        return true;
    }
    
    /**
     * Ajoute une nouvelle matière et renvoie la matière créée avec son identifiant.
     *
     * @param matiere la matière à ajouter.
     * @return la matière créée avec son ID mis à jour, ou null en cas d'échec.
     */
    public Matiere addMatiereAndReturn(Matiere matiere) {
        String insertSql = "INSERT INTO matiere (nom) VALUES (?)";
        String getLastIdSql = "SELECT LAST_INSERT_ID()";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Insertion de la matière
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, matiere.getNom());
                insertStmt.executeUpdate();
            }
            
            int lastInsertId = 0;
            // Récupération de l'identifiant généré pour la matière insérée
            try (PreparedStatement idStmt = conn.prepareStatement(getLastIdSql);
                 ResultSet rs = idStmt.executeQuery()) {
                if (rs.next()) {
                    lastInsertId = rs.getInt(1);
                }
            }
            
            conn.commit();
            
            if (lastInsertId > 0) {
                matiere.setId(lastInsertId);
                return matiere;
            }
            
            return null;
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Annule la transaction en cas d'erreur
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Rétablissement du mode auto-commit
                    conn.close();
                }
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }
    
    /**
     * Vérifie si une matière avec un nom donné existe déjà dans la base.
     *
     * @param nom le nom de la matière à vérifier.
     * @return true si la matière existe, false sinon.
     */
    public boolean matiereExists(String nom) {
        String sql = "SELECT COUNT(*) FROM matiere WHERE nom = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nom);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}
