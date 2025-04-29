package controllers;

import dao.UtilisateurDAO;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Utilisateur;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for user management (admin only)
 */
public class UserManagementController {
    private static final Logger LOGGER = Logger.getLogger(UserManagementController.class.getName());
    
    @FXML private TableView<Utilisateur> userTable;
    @FXML private TableColumn<Utilisateur, Integer> idColumn;
    @FXML private TableColumn<Utilisateur, String> nomColumn;
    @FXML private TableColumn<Utilisateur, String> emailColumn;
    @FXML private TableColumn<Utilisateur, String> roleColumn;
    @FXML private TableColumn<Utilisateur, Void> actionsColumn;
    @FXML private Label statusLabel;
    
    private int adminId;
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final ObservableList<Utilisateur> userList = FXCollections.observableArrayList();
    
    public void setAdminId(int adminId) {
        this.adminId = adminId;
        LOGGER.info("Admin ID set to: " + adminId);
        loadUsers();
    }
    
    @FXML
    public void initialize() {
        configureTableView();
        statusLabel.setText("");
    }
    
    /**
     * Configure the table view columns and cell factories
     */
    private void configureTableView() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("role"));
        
        // Add action buttons
        actionsColumn.setCellFactory(param -> {
            return new TableCell<>() {
                private final Button viewButton = new Button("Voir");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    viewButton.getStyleClass().add("button-blue");
                    deleteButton.getStyleClass().add("button-red");
                    
                    viewButton.setOnAction(event -> showUserDetails(getTableRow().getItem()));
                    deleteButton.setOnAction(event -> confirmAndDeleteUser(getTableRow().getItem()));
                }
                
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty) {
                        setGraphic(null);
                        return;
                    }
                    
                    Utilisateur user = getTableRow().getItem();
                    if (user == null) {
                        setGraphic(null);
                        return;
                    }
                    
                    // Don't allow admin to delete themselves
                    if (user.getId() == adminId) {
                        setGraphic(new HBox(10, viewButton));
                    } else {
                        setGraphic(new HBox(10, viewButton, deleteButton));
                    }
                }
            };
        });
    }
    
    /**
     * Load all users from the database
     */
    private void loadUsers() {
        userList.clear();
        userList.addAll(utilisateurDAO.getAllUsers());
        userTable.setItems(userList);
    }
    
    /**
     * Show user details in a dialog
     */
    private void showUserDetails(Utilisateur user) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'utilisateur");
        
        DialogPane dialogPane = dialog.getDialogPane();
        dialogPane.getButtonTypes().add(ButtonType.OK);
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("ID: " + user.getId()),
            new Label("Nom: " + user.getNom()),
            new Label("Email: " + user.getEmail()),
            new Label("Rôle: " + user.getRole())
        );
        
        dialogPane.setContent(content);
        dialog.showAndWait();
    }
    
    /**
     * Confirm and delete a user
     */
    private void confirmAndDeleteUser(Utilisateur user) {
        if (user.getId() == adminId) {
            showStatus("Vous ne pouvez pas supprimer votre propre compte.", true);
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer cet utilisateur ?");
        confirmation.setContentText("Cette action est irréversible.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                boolean success = utilisateurDAO.deleteUser(user.getId());
                
                if (success) {
                    userList.remove(user);
                    showStatus("Utilisateur supprimé avec succès.", false);
                } else {
                    showStatus("Erreur lors de la suppression de l'utilisateur.", true);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error deleting user", e);
                showStatus("Erreur: " + e.getMessage(), true);
            }
        }
    }
    
    /**
     * Open the form to add a new user
     */
    @FXML
    private void openAddUserForm() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/register_view.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Ajouter un utilisateur");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            
            stage.setOnHidden(event -> {
                if (Boolean.TRUE.equals(stage.getUserData())) {
                    loadUsers();
                    showStatus("Utilisateur ajouté avec succès.", false);
                }
            });
            
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error opening add user form", e);
            showStatus("Erreur lors de l'ouverture du formulaire: " + e.getMessage(), true);
        }
    }
    
    /**
     * Return to the previous screen
     */
    @FXML
    private void handleBack() {
        Stage stage = (Stage) userTable.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
    
    /**
     * Show a status message
     */
    private void showStatus(String message, boolean isError) {
        statusLabel.setText(message);
        statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green"));
    }
} 
