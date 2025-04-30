package controllers;

import dao.ExerciceDAO;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.geometry.Insets;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Exercice;
import models.Matiere;
import utils.IconHelper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;

/**
 * Controller for exercise management (viewing, adding, editing, and deleting)
 */
public class ExerciceController {
    private static final Logger LOGGER = Logger.getLogger(ExerciceController.class.getName());

    // FXML components
    @FXML private TableView<Exercice> exerciceTable;
    @FXML private TableColumn<Exercice, String> titreColumn;
    @FXML private TableColumn<Exercice, String> matiereColumn;
    @FXML private TableColumn<Exercice, LocalDateTime> dateColumn;
    @FXML private TableColumn<Exercice, Void> actionsColumn;
    @FXML private Label titleLabel;
    @FXML private Button addExerciceButton;
    
    // Form fields for add/edit
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private Button submitButton;
    
    // State variables
    private int userId;
    private String userRole;
    private Matiere matiere;
    private Exercice currentExercice;
    private boolean isEditing = false;
    private boolean showUserExercisesOnly = false;
    
    private final ExerciceDAO exerciceDAO = new ExerciceDAO();
    private final ObservableList<Exercice> exerciceList = FXCollections.observableArrayList();
    
    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        // Initialize table if the controller is being used in table view mode
        if (exerciceTable != null) {
            configureTableView();
            exerciceTable.setItems(exerciceList);
            
            // Add a listener to detect when an exercise is selected
            exerciceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    LOGGER.info("Exercise selected: " + newSelection.getTitre());
                }
            });
        }
        
        // Initialize form fields if the controller is being used in form mode
        if (submitButton != null) {
            submitButton.textProperty().bind(
                javafx.beans.binding.Bindings.when(
                    javafx.beans.binding.Bindings.createBooleanBinding(() -> isEditing)
                ).then("Modifier").otherwise("Ajouter")
            );
        }
    }
    
    /**
     * Configure the table view columns and cell factories
     */
    private void configureTableView() {
        if (exerciceTable == null) return;
        
        // Set up column cell value factories
        if (titreColumn != null) {
            titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        }
        
        // Add the matiere column cell factory
        if (matiereColumn != null) {
            matiereColumn.setCellValueFactory(new PropertyValueFactory<>("matiereNom"));
        }
        
        if (dateColumn != null) {
            dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
            dateColumn.setCellFactory(column -> new TableCell<>() {
                private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                
                @Override
                protected void updateItem(LocalDateTime date, boolean empty) {
                    super.updateItem(date, empty);
                    
                    if (empty || date == null) {
                        setText(null);
                    } else {
                        setText(formatter.format(date));
                    }
                }
            });
        }
        
        // Add action buttons for each row
        if (actionsColumn != null) {
            setUpActionsColumn();
        }
    }
    
    /**
     * Set up the actions column with buttons
     */
    private void setUpActionsColumn() {
        actionsColumn.setCellFactory(param -> {
            return new TableCell<>() {
                private final Button viewButton = new Button("Voir");
                private final Button solutionsButton = new Button("Solutions");
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    // Set up button actions
                    viewButton.setOnAction(event -> showExerciseDetails(getTableRow().getItem()));
                    solutionsButton.setOnAction(event -> openSolutionsView(getTableRow().getItem()));
                    editButton.setOnAction(event -> openExerciseEditor(getTableRow().getItem()));
                    deleteButton.setOnAction(event -> confirmAndDeleteExercise(getTableRow().getItem()));
                    
                    // Apply CSS classes
                    viewButton.getStyleClass().add("button-blue");
                    solutionsButton.getStyleClass().add("button-green");
                    editButton.getStyleClass().add("button-yellow");
                    deleteButton.getStyleClass().add("button-red");
                    
                    // Set minimum width for buttons
                    viewButton.setMinWidth(60);
                    solutionsButton.setMinWidth(80);
                    editButton.setMinWidth(70);
                    deleteButton.setMinWidth(80);
                    
                    // Set max width for all buttons
                    viewButton.setMaxWidth(Double.MAX_VALUE);
                    solutionsButton.setMaxWidth(Double.MAX_VALUE);
                    editButton.setMaxWidth(Double.MAX_VALUE);
                    deleteButton.setMaxWidth(Double.MAX_VALUE);
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty) {
                        setGraphic(null);
                        return;
                    }
                    
                    Exercice exercice = getTableRow().getItem();
                    if (exercice == null) {
                        setGraphic(null);
                        return;
                    }
                    
                    // Create two separate HBoxes for better organization
                    HBox viewButtonsBox = new HBox(5);
                    viewButtonsBox.setAlignment(javafx.geometry.Pos.CENTER);
                    viewButtonsBox.getChildren().addAll(viewButton, solutionsButton);
                    
                    HBox editButtonsBox = new HBox(5);
                    editButtonsBox.setAlignment(javafx.geometry.Pos.CENTER);
                    
                    // Only creator can edit/delete their exercises
                    if (exercice.getCreateurId() == userId) {
                        editButtonsBox.getChildren().addAll(editButton, deleteButton);
                    }
                    // Additionally, professors can edit/delete any exercise
                    else if ("Professeur".equals(userRole)) {
                        editButtonsBox.getChildren().addAll(editButton, deleteButton);
                    }
                    
                    // Combine both HBoxes in a VBox for vertical arrangement
                    javafx.scene.layout.VBox buttonContainer = new javafx.scene.layout.VBox(5);
                    buttonContainer.setAlignment(javafx.geometry.Pos.CENTER);
                    buttonContainer.getChildren().add(viewButtonsBox);
                    
                    if (!editButtonsBox.getChildren().isEmpty()) {
                        buttonContainer.getChildren().add(editButtonsBox);
                    }
                    
                    setGraphic(buttonContainer);
                }
            };
        });
        actionsColumn.getStyleClass().add("actions-column");
    }
    
    /**
     * Set the user ID
     */
    public void setUserId(int userId) {
        this.userId = userId;
        loadExercises();
    }
    
    /**
     * Set the user role
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        LOGGER.info("User role set to: " + userRole);
        
        // Update UI based on role
        updateUIForUserRole();
    }
    
    /**
     * Update UI components based on user role
     */
    private void updateUIForUserRole() {
        if (addExerciceButton != null) {
            // Show the add button for both students and professors now
            addExerciceButton.setVisible(true);
            
            // Update any labels or tooltips to be role-specific
            if (titleLabel != null) {
                if ("Etudiant".equals(userRole) && showUserExercisesOnly) {
                    titleLabel.setText("Mes Exercices");
                } else if ("Professeur".equals(userRole) && showUserExercisesOnly) {
                    titleLabel.setText("Mes Exercices Créés");
                }
            }
        }
    }
    
    /**
     * Set the matière for filtering exercises
     */
    public void setMatiere(Matiere matiere) {
        this.matiere = matiere;
        loadExercises();
    }
    
    /**
     * Set whether to show only user's exercises
     */
    public void setShowUserExercisesOnly(boolean showUserExercisesOnly) {
        this.showUserExercisesOnly = showUserExercisesOnly;
        loadExercises();
        updateUIForUserRole();
    }
    
    /**
     * Load exercises based on the current filter settings
     */
    private void loadExercises() {
        exerciceList.clear();
        
        try {
            List<Exercice> exercises;
            if (showUserExercisesOnly) {
                // Show only user's exercises
                exercises = exerciceDAO.getExercicesByCreateur(userId);
                LOGGER.info("Loaded user's exercises: " + exercises.size());
            } else if (matiere != null) {
                // Show exercises for a specific matière
                exercises = exerciceDAO.getExercicesByMatiere(matiere.getId());
                LOGGER.info("Loaded exercises for matiere " + matiere.getId() + ": " + exercises.size());
            } else {
                // Default to loading all exercises
                exercises = exerciceDAO.getAllExercices();
                LOGGER.info("Loaded all exercises: " + exercises.size());
            }
            
            exerciceList.addAll(exercises);
            
            // Make sure to only set items if the table exists
            if (exerciceTable != null) {
                exerciceTable.setItems(exerciceList);
                exerciceTable.refresh();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error loading exercises", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement", 
                    "Impossible de charger les exercices: " + e.getMessage());
        }
    }
    
    /**
     * Show exercise details in a dialog
     */
    private void showExerciseDetails(Exercice exercice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'exercice");
        
        DialogPane dialogPane = new DialogPane();
        
        Label titreLabel = new Label("Titre: " + exercice.getTitre());
        titreLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label matiereLabel = new Label("Matière: " + exercice.getMatiereNom());
        matiereLabel.setStyle("-fx-font-weight: bold;");
        
        TextArea descriptionArea = new TextArea(exercice.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefHeight(200);
        
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10, 
                                          titreLabel,
                                          matiereLabel,
                                          new Label("Description:"), 
                                          descriptionArea);
        content.setPadding(new javafx.geometry.Insets(10));
        
        dialogPane.setContent(content);
        dialogPane.getButtonTypes().add(ButtonType.OK);
        
        dialog.setDialogPane(dialogPane);
        dialog.showAndWait();
    }
    
    /**
     * Open the exercise editor for a new or existing exercise
     */
    private void openExerciseEditor(Exercice exercice) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exercice_form.fxml"));
            Parent root = loader.load();
            
            ExerciceController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            
            if (exercice != null) {
                // Editing existing exercise
                controller.setupForEditing(exercice);
            } else {
                // Adding new exercise
                // Handle the case where matiere is null (in "My Exercises" view)
                int matiereId = (matiere != null) ? matiere.getId() : 1; // Default to matiere ID 1 if null
                controller.setupForAdding(matiereId);
            }
            
            Stage stage = new Stage();
            stage.setTitle(exercice != null ? "Modifier l'exercice" : "Ajouter un exercice");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            
            // Add a listener to refresh the list when the window is closed
            stage.setOnHidden(event -> {
                if (stage.getUserData() instanceof Exercice) {
                    // If a new exercise was created, add it directly to the list
                    Exercice newExercice = (Exercice) stage.getUserData();
                    exerciceList.add(newExercice);
                    if (exerciceTable != null) {
                        exerciceTable.refresh();
                    }
                } else if (Boolean.TRUE.equals(stage.getUserData())) {
                    // Otherwise just reload all exercises
                    loadExercises();
                }
            });
            
            // Add window close handler for cleanup
            stage.setOnCloseRequest(event -> {
                // For modal dialogs, we don't need to show confirmation
                // Just let it close normally
            });
            
            IconHelper.setStageIcon(stage);
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening exercise editor", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ouverture", 
                    "Impossible d'ouvrir l'éditeur d'exercice: " + e.getMessage());
        }
    }
    
    /**
     * Helper method to check if a window with a specific title already exists
     * 
     * @param windowTitle The title to check for
     * @return The existing Stage if found, or null if not found
     */
    private Stage findExistingWindow(String windowTitle) {
        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
            if (window instanceof Stage && window.isShowing()) {
                Stage stage = (Stage) window;
                if (windowTitle.equals(stage.getTitle())) {
                    return stage;
                }
            }
        }
        return null;
    }
    
    /**
     * Open "My Exercises" view for the current user
     */
    @FXML
    private void openMyExercises() {
        try {
            String windowTitle = "Mes Exercices";
            Stage existingStage = findExistingWindow(windowTitle);
            
            if (existingStage != null) {
                // Window found - bring it to front
                existingStage.toFront();
                LOGGER.info("Reusing existing 'Mes Exercices' window");
            } else {
                // No window exists, create a new one
                LOGGER.info("Creating new 'Mes Exercices' window");
                
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exercice_view.fxml"));
                Parent root = loader.load();
                
                ExerciceController controller = loader.getController();
                controller.setUserId(userId);
                controller.setUserRole(userRole);
                controller.setShowUserExercisesOnly(true);
                
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                
                Stage stage = new Stage();
                stage.setTitle(windowTitle);
                stage.setScene(scene);
                IconHelper.setStageIcon(stage);
                stage.show();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening my exercises", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                    "Impossible d'afficher mes exercices: " + e.getMessage());
        }
    }
    
    /**
     * Open the solutions view for an exercise
     */
    private void openSolutionsView(Exercice exercice) {
        try {
            String windowTitle = "Solutions - " + exercice.getTitre();
            Stage existingStage = findExistingWindow(windowTitle);
            
            if (existingStage != null) {
                // Window found - bring it to front
                existingStage.toFront();
                LOGGER.info("Reusing existing solutions window for exercise: " + exercice.getTitre());
            } else {
                // No window exists, create a new one
                LOGGER.info("Creating new solutions window for exercise: " + exercice.getTitre());
                
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/solution_view.fxml"));
                Parent root = loader.load();

                SolutionController controller = loader.getController();
                controller.setUserId(userId);
                controller.setUserRole(userRole);
                controller.setExerciceId(exercice.getId());

                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                
                Stage stage = new Stage();
                stage.setTitle(windowTitle);
                stage.setScene(scene);
                IconHelper.setStageIcon(stage);
                stage.show();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening solutions view", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                    "Impossible d'afficher les solutions: " + e.getMessage());
        }
    }
    
    /**
     * Confirm and delete an exercise
     */
    private void confirmAndDeleteExercise(Exercice exercice) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'exercice: " + exercice.getTitre());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cet exercice? Cette action ne peut pas être annulée.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = exerciceDAO.deleteExercice(exercice.getId());
            
            if (success) {
                exerciceList.remove(exercice);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Exercice supprimé", 
                        "L'exercice a été supprimé avec succès.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", 
                        "Impossible de supprimer l'exercice: opération échouée.");
            }
        }
    }
    
    /**
     * Setup the form for adding a new exercise
     */
    public void setupForAdding(int matiereId) {
        this.matiere = new Matiere(matiereId, "");
        this.isEditing = false;
        clearForm();
    }
    
    /**
     * Setup the form for editing an existing exercise
     */
    public void setupForEditing(Exercice exercice) {
        this.currentExercice = exercice;
        this.isEditing = true;
        
        if (titreField != null && descriptionField != null) {
            titreField.setText(exercice.getTitre());
            descriptionField.setText(exercice.getDescription());
        }
    }
    
    /**
     * Clear the form fields
     */
    private void clearForm() {
        if (titreField != null && descriptionField != null) {
            titreField.clear();
            descriptionField.clear();
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
            updateExercise();
        } else {
            addExercise();
        }
        
        closeForm();
    }
    
    /**
     * Validate the form inputs
     */
    private boolean validateForm() {
        return titreField != null && !titreField.getText().trim().isEmpty() &&
               descriptionField != null && !descriptionField.getText().trim().isEmpty();
    }
    
    /**
     * Add a new exercise
     */
    private void addExercise() {
        try {
            String titre = titreField.getText().trim();
            String description = descriptionField.getText().trim();
            
            Exercice exercice = new Exercice(0, titre, description, LocalDateTime.now(), 
                                          matiere.getId(), userId);
            
            // Use the DAO method to add and return the created exercise with ID
            Exercice createdExercice = exerciceDAO.addExerciceAndReturn(exercice);
            
            if (createdExercice != null) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Exercice ajouté", 
                        "L'exercice a été ajouté avec succès.");
                
                // Pass the new exercise to the parent window
                Stage currentStage = (Stage) titreField.getScene().getWindow();
                currentStage.setUserData(createdExercice);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ajout", 
                        "Impossible d'ajouter l'exercice: opération échouée.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding exercise", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ajout", 
                    "Impossible d'ajouter l'exercice: " + e.getMessage());
        }
    }
    
    /**
     * Update an existing exercise
     */
    private void updateExercise() {
        try {
            String titre = titreField.getText().trim();
            String description = descriptionField.getText().trim();
            
            currentExercice.setTitre(titre);
            currentExercice.setDescription(description);
            
            boolean success = exerciceDAO.updateExercice(currentExercice);
            
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Exercice modifié", 
                        "L'exercice a été modifié avec succès.");
                
                Stage currentStage = (Stage) titreField.getScene().getWindow();
                currentStage.setUserData(Boolean.TRUE); // Signal that an update was made
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de modification", 
                        "Impossible de modifier l'exercice: opération échouée.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating exercise", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de modification", 
                    "Impossible de modifier l'exercice: " + e.getMessage());
        }
    }
    
    /**
     * Cancel the form
     */
    @FXML
    private void cancelForm() {
        closeForm();
    }
    
    /**
     * Close the form
     */
    private void closeForm() {
        if (titreField != null) {
            Stage stage = (Stage) titreField.getScene().getWindow();
            stage.close();
        }
    }
    
    /**
     * Open form to add a new exercise
     */
    @FXML
    private void openAddExerciseForm() {
        openExerciseEditor(null);
    }
    
    /**
     * Go back to matiere selection
     */
    @FXML
    private void backToMatiereSelection() {
        try {
            // Check if we have a valid window to work with
            if (exerciceTable != null) {
                Stage currentStage = (Stage) exerciceTable.getScene().getWindow();
                
                // Close all other windows EXCEPT the current one
                closeAllOtherWindows(currentStage);
                
                // Instead of just closing, load the matiere view to navigate back
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matiere_view.fxml"));
                Parent root = loader.load();
                
                // Set up the controller
                MatiereController controller = loader.getController();
                controller.setUserId(userId);
                controller.setUserRole(userRole);
                
                // Create a new scene and set it on the current stage
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                
                currentStage.setTitle("Sélection de matière");
                currentStage.setScene(scene);
                
                LOGGER.info("Navigating back to matiere selection view and closing other windows");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error navigating back to matiere selection", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                    "Impossible de retourner à la sélection de matière: " + e.getMessage());
        }
    }
    
    /**
     * Close all other open windows except the specified one
     */
    private void closeAllOtherWindows(Stage exceptStage) {
        // Create a list to hold stages to close to avoid ConcurrentModificationException
        List<Stage> stagesToClose = new ArrayList<>();
        
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
     * Refresh the list of exercises
     */
    @FXML
    private void refreshExercises() {
        loadExercises();
    }
    
    /**
     * Set the exercise ID for the current view
     */
    public void setExerciceId(int exerciceId) {
        try {
            // Fetch the exercise details from the database
            Exercice exercice = exerciceDAO.getExerciceById(exerciceId);
            if (exercice != null) {
                // If we found the exercise, set the matiere ID
                this.matiere = new Matiere(exercice.getMatiereId(), "");
                loadExercises();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error setting exercice ID", e);
        }
    }
}
