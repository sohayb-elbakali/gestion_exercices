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
 * Unified controller for managing matieres (subjects) - includes editor functionality
 */
public class MatiereManagementController {
    private static final Logger LOGGER = Logger.getLogger(MatiereManagementController.class.getName());
    
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
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        configureTableView();
        loadMatieres();
    }
    
    /**
     * Set the user ID
     */
    public void setUserId(int userId) {
        this.userId = userId;
    }
    
    /**
     * Set the user role
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        LOGGER.info("User role set to: " + userRole);
    }
    
    /**
     * Configure the table view columns and cell factories
     */
    private void configureTableView() {
        // Set up column cell value factories
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        
        // Add action buttons for each row
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
        
        // Set items
        matiereTable.setItems(matiereList);
    }
    
    /**
     * Load all matieres from the database
     */
    private void loadMatieres() {
        matiereList.clear();
        matiereList.addAll(matiereDAO.getAllMatieres());
    }
    
    /**
     * View exercices for a matiere
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
            
            // Add listener to update the table when the window is closed
            dialog.setOnHidden(event -> {
                if (dialog.getUserData() instanceof Matiere || Boolean.TRUE.equals(dialog.getUserData())) {
                    loadMatieres();
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
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible", 
                        "Impossible de supprimer cette matière. Elle contient peut-être des exercices.");
            }
        }
    }
    
    /**
     * Open the add matiere form
     */
    @FXML
    private void openAddMatiereForm() {
        openMatiereEditor(null);
    }
    
    /**
     * Return to the main menu
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
    
    /**
     * Close the current stage
     */
    private void closeCurrentStage() {
        Stage stage = (Stage) matiereTable.getScene().getWindow();
        stage.close();
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
     * Refresh the matieres list
     */
    @FXML
    private void refreshMatieres() {
        loadMatieres();
    }
} 
