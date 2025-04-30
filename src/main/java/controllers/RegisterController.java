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
 *  • Validation des champs du formulaire
 *  • Création d'un nouvel Utilisateur via UtilisateurDAO
 */
public class RegisterController {
    private static final Logger LOGGER = Logger.getLogger(RegisterController.class.getName());
    
    // === Composants FXML : Champs du formulaire d'inscription ===
    @FXML private TextField nomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private ComboBox<String> roleComboBox;
    @FXML private Label statusLabel;
    
    // === Instance du DAO pour accéder aux données des utilisateurs ===
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    
    // === Initialisation de la vue ===
    @FXML
    /**
     * Initialise le ComboBox des rôles et réinitialise le label de statut.
     */
    public void initialize() {
        // Remplit le ComboBox des rôles disponibles
        roleComboBox.getItems().addAll("Étudiant", "Professeur");
        // Définit la valeur par défaut
        roleComboBox.setValue("Étudiant");
        // Réinitialise le message de status
        statusLabel.setText("");
    }
    
    // === Gestion des événements utilisateur ===
    @FXML
    /**
     * Validation des champs et traitement de l'inscription.
     * Vérifie que le formulaire est bien rempli, que l'email n'est pas déjà utilisé,
     * et crée un nouvel utilisateur en appelant le DAO.
     */
    private void handleRegister() {
        // Validation du formulaire d'inscription
        if (!validateForm()) {
            return;
        }
        
        // Récupération des valeurs saisies
        String nom = nomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();
        
        try {
            // Vérifie si un utilisateur avec cet email existe déjà
            if (utilisateurDAO.userExists(email)) {
                showStatus("Un utilisateur avec cet email existe déjà.", true);
                return;
            }
            
            // Création et sauvegarde du nouvel utilisateur
            Utilisateur newUser = new Utilisateur(email, password, role);
            newUser.setNom(nom);
            
            boolean success = utilisateurDAO.addUtilisateur(newUser);
            
            if (success) {
                // Si l'inscription est réussie, marque le succès et ferme la fenêtre
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
     * @return true si le formulaire est correctement rempli, false sinon
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
     * @param message Le message à afficher.
     * @param isError true si le message est une erreur (texte en rouge), false en cas de succès (texte en vert).
     */
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green"));
    }
    
    /**
     * Récupère la Stage actuelle en se basant sur le champ emailField.
     * Permet de fermer la fenêtre de dialogue.
     * @return La Scene parent sous forme de Stage.
     */
    private Stage getStage() {
        return (Stage) emailField.getScene().getWindow();
    }
}
