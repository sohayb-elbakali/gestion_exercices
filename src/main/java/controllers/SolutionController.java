package controllers;

import dao.SolutionDAO;
import dao.UtilisateurDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Solution;
import models.Utilisateur;
import utils.IconHelper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for solution management (viewing, adding, editing, and deleting)
 */
public class SolutionController {
    private static final Logger LOGGER = Logger.getLogger(SolutionController.class.getName());

    // FXML components for solution list view
    @FXML private TableView<Solution> solutionTable;
    @FXML private TableColumn<Solution, String> contenuColumn;
    @FXML private TableColumn<Solution, LocalDateTime> dateColumn;
    @FXML private TableColumn<Solution, String> auteurColumn;
    @FXML private TableColumn<Solution, Void> actionsColumn;
    @FXML private Button addSolutionButton;
    
    // FXML components for solution form
    @FXML private TextArea contenuField;
    @FXML private Button submitButton;
    
    // State variables
    private int userId;
    private String userRole;
    private int exerciceId;
    private Solution currentSolution;
    private boolean isEditing = false;
    private boolean showUserSolutionsOnly = false;
    
    private final SolutionDAO solutionDAO = new SolutionDAO();
    private final ObservableList<Solution> solutionList = FXCollections.observableArrayList();

    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        configureTableView();
        
        if (submitButton != null) {
            submitButton.textProperty().bind(
                javafx.beans.binding.Bindings.when(
                    javafx.beans.binding.Bindings.createBooleanBinding(() -> isEditing)
                ).then("Modifier").otherwise("Ajouter")
            );
        }
        
