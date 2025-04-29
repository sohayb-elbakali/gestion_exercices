package controllers;

import dao.UtilisateurDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Utilisateur;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * Controller for user management including login, registration, and authentication
 */
public class UtilisateurController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private Stage currentStage;
    
    /**
     * Set the current stage for navigation purposes
     */
    public void setCurrentStage(Stage stage) {
        this.currentStage = stage;
    }

    @FXML
    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("Étudiant", "Professeur");
        }
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();

        try {
            Utilisateur utilisateur = utilisateurDAO.findByEmailAndPasswordAndRole(email, password, role);
            
            if (utilisateur != null) {
                int userId = utilisateur.getId();
                openMatiereSelection(userId);
            } else {
                showAlert(Alert.AlertType.ERROR, "Authentification", "Identifiants incorrects", 
                          "Veuillez vérifier votre email et mot de passe.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de connexion", e.getMessage());
        }
    }
    
    /**
     * Opens the matiere selection screen
     */
    private void openMatiereSelection(int userId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matiere_view.fxml"));
            Parent root = loader.load();

            MatiereController controller = loader.getController();
            controller.setUserId(userId);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Sélection de matière");
            stage.setScene(scene);
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                     "Impossible d'ouvrir l'écran de sélection de matière: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login_view.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Connexion");
            stage.setScene(scene);
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Déconnexion impossible", e.getMessage());
        }
    }
    
    private void closeCurrentStage() {
        if (currentStage != null) {
            currentStage.close();
        } else if (emailField != null) {
            ((Stage) emailField.getScene().getWindow()).close();
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
        alert.showAndWait();
    }
} 
