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
        String sql = "SELECT * FROM exercice WHERE matiere_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, matiereId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
                exercices.add(new Exercice(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        dateCreation,
                        rs.getInt("matiere_id"),
                        rs.getInt("createur_id")
                ));
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
        String sql = "SELECT * FROM exercice WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
                return new Exercice(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        dateCreation,
                        rs.getInt("matiere_id"),
                        rs.getInt("createur_id")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Exercice> getExercicesByCreateur(int createurId) {
        List<Exercice> exercices = new ArrayList<>();
        String sql = "SELECT * FROM exercice WHERE createur_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, createurId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
                exercices.add(new Exercice(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        dateCreation,
                        rs.getInt("matiere_id"),
                        rs.getInt("createur_id")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exercices;
    }
} 
