package controllers;

import dao.UtilisateurDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Utilisateur;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for user registration
 */
public class RegisterController {
    private static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());
    
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;
    
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    
    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("Étudiant", "Professeur");
        roleComboBox.setValue("Étudiant");
        statusLabel.setText("");
    }
    
    @FXML
    private void handleRegister() {
        // Validate form
        if (!validateForm()) {
            return;
        }
        
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();
        
        try {
            // Check if user already exists
            if (utilisateurDAO.userExists(email)) {
                showStatus("Un utilisateur avec cet email existe déjà.", true);
                return;
            }
            
            // Create and save new user
            Utilisateur newUser = new Utilisateur(email, password, role);
            newUser.setNom(nom);
            
            boolean success = utilisateurDAO.addUtilisateur(newUser);
            
            if (success) {
                // Mark successful registration
                getStage().setUserData(Boolean.TRUE);
                getStage().close();
            } else {
                showStatus("Erreur lors de l'inscription. Veuillez réessayer.", true);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during registration", e);
            showStatus("Erreur: " + e.getMessage(), true);
        }
    }
    
    @FXML
    private void handleCancel() {
        getStage().close();
    }
    
    /**
     * Validate registration form inputs
     */
    private boolean validateForm() {
        if (nomField.getText().trim().isEmpty()) {
            showStatus("Le nom est obligatoire.", true);
            return false;
        }
        
        if (emailField.getText().trim().isEmpty()) {
            showStatus("L'email est obligatoire.", true);
            return false;
        }
        
        if (!emailField.getText().contains("@")) {
            showStatus("Email invalide. Veuillez entrer un email valide.", true);
            return false;
        }
        
        if (passwordField.getText().isEmpty()) {
            showStatus("Le mot de passe est obligatoire.", true);
            return false;
        }
        
        if (passwordField.getText().length() < 6) {
            showStatus("Le mot de passe doit contenir au moins 6 caractères.", true);
            return false;
        }
        
        if (!passwordField.getText().equals(confirmPasswordField.getText())) {
            showStatus("Les mots de passe ne correspondent pas.", true);
            return false;
        }
        
        if (roleComboBox.getValue() == null) {
            showStatus("Veuillez sélectionner un rôle.", true);
            return false;
        }
        
        return true;
    }
    
    /**
     * Show a status message
     */
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green"));
    }
    
    /**
     * Get the current stage
     */
    private Stage getStage() {
        return (Stage) emailField.getScene().getWindow();
    }
} 