        // Add a listener to handle initialization after the scene is loaded
        if (solutionTable != null) {
            solutionTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    // Scene is now available, we can update the UI permissions
                    updateUIPermissions();
                }
            });
        }
    }
    
    /**
     * Configure the table view columns and cell factories
     */
    private void configureTableView() {
        if (solutionTable == null) return;
        
        // Set up the column cell value factories
        contenuColumn.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        auteurColumn.setCellValueFactory(new PropertyValueFactory<>("auteurNom"));
        
        // Format the date column
        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
        });
        
        // Add the action buttons for each row
        actionsColumn.setCellFactory(param -> createActionButtons());
    }
    
    /**
     * Create action buttons for each table row
     */
    private TableCell<Solution, Void> createActionButtons() {
        return new TableCell<>() {
            private final Button viewButton = new Button("Voir");
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");
            
            {
                // Set up button actions
                viewButton.setOnAction(event -> showSolutionDetails(getTableRow().getItem()));
                editButton.setOnAction(event -> openSolutionEditor(getTableRow().getItem()));
                deleteButton.setOnAction(event -> confirmAndDeleteSolution(getTableRow().getItem()));
                
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
                
                Solution solution = getTableRow().getItem();
                if (solution == null) {
                    setGraphic(null);
                    return;
                }
                
                HBox buttonBox = new HBox(5);
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
                
                // Everyone can view solutions
                buttonBox.getChildren().add(viewButton);
                
                // Only professors or the author can edit/delete
                boolean canModify = "Professeur".equals(userRole) || solution.getAuteurId() == userId;
                if (canModify) {
                    buttonBox.getChildren().addAll(editButton, deleteButton);
                }
                
                setGraphic(buttonBox);
            }
        };
    }
    
    /**
     * Set the user ID
     */
    public void setUserId(int userId) {
        this.userId = userId;
        loadSolutions();
    }
    
    /**
     * Set the user role
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        LOGGER.info("User role set to: " + userRole);
        
        // Hide add solution button for students
        if (addSolutionButton != null && "Étudiant".equals(userRole)) {
            addSolutionButton.setVisible(false);
            addSolutionButton.setManaged(false);
        }
        
        // Also update other UI elements when the scene is loaded
        updateUIPermissions();
    }
    
    /**
     * Set the exercise ID for filtering solutions
     */
    public void setExerciceId(int exerciceId) {
        this.exerciceId = exerciceId;
        loadSolutions();
    }
    
    /**
     * Set whether to show only user's solutions
     */
    public void setShowUserSolutionsOnly(boolean showUserSolutionsOnly) {
        this.showUserSolutionsOnly = showUserSolutionsOnly;
        loadSolutions();
    }
    
    /**
     * Load solutions based on the current filter settings
     */
    private void loadSolutions() {
        solutionList.clear();
        
        try {
            List<Solution> solutions = new ArrayList<>();
            
            if (showUserSolutionsOnly) {
                // Show only user's solutions
                try {
                    solutions = solutionDAO.getSolutionsByCreateur(userId);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error loading solutions by creator, falling back to solutions by author", e);
                    solutions = solutionDAO.getSolutionsByAuteur(userId);
                }
            } else if (exerciceId > 0) {
                // Show solutions for a specific exercise
                solutions = solutionDAO.getSolutionsByExercice(exerciceId);
            }
            
            solutionList.addAll(solutions);
            
            if (solutionTable != null) {
                solutionTable.setItems(solutionList);
                solutionTable.refresh();
            }
            
            LOGGER.info("Loaded " + solutionList.size() + " solutions");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading solutions", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement", 
                    "Impossible de charger les solutions: " + e.getMessage());
        }
    }
    
    /**
     * Show solution details in a dialog
     */
    private void showSolutionDetails(Solution solution) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détails de la solution");
        
        DialogPane dialogPane = new DialogPane();
        
        Label authorLabel = new Label("Auteur: " + solution.getAuteurNom());
        authorLabel.setStyle("-fx-font-weight: bold;");
        
        Label dateLabel = new Label("Date: " + solution.getDateCreation().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        
        TextArea contenuArea = new TextArea(solution.getContenu());
        contenuArea.setEditable(false);
        contenuArea.setWrapText(true);
        contenuArea.setPrefHeight(300);
        
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10, 
                                          authorLabel, 
                                          dateLabel,
                                          new Label("Contenu:"),
                                          contenuArea);
        content.setPadding(new javafx.geometry.Insets(10));
        
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.OK);
        
        dialog.setDialogPane(dialogPane);
        dialog.showAndWait();
    }
    
    /**
     * Open the solution editor for a new or existing solution
     */
    private void openSolutionEditor(Solution solution) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/solution_form.fxml"));
            Parent root = loader.load();
            
            SolutionController controller = loader.getController();
            controller.setUserId(userId);
            
            if (solution != null) {
                // Editing existing solution
                controller.setupForEditing(solution);
            } else {
                // Adding new solution
                controller.setupForAdding(exerciceId);
            }
            
            Stage stage = new Stage();
            stage.setTitle(solution != null ? "Modifier la solution" : "Ajouter une solution");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            IconHelper.setStageIcon(stage);
            
            // Add a listener to refresh the list when the window is closed
            stage.setOnHidden(event -> {
                if (stage.getUserData() instanceof Solution) {
                    // If a new solution was created, add it directly to the list
                    Solution newSolution = (Solution) stage.getUserData();
                    solutionList.add(newSolution);
                    if (solutionTable != null) {
                        solutionTable.refresh();
                    }
                } else if (Boolean.TRUE.equals(stage.getUserData())) {
                    // Otherwise just reload all solutions
                    loadSolutions();
                }
            });
            
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening solution editor", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ouverture", 
                    "Impossible d'ouvrir l'éditeur de solution: " + e.getMessage());
        }
    }
    
    /**
     * Setup the form for adding a new solution
     */
    public void setupForAdding(int exerciceId) {
        this.exerciceId = exerciceId;
        this.isEditing = false;
        clearForm();
    }
    
    /**
     * Setup the form for editing an existing solution
     */
    public void setupForEditing(Solution solution) {
        this.currentSolution = solution;
        this.isEditing = true;
        
        if (contenuField != null) {
            contenuField.setText(solution.getContenu());
        }
    }
    
    /**
     * Clear the form fields
     */
    private void clearForm() {
        if (contenuField != null) {
            contenuField.clear();
        }
    }
    
    /**
     * Handle form submission (add/edit)
     */
    @FXML
    private void handleSubmit() {
        if (!validateForm()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Formulaire incomplet", 
                    "Veuillez remplir tous les champs obligatoires.");
            return;
        }
        
        if (isEditing) {
            updateSolution();
        } else {
            addSolution();
        }
        
        closeForm();
    }
    
    /**
     * Validate the form inputs
     */
    private boolean validateForm() {
        return contenuField != null && !contenuField.getText().trim().isEmpty();
    }
    
    /**
     * Add a new solution
     */
    private void addSolution() {
        try {
            String contenu = contenuField.getText().trim();
            
            Solution solution = new Solution();
            solution.setContenu(contenu);
            solution.setDateCreation(LocalDateTime.now());
            solution.setExerciceId(exerciceId);
            solution.setAuteurId(userId);
            
            // Try to get user name from database for better display
            try {
                String authorName = getUserName(userId);
                solution.setAuteurNom(authorName != null ? authorName : "Utilisateur " + userId);
            } catch (Exception e) {
                solution.setAuteurNom("Utilisateur " + userId);
            }
            
            Solution createdSolution = solutionDAO.addSolutionAndReturn(solution);
            
            if (createdSolution != null) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Solution ajoutée", 
                        "La solution a été ajoutée avec succès.");
                
                // Pass the newly created solution back to the parent window
                Stage currentStage = (Stage) contenuField.getScene().getWindow();
                currentStage.setUserData(createdSolution);
                
                // Refresh solution list immediately
                if (solutionList != null) {
                    loadSolutions();
                }
            } else {
                // Fall back to old method if the new one fails
                boolean success = solutionDAO.addSolution(solution);
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Solution ajoutée", 
                            "La solution a été ajoutée avec succès.");
                    
                    // Mark that an item was added
                    Stage currentStage = (Stage) contenuField.getScene().getWindow();
                    currentStage.setUserData(Boolean.TRUE);
                    
                    // Refresh solution list immediately
                    if (solutionList != null) {
                        loadSolutions();
                    }
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ajout", 
                            "Impossible d'ajouter la solution: opération échouée.");
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding solution", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ajout", 
                    "Impossible d'ajouter la solution: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to get user name
     */
    private String getUserName(int userId) {
        try {
            UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
            Utilisateur user = utilisateurDAO.getById(userId);
            return user != null ? user.getNom() : null;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error getting user name", e);
            return null;
        }
    }
    
    /**
     * Update an existing solution
     */
    private void updateSolution() {
        try {
            String contenu = contenuField.getText().trim();
            
            currentSolution.setContenu(contenu);
            currentSolution.setDateCreation(LocalDateTime.now());
            
            boolean success = solutionDAO.updateSolution(currentSolution);
            
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Solution modifiée", 
                        "La solution a été modifiée avec succès.");
                
                // Pass the updated solution back to the parent
                Stage currentStage = (Stage) contenuField.getScene().getWindow();
                currentStage.setUserData(currentSolution);
                
                // Refresh solution list immediately
                if (solutionList != null) {
                    loadSolutions();
                }
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de modification", 
                        "Impossible de modifier la solution: opération échouée.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating solution", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de modification", 
                    "Impossible de modifier la solution: " + e.getMessage());
        }
    }
    
    /**
     * Confirm and delete a solution
     */
    private void confirmAndDeleteSolution(Solution solution) {
        if (solution == null) {
            LOGGER.warning("Attempted to delete a null solution");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer cette solution ?");
        confirmation.setContentText("Cette action est irréversible.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = solutionDAO.deleteSolution(solution.getId());
                
                if (success) {
                    // Remove the solution from the list directly
                    solutionList.remove(solution);
                    if (solutionTable != null) {
                        solutionTable.refresh();
                    }
                    
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Solution supprimée", 
                            "La solution a été supprimée avec succès.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", 
                            "Impossible de supprimer la solution: opération échouée.");
                    // If delete failed in database but UI already removed it, reload to restore
                    loadSolutions();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error deleting solution", e);
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", 
                        "Impossible de supprimer la solution: " + e.getMessage());
                loadSolutions(); // Reload to ensure UI consistency
            }
        }
    }
    
    /**
     * Open the form to add a new solution
     */
    @FXML
    private void openAddSolutionForm() {
        // Only professors can add solutions
        if ("Étudiant".equals(userRole)) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Permission refusée", 
                    "Seuls les professeurs peuvent ajouter des solutions.");
            return;
        }
        
        // Additional check for exercice id
        if (exerciceId <= 0) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Information incomplète", 
                    "Veuillez sélectionner un exercice spécifique avant d'ajouter une solution.");
            return;
        }
        
        openSolutionEditor(null);
    }
    
    /**
     * Return to the previous screen
     */
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exercice_view.fxml"));
            Parent root = loader.load();
            
            ExerciceController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            
            if (exerciceId > 0) {
                // If we have an exercise ID, we should return to that exercise's view
                // You'll need to get the matiere ID for this exercise
                controller.setExerciceId(exerciceId);
            } else if (showUserSolutionsOnly) {
                controller.setShowUserExercisesOnly(true);
            }
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle("Exercices");
            stage.setScene(scene);
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error returning to exercise view", e);
            closeCurrentStage();
        }
    }
    
    /**
     * Cancel form editing
     */
    @FXML
    private void handleCancel() {
        closeForm();
    }
    
    /**
     * Close the current form
     */
    private void closeForm() {
        if (contenuField != null) {
            Stage stage = (Stage) contenuField.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
    
    /**
     * Close the current stage
     */
    private void closeCurrentStage() {
        if (solutionTable != null) {
            Stage stage = (Stage) solutionTable.getScene().getWindow();
            if (stage != null) {
                stage.close();
            }
        }
    }
    
    /**
     * Show an alert dialog
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
     * Refresh the solutions list
     */
    @FXML
    private void refreshSolutions() {
        loadSolutions();
    }

    /**
     * After setting user role, update UI elements visibility
     */
    private void updateUIPermissions() {
        // Only proceed if we're a student - professors can see everything
        if (!"Étudiant".equals(userRole)) {
            return;
        }
        
        // First check if we have direct reference to the add button
        if (addSolutionButton != null) {
            addSolutionButton.setVisible(false);
            addSolutionButton.setManaged(false);
            return;
        }
        
        // Find the Add Solution button and hide it for students (fallback method)
        if (solutionTable != null && solutionTable.getScene() != null) {
            // Look for the button in the top VBox
            BorderPane root = (BorderPane) solutionTable.getScene().getRoot();
            if (root.getTop() instanceof VBox) {
                VBox topBox = (VBox) root.getTop();
                for (javafx.scene.Node node : topBox.getChildren()) {
                    if (node instanceof HBox) {
                        HBox hbox = (HBox) node;
                        for (javafx.scene.Node hboxChild : hbox.getChildren()) {
                            if (hboxChild instanceof Button) {
                                Button btn = (Button) hboxChild;
                                if ("Ajouter une solution".equals(btn.getText())) {
                                    btn.setVisible(false);
                                    btn.setManaged(false);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 
