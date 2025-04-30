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
 * Contrôleur pour la gestion des solutions :
 * • Affichage de la liste des solutions
 * • Ajout, modification et suppression de solutions
 *
 * Remarque : La logique métier n'est pas modifiée, seul les commentaires
 * importants en français ont été ajoutés pour clarifier le fonctionnement.
 */
public class SolutionController {
    // Logger pour suivre les opérations et afficher les informations importantes
    private static final Logger LOGGER = Logger.getLogger(SolutionController.class.getName());

    // Composants FXML pour la vue liste des solutions
    @FXML private TableView<Solution> solutionTable;
    @FXML private TableColumn<Solution, String> contenuColumn;
    @FXML private TableColumn<Solution, LocalDateTime> dateColumn;
    @FXML private TableColumn<Solution, String> auteurColumn;
    @FXML private TableColumn<Solution, Void> actionsColumn;
    @FXML private Button addSolutionButton;
    
    // Composants FXML pour le formulaire de solution
    @FXML private TextArea contenuField;
    @FXML private Button submitButton;
    
    // Variables d'état
    private int userId;
    private String userRole;
    private int exerciceId;
    private Solution currentSolution;
    private boolean isEditing = false;
    private boolean showUserSolutionsOnly = false;
    
    // Accès aux données via le DAO et liste observable pour le TableView
    private final SolutionDAO solutionDAO = new SolutionDAO();
    private final ObservableList<Solution> solutionList = FXCollections.observableArrayList();

