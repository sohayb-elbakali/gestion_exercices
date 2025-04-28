package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import models.Exercice;
import utils.DatabaseConnection;

import java.util.function.Consumer;

public class ModifierExerciceController {

    @FXML private TextField titreField;
    @FXML private TextArea descriptionArea;

    private Exercice exercice;
    private Consumer<Void> refreshCallback;

    /**
     * Définit l'exercice à modifier.
     */
    public void setExercice(Exercice exercice) {
        this.exercice = exercice;
        titreField.setText(exercice.getTitre());
        descriptionArea.setText(exercice.getDescription());
    }

    /**
     * Définit le callback pour rafraîchir la liste des exercices.
     */
    public void setRefreshCallback(Consumer<Void> callback) {
        this.refreshCallback = callback;
    }

    /**
     * Gère l'action "Enregistrer".
     */
    @FXML
    private void handleSave() {
        String nouveauTitre = titreField.getText();
        String nouvelleDescription = descriptionArea.getText();

        if (nouveauTitre.isEmpty() || nouvelleDescription.isEmpty()) {
            showErrorAlert("Erreur", "Veuillez remplir tous les champs.");
            return;
        }

        String sql = "UPDATE exercice SET titre = ?, description = ? WHERE id = ?";
        try (var conn = DatabaseConnection.getConnection();
             var stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nouveauTitre);
            stmt.setString(2, nouvelleDescription);
            stmt.setInt(3, exercice.getId());

            stmt.executeUpdate();

            // Appeler le callback pour rafraîchir la liste des exercices
            if (refreshCallback != null) {
                refreshCallback.accept(null);
            }

            closeWindow();

        } catch (Exception e) {
            showErrorAlert("Erreur SQL", "Impossible de mettre à jour l'exercice : " + e.getMessage());
        }
    }

    /**
     * Gère l'action "Annuler".
     */
    @FXML
    private void handleCancel() {
        closeWindow();
    }

    /**
     * Ferme la fenêtre actuelle.
     */
    private void closeWindow() {
        titreField.getScene().getWindow().hide();
    }

    /**
     * Affiche une alerte d'erreur.
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}