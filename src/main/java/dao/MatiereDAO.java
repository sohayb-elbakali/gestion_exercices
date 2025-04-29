package dao;

import models.Matiere;
import utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MatiereDAO {
    public List<Matiere> getAllMatieres() {
        List<Matiere> matieres = new ArrayList<>();
        String sql = "SELECT id, nom FROM matiere ORDER BY nom";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
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
     * Add a new matiere to the database
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
     * Update an existing matiere
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
     * Delete a matiere by ID
     */
    public boolean deleteMatiere(int id) {
        // First check if there are any exercises linked to this matiere
        if (hasDependentExercices(id)) {
            // Cannot delete a matiere with linked exercises
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
     * Check if a matiere has dependent exercices
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
        // In case of error, assume there are dependencies to prevent accidental deletion
        return true;
    }
    
    /**
     * Add a new matiere and return the created matiere with its ID
     */
    public Matiere addMatiereAndReturn(Matiere matiere) {
        String insertSql = "INSERT INTO matiere (nom) VALUES (?)";
        String getLastIdSql = "SELECT LAST_INSERT_ID()";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, matiere.getNom());
                insertStmt.executeUpdate();
            }
            
            int lastInsertId = 0;
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
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }
    
    /**
     * Check if a matiere with the given name already exists
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
