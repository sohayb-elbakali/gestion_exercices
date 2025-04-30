package controllers;

import dao.MatiereDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;
import models.Matiere;
import utils.IconHelper;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified controller for subject (matiere) selection and navigation
 */
public class MatiereController {
    private static final Logger LOGGER = Logger.getLogger(MatiereController.class.getName());
    
    @FXML private ComboBox<Matiere> matiereComboBox;
    @FXML private Button mySolutionsButton;
    @FXML private Button manageUsersButton;
    
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
     * Sets the user role for the current logged-in user and updates UI accordingly
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        LOGGER.info("User role set to: " + userRole);
        
        // Try to update UI if JavaFX components are ready
        if (matiereComboBox != null && matiereComboBox.getScene() != null) {
            updateUIForRole();
        }
    }

    @FXML
    public void initialize() {
        loadMatieres();
        
        // Add a listener to update UI when the scene is available
        matiereComboBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null && userRole != null) {
                updateUIForRole();
            }
        });
    }
    
    /**
     * Configure UI elements based on user role
     */
    public void updateUIForRole() {
        if (userRole != null) {
            if (!"Professeur".equals(userRole)) {
                // Student role - hide certain buttons
                if (mySolutionsButton != null) {
                    mySolutionsButton.setVisible(false);
                    mySolutionsButton.setManaged(false);
                    LOGGER.info("Hiding 'Mes Solutions' button for students");
                }
                
                if (manageUsersButton != null) {
                    manageUsersButton.setVisible(false);
                    manageUsersButton.setManaged(false);
                    LOGGER.info("Hiding 'Gérer les utilisateurs' button for students");
                }
            } else {
                // Professor role - ensure buttons are visible
                if (mySolutionsButton != null) {
                    mySolutionsButton.setVisible(true);
                    mySolutionsButton.setManaged(true);
                }
                
                if (manageUsersButton != null) {
                    manageUsersButton.setVisible(true);
                    manageUsersButton.setManaged(true);
                }
            }
        }
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
        openSelectedMatiere();
    }
    
    /**
     * Open the selected matiere and close other windows
     */
    private void openSelectedMatiere() {
        Matiere matiere = matiereComboBox.getValue();
        
        if (matiere == null) {
            showAlert(Alert.AlertType.WARNING, "Selection", "Aucune matiere selectionnee", 
                     "Veuillez selectionner une matiere.");
            return;
        }
        
        try {
            // Get the current stage before we create a new one
            Stage currentStage = (Stage) matiereComboBox.getScene().getWindow();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exercice_view.fxml"));
            Parent root = loader.load();
            
            ExerciceController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            controller.setMatiere(matiere);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Close all other windows except the current one
            closeAllOtherWindows(currentStage);
            
            // Change the scene of the current stage instead of creating a new one
            currentStage.setTitle("Exercices - " + matiere.getNom());
            currentStage.setScene(scene);
            IconHelper.setStageIcon(currentStage);
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
            // Get the current stage
            Stage currentStage = (Stage) matiereComboBox.getScene().getWindow();
            
            // Close all other windows
            closeAllOtherWindows(currentStage);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exercice_view.fxml"));
            Parent root = loader.load();
            
            ExerciceController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            controller.setShowUserExercisesOnly(true);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Change the scene of the current stage instead of creating a new one
            currentStage.setTitle("Mes Exercices");
            currentStage.setScene(scene);
            IconHelper.setStageIcon(currentStage);
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
        // Only professors can access solutions
        if (!"Professeur".equals(userRole)) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Permission insuffisante", 
                     "Seuls les professeurs peuvent accéder aux solutions.");
            return;
        }
        
        try {
            // Get the current stage
            Stage currentStage = (Stage) matiereComboBox.getScene().getWindow();
            
            // Close all other windows
            closeAllOtherWindows(currentStage);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/solution_view.fxml"));
            Parent root = loader.load();
            
            SolutionController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            controller.setShowUserSolutionsOnly(true);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Change the scene of the current stage instead of creating a new one
            currentStage.setTitle("Mes Solutions");
            currentStage.setScene(scene);
            IconHelper.setStageIcon(currentStage);
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
            IconHelper.setStageIcon(stage);
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
            IconHelper.setStageIcon(stage);
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
            // Get the current stage
            Stage currentStage = (Stage) matiereComboBox.getScene().getWindow();
            
            // Close all other windows
            closeAllOtherWindows(currentStage);
            
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
            showAlert(Alert.AlertType.ERROR, "Erreur", "Déconnexion impossible", 
                     "Impossible de se déconnecter: " + e.getMessage());
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
        IconHelper.setDialogIcon(alert);
        alert.showAndWait();
    }
    
    /**
     * Alternative method name for choix_matiere.fxml
     */
    @FXML
    private void ouvrirMesExercices() {
        showMyExercises();
    }
    
    /**
     * Alternative method name for choix_matiere.fxml
     */
    @FXML
    private void ouvrirMesSolutions() {
        showMySolutions();
    }
    
    /**
     * Return to the login screen
     */
    @FXML
    private void handleRetour() {
        handleLogout();
    }
    
    /**
     * Close all other open windows except the specified one
     */
    private void closeAllOtherWindows(Stage exceptStage) {
        // Create a list to hold stages to close to avoid ConcurrentModificationException
        java.util.List<Stage> stagesToClose = new java.util.ArrayList<>();
        
        // Find all open windows
        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
            if (window instanceof Stage && window.isShowing() && window != exceptStage) {
                stagesToClose.add((Stage) window);
            }
        }
        
        // Close each stage
        for (Stage stage : stagesToClose) {
            LOGGER.info("Closing window: " + stage.getTitle());
            stage.close();
        }
        
        LOGGER.info("Closed " + stagesToClose.size() + " additional windows");
    }
}
