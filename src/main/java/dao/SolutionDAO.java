package dao;

import models.Solution;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO (Data Access Object) de la solution.
 * Fournit des méthodes pour accéder aux données de la table "solution" de la base de données.
 */
public class SolutionDAO {

    /**
     * Récupère la liste des solutions associées à un exercice donné.
     *
     * @param exerciceId l'identifiant de l'exercice
     * @return une liste de solutions
     */
    public List<Solution> getSolutionsByExercice(int exerciceId) {
        List<Solution> solutions = new ArrayList<>();
        String sql = "SELECT s.* FROM solution s WHERE s.exercice_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Définition du paramètre dans la requête
            stmt.setInt(1, exerciceId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                // Création d'une instance de Solution avec les valeurs récupérées
                Solution solution = new Solution(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getInt("exercice_id"),
                        rs.getInt("auteur_id")
                );
                
                // Récupération du nom de l'auteur de manière indépendante pour plus de robustesse
                try {
                    int auteurId = rs.getInt("auteur_id");
                    solution.setAuteurNom(getUserName(auteurId));
                } catch (Exception e) {
                    // En cas d'erreur, on définit un nom par défaut
                    solution.setAuteurNom("Utilisateur " + rs.getInt("auteur_id"));
                }
                
                solutions.add(solution);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return solutions;
    }

    /**
     * Méthode utilitaire pour récupérer le nom d'un utilisateur à partir de son ID.
     *
     * @param userId l'identifiant de l'utilisateur
     * @return le nom de l'utilisateur ou un nom par défaut si non trouvé
     */
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
            // En cas d'erreur, retourne un nom par défaut
        }
        return "Utilisateur " + userId;
    }

    /**
     * Ajoute une nouvelle solution dans la base de données.
     *
     * @param solution la solution à ajouter
     * @return true si l'ajout a réussi, false sinon
     */
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

    /**
     * Met à jour une solution existante dans la base de données.
     *
     * @param solution la solution à mettre à jour
     * @return true si la mise à jour a réussi, false sinon
     */
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

    /**
     * Supprime une solution à partir de son identifiant.
     *
     * @param id l'identifiant de la solution à supprimer
     * @return true si la suppression a réussi, false sinon
     */
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

    /**
     * Récupère une solution par son identifiant.
     *
     * @param id l'identifiant de la solution
     * @return la solution si trouvée, sinon null
     */
    public Solution getSolutionById(int id) {
        String sql = "SELECT s.* FROM solution s WHERE s.id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                // Création de l'objet Solution avec les champs extraits de la base
                Solution solution = new Solution(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getInt("exercice_id"),
                        rs.getInt("auteur_id")
                );
                
                // Récupération du nom de l'auteur via la méthode utilitaire
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

    /**
     * Récupère la liste des solutions dont l'exercice est créé par un utilisateur donné.
     *
     * @param createurId l'identifiant du créateur de l'exercice
     * @return une liste de solutions
     */
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
                    // Instanciation de la solution à partir des résultats
                    Solution solution = new Solution(
                            rs.getInt("id"),
                            rs.getString("contenu"),
                            rs.getTimestamp("date_creation").toLocalDateTime(),
                            rs.getInt("exercice_id"),
                            rs.getInt("auteur_id")
                    );
                    
                    // Récupération du nom de l'auteur
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
    
    /**
     * Récupère la liste des solutions soumises par un auteur spécifique.
     *
     * @param auteurId l'identifiant de l'auteur
     * @return une liste de solutions
     */
    public List<Solution> getSolutionsByAuteur(int auteurId) {
        List<Solution> solutions = new ArrayList<>();
        String sql = "SELECT s.* FROM solution s WHERE s.auteur_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, auteurId);
            
            try {
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    // Création d'une solution avec les données de la requête
                    Solution solution = new Solution(
                            rs.getInt("id"),
                            rs.getString("contenu"),
                            rs.getTimestamp("date_creation").toLocalDateTime(),
                            rs.getInt("exercice_id"),
                            rs.getInt("auteur_id")
                    );
                    
                    // On utilise l'ID de l'auteur pour obtenir son nom
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

    /**
     * Ajoute une solution dans la base de données et renvoie l'objet solution avec son ID attribué.
     *
     * @param solution la solution à ajouter
     * @return la solution créée avec son identifiant ou null si l'opération échoue
     */
    public Solution addSolutionAndReturn(Solution solution) {
        String insertSql = "INSERT INTO solution (contenu, date_creation, exercice_id, auteur_id) VALUES (?, ?, ?, ?)";
        String getLastIdSql = "SELECT LAST_INSERT_ID()";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Démarrage de la transaction
            
            // Exécution de l'insertion
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, solution.getContenu());
                insertStmt.setTimestamp(2, Timestamp.valueOf(solution.getDateCreation()));
                insertStmt.setInt(3, solution.getExerciceId());
                insertStmt.setInt(4, solution.getAuteurId());
                insertStmt.executeUpdate();
            }
            
            int lastInsertId = 0;
            // Récupération de l'identifiant généré pour la solution insérée
            try (PreparedStatement idStmt = conn.prepareStatement(getLastIdSql);
                 ResultSet rs = idStmt.executeQuery()) {
                if (rs.next()) {
                    lastInsertId = rs.getInt(1);
                }
            }
            
            conn.commit(); // Validation de la transaction
            
            if (lastInsertId > 0) {
                solution.setId(lastInsertId);
                return solution;
            }
            
            return null;
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Annulation en cas d'erreur
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Rétablissement du mode autocommit
                    conn.close();
                }
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }
}
