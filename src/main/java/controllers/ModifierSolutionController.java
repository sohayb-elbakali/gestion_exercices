package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import models.Solution;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class ModifierSolutionController {

    @FXML private TextArea contenuField;

    private Solution solution;

    /**
     * Définit la solution à modifier.
     */
    public void setSolution(Solution solution) {
        this.solution = solution;
        contenuField.setText(solution.getContenu()); // Pré-remplir le champ avec le contenu actuel
    }

    /**
     * Gère l'action de soumission pour enregistrer les modifications.
     */
    @FXML
    private void handleSubmit() {
        String nouveauContenu = contenuField.getText().trim();

        if (nouveauContenu.isEmpty()) {
            showAlert("Erreur", "Le contenu de la solution ne peut pas être vide.");
            return;
        }

        String sql = "UPDATE solution SET contenu = ?, date_creation = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nouveauContenu);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(3, solution.getId());

            stmt.executeUpdate();
            closeStage(); // Fermer la fenêtre après modification

        } catch (SQLException e) {
            showAlert("Erreur SQL", "Impossible de mettre à jour la solution: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Affiche une alerte à l'utilisateur.
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Ferme la fenêtre modale.
     */
    private void closeStage() {
        Stage stage = (Stage) contenuField.getScene().getWindow();
        stage.close();
    }
}
