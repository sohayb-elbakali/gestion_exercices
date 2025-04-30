package controllers;

import dao.UtilisateurDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import models.Utilisateur;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contrôleur pour le formulaire d'inscription des utilisateurs :
 *  • Validation des champs
 *  • Création d'un nouvel Utilisateur via UtilisateurDAO
 */
public class RegisterController {
    private static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());
    
    // === Composants FXML : Champs du formulaire ===
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;
    
    // === Attributs principaux ===
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    
    // === Initialisation de la vue ===
    @FXML
    /**
     * Initialise le ComboBox des rôles et réinitialise le label de statut.
     */
    public void initialize() {
        roleComboBox.getItems().addAll("Étudiant", "Professeur");
        roleComboBox.setValue("Étudiant");
        statusLabel.setText("");
    }
    
    // === Gestion des événements utilisateur ===
    @FXML
    /**
     * Valide les champs et traite l'inscription de l'utilisateur.
     */
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
    /**
     * Annule l'inscription et ferme la fenêtre.
     */
    private void handleCancel() {
        getStage().close();
    }
    
    // === Méthodes utilitaires ===
    /**
     * Vérifie la validité des saisies du formulaire et affiche les erreurs le cas échéant.
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
     * Affiche un message de succès ou d'erreur sous le formulaire.
     */
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green"));
    }
    
    /**
     * Récupère la stage actuelle pour fermer la fenêtre de dialogue.
     */
    private Stage getStage() {
        return (Stage) emailField.getScene().getWindow();
    }
} 
