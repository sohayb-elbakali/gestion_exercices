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
 * Contrôleur pour la gestion des matières :
 *  • Affichage de la liste des matières
 *  • Création, modification et suppression de matières
 */
public class MatiereController {
    private static final Logger LOGGER = Logger.getLogger(MatiereController.class.getName());
    
    // Composants FXML pour la vue de sélection de matière
    @FXML private ComboBox<Matiere> matiereComboBox;
    @FXML private Button mySolutionsButton;
    @FXML private Button manageUsersButton;
    
    // Composants FXML pour la vue de gestion des matières
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
     * Définit l'identifiant de l'utilisateur actuellement connecté.
     */
    public void setUserId(int userId) {
        this.userId = userId;
        LOGGER.info("User ID set to: " + userId);
    }

    /**
     * Définit le rôle de l'utilisateur et met à jour l'interface.
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        LOGGER.info("User role set to: " + userRole);
        
        // Mise à jour de l'UI si le ComboBox est déjà initialisé
        if (matiereComboBox != null && matiereComboBox.getScene() != null) {
            updateUIForRole();
        }
    }

    @FXML
    /**
     * Initialise le contrôleur.
     * Configure le ComboBox pour la sélection et le TableView pour la gestion.
     */
    public void initialize() {
        // Initialisation de la vue de sélection de matière
        if (matiereComboBox != null) {
            loadMatieres();
            
            // Met à jour l'IU une fois la scène disponible
            matiereComboBox.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null && userRole != null) {
                    updateUIForRole();
                }
            });
        }
        
        // Initialisation de la vue de gestion des matières
        if (matiereTable != null) {
            configureTableView();
            loadAllMatieres();
        }
    }
    
    /**
     * Met à jour l'interface en fonction du rôle de l'utilisateur.
     * Les étudiants voient moins d'options qu'un professeur.
     */
    public void updateUIForRole() {
        if (userRole != null) {
            if (!"Professeur".equals(userRole)) {
                // Pour les étudiants : cacher certains boutons
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
                // Pour les professeurs : affichage complet des boutons
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
     * Charge les matières depuis la base de données dans le ComboBox.
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
     * Configure le TableView pour la gestion des matières.
     * Définit les colonnes et ajoute les boutons d'action.
     */
    private void configureTableView() {
        // Configuration de la colonne ID
        if (idColumn != null) {
            idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        }
        
        // Configuration de la colonne nom
        if (nomColumn != null) {
            nomColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        }
        
        // Ajout des boutons d'actions pour chaque ligne (Exercices, Modifier, Supprimer)
        if (actionsColumn != null) {
            actionsColumn.setCellFactory(param -> {
                return new TableCell<>() {
                    private final Button viewButton = new Button("Exercices");
                    private final Button editButton = new Button("Modifier");
                    private final Button deleteButton = new Button("Supprimer");
                    
                    {
                        // Définition des actions au clic
                        viewButton.setOnAction(event -> viewExercices(getTableRow().getItem()));
                        editButton.setOnAction(event -> openMatiereEditor(getTableRow().getItem()));
                        deleteButton.setOnAction(event -> confirmAndDeleteMatiere(getTableRow().getItem()));
                        
                        // Application des classes CSS
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
                        buttonBox.setAlignment(Pos.CENTER);
                        
                        // Toujours afficher le bouton pour voir les exercices
                        buttonBox.getChildren().add(viewButton);
                        
                        // Seuls les professeurs peuvent modifier ou supprimer les matières
                        if ("Professeur".equals(userRole)) {
                            buttonBox.getChildren().addAll(editButton, deleteButton);
                        }
                        
                        setGraphic(buttonBox);
                    }
                };
            });
        }
        
        // Affectation de la liste des matières au TableView
        if (matiereTable != null) {
            matiereTable.setItems(matiereList);
        }
    }
    
    /**
     * Charge toutes les matières depuis la base de données pour la vue de gestion.
     */
    private void loadAllMatieres() {
        matiereList.clear();
        matiereList.addAll(matiereDAO.getAllMatieres());
    }
    
    /**
     * Gère la sélection d'une matière et ouvre la liste des exercices correspondants.
     */
    @FXML
    private void selectMatiere() {
        openSelectedMatiere();
    }
    
    /**
     * Ouvre la matière sélectionnée et ferme les autres fenêtres.
     */
    private void openSelectedMatiere() {
        Matiere matiere = matiereComboBox.getValue();
        
        if (matiere == null) {
            showAlert(Alert.AlertType.WARNING, "Selection", "Aucune matiere selectionnee", 
                     "Veuillez selectionner une matiere.");
            return;
        }
        
        try {
            // Récupérer la fenêtre actuelle avant d'en créer une nouvelle
            Stage currentStage = (Stage) matiereComboBox.getScene().getWindow();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exercice_view.fxml"));
            Parent root = loader.load();
            
            ExerciceController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            controller.setMatiere(matiere);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Ferme toutes les autres fenêtres sauf celle en cours
            closeAllOtherWindows(currentStage);
            
            // Change la scène de la fenêtre actuelle
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
     * Affiche les exercices pour une matière depuis la vue de gestion.
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
     * Ouvre la vue "Mes Exercices".
     * Change la scène de la fenêtre actuelle et ferme les autres fenêtres.
     */
    @FXML
    private void showMyExercises() {
        try {
            Stage currentStage = (Stage) matiereComboBox.getScene().getWindow();
            
            // Ferme toutes les autres fenêtres
            closeAllOtherWindows(currentStage);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exercice_view.fxml"));
            Parent root = loader.load();
            
            ExerciceController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            controller.setShowUserExercisesOnly(true);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Modification de la scène de la fenêtre actuelle
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
     * Ouvre la vue "Mes Solutions" (accessible uniquement aux professeurs).
     */
    @FXML
    private void showMySolutions() {
        if (!"Professeur".equals(userRole)) {
            showAlert(Alert.AlertType.WARNING, "Accès refusé", "Permission insuffisante", 
                     "Seuls les professeurs peuvent accéder aux solutions.");
            return;
        }
        
        try {
            Stage currentStage = (Stage) matiereComboBox.getScene().getWindow();
            closeAllOtherWindows(currentStage);
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/solution_view.fxml"));
            Parent root = loader.load();
            
            SolutionController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            controller.setShowUserSolutionsOnly(true);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
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
     * Ouvre l'éditeur pour ajouter ou modifier une matière.
     * Crée une boîte de dialogue directement via JavaFX plutôt que d'utiliser un FXML séparé.
     */
    private void openMatiereEditor(Matiere matiere) {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle(matiere != null ? "Modifier la matière" : "Ajouter une matière");
            
            // Création du formulaire d'édition
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
            
            final boolean isEditing = matiere != null;
            
            // Gestion de l'enregistrement de la matière
            saveButton.setOnAction(event -> {
                if (nomField.getText().trim().isEmpty()) {
                    editorStatusLabel.setText("Le nom de la matière est obligatoire.");
                    editorStatusLabel.setStyle("-fx-text-fill: red;");
                    return;
                }
                
                String nom = nomField.getText().trim();
                
                // Vérifie si une matière avec le même nom existe déjà
                if ((!isEditing || (isEditing && !matiere.getNom().equals(nom))) && 
                     matiereDAO.matiereExists(nom)) {
                    editorStatusLabel.setText("Une matière avec ce nom existe déjà.");
                    editorStatusLabel.setStyle("-fx-text-fill: red;");
                    return;
                }
                
                try {
                    boolean success;
                    
                    if (isEditing) {
                        // Mise à jour d'une matière existante
                        matiere.setNom(nom);
                        success = matiereDAO.updateMatiere(matiere);
                        if (success) {
                            dialog.setUserData(matiere);
                        }
                    } else {
                        // Ajout d'une nouvelle matière
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
                        
                        // Fermeture du dialogue après un court délai
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
            
            // Affichage de la boîte de dialogue
            Scene dialogScene = new Scene(dialogRoot);
            dialogScene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            dialog.setScene(dialogScene);
            dialog.setMinWidth(400);
            dialog.setMinHeight(250);
            IconHelper.setStageIcon(dialog);
            
            // Mise à jour de la liste des matières à la fermeture du dialogue
            dialog.setOnHidden(event -> {
                if (dialog.getUserData() instanceof Matiere || Boolean.TRUE.equals(dialog.getUserData())) {
                    loadAllMatieres();
                    loadMatieres(); // Rafraîchit aussi le ComboBox si présent
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
     * Demande confirmation avant de supprimer une matière.
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
                // Retirer la matière de la liste et rafraîchir le ComboBox
                matiereList.remove(matiere);
                showStatus("Matière supprimée avec succès.", false);
                loadMatieres();
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Suppression impossible", 
                        "Impossible de supprimer cette matière. Elle contient peut-être des exercices.");
            }
        }
    }
    
    /**
     * Ouvre l'écran de gestion des utilisateurs (exclusif aux professeurs).
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
     * Ouvre l'écran de gestion des matières.
     */
    @FXML
    private void manageMatieres() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matiere_management.fxml"));
            Parent root = loader.load();
            
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
     * Ouvre le formulaire d'ajout d'une nouvelle matière depuis la vue de gestion.
     */
    @FXML
    private void openAddMatiereForm() {
        openMatiereEditor(null);
    }
    
    /**
     * Retourne au menu principal depuis la vue de gestion.
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
     * Gère la déconnexion et retourne à l'écran de connexion.
     */
    @FXML
    private void handleLogout() {
        try {
            Stage currentStage = (Stage) matiereComboBox.getScene().getWindow();
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
    
    /**
     * Ferme la fenêtre actuelle.
     */
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
     * Affiche une boîte de dialogue d'alerte.
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
     * Affiche un message de statut dans l'interface.
     */
    private void showStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green"));
        }
    }
    
    /**
     * Rafraîchit la liste des matières.
     */
    @FXML
    private void refreshMatieres() {
        loadAllMatieres();
        loadMatieres();
    }
    
    /**
     * Méthode alternative pour ouvrir "Mes Exercices" depuis choix_matiere.fxml.
     */
    @FXML
    private void ouvrirMesExercices() {
        showMyExercises();
    }
    
    /**
     * Méthode alternative pour ouvrir "Mes Solutions" depuis choix_matiere.fxml.
     */
    @FXML
    private void ouvrirMesSolutions() {
        showMySolutions();
    }
    
    /**
     * Retourne à l'écran de connexion.
     */
    @FXML
    private void handleRetour() {
        handleLogout();
    }
    
    /**
     * Ferme toutes les fenêtres ouvertes, à l'exception de celle spécifiée.
     */
    private void closeAllOtherWindows(Stage exceptStage) {
        java.util.List<Stage> stagesToClose = new java.util.ArrayList<>();
        
        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
            if (window instanceof Stage && window.isShowing() && window != exceptStage) {
                stagesToClose.add((Stage) window);
            }
        }
        
        for (Stage stage : stagesToClose) {
            LOGGER.info("Closing window: " + stage.getTitle());
            stage.close();
        }
        
        LOGGER.info("Closed " + stagesToClose.size() + " additional windows");
    }
}
