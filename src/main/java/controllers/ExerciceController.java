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
 * Contrôleur pour la gestion des exercices (affichage, ajout, modification et suppression).
 * 
 * Ce contrôleur gère la vue des exercices ainsi que le formulaire d'ajout et d'édition.
 * La logique métier reste inchangée.
 */
public class ExerciceController {
    // Logger pour le suivi des événements importants
    private static final Logger LOGGER = Logger.getLogger(ExerciceController.class.getName());

    // Composants FXML pour la gestion de la vue
    @FXML private TableView<Exercice> exerciceTable;
    @FXML private TableColumn<Exercice, String> titreColumn;
    @FXML private TableColumn<Exercice, String> matiereColumn;
    @FXML private TableColumn<Exercice, LocalDateTime> dateColumn;
    @FXML private TableColumn<Exercice, Void> actionsColumn;
    @FXML private Label titleLabel;
    @FXML private Button addExerciceButton;
    
    // Champs du formulaire pour l'ajout/modification d'exercice
    @FXML private TextField titreField;
    @FXML private TextArea descriptionField;
    @FXML private Button submitButton;
    
    // Variables d'état
    private int userId;
    private String userRole;
    private Matiere matiere;
    private Exercice currentExercice;
    private boolean isEditing = false; // vrai si en mode édition
    private boolean showUserExercisesOnly = false; // filtre pour afficher uniquement les exercices de l'utilisateur
    
    // Accès aux données via le DAO et liste observable des exercices
    private final ExerciceDAO exerciceDAO = new ExerciceDAO();
    private final ObservableList<Exercice> exerciceList = FXCollections.observableArrayList();
    
