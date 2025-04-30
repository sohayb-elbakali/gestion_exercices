package dao;

import models.Exercice;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO pour la gestion des exercices.
 * Cette classe fournit des méthodes pour interagir avec la table "exercice" de la base de données.
 */
public class ExerciceDAO {

    /**
     * Récupère la liste des exercices filtrés par l'identifiant de la matière.
     *
     * @param matiereId l'identifiant de la matière
     * @return une liste d'objets Exercice correspondant à la matière
     */
    public List<Exercice> getExercicesByMatiere(int matiereId) {
        List<Exercice> exercices = new ArrayList<>();
        String sql = "SELECT e.*, m.nom as matiere_nom FROM exercice e " +
                     "JOIN matiere m ON e.matiere_id = m.id " +
                     "WHERE e.matiere_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            // Définir le paramètre de requête pour le matiereId
            stmt.setInt(1, matiereId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                // Récupérer le timestamp de création et le convertir en LocalDateTime
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();
                // Création de l'objet Exercice avec les données extraites
                Exercice exercice = new Exercice(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        dateCreation,
                        rs.getInt("matiere_id"),
                        rs.getInt("createur_id")
                );
                // Définir le nom de la matière récupéré depuis la jointure
                exercice.setMatiereNom(rs.getString("matiere_nom"));
                exercices.add(exercice);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exercices;
    }

    /**
     * Ajoute un nouvel exercice dans la base de données.
     *
     * @param exercice l'objet Exercice à ajouter
     * @return true si l'ajout est réussi, false sinon
     */
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

    /**
     * Met à jour un exercice existant dans la base de données.
     *
     * @param exercice l'objet Exercice contenant les nouvelles valeurs
     * @return true si la mise à jour a réussi, false sinon
     */
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

    /**
     * Supprime un exercice de la base de données.
     * Avant la suppression, toutes les solutions associées à l'exercice sont supprimées.
     *
     * @param id l'identifiant de l'exercice à supprimer
     * @return true si la suppression est réussie, false sinon
     */
    public boolean deleteExercice(int id) {
        // Première étape : suppression des solutions liées à cet exercice
        String deleteSolutions = "DELETE FROM solution WHERE exercice_id = ?";
        // Deuxième étape : suppression de l'exercice lui-même
        String deleteExercice = "DELETE FROM exercice WHERE id = ?";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Démarrage d'une transaction
            
            // Suppression des solutions associées
            try (PreparedStatement stmt = conn.prepareStatement(deleteSolutions)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
            }
            
            // Suppression de l'exercice
            try (PreparedStatement stmt = conn.prepareStatement(deleteExercice)) {
                stmt.setInt(1, id);
                int rowsAffected = stmt.executeUpdate();
                conn.commit(); // Valider la transaction
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            // En cas d'erreur, annuler la transaction
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
                    conn.setAutoCommit(true); // Réinitialisation du mode automatique
                    conn.close();
                }
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }

    /**
     * Récupère un exercice par son identifiant.
     *
     * @param id l'identifiant de l'exercice
     * @return l'objet Exercice correspondant, ou null si non trouvé
     */
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
                // Affecte le nom de la matière à l'exercice
                exercice.setMatiereNom(rs.getString("matiere_nom"));
                return exercice;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Récupère la liste des exercices créés par un utilisateur spécifique.
     *
     * @param createurId l'identifiant du créateur
     * @return une liste d'exercices créés par cet utilisateur
     */
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
                // Affecte le nom de la matière récupéré
                exercice.setMatiereNom(rs.getString("matiere_nom"));
                exercices.add(exercice);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exercices;
    }

    /**
     * Récupère la liste de tous les exercices présents dans la base de données.
     *
     * @return une liste de tous les exercices
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
     * Ajoute un exercice à la base de données et renvoie l'exercice créé avec son identifiant.
     *
     * @param exercice l'exercice à ajouter
     * @return l'objet Exercice créé avec son ID mis à jour, ou null en cas d'échec
     */
    public Exercice addExerciceAndReturn(Exercice exercice) {
        String insertSql = "INSERT INTO exercice (titre, description, date_creation, matiere_id, createur_id) VALUES (?, ?, ?, ?, ?)";
        String getLastIdSql = "SELECT LAST_INSERT_ID()";
        
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);
            
            // Insertion de l'exercice dans la base
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, exercice.getTitre());
                insertStmt.setString(2, exercice.getDescription());
                insertStmt.setTimestamp(3, Timestamp.valueOf(exercice.getDateCreation()));
                insertStmt.setInt(4, exercice.getMatiereId());
                insertStmt.setInt(5, exercice.getCreateurId());
                insertStmt.executeUpdate();
            }
            
            int lastInsertId = 0;
            // Récupération de l'identifiant généré pour l'exercice inséré
            try (PreparedStatement idStmt = conn.prepareStatement(getLastIdSql);
                 ResultSet rs = idStmt.executeQuery()) {
                if (rs.next()) {
                    lastInsertId = rs.getInt(1);
                }
            }
            
            conn.commit();
            
            if (lastInsertId > 0) {
                // Récupère et renvoie l'exercice inséré via son ID
                return getExerciceById(lastInsertId);
            }
            
            return null;
        } catch (SQLException e) {
            try {
                if (conn != null) {
                    conn.rollback(); // Annuler la transaction en cas d'erreur
                }
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Réinitialiser le mode auto-commit
                    conn.close();
                }
            } catch (SQLException closeEx) {
                closeEx.printStackTrace();
            }
        }
    }
}
