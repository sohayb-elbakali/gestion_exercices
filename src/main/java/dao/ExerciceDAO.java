package dao;

import models.Exercice;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ExerciceDAO {
    public List<Exercice> getExercicesByMatiere(int matiereId) {
        List<Exercice> exercices = new ArrayList<>();
        String sql = "SELECT e.*, m.nom as matiere_nom FROM exercice e " +
                    "JOIN matiere m ON e.matiere_id = m.id " +
                    "WHERE e.matiere_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, matiereId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
                Exercice exercice = new Exercice(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        dateCreation,
                        rs.getInt("matiere_id"),
                        rs.getInt("createur_id")
                );
                exercice.setMatiereNom(rs.getString("matiere_nom"));
                exercices.add(exercice);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exercices;
    }

    public boolean addExercice(Exercice exercice) {
        String sql = "INSERT INTO exercice (titre, description, date_creation, matiere_id, createur_id) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, exercice.getTitre());
            stmt.setString(2, exercice.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(exercice.getDateCreation()));
            stmt.setInt(4, exercice.getMatiereId());
            stmt.setInt(5, exercice.getCreateurId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateExercice(Exercice exercice) {
        String sql = "UPDATE exercice SET titre = ?, description = ?, date_creation = ?, matiere_id = ?, createur_id = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, exercice.getTitre());
            stmt.setString(2, exercice.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(exercice.getDateCreation()));
            stmt.setInt(4, exercice.getMatiereId());
            stmt.setInt(5, exercice.getCreateurId());
            stmt.setInt(6, exercice.getId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteExercice(int id) {
        // First, delete all solutions associated with this exercise
        String deleteSolutions = "DELETE FROM solution WHERE exercice_id = ?";
        String deleteExercice = "DELETE FROM exercice WHERE id = ?";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Start a transaction
            
            // Delete associated solutions first
            try (PreparedStatement stmt = conn.prepareStatement(deleteSolutions)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            
            // Then delete the exercise
            try (PreparedStatement stmt = conn.prepareStatement(deleteExercice)) {
                stmt.setInt(1, id);
                int rowsAffected = stmt.executeUpdate();
                
                conn.commit(); // Commit the transaction
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            // If there's an error, rollback the transaction
            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Reset auto-commit mode
                    conn.close();
                }
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }

    public Exercice getExerciceById(int id) {
        String sql = "SELECT e.*, m.nom as matiere_nom FROM exercice e " +
                    "JOIN matiere m ON e.matiere_id = m.id " +
                    "WHERE e.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
                Exercice exercice = new Exercice(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        dateCreation,
                        rs.getInt("matiere_id"),
                        rs.getInt("createur_id")
                );
                exercice.setMatiereNom(rs.getString("matiere_nom"));
                return exercice;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Exercice> getExercicesByCreateur(int createurId) {
        List<Exercice> exercices = new ArrayList<>();
        String sql = "SELECT e.*, m.nom as matiere_nom FROM exercice e " +
                    "JOIN matiere m ON e.matiere_id = m.id " +
                    "WHERE e.createur_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, createurId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
                Exercice exercice = new Exercice(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        dateCreation,
                        rs.getInt("matiere_id"),
                        rs.getInt("createur_id")
                );
                exercice.setMatiereNom(rs.getString("matiere_nom"));
                exercices.add(exercice);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exercices;
    }

    /**
     * Get a list of all exercises in the database
     */
    public List<Exercice> getAllExercices() {
        List<Exercice> exercices = new ArrayList<>();
        String sql = "SELECT e.*, m.nom as matiere_nom FROM exercice e " +
                    "JOIN matiere m ON e.matiere_id = m.id";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
                Exercice exercice = new Exercice(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        dateCreation,
                        rs.getInt("matiere_id"),
                        rs.getInt("createur_id")
                );
                exercice.setMatiereNom(rs.getString("matiere_nom"));
                exercices.add(exercice);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exercices;
    }
    
    /**
     * Add an exercise and return the created exercise with its ID
     */
    public Exercice addExerciceAndReturn(Exercice exercice) {
        String insertSql = "INSERT INTO exercice (titre, description, date_creation, matiere_id, createur_id) VALUES (?, ?, ?, ?, ?)";
        String getLastIdSql = "SELECT LAST_INSERT_ID()";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, exercice.getTitre());
                insertStmt.setString(2, exercice.getDescription());
                insertStmt.setTimestamp(3, Timestamp.valueOf(exercice.getDateCreation()));
                insertStmt.setInt(4, exercice.getMatiereId());
                insertStmt.setInt(5, exercice.getCreateurId());
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
                return getExerciceById(lastInsertId);
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
} 
