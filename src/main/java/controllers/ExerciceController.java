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
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Exercice;
import models.Matiere;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for exercise management (viewing, adding, editing, and deleting)
 */
public class ExerciceController {
    private static final Logger LOGGER = Logger.getLogger(ExerciceController.class.getName());

    // FXML components
    @FXML private TableView<Exercice> exerciceTable;
    @FXML private TableColumn<Exercice, String> titreColumn;
    @FXML private TableColumn<Exercice, LocalDateTime> dateColumn;
    @FXML private TableColumn<Exercice, Void> actionsColumn;
    
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
        configureTableView();
        
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
        
        // Set up the column cell value factories
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        
        // Format the date column
        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(item));
            }
        });
        
        // Add the action buttons for each row
        actionsColumn.setCellFactory(param -> createActionButtons());
    }
    
    /**
     * Create action buttons for each table row
     */
    private TableCell<Exercice, Void> createActionButtons() {
        return new TableCell<>() {
            private final Button viewButton = new Button("Détails");
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");
            private final Button solutionsButton = new Button("Solutions");
            
            {
                // Set up button actions
                viewButton.setOnAction(event -> showExerciseDetails(getTableRow().getItem()));
                editButton.setOnAction(event -> openExerciseEditor(getTableRow().getItem()));
                deleteButton.setOnAction(event -> confirmAndDeleteExercise(getTableRow().getItem()));
                solutionsButton.setOnAction(event -> viewSolutions(getTableRow().getItem()));
                
                // Apply CSS classes
                viewButton.getStyleClass().add("button-blue");
                editButton.getStyleClass().add("button-yellow");
                deleteButton.getStyleClass().add("button-red");
                solutionsButton.getStyleClass().add("button-green");
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
                
                HBox buttonBox = new HBox(5);
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
                
                // Everyone can view exercises and solutions
                buttonBox.getChildren().addAll(viewButton, solutionsButton);
                
                // Only creator can edit/delete their exercises
                if (exercice.getCreateurId() == userId) {
                    buttonBox.getChildren().addAll(editButton, deleteButton);
                }
                // Additionally, professors can edit/delete any exercise
                else if ("Professeur".equals(userRole)) {
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
        loadExercises();
    }
    
    /**
     * Set the user role
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        LOGGER.info("User role set to: " + userRole);
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
    }
    
    /**
     * Load exercises based on the current filter settings
     */
    private void loadExercises() {
        exerciceList.clear();
        
        try {
            if (showUserExercisesOnly) {
                // Show only user's exercises
                exerciceList.addAll(exerciceDAO.getExercicesByCreateur(userId));
            } else if (matiere != null) {
                // Show exercises for a specific matière
                exerciceList.addAll(exerciceDAO.getExercicesByMatiere(matiere.getId()));
            }
            
            exerciceTable.setItems(exerciceList);
            
            LOGGER.info("Loaded " + exerciceList.size() + " exercises");
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
        
        TextArea descriptionArea = new TextArea(exercice.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefHeight(200);
        
        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10, 
                                          titreLabel, 
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
            
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening exercise editor", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ouverture", 
                    "Impossible d'ouvrir l'éditeur d'exercice: " + e.getMessage());
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
            
            boolean success = exerciceDAO.addExercice(exercice);
            
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Exercice ajouté", 
                        "L'exercice a été ajouté avec succès.");
                
                // Get the exercise with its ID from the database
                List<Exercice> exercices = matiere != null ? 
                    exerciceDAO.getExercicesByMatiere(matiere.getId()) :
                    exerciceDAO.getExercicesByCreateur(userId);
                
                if (exercices != null && !exercices.isEmpty()) {
                    // Find the newly added exercise
                    for (Exercice e : exercices) {
                        if (e.getTitre().equals(titre) && e.getDescription().equals(description) && 
                            e.getCreateurId() == userId) {
                            Stage currentStage = (Stage) titreField.getScene().getWindow();
                            currentStage.setUserData(e); // Pass the new exercise to the parent window
                            break;
                        }
                    }
                }
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
                
                // Pass the updated exercise back to the parent
                Stage currentStage = (Stage) titreField.getScene().getWindow();
                currentStage.setUserData(currentExercice);
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
     * Confirm and delete an exercise
     */
    private void confirmAndDeleteExercise(Exercice exercice) {
        if (exercice == null) {
            LOGGER.warning("Attempted to delete a null exercise");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer cet exercice ?");
        confirmation.setContentText("Cette action est irréversible. Toutes les solutions associées seront également supprimées.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = exerciceDAO.deleteExercice(exercice.getId());
                
                if (success) {
                    // Remove the exercise from the list directly
                    exerciceList.remove(exercice);
                    if (exerciceTable != null) {
                        exerciceTable.refresh();
                    }
                    
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Exercice supprimé", 
                            "L'exercice a été supprimé avec succès.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", 
                            "Impossible de supprimer l'exercice: opération échouée.");
                    // If delete failed in database but UI already removed it, reload to restore
                    loadExercises();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error deleting exercise", e);
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", 
                        "Impossible de supprimer l'exercice: " + e.getMessage());
                loadExercises(); // Reload to ensure UI consistency
            }
        }
    }
    
    /**
     * View solutions for an exercise
     */
    private void viewSolutions(Exercice exercice) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/solution_view.fxml"));
            Parent root = loader.load();

            SolutionController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            controller.setExerciceId(exercice.getId());
                
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                
            Stage stage = new Stage();
            stage.setTitle("Solutions pour " + exercice.getTitre());
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening solutions view", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ouverture", 
                    "Impossible d'afficher les solutions: " + e.getMessage());
        }
    }
    
    /**
     * Open the form to add a new exercise
     */
    @FXML
    private void openAddExerciseForm() {
        // Now both students and professors can add exercises
        if (this.matiere == null && !showUserExercisesOnly) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Matière non définie", 
                    "Veuillez sélectionner une matière avant d'ajouter un exercice.");
            return;
        }
        openExerciseEditor(null);
    }
    
    /**
     * Return to the matière selection screen
     */
    @FXML
    private void backToMatiereSelection() {
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
            stage.show();
            
            closeCurrentStage();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error returning to matiere selection", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                    "Impossible de revenir à la sélection de matière: " + e.getMessage());
        }
    }
    
    /**
     * Cancel form editing
     */
    @FXML
    private void cancelForm() {
        closeForm();
    }
    
    /**
     * Close the current form
     */
    private void closeForm() {
        if (titreField != null) {
            Stage stage = (Stage) titreField.getScene().getWindow();
            stage.close();
        }
    }
    
    /**
     * Close the current stage
     */
    private void closeCurrentStage() {
        if (exerciceTable != null) {
            Stage stage = (Stage) exerciceTable.getScene().getWindow();
            stage.close();
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
        alert.showAndWait();
    }
    
    /**
     * Refresh the exercises list
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
