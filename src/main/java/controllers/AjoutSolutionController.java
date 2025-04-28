package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import models.Solution;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class AjoutSolutionController {
    @FXML private TextArea contenuField;

    private int exerciceId;

    public void setExerciceId(int exerciceId) {
        this.exerciceId = exerciceId;

    }
    private int createurId;

    public void setCreateurId(int id) {
        this.createurId = id;
    }

    @FXML
    private void handleSubmit() {
        String contenu = contenuField.getText().trim();
        if (!contenu.isEmpty()) {
            try {
                // Supposons que l'ID de l'auteur soit disponible via createurId
                int auteurId = createurId; // Remplacez par la logique appropriée pour obtenir l'auteur_id

                String sql = "INSERT INTO solution (contenu, date_creation, exercice_id, auteur_id) VALUES (?, ?, ?, ?)";
                try (Connection conn = DatabaseConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(sql)) {

                    stmt.setString(1, contenu);
                    stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
                    stmt.setInt(3, exerciceId); // ID de l'exercice
                    stmt.setInt(4, auteurId); // ID de l'auteur

                    stmt.executeUpdate();
                    closeStage(); // Fermer la fenêtre après ajout
                }
            } catch (SQLException e) {
                showAlert("Erreur SQL: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showAlert("Le contenu de la solution ne peut pas être vide.");
        }
    }

    private void showAlert(String message) {
        // Implémentez la méthode comme dans ExerciceController
    }

    private void closeStage() {
        // Fermez la fenêtre après ajout
    }
}