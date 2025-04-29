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
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Matiere;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for managing matieres (subjects)
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
            stage.show();
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening exercise view", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                     "Impossible d'afficher les exercices: " + e.getMessage());
        }
    }
    
    /**
     * Open the editor for adding or editing a matiere
     */
    private void openMatiereEditor(Matiere matiere) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matiere_editor.fxml"));
            Parent root = loader.load();
            
            MatiereEditorController controller = loader.getController();
            
            if (matiere != null) {
                // Editing existing matiere
                controller.setupForEditing(matiere);
            } else {
                // Adding new matiere
                controller.setupForAdding();
            }
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            Stage stage = new Stage();
            stage.setTitle(matiere != null ? "Modifier la matière" : "Ajouter une matière");
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            
            // Add listener to update the table when the window is closed
            stage.setOnHidden(event -> {
                if (stage.getUserData() instanceof Matiere) {
                    // A matiere was added or updated
                    loadMatieres();
                } else if (Boolean.TRUE.equals(stage.getUserData())) {
                    // Something was changed but we don't have the object
                    loadMatieres();
                }
            });
            
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening matiere editor", e);
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