    /**
     * Méthode d'initialisation du contrôleur.
     * Elle configure la vue tableau et le formulaire selon le contexte.
     */
    @FXML
    public void initialize() {
        // Initialisation du TableView si présent
        if (exerciceTable != null) {
            configureTableView();
            exerciceTable.setItems(exerciceList);
            
            // Ajout d'un listener pour détecter la sélection d'un exercice
            exerciceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    LOGGER.info("Exercice sélectionné : " + newSelection.getTitre());
                }
            });
        }
        
        // Initialisation du formulaire si le bouton de soumission est présent
        if (submitButton != null) {
            // Change le texte du bouton selon le mode (édition ou ajout)
            submitButton.textProperty().bind(
                javafx.beans.binding.Bindings.when(
                    javafx.beans.binding.Bindings.createBooleanBinding(() -> isEditing)
                ).then("Modifier").otherwise("Ajouter")
            );
        }
    }
    
    /**
     * Configure les colonnes du TableView et leurs usines à cellules.
     * Important pour l'affichage correct des données.
     */
    private void configureTableView() {
        if (exerciceTable == null) return;
        
        // Configuration de la colonne titre
        if (titreColumn != null) {
            titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        }
        
        // Configuration de la colonne matière
        if (matiereColumn != null) {
            matiereColumn.setCellValueFactory(new PropertyValueFactory<>("matiereNom"));
        }
        
        // Configuration de la colonne date avec format personnalisé
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
        
        // Ajout des boutons d'actions (Voir, Solutions, Modifier, Supprimer) pour chaque ligne
        if (actionsColumn != null) {
            setUpActionsColumn();
        }
    }
    
    /**
     * Configure la colonne d'actions avec les boutons d'interactions.
     * Chaque bouton appelle une méthode spécifique selon l'action.
     */
    private void setUpActionsColumn() {
        actionsColumn.setCellFactory(param -> {
            return new TableCell<>() {
                // Déclaration des boutons d'action avec leur style CSS associé
                private final Button viewButton = new Button("Voir");
                private final Button solutionsButton = new Button("Solutions");
                private final Button editButton = new Button("Modifier");
                private final Button deleteButton = new Button("Supprimer");
                
                {
                    // Association des actions au clic pour chaque bouton
                    viewButton.setOnAction(event -> showExerciseDetails(getTableRow().getItem()));
                    solutionsButton.setOnAction(event -> openSolutionsView(getTableRow().getItem()));
                    editButton.setOnAction(event -> openExerciseEditor(getTableRow().getItem()));
                    deleteButton.setOnAction(event -> confirmAndDeleteExercise(getTableRow().getItem()));
                    
                    // Application des classes CSS pour le style
                    viewButton.getStyleClass().add("button-blue");
                    solutionsButton.getStyleClass().add("button-green");
                    editButton.getStyleClass().add("button-yellow");
                    deleteButton.getStyleClass().add("button-red");
                    
                    // Définition de la largeur minimale pour les boutons
                    viewButton.setMinWidth(60);
                    solutionsButton.setMinWidth(80);
                    editButton.setMinWidth(70);
                    deleteButton.setMinWidth(80);
                    
                    // Permet que tous les boutons aient une largeur maximum égale
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
                    
                    // Organisation des boutons dans des conteneurs HBox et VBox pour une meilleure disposition
                    HBox viewButtonsBox = new HBox(5);
                    viewButtonsBox.setAlignment(javafx.geometry.Pos.CENTER);
                    viewButtonsBox.getChildren().addAll(viewButton, solutionsButton);
                    
                    HBox editButtonsBox = new HBox(5);
                    editButtonsBox.setAlignment(javafx.geometry.Pos.CENTER);
                    
                    // Seul le créateur ou un professeur peut modifier/supprimer un exercice
                    if (exercice.getCreateurId() == userId) {
                        editButtonsBox.getChildren().addAll(editButton, deleteButton);
                    }
                    else if ("Professeur".equals(userRole)) {
                        editButtonsBox.getChildren().addAll(editButton, deleteButton);
                    }
                    
                    // Regroupement vertical des boutons pour obtenir une organisation claire
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
     * Définit l'ID de l'utilisateur et charge les exercices correspondants.
     */
    public void setUserId(int userId) {
        this.userId = userId;
        loadExercises();
    }
    
    /**
     * Définit le rôle de l'utilisateur et met à jour l'interface en conséquence.
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        LOGGER.info("Rôle d'utilisateur défini sur : " + userRole);
        
        // Mise à jour de l'interface en fonction du rôle
        updateUIForUserRole();
    }
    
    /**
     * Met à jour les composants de l'interface utilisateur en fonction du rôle.
     */
    private void updateUIForUserRole() {
        if (addExerciceButton != null) {
            // Affiche le bouton d'ajout pour tous les rôles
            addExerciceButton.setVisible(true);
            
            // Mise à jour du titre selon le rôle et l'affichage filtré
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
     * Définit la matière utilisée pour filtrer les exercices.
     */
    public void setMatiere(Matiere matiere) {
        this.matiere = matiere;
        loadExercises();
    }
    
    /**
     * Définit le filtre pour n'afficher que les exercices de l'utilisateur.
     */
    public void setShowUserExercisesOnly(boolean showUserExercisesOnly) {
        this.showUserExercisesOnly = showUserExercisesOnly;
        loadExercises();
        updateUIForUserRole();
    }
    
    /**
     * Charge les exercices selon les filtres actifs (créateur, matière, etc.).
     */
    private void loadExercises() {
        exerciceList.clear();
        
        try {
            List<Exercice> exercises;
            if (showUserExercisesOnly) {
                // Récupération des exercices créés par l'utilisateur
                exercises = exerciceDAO.getExercicesByCreateur(userId);
                LOGGER.info("Nombre d'exercices de l'utilisateur : " + exercises.size());
            } else if (matiere != null) {
                // Récupération des exercices pour une matière spécifique
                exercises = exerciceDAO.getExercicesByMatiere(matiere.getId());
                LOGGER.info("Exercices de la matière " + matiere.getId() + " : " + exercises.size());
            } else {
                // Chargement de tous les exercices
                exercises = exerciceDAO.getAllExercices();
                LOGGER.info("Tous les exercices chargés : " + exercises.size());
            }
            
            exerciceList.addAll(exercises);
            
            // Mise à jour du TableView si disponible
            if (exerciceTable != null) {
                exerciceTable.setItems(exerciceList);
                exerciceTable.refresh();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des exercices", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de chargement", 
                    "Impossible de charger les exercices : " + e.getMessage());
        }
    }
    
    /**
     * Affiche les détails d'un exercice dans une boite de dialogue.
     */
    private void showExerciseDetails(Exercice exercice) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détails de l'exercice");
        
        DialogPane dialogPane = new DialogPane();
        
        // Affichage du titre et de la matière avec formatage en gras
        Label titreLabel = new Label("Titre: " + exercice.getTitre());
        titreLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label matiereLabel = new Label("Matière: " + exercice.getMatiereNom());
        matiereLabel.setStyle("-fx-font-weight: bold;");
        
        // Zone de texte pour la description de l'exercice
        TextArea descriptionArea = new TextArea(exercice.getDescription());
        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        descriptionArea.setPrefHeight(200);
        
        // Organisation verticale du contenu de la boite de dialogue
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
     * Ouvre l'éditeur d'exercice pour l'ajout ou la modification.
     * Crée une nouvelle fenêtre modale.
     */
    private void openExerciseEditor(Exercice exercice) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/exercice_form.fxml"));
            Parent root = loader.load();
            
            ExerciceController controller = loader.getController();
            controller.setUserId(userId);
            controller.setUserRole(userRole);
            
            if (exercice != null) {
                // Edition d'un exercice existant
                controller.setupForEditing(exercice);
            } else {
                // Ajout d'un nouvel exercice, en utilisant l'identifiant de matière courant ou par défaut
                int matiereId = (matiere != null) ? matiere.getId() : 1; // Matière par défaut = ID 1
                controller.setupForAdding(matiereId);
            }
            
            Stage stage = new Stage();
            stage.setTitle(exercice != null ? "Modifier l'exercice" : "Ajouter un exercice");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            
            // Rafraîchit la liste des exercices lors de la fermeture de la fenêtre
            stage.setOnHidden(event -> {
                if (stage.getUserData() instanceof Exercice) {
                    Exercice newExercice = (Exercice) stage.getUserData();
                    exerciceList.add(newExercice);
                    if (exerciceTable != null) {
                        exerciceTable.refresh();
                    }
                } else if (Boolean.TRUE.equals(stage.getUserData())) {
                    loadExercises();
                }
            });
            
            // Gestion de la fermeture de la fenêtre (aucune confirmation nécessaire pour les boites de dialogue modales)
            stage.setOnCloseRequest(event -> { });
            
            IconHelper.setStageIcon(stage);
            stage.showAndWait();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture de l'éditeur d'exercice", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ouverture", 
                    "Impossible d'ouvrir l'éditeur d'exercice : " + e.getMessage());
        }
    }
    
    /**
     * Recherche une fenêtre existante ayant un titre donné.
     * Utile pour éviter l'ouverture de fenêtres en double.
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
     * Ouvre la vue "Mes Exercices" pour l'utilisateur courant.
     */
    @FXML
    private void openMyExercises() {
        try {
            String windowTitle = "Mes Exercices";
            Stage existingStage = findExistingWindow(windowTitle);
            
            if (existingStage != null) {
                // Si la fenêtre existe déjà, l'afficher au premier plan
                existingStage.toFront();
                LOGGER.info("Réutilisation de la fenêtre existante 'Mes Exercices'");
            } else {
                LOGGER.info("Création d'une nouvelle fenêtre 'Mes Exercices'");
                
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
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture de 'Mes Exercices'", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                    "Impossible d'afficher mes exercices : " + e.getMessage());
        }
    }
    
    /**
     * Ouvre la vue des solutions pour un exercice donné.
     * Vérifie d'abord si une fenêtre existe déjà pour cet exercice.
     */
    private void openSolutionsView(Exercice exercice) {
        try {
            String windowTitle = "Solutions - " + exercice.getTitre();
            Stage existingStage = findExistingWindow(windowTitle);
            
            if (existingStage != null) {
                existingStage.toFront();
                LOGGER.info("Réutilisation de la fenêtre existante de solutions pour l'exercice : " + exercice.getTitre());
            } else {
                LOGGER.info("Création d'une nouvelle fenêtre de solutions pour l'exercice : " + exercice.getTitre());
                
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
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ouverture de la vue des solutions", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                    "Impossible d'afficher les solutions : " + e.getMessage());
        }
    }
    
    /**
     * Demande confirmation à l'utilisateur avant de supprimer un exercice.
     * Supprime l'exercice si confirmé et affiche une alerte.
     */
    private void confirmAndDeleteExercise(Exercice exercice) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'exercice : " + exercice.getTitre());
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cet exercice ? Cette action ne peut pas être annulée.");
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = exerciceDAO.deleteExercice(exercice.getId());
            
            if (success) {
                exerciceList.remove(exercice);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Exercice supprimé", 
                        "L'exercice a été supprimé avec succès.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", 
                        "Impossible de supprimer l'exercice : opération échouée.");
            }
        }
    }
    
    /**
     * Prépare le formulaire dans le cas de l'ajout d'un nouvel exercice.
     * Initialise la matière et vide les champs du formulaire.
     */
    public void setupForAdding(int matiereId) {
        this.matiere = new Matiere(matiereId, "");
        this.isEditing = false;
        clearForm();
    }
    
    /**
     * Prépare le formulaire dans le cas de la modification d'un exercice existant.
     * Remplit les champs du formulaire avec les données de l'exercice sélectionné.
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
     * Vide les champs du formulaire.
     */
    private void clearForm() {
        if (titreField != null && descriptionField != null) {
            titreField.clear();
            descriptionField.clear();
        }
    }
    
    /**
     * Gère la soumission du formulaire.
     * Valide les entrées et appelle la méthode d'ajout ou de mise à jour selon le mode.
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
     * Valide les champs du formulaire.
     * Retourne vrai si tous les champs obligatoires sont remplis.
     */
    private boolean validateForm() {
        return titreField != null && !titreField.getText().trim().isEmpty() &&
               descriptionField != null && !descriptionField.getText().trim().isEmpty();
    }
    
    /**
     * Ajoute un nouvel exercice via le DAO.
     */
    private void addExercise() {
        try {
            String titre = titreField.getText().trim();
            String description = descriptionField.getText().trim();
            
            Exercice exercice = new Exercice(0, titre, description, LocalDateTime.now(), 
                                          matiere.getId(), userId);
            
            // Utilisation du DAO pour ajouter l'exercice et récupérer l'objet avec son ID
            Exercice createdExercice = exerciceDAO.addExerciceAndReturn(exercice);
            
            if (createdExercice != null) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Exercice ajouté", 
                        "L'exercice a été ajouté avec succès.");
                
                // Passage du nouvel exercice à la fenêtre parente pour mise à jour
                Stage currentStage = (Stage) titreField.getScene().getWindow();
                currentStage.setUserData(createdExercice);
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ajout", 
                        "Impossible d'ajouter l'exercice : opération échouée.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de l'ajout de l'exercice", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur d'ajout", 
                    "Impossible d'ajouter l'exercice : " + e.getMessage());
        }
    }
    
    /**
     * Met à jour un exercice existant via le DAO.
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
                currentStage.setUserData(Boolean.TRUE); // Signale qu'une mise à jour a été effectuée
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de modification", 
                        "Impossible de modifier l'exercice : opération échouée.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la modification de l'exercice", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de modification", 
                    "Impossible de modifier l'exercice : " + e.getMessage());
        }
    }
    
    /**
     * Annule le formulaire et ferme la fenêtre active.
     */
    @FXML
    private void cancelForm() {
        closeForm();
    }
    
    /**
     * Ferme la fenêtre du formulaire.
     */
    private void closeForm() {
        if (titreField != null) {
            Stage stage = (Stage) titreField.getScene().getWindow();
            stage.close();
        }
    }
    
    /**
     * Ouvre le formulaire pour ajouter un nouvel exercice.
     */
    @FXML
    private void openAddExerciseForm() {
        openExerciseEditor(null);
    }
    
    /**
     * Retourne à la vue de sélection de matière.
     * Ferme toutes les autres fenêtres ouvertes.
     */
    @FXML
    private void backToMatiereSelection() {
        try {
            if (exerciceTable != null) {
                Stage currentStage = (Stage) exerciceTable.getScene().getWindow();
                
                // Ferme toutes les autres fenêtres sauf celle courante
                closeAllOtherWindows(currentStage);
                
                // Chargement de la vue de sélection de matière
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/matiere_view.fxml"));
                Parent root = loader.load();
                
                // Configuration du contrôleur de la vue matière
                MatiereController controller = loader.getController();
                controller.setUserId(userId);
                controller.setUserRole(userRole);
                
                // Mise à jour de la scène actuelle avec la nouvelle vue
                Scene scene = new Scene(root);
                scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
                
                currentStage.setTitle("Sélection de matière");
                currentStage.setScene(scene);
                
                LOGGER.info("Navigation vers la vue de sélection de matière et fermeture des autres fenêtres");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du retour à la sélection de matière", e);
            showAlert(Alert.AlertType.ERROR, "Erreur", "Navigation impossible", 
                    "Impossible de retourner à la sélection de matière : " + e.getMessage());
        }
    }
    
    /**
     * Ferme toutes les fenêtres ouvertes sauf celle spécifiée.
     */
    private void closeAllOtherWindows(Stage exceptStage) {
        List<Stage> stagesToClose = new ArrayList<>();
        
        // Recherche de toutes les fenêtres ouvertes
        for (javafx.stage.Window window : javafx.stage.Window.getWindows()) {
            if (window instanceof Stage && window.isShowing() && window != exceptStage) {
                stagesToClose.add((Stage) window);
            }
        }
        
        // Fermeture de chaque fenêtre identifiée
        for (Stage stage : stagesToClose) {
            LOGGER.info("Fermeture de la fenêtre : " + stage.getTitle());
            stage.close();
        }
        
        LOGGER.info("Fermeture de " + stagesToClose.size() + " fenêtre(s) supplémentaire(s)");
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
     * Rafraîchit la liste des exercices en rechargant les données.
     */
    @FXML
    private void refreshExercises() {
        loadExercises();
    }
    
    /**
     * Définit l'identifiant d'exercice pour la vue courante.
     * Charge l'exercice correspondant et met à jour la matière.
     */
    public void setExerciceId(int exerciceId) {
        try {
            Exercice exercice = exerciceDAO.getExerciceById(exerciceId);
            if (exercice != null) {
                this.matiere = new Matiere(exercice.getMatiereId(), "");
                loadExercises();
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Erreur lors de la définition de l'ID d'exercice", e);
        }
    }
}
