package controllers;

import dao.UtilisateurDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Utilisateur;
import utils.IconHelper;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for login view
 */
public class LoginController {
    private static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;
    
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    
    @FXML
    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("Étudiant", "Professeur");
            roleComboBox.setValue("Étudiant"); // Default selection
        }
        
        if (statusLabel != null) {
            statusLabel.setText(""); // Clear status message
        }
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        // Basic validation
        if (email == null || email.isEmpty() || password == null || password.isEmpty() || role == null) {
            showStatus("Veuillez remplir tous les champs.", true);
            return;
        }

        try {
            Utilisateur utilisateur = utilisateurDAO.findByEmailAndPasswordAndRole(email, password, role);
            
            if (utilisateur != null) {
                int userId = utilisateur.getId();
                openMatiereSelection(userId, role);
            } else {
                showStatus("Identifiants incorrects. Veuillez vérifier votre email et mot de passe.", true);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during login", e);
            showStatus("Erreur de connexion: " + e.getMessage(), true);
        }
    }
    
    @FXML
    private void handleRegister() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register_view.fxml"));
            Parent root = loader.load();
            
            RegisterController controller = loader.getController();
            
            Stage stage = new Stage();
            stage.setTitle("Inscription");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();
            
            // Show success message if registration was successful
            if (Boolean.TRUE.equals(stage.getUserData())) {
                showStatus("Inscription réussie! Vous pouvez maintenant vous connecter.", false);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening registration form", e);
            showStatus("Erreur lors de l'ouverture du formulaire d'inscription: " + e.getMessage(), true);
        }
    }
    
    @FXML
    private void handleRetour() {
        // If there's a welcome screen to go back to, implement navigation here
        // For now, we'll just clear the form
        clearForm();
    }
    
    private void clearForm() {
        if (emailField != null) emailField.clear();
        if (passwordField != null) passwordField.clear();
        if (roleComboBox != null) roleComboBox.setValue("Étudiant");
        if (statusLabel != null) statusLabel.setText("");
    }
    
    /**
     * Display a status message
     */
    private void showStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green"));
        }
    }
    
    /**
     * Opens the matiere selection screen
     */
    private void openMatiereSelection(int userId, String role) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matiere_view.fxml"));
            Parent root = loader.load();

            MatiereController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(role);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Sélection de matière");
            stage.setScene(scene);
            IconHelper.setStageIcon(stage);
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening matiere selection", e);
            showStatus("Impossible d'ouvrir l'écran de sélection de matière: " + e.getMessage(), true);
        }
    }
    
    private void closeCurrentStage() {
        if (emailField != null) {
            Stage stage = (Stage) emailField.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
    
    /**
     * Display an alert dialog
     */
    private void showAlert(Alert.AlertType type, String title, String header, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        IconHelper.setDialogIcon(alert);
        alert.showAndWait();
    }
} 
