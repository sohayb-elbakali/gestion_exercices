package controllers;

import dao.MatiereDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.GridPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Matiere;
import utils.IconHelper;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Unified controller for subject (matiere) selection and management
 * This controller consolidates functionality from MatiereManagementController
 */
public class MatiereController {
    private static final Logger LOGGER = Logger.getLogger(MatiereController.class.getName());
    
    // FXML components for matiere selection view
    @FXML private ComboBox<Matiere> matiereComboBox;
    @FXML private Button mySolutionsButton;
    @FXML private Button manageUsersButton;
    
    // FXML components for matiere management view
    @FXML private TableView<Matiere> matiereTable;
    @FXML private TableColumn<Matiere, Integer> idColumn;
    @FXML private TableColumn<Matiere, String> nomColumn;
    @FXML private TableColumn<Matiere, Void> actionsColumn;
    @FXML private Label statusLabel;
    
    private int userId;
    private String userRole;
    private final MatiereDAO matiereDAO = new MatiereDAO();
    private final ObservableList<Matiere> matiereList = FXCollections.observableArrayList();
    
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
        // Initialize the ComboBox if we're in selection view
        if (matiereComboBox != null) {
            loadMatieres();
            
            // Add a listener to update UI when the scene is available
            matiereComboBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && userRole != null) {
                    updateUIForRole();
                }
            });
        }
        
        // Initialize the TableView if we're in management view
        if (matiereTable != null) {
            configureTableView();
            loadAllMatieres();
        }
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
     * Loads the available subjects from the database into the ComboBox
     */
    private void loadMatieres() {
        try {
            if (matiereComboBox != null) {
                matiereComboBox.getItems().clear();
                matiereComboBox.getItems().addAll(matiereDAO.getAllMatieres());
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading matieres", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement", 
                     "Impossible de charger les matieres: " + e.getMessage());
        }
    }
    
    /**
     * Configure the table view columns and cell factories for management view
     */
    private void configureTableView() {
        // Set up column cell value factories
        if (idColumn != null) {
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        }
        
        if (nomColumn != null) {
            nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        }
        
        // Add action buttons for each row
        if (actionsColumn != null) {
            actionsColumn.setCellFactory(param -> {
                return new TableCell<>() {
                    private final Button viewButton = new Button("Exercices");
                    private final Button editButton = new Button("Modifier");
                    private final Button deleteButton = new Button("Supprimer");
                    
                    {
                        // Set up button actions
                        viewButton.setOnAction(event -> viewExercices(getTableRow().getItem()));
                        editButton.setOnAction(event -> openMatiereEditor(getTableRow().getItem()));
                        deleteButton.setOnAction(event -> confirmAndDeleteMatiere(getTableRow().getItem()));
                        
                        // Apply CSS classes
                        viewButton.getStyleClass().add("button-blue");
                        editButton.getStyleClass().add("button-yellow");
                        deleteButton.getStyleClass().add("button-red");
                    }
                    
                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        
                        if (empty) {
                            setGraphic(null);
                            return;
                        }
                        
                        Matiere matiere = getTableRow().getItem();
                        if (matiere == null) {
                            setGraphic(null);
                            return;
                        }
                        
                        HBox buttonBox = new HBox(5);
                        buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
                        
                        // Always show view button
                        buttonBox.getChildren().add(viewButton);
                        
                        // Only professors can edit/delete subjects
                        if ("Professeur".equals(userRole)) {
                            buttonBox.getChildren().addAll(editButton, deleteButton);
                        }
                        
                        setGraphic(buttonBox);
                    }
                };
            });
        }
        
        // Set items if we have a table
        if (matiereTable != null) {
            matiereTable.setItems(matiereList);
        }
    }
    
    /**
     * Load all matieres from the database for the management view
     */
    private void loadAllMatieres() {
        matiereList.clear();
        matiereList.addAll(matiereDAO.getAllMatieres());
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
     * View exercices for a matiere from the management view
     */
    private void viewExercices(Matiere matiere) {
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
            IconHelper.setStageIcon(stage);
            stage.show();
            
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
     * Open the editor for adding or editing a matiere
     * Now creates a dialog directly instead of loading a separate FXML
     */
    private void openMatiereEditor(Matiere matiere) {
        try {
            // Create dialog window
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(matiere != null ? "Modifier la matière" : "Ajouter une matière");
            
            // Create editor form
            VBox dialogRoot = new VBox(15);
            dialogRoot.setPadding(new Insets(20));
            dialogRoot.setAlignment(Pos.CENTER);
            
            Label titleLabel = new Label(matiere != null ? "Modifier la matière" : "Ajouter une matière");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            
            GridPane formGrid = new GridPane();
            formGrid.setHgap(10);
            formGrid.setVgap(10);
            formGrid.setAlignment(Pos.CENTER);
            
            Label nomLabel = new Label("Nom de la matière:");
            TextField nomField = new TextField();
            nomField.setPromptText("Entrez le nom de la matière");
            if (matiere != null) {
                nomField.setText(matiere.getNom());
            }
            
            formGrid.add(nomLabel, 0, 0);
            formGrid.add(nomField, 1, 0);
            
            Label editorStatusLabel = new Label("");
            
            HBox buttonBox = new HBox(10);
            buttonBox.setAlignment(Pos.CENTER_RIGHT);
            
            Button saveButton = new Button(matiere != null ? "Modifier" : "Ajouter");
            saveButton.getStyleClass().add("button-green");
            Button cancelButton = new Button("Annuler");
            cancelButton.getStyleClass().add("button-red");
            
            buttonBox.getChildren().addAll(saveButton, cancelButton);
            
            dialogRoot.getChildren().addAll(titleLabel, formGrid, editorStatusLabel, buttonBox);
            
            // Add event handlers
            final boolean isEditing = matiere != null;
            
            saveButton.setOnAction(event -> {
                if (nomField.getText().trim().isEmpty()) {
                    editorStatusLabel.setText("Le nom de la matière est obligatoire.");
                    editorStatusLabel.setStyle("-fx-text-fill: red;");
                    return;
                }
                
                String nom = nomField.getText().trim();
                
                // Check for existing matiere with same name
                if ((!isEditing || (isEditing && !matiere.getNom().equals(nom))) && 
                     matiereDAO.matiereExists(nom)) {
                    editorStatusLabel.setText("Une matière avec ce nom existe déjà.");
                    editorStatusLabel.setStyle("-fx-text-fill: red;");
                    return;
                }
                
                try {
                    boolean success;
                    
                    if (isEditing) {
                        // Update existing matiere
                        matiere.setNom(nom);
                        success = matiereDAO.updateMatiere(matiere);
                        if (success) {
                            dialog.setUserData(matiere);
                        }
                    } else {
                        // Add new matiere
                        Matiere newMatiere = new Matiere(nom);
                        Matiere createdMatiere = matiereDAO.addMatiereAndReturn(newMatiere);
                        
                        if (createdMatiere != null) {
                            dialog.setUserData(createdMatiere);
                            success = true;
                        } else {
                            success = matiereDAO.addMatiere(newMatiere);
                            if (success) {
                                dialog.setUserData(Boolean.TRUE);
                            }
                        }
                    }
                    
                    if (success) {
                        editorStatusLabel.setText(isEditing ? "Matière modifiée avec succès!" : "Matière ajoutée avec succès!");
                        editorStatusLabel.setStyle("-fx-text-fill: green;");
                        
                        // Close dialog after short delay
                        javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                        pause.setOnFinished(e -> dialog.close());
                        pause.play();
                    } else {
                        editorStatusLabel.setText("Erreur lors de l'opération.");
                        editorStatusLabel.setStyle("-fx-text-fill: red;");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Error in matiere editor", e);
                    editorStatusLabel.setText("Erreur: " + e.getMessage());
                    editorStatusLabel.setStyle("-fx-text-fill: red;");
                }
            });
            
            cancelButton.setOnAction(event -> dialog.close());
            
            // Show dialog
            Scene dialogScene = new Scene(dialogRoot);
            dialogScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialog.setScene(dialogScene);
            dialog.setMinWidth(400);
            dialog.setMinHeight(250);
            IconHelper.setStageIcon(dialog);
            
            // Add listener to update the table when the window is closed
            dialog.setOnHidden(event -> {
                if (dialog.getUserData() instanceof Matiere || Boolean.TRUE.equals(dialog.getUserData())) {
                    loadAllMatieres();
                    loadMatieres(); // Also refresh combo box if present
                }
            });
            
            dialog.showAndWait();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error creating matiere editor dialog", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ouverture", 
                    "Impossible d'ouvrir l'éditeur de matière: " + e.getMessage());
        }
    }
    
    /**
     * Confirm and delete a matiere
     */
    private void confirmAndDeleteMatiere(Matiere matiere) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer cette matière ?");
        confirmation.setContentText("Cette action est irréversible. Si des exercices sont liés à cette matière, la suppression sera impossible.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = matiereDAO.deleteMatiere(matiere.getId());
            
            if (success) {
                // Remove the matiere from the list
                matiereList.remove(matiere);
                showStatus("Matière supprimée avec succès.", false);
                
                // Also refresh the combo box if present
                loadMatieres();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible", 
                        "Impossible de supprimer cette matière. Elle contient peut-être des exercices.");
            }
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
            
            // This function will be removed in future as we're unifying controllers
            MatiereController controller = loader.getController();
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
    
    /**
     * Open form to add a new matiere from management view
     */
    @FXML
    private void openAddMatiereForm() {
        openMatiereEditor(null);
    }
    
    /**
     * Return to the main menu from management view
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matiere_view.fxml"));
            Parent root = loader.load();
            
            MatiereController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Sélection de matière");
            stage.setScene(scene);
            IconHelper.setStageIcon(stage);
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error returning to matiere selection", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                    "Impossible de revenir à la sélection de matière: " + e.getMessage());
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
        if (matiereComboBox != null) {
            Stage stage = (Stage) matiereComboBox.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        } else if (matiereTable != null) {
            Stage stage = (Stage) matiereTable.getScene().getWindow();
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
    
    /**
     * Show a status message 
     */
    private void showStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green"));
        }
    }
    
    /**
     * Refresh the matieres list
     */
    @FXML
    private void refreshMatieres() {
        loadAllMatieres();
        loadMatieres();
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
