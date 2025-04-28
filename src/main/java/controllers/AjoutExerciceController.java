package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class AjoutExerciceController {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private Button validerButton;

    private int matiereId;
    private int createurId;

    @FXML
    public void initialize() {
        validerButton.setOnAction(event -> {
            if (validerFormulaire()) {
                if (insererExerciceDansBase()) {
                    fermerFenetre();
                }
            } else {
                afficherAlerte("Veuillez remplir tous les champs.");
            }
        });
    }

    private boolean validerFormulaire() {
        return !titreField.getText().isEmpty() && !descriptionField.getText().isEmpty();
    }

    private boolean insererExerciceDansBase() {
        // Afficher les valeurs de matiereId et createurId pour débogage
        System.out.println("matiereId : " + matiereId);
        System.out.println("createurId : " + createurId);

        String sql = "INSERT INTO exercice (titre, description, date_creation, matiere_id, createur_id) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, titreField.getText());
            stmt.setString(2, descriptionField.getText());
            stmt.setTimestamp(3, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, matiereId);
            stmt.setInt(5, createurId);

            stmt.executeUpdate();
            return true;

        } catch (SQLException e) {
            afficherAlerte("Erreur SQL : " + e.getMessage());
            return false;
        }
    }

    private void fermerFenetre() {
        Stage stage = (Stage) validerButton.getScene().getWindow();
        stage.close();
    }

    private void afficherAlerte(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setMatiereId(int id) {
        this.matiereId = id;
    }

    public void setCreateurId(int id) {
        this.createurId = id;
    }
}
