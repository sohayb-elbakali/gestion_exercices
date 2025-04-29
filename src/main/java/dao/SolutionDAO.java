package dao;

import models.Solution;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SolutionDAO {
    public List<Solution> getSolutionsByExercice(int exerciceId) {
        List<Solution> solutions = new ArrayList<>();
        String sql = "SELECT s.* FROM solution s WHERE s.exercice_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, exerciceId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Solution solution = new Solution(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getInt("exercice_id"),
                        rs.getInt("auteur_id")
                );
                
                // Get author name separately to be more resilient
                try {
                    int auteurId = rs.getInt("auteur_id");
                    solution.setAuteurNom(getUserName(auteurId));
                } catch (Exception e) {
                    solution.setAuteurNom("Utilisateur " + rs.getInt("auteur_id"));
                }
                
                solutions.add(solution);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return solutions;
    }

    // Helper method to get user name
    private String getUserName(int userId) {
        String sql = "SELECT nom FROM utilisateur WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String nom = rs.getString("nom");
                return (nom != null && !nom.isEmpty()) ? nom : "Utilisateur " + userId;
            }
        } catch (SQLException e) {
            // If there's an error, just return a default name
        }
        return "Utilisateur " + userId;
    }

    public boolean addSolution(Solution solution) {
        String sql = "INSERT INTO solution (contenu, date_creation, exercice_id, auteur_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, solution.getContenu());
            stmt.setTimestamp(2, Timestamp.valueOf(solution.getDateCreation()));
            stmt.setInt(3, solution.getExerciceId());
            stmt.setInt(4, solution.getAuteurId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateSolution(Solution solution) {
        String sql = "UPDATE solution SET contenu = ?, date_creation = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, solution.getContenu());
            stmt.setTimestamp(2, Timestamp.valueOf(solution.getDateCreation()));
            stmt.setInt(3, solution.getId());
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteSolution(int id) {
        String sql = "DELETE FROM solution WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Solution getSolutionById(int id) {
        String sql = "SELECT s.* FROM solution s WHERE s.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Solution solution = new Solution(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getInt("exercice_id"),
                        rs.getInt("auteur_id")
                );
                
                // Get author name separately
                try {
                    int auteurId = rs.getInt("auteur_id");
                    solution.setAuteurNom(getUserName(auteurId));
                } catch (Exception e) {
                    solution.setAuteurNom("Utilisateur " + rs.getInt("auteur_id"));
                }
                
                return solution;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Solution> getSolutionsByCreateur(int createurId) {
        List<Solution> solutions = new ArrayList<>();
        String sql = "SELECT s.* FROM solution s " +
                    "JOIN exercice e ON s.exercice_id = e.id " +
                    "WHERE e.createur_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, createurId);
            
            try {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Solution solution = new Solution(
                            rs.getInt("id"),
                            rs.getString("contenu"),
                            rs.getTimestamp("date_creation").toLocalDateTime(),
                            rs.getInt("exercice_id"),
                            rs.getInt("auteur_id")
                    );
                    
                    // Get author name separately
                    try {
                        int auteurId = rs.getInt("auteur_id");
                        solution.setAuteurNom(getUserName(auteurId));
                    } catch (Exception e) {
                        solution.setAuteurNom("Utilisateur " + rs.getInt("auteur_id"));
                    }
                    
                    solutions.add(solution);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return solutions;
    }
    
    public List<Solution> getSolutionsByAuteur(int auteurId) {
        List<Solution> solutions = new ArrayList<>();
        String sql = "SELECT s.* FROM solution s WHERE s.auteur_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auteurId);
            
            try {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Solution solution = new Solution(
                            rs.getInt("id"),
                            rs.getString("contenu"),
                            rs.getTimestamp("date_creation").toLocalDateTime(),
                            rs.getInt("exercice_id"),
                            rs.getInt("auteur_id")
                    );
                    
                    // Get author name separately
                    solution.setAuteurNom(getUserName(auteurId));
                    
                    solutions.add(solution);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return solutions;
    }

    public Solution addSolutionAndReturn(Solution solution) {
        String insertSql = "INSERT INTO solution (contenu, date_creation, exercice_id, auteur_id) VALUES (?, ?, ?, ?)";
        String getLastIdSql = "SELECT LAST_INSERT_ID()";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, solution.getContenu());
                insertStmt.setTimestamp(2, Timestamp.valueOf(solution.getDateCreation()));
                insertStmt.setInt(3, solution.getExerciceId());
                insertStmt.setInt(4, solution.getAuteurId());
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
                solution.setId(lastInsertId);
                return solution;
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