    /**
     * Méthode d'initialisation du contrôleur.
     * Configure le TableView, associe la propriété du bouton de soumission et met à jour les permissions de l'IU.
     */
    @FXML
    public void initialize() {
        configureTableView();
        
        // Lie le texte du bouton de soumission à l'état d'édition (Modifier/Ajouter)
        if (submitButton != null) {
            submitButton.textProperty().bind(
                javafx.beans.binding.Bindings.when(
                    javafx.beans.binding.Bindings.createBooleanBinding(() -> isEditing)
                ).then("Modifier").otherwise("Ajouter")
            );
        }
        
        // Dès que la scène est chargée, mettre à jour les permissions d'affichage
        if (solutionTable != null) {
            solutionTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    updateUIPermissions();
                }
            });
        }
    }
    
    /**
     * Configure les colonnes du TableView et définit les usines à cellules.
     * Important pour l'affichage et la mise en forme des données, notamment la date.
     */
    private void configureTableView() {
        if (solutionTable == null) return;
        
        // Attribution des valeurs des colonnes aux propriétés de l'objet Solution
        contenuColumn.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        auteurColumn.setCellValueFactory(new PropertyValueFactory<>("auteurNom"));
        
        // Formatage de la colonne date
        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
        });
        
        // Attribution de boutons d'action pour chaque ligne de solution
        actionsColumn.setCellFactory(param -> createActionButtons());
    }
    
    /**
     * Crée les boutons d'action (Voir, Modifier, Supprimer) pour chaque ligne du TableView.
     * Seuls les professeurs ou l'auteur de la solution peuvent modifier ou supprimer.
     */
    private TableCell<Solution, Void> createActionButtons() {
        return new TableCell<>() {
            // Déclaration et configuration des boutons
            private final Button viewButton = new Button("Voir");
            private final Button editButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");
            
            {
                // Définition des actions lors du clic sur chaque bouton
                viewButton.setOnAction(event -> showSolutionDetails(getTableRow().getItem()));
                editButton.setOnAction(event -> openSolutionEditor(getTableRow().getItem()));
                deleteButton.setOnAction(event -> confirmAndDeleteSolution(getTableRow().getItem()));
                
                // Application des styles CSS personnalisés
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
                
                // Récupération de la solution associée à la ligne
                Solution solution = getTableRow().getItem();
                if (solution == null) {
                    setGraphic(null);
                    return;
                }
                
                // Création d'un conteneur HBox pour organiser les boutons horizontalement
                HBox buttonBox = new HBox(5);
                buttonBox.setAlignment(javafx.geometry.Pos.CENTER);
                
                // Bouton Vue disponible pour tous
                buttonBox.getChildren().add(viewButton);
                
                // Seuls les professeurs ou l'auteur de la solution peuvent modifier ou supprimer
                boolean canModify = "Professeur".equals(userRole) || solution.getAuteurId() == userId;
                if (canModify) {
                    buttonBox.getChildren().addAll(editButton, deleteButton);
                }
                
                setGraphic(buttonBox);
            }
        };
    }
    
    /**
     * Définit l'ID de l'utilisateur et charge les solutions correspondantes.
     */
    public void setUserId(int userId) {
        this.userId = userId;
        loadSolutions();
    }
    
    /**
     * Définit le rôle de l'utilisateur.
     * Pour les étudiants, le bouton d'ajout de solution est caché.
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
        LOGGER.info("User role set to: " + userRole);
        
        if (addSolutionButton != null && "Étudiant".equals(userRole)) {
            addSolutionButton.setVisible(false);
            addSolutionButton.setManaged(false);
        }
        
        updateUIPermissions();
    }
    
    /**
     * Définit l'ID de l'exercice pour filtrer les solutions.
     */
    public void setExerciceId(int exerciceId) {
        this.exerciceId = exerciceId;
        loadSolutions();
    }
    
    /**
     * Détermine si seuls les solutions de l'utilisateur doivent être affichées.
     */
    public void setShowUserSolutionsOnly(boolean showUserSolutionsOnly) {
        this.showUserSolutionsOnly = showUserSolutionsOnly;
        loadSolutions();
    }
    
    /**
     * Charge les solutions selon les filtres actuels (par créateur ou par exercice).
     */
    private void loadSolutions() {
        solutionList.clear();
        
        try {
            List<Solution> solutions = new ArrayList<>();
            
            if (showUserSolutionsOnly) {
                // Récupère uniquement les solutions créées par l'utilisateur
                try {
                    solutions = solutionDAO.getSolutionsByCreateur(userId);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error loading solutions by creator, falling back to solutions by author", e);
                    solutions = solutionDAO.getSolutionsByAuteur(userId);
                }
            } else if (exerciceId > 0) {
                // Récupère les solutions pour un exercice spécifique
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
     * Affiche les détails d'une solution dans une boite de dialogue.
     */
    private void showSolutionDetails(Solution solution) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Détails de la solution");
        
        DialogPane dialogPane = new DialogPane();
        
        // Affiche l'auteur de la solution en gras
        Label authorLabel = new Label("Auteur: " + solution.getAuteurNom());
        authorLabel.setStyle("-fx-font-weight: bold;");
        
        // Affiche la date de création au format personnalisé
        Label dateLabel = new Label("Date: " + solution.getDateCreation().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        
        // Zone de texte pour afficher le contenu de la solution (non éditable)
        TextArea contenuArea = new TextArea(solution.getContenu());
        contenuArea.setEditable(false);
        contenuArea.setWrapText(true);
        contenuArea.setPrefHeight(300);
        
        VBox content = new VBox(10, 
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
     * Ouvre l'éditeur de solution pour ajouter ou modifier une solution.
     * Lance une nouvelle fenêtre modale pour la saisie du formulaire.
     */
    private void openSolutionEditor(Solution solution) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/solution_form.fxml"));
            Parent root = loader.load();
            
            // Récupère le contrôleur de l'éditeur de solution
            SolutionController controller = loader.getController();
            controller.setUserId(userId);
            
            if (solution != null) {
                // Mode édition
                controller.setupForEditing(solution);
            } else {
                // Mode ajout
                controller.setupForAdding(exerciceId);
            }
            
            Stage stage = new Stage();
            stage.setTitle(solution != null ? "Modifier la solution" : "Ajouter une solution");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.initModality(Modality.APPLICATION_MODAL);
            IconHelper.setStageIcon(stage);
            
            // À la fermeture de la fenêtre, rafraîchit la liste des solutions
            stage.setOnHidden(event -> {
                if (stage.getUserData() instanceof Solution) {
                    // Une solution a été créée et est ajoutée directement à la liste
                    Solution newSolution = (Solution) stage.getUserData();
                    solutionList.add(newSolution);
                    if (solutionTable != null) {
                        solutionTable.refresh();
                    }
                } else if (Boolean.TRUE.equals(stage.getUserData())) {
                    // Sinon, recharge toutes les solutions
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
     * Prépare le formulaire pour l'ajout d'une nouvelle solution.
     */
    public void setupForAdding(int exerciceId) {
        this.exerciceId = exerciceId;
        this.isEditing = false;
        clearForm();
    }
    
    /**
     * Prépare le formulaire pour l'édition d'une solution existante.
     */
    public void setupForEditing(Solution solution) {
        this.currentSolution = solution;
        this.isEditing = true;
        
        if (contenuField != null) {
            contenuField.setText(solution.getContenu());
        }
    }
    
    /**
     * Vide le champ du formulaire.
     */
    private void clearForm() {
        if (contenuField != null) {
            contenuField.clear();
        }
    }
    
    /**
     * Gère la soumission du formulaire (ajout ou modification de solution).
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
     * Valide que le formulaire a été correctement rempli.
     */
    private boolean validateForm() {
        return contenuField != null && !contenuField.getText().trim().isEmpty();
    }
    
    /**
     * Ajoute une nouvelle solution.
     */
    private void addSolution() {
        try {
            String contenu = contenuField.getText().trim();
            
            Solution solution = new Solution();
            solution.setContenu(contenu);
            solution.setDateCreation(LocalDateTime.now());
            solution.setExerciceId(exerciceId);
            solution.setAuteurId(userId);
            
            // Récupère le nom de l'auteur pour un meilleur affichage
            try {
                String authorName = getUserName(userId);
                solution.setAuteurNom(authorName != null ? authorName : "Utilisateur " + userId);
            } catch (Exception e) {
                solution.setAuteurNom("Utilisateur " + userId);
            }
            
            // Ajoute la solution et récupère l'objet créé (avec ID attribué)
            Solution createdSolution = solutionDAO.addSolutionAndReturn(solution);
            
            if (createdSolution != null) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Solution ajoutée", 
                        "La solution a été ajoutée avec succès.");
                
                Stage currentStage = (Stage) contenuField.getScene().getWindow();
                currentStage.setUserData(createdSolution);
                
                // Recharge la liste des solutions
                if (solutionList != null) {
                    loadSolutions();
                }
            } else {
                // Méthode de repli si la première méthode échoue
                boolean success = solutionDAO.addSolution(solution);
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Solution ajoutée", 
                            "La solution a été ajoutée avec succès.");
                    
                    Stage currentStage = (Stage) contenuField.getScene().getWindow();
                    currentStage.setUserData(Boolean.TRUE);
                    
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
     * Méthode d'assistance pour obtenir le nom de l'utilisateur à partir de son ID.
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
     * Met à jour une solution existante.
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
                
                Stage currentStage = (Stage) contenuField.getScene().getWindow();
                currentStage.setUserData(currentSolution);
                
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
     * Demande confirmation et supprime une solution.
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
                    // Supprime la solution de la liste et rafraîchit le TableView
                    solutionList.remove(solution);
                    if (solutionTable != null) {
                        solutionTable.refresh();
                    }
                    
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Solution supprimée", 
                            "La solution a été supprimée avec succès.");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", 
                            "Impossible de supprimer la solution: opération échouée.");
                    loadSolutions();
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error deleting solution", e);
                showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur de suppression", 
                        "Impossible de supprimer la solution: " + e.getMessage());
                loadSolutions();
            }
        }
    }
    
    /**
     * Ouvre le formulaire pour l'ajout d'une nouvelle solution.
     * Seules les solutions peuvent être ajoutées par des professeurs.
     */
    @FXML
    private void openAddSolutionForm() {
        if ("Étudiant".equals(userRole)) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Permission refusée", 
                    "Seuls les professeurs peuvent ajouter des solutions.");
            return;
        }
        
        if (exerciceId <= 0) {
            showAlert(Alert.AlertType.WARNING, "Action impossible", "Information incomplète", 
                    "Veuillez sélectionner un exercice spécifique avant d'ajouter une solution.");
            return;
        }
        
        openSolutionEditor(null);
    }
    
    /**
     * Retourne à l'écran précédent (vue des exercices).
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
     * Annule l'édition et ferme le formulaire.
     */
    @FXML
    private void handleCancel() {
        closeForm();
    }
    
    /**
     * Ferme le formulaire en cours.
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
     * Ferme la fenêtre ou le stage courant.
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
     * Affiche une boîte de dialogue d'alerte avec le type, titre, en-tête et contenu spécifiés.
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
     * Rafraîchit la liste des solutions en rechargeant les données.
     */
    @FXML
    private void refreshSolutions() {
        loadSolutions();
    }

    /**
     * Met à jour la visibilité des éléments de l'IU en fonction des permissions d'utilisateur.
     * Pour les étudiants, le bouton d'ajout de solution est masqué.
     */
    private void updateUIPermissions() {
        if (!"Étudiant".equals(userRole)) {
            return;
        }
        
        if (addSolutionButton != null) {
            addSolutionButton.setVisible(false);
            addSolutionButton.setManaged(false);
            return;
        }
        
        // Si le bouton n'est pas directement référencé, recherche dans la hiérarchie de la scène
        if (solutionTable != null && solutionTable.getScene() != null) {
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
