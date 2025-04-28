package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import models.Exercice;
import models.Solution;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SolutionController {
    @FXML
    private TextArea solutionArea;
    @FXML
    private ListView<Solution> solutionList;

    private Exercice exercice;
    private ObservableList<Solution> solutions = FXCollections.observableArrayList();

    public void setExercice(Exercice exercice) {
        this.exercice = exercice;
        loadSolutions();
    }

    @FXML
    public void initialize() {
        // Configuration initiale si nécessaire
        solutionList.setItems(solutions);
    }

    private void loadSolutions() {
        String sql = "SELECT s.*, u.nom FROM solution s " +
                "JOIN utilisateur u ON s.auteur_id = u.id " +
                "WHERE s.exercice_id = ? ORDER BY s.date_creation DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, exercice.getId());
            ResultSet rs = stmt.executeQuery();

            solutions.clear();
            while (rs.next()) {
                Solution solution = new Solution(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getInt("exercice_id"),
                        rs.getInt("auteur_id")
                );
                solution.setAuteurNom(rs.getString("nom"));
                solutions.add(solution);
            }

        } catch (SQLException e) {
            showErrorAlert("Erreur de chargement des solutions",
                    "Une erreur est survenue lors du chargement des solutions : " + e.getMessage());
        }
    }

    @FXML
    private void handleAddSolution() {
        String contenu = solutionArea.getText().trim();
        if (!contenu.isEmpty()) {
            String sql = "INSERT INTO solution (contenu, exercice_id, auteur_id) VALUES (?, ?, ?)";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                // Remplacez 1 par l'ID de l'utilisateur connecté
                stmt.setString(1, contenu);
                stmt.setInt(2, exercice.getId());
                stmt.setInt(3, 1); // ID de l'utilisateur actuel

                stmt.executeUpdate();
                solutionArea.clear();
                loadSolutions();

            } catch (SQLException e) {
                showErrorAlert("Erreur d'ajout",
                        "Impossible d'ajouter la solution : " + e.getMessage());
            }
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}