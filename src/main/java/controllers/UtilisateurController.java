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
 * Controller principal pour la gestion des utilisateurs :
 *  • Connexion (login)
 *  • Inscription (register)
 *  • Déconnexion (logout)
 *  • Navigation vers la sélection de matière
 */
public class UtilisateurController {
    private static final Logger LOGGER = Logger.getLogger(UtilisateurController.class.getName());

    // === Composants FXML : Champs de la vue ===
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;
    
    // === Attributs principaux ===
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private Stage currentStage;
    
    // === Constructeur et initialisation ===
    /**
     * Définit la fenêtre courante pour la navigation.
     */
    public void setCurrentStage(Stage stage) {
        this.currentStage = stage;
    }

    @FXML
    /**
     * Initialise la vue : configure le ComboBox et vide le statut.
     */
    public void initialize() {
        if (roleComboBox != null) {
            roleComboBox.getItems().addAll("Étudiant", "Professeur");
            roleComboBox.setValue("Étudiant"); // Default selection
        }
        
        if (statusLabel != null) {
            statusLabel.setText(""); // Clear status message
        }
    }

    // === Gestion des événements utilisateur ===
    /**
     * Traite la tentative de connexion de l'utilisateur.
     */
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
    
    /**
     * Ouvre et gère le formulaire d'inscription.
     */
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
            IconHelper.setStageIcon(stage);
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
    
    /**
     * Réinitialise le formulaire (action Retour).
     */
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
     * Affiche un message de statut sur l'interface.
     */
    private void showStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green"));
        }
    }
    
    /**
     * Ouvre l'écran de sélection de matière après connexion.
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
    
    /**
     * Traite la déconnexion et réaffiche l'écran de login.
     */
    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Connexion");
            stage.setScene(scene);
            IconHelper.setStageIcon(stage);
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error logging out", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Déconnexion impossible", e.getMessage());
        }
    }
    
    /**
     * Ferme la fenêtre courante.
     */
    private void closeCurrentStage() {
        if (currentStage != null) {
            currentStage.close();
        } else if (emailField != null) {
            Stage stage = (Stage) emailField.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
    
    /**
     * Affiche une boîte de dialogue d'alerte.
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
