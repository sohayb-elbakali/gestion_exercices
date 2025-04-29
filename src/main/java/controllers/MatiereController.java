package controllers;

import dao.MatiereDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import models.Matiere;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for subject (matiere) management and selection
 */
public class MatiereController {
    private static final Logger LOGGER = Logger.getLogger(MatiereController.class.getName());
    
    @FXML private ComboBox<Matiere> matiereComboBox;
    
    private int userId;
    private String userRole;
    private final MatiereDAO matiereDAO = new MatiereDAO();
    
    /**
     * Sets the user ID for the current logged-in user
     */
    public void setUserId(int userId) {
        this.userId = userId;
        LOGGER.info("User ID set to: " + userId);
    }

    /**
     * Sets the user role for the current logged-in user
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        LOGGER.info("User role set to: " + userRole);
    }

    @FXML
    public void initialize() {
        loadMatieres();
    }
    
    /**
     * Loads the available subjects from the database
     */
    private void loadMatieres() {
        try {
            matiereComboBox.getItems().clear();
            matiereComboBox.getItems().addAll(matiereDAO.getAllMatieres());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading matieres", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement", 
                     "Impossible de charger les matieres: " + e.getMessage());
        }
    }
    
    /**
     * Handles matiere selection and opens the exercise list for that subject
     */
    @FXML
    private void selectMatiere() {
        Matiere matiere = matiereComboBox.getValue();
        
        if (matiere == null) {
            showAlert(Alert.AlertType.WARNING, "Selection", "Aucune matiere selectionnee", 
                     "Veuillez selectionner une matiere.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exercice_view.fxml"));
            Parent root = loader.load();
            
            ExerciceController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            controller.setMatiere(matiere);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Exercices - " + matiere.getNom());
            stage.setScene(scene);
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening exercise view", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                     "Impossible d'afficher les exercices: " + e.getMessage());
        }
    }
    
    /**
     * Opens the "My Exercises" view
     */
    @FXML
    private void showMyExercises() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exercice_view.fxml"));
            Parent root = loader.load();
            
            ExerciceController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            controller.setShowUserExercisesOnly(true);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Mes Exercices");
            stage.setScene(scene);
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening my exercises view", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                     "Impossible d'afficher mes exercices: " + e.getMessage());
        }
    }
    
    /**
     * Opens the "My Solutions" view
     */
    @FXML
    private void showMySolutions() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/solution_view.fxml"));
            Parent root = loader.load();
            
            SolutionController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            controller.setShowUserSolutionsOnly(true);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Mes Solutions");
            stage.setScene(scene);
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening my solutions view", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                     "Impossible d'afficher mes solutions: " + e.getMessage());
        }
    }
    
    /**
     * Opens the user management screen (only for admins/professors)
     */
    @FXML
    private void manageUsers() {
        if (!"Professeur".equals(userRole)) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Permission insuffisante", 
                     "Seuls les professeurs peuvent gérer les utilisateurs.");
            return;
        }
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/user_management.fxml"));
            Parent root = loader.load();
            
            UserManagementController controller = loader.getController();
            controller.setAdminId(userId);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Gestion des utilisateurs");
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening user management view", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                     "Impossible d'afficher la gestion des utilisateurs: " + e.getMessage());
        }
    }
    
    /**
     * Opens the matiere management screen
     */
    @FXML
    private void manageMatieres() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matiere_management.fxml"));
            Parent root = loader.load();
            
            MatiereManagementController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Gestion des matières");
            stage.setScene(scene);
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening matiere management view", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                     "Impossible d'afficher la gestion des matières: " + e.getMessage());
        }
    }
    
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
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error during logout", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Deconnexion impossible", e.getMessage());
        }
    }
    
    private void closeCurrentStage() {
        Stage stage = (Stage) matiereComboBox.getScene().getWindow();
        if (stage != null) {
            stage.close();
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
