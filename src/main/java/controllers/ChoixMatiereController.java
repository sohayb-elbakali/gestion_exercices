package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import models.Matiere;
import utils.DatabaseConnection;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChoixMatiereController {

    // Logger for this class
    private static final Logger LOGGER = Logger.getLogger(ChoixMatiereController.class.getName());
    private int createurId; // ID de l'utilisateur connecté
    // FXML-injected components
    @FXML
    private ComboBox<Matiere> matiereComboBox;

    /**
     * Initializes the controller after the FXML file has been loaded.
     * This method is automatically called by JavaFX.
     */
    @FXML
    public void initialize() {
        loadMatieres();
    }
    public void setCreateurId(int id) {
        this.createurId = id;
    }

    /**
     * Loads the list of subjects (matières) from the database and populates the ComboBox.
     */
    private void loadMatieres() {
        String sql = "SELECT id, nom FROM matiere ORDER BY nom";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Matiere m = new Matiere(
                        rs.getInt("id"),
                        rs.getString("nom")
                );
                matiereComboBox.getItems().add(m);
            }

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des matières", e);
            showAlert(
                    AlertType.ERROR,
                    "Erreur de base de données",
                    "Impossible de charger la liste des matières",
                    e.getMessage()
            );
        }
    }

    /**
     * Handles the action when the user validates their subject selection.
     * Opens the exercise list view for the selected subject.
     */
    @FXML
    private void validerMatiere() {
        Matiere matiere = matiereComboBox.getValue();
        System.out.println("createurId : " + createurId);

        if (matiere == null) {
            showAlert(
                    AlertType.WARNING,
                    "Sélection requise",
                    "Aucune matière sélectionnée",
                    "Veuillez choisir une matière dans la liste."
            );
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/liste_exercices.fxml"));
            Parent root = loader.load();

            ExerciceController exerciceController = loader.getController();
            exerciceController.setMatiere(matiere);
            exerciceController.setCreateurId(createurId);



            Stage stage = new Stage();
            stage.setTitle("Exercices - " + matiere.getNom());
            stage.setScene(new Scene(root));

            Stage currentStage = (Stage) matiereComboBox.getScene().getWindow();
            currentStage.close();

            stage.show();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de la vue des exercices", e);
            showAlert(
                    AlertType.ERROR,
                    "Erreur d'interface",
                    "Impossible d'ouvrir la vue des exercices",
                    e.getMessage()
            );
        }
    }

    /**
     * Opens the "Mes Exercices" page.
     */
    @FXML
    private void ouvrirMesExercices() {
        System.out.println("createurId : " + createurId);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MesExercices.fxml"));
            Parent root = loader.load();

            MesExercicesController mesexercicescontroller = loader.getController();
            mesexercicescontroller.setCreateurId(createurId);

            Stage stage = new Stage();
            stage.setTitle("Mes Exercices");
            stage.setScene(new Scene(root));

            Stage currentStage = (Stage) matiereComboBox.getScene().getWindow();
            currentStage.close();

            stage.show();

        } catch (Exception e) {
            handleException(e, "Erreur lors de l'ouverture de la page Mes Exercices");
        }
    }

    /**
     * Displays an alert dialog to the user.
     *
     * @param type    The type of alert (e.g., ERROR, WARNING).
     * @param title   The title of the alert.
     * @param header  The header text of the alert.
     * @param content The detailed content of the alert.
     */
    private void showAlert(AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /**
     * Handles exceptions by logging them and displaying an alert to the user.
     *
     * @param e       The exception to handle.
     * @param message A custom error message to log and display.
     */
    private void handleException(Exception e, String message) {
        LOGGER.log(Level.SEVERE, message, e);
        showAlert(
                AlertType.ERROR,
                "Erreur d'interface",
                "Une erreur est survenue",
                message + ". Veuillez vérifier les logs pour plus de détails."
        );
    }
}