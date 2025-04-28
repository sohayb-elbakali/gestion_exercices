package controllers;

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
import utils.DatabaseConnection;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MesExercicesController {

    private static final Logger LOGGER = Logger.getLogger(MesExercicesController.class.getName());

    // Composants FXML
    @FXML private TableView<Exercice> exerciceTable;
    @FXML private TableColumn<Exercice, String> titreColumn;
    @FXML private TableColumn<Exercice, LocalDateTime> dateColumn;
    @FXML private TableColumn<Exercice, Void> actionsColumn;

    // Variables pour gérer l'état
    private int createurId; // ID de l'utilisateur connecté
    private Stage primaryStage;

    // Liste observable pour stocker les exercices
    private final ObservableList<Exercice> exerciceList = FXCollections.observableArrayList();

    /**
     * Définit l'ID du créateur et charge ses exercices.
     */
    public void setCreateurId(int createurId) {
        this.createurId = createurId;
        LOGGER.info("Set createurId to: " + createurId);
        loadExercices(); // Charge les exercices après avoir défini le createurId
    }

    /**
     * Définit la fenêtre principale pour la navigation.
     */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
        LOGGER.info("Primary stage set successfully.");
    }

    /**
     * Initialise le contrôleur après le chargement du fichier FXML.
     */
    @FXML
    public void initialize() {
        // Configure les colonnes du tableau
        configureTableColumns();
        LOGGER.info("Colonnes du tableau initialisées avec succès.");
    }

    /**
     * Configure les colonnes du tableau (titre, date, actions).
     */
    private void configureTableColumns() {
        // Titre
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));

        // Date de création
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
        });

        // Boutons d'action (Modifier/Supprimer)
        actionsColumn.setCellFactory(param -> createActionButtons());
    }

    /**
     * Crée les boutons d'action (Modifier/Supprimer) pour chaque ligne.
     */
    private TableCell<Exercice, Void> createActionButtons() {
        return new TableCell<>() {
            private final Button modifyButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");

            {
                // Style des boutons
                modifyButton.getStyleClass().add("button-modify");
                deleteButton.getStyleClass().add("button-delete");

                // Action pour modifier un exercice
                modifyButton.setOnAction(event -> {
                    Exercice exercice = getTableView().getItems().get(getIndex());
                    openModifyDialog(exercice);
                });

                // Action pour supprimer un exercice
                deleteButton.setOnAction(event -> {
                    Exercice exercice = getTableView().getItems().get(getIndex());
                    confirmAndDeleteExercise(exercice);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttonContainer = new HBox(5, modifyButton, deleteButton);
                    buttonContainer.setAlignment(javafx.geometry.Pos.CENTER);
                    setGraphic(buttonContainer);
                }
            }
        };
    }

    /**
     * Charge les exercices créés par l'utilisateur connecté depuis la base de données.
     */
    private void loadExercices() {
        exerciceList.clear();
        String sql = "SELECT * FROM exercice WHERE createur_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, createurId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                exerciceList.add(new Exercice(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getTimestamp("date_creation") != null ?
                                rs.getTimestamp("date_creation").toLocalDateTime() : null,
                        rs.getInt("matiere_id"),
                        rs.getInt("createur_id")
                ));
            }

            exerciceTable.setItems(exerciceList);

            LOGGER.info("Chargé " + exerciceList.size() + " exercices pour createurId: " + createurId);

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement des exercices", e);
            showErrorAlert("Erreur SQL", "Impossible de charger les exercices: " + e.getMessage());
        }
    }

    /**
     * Ouvre la boîte de dialogue pour modifier un exercice.
     */
    private void openModifyDialog(Exercice exercice) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/modifier_exercice.fxml"));
            Parent root = loader.load();

            ModifierExerciceController controller = loader.getController();
            controller.setExercice(exercice);

            Stage stage = new Stage();
            stage.setTitle("Modifier l'exercice");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadExercices(); // Rafraîchit la liste après modification

        } catch (IOException e) {
            showErrorAlert("Erreur", "Impossible d'ouvrir l'éditeur: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Demande confirmation avant de supprimer un exercice.
     */
    private void confirmAndDeleteExercise(Exercice exercice) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer cet exercice ?");
        confirmation.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM exercice WHERE id = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, exercice.getId());
                stmt.executeUpdate();
                loadExercices(); // Rafraîchit la liste après suppression

            } catch (SQLException e) {
                showErrorAlert("Erreur SQL", "Échec de la suppression: " + e.getMessage());
            }
        }
    }

    /**
     * Gère le retour à l'écran précédent.
     */


    @FXML
    private void handleBack() {
        if (primaryStage == null) {
            LOGGER.severe("primaryStage is null. Ensure setPrimaryStage is called.");
            showErrorAlert("Erreur", "Impossible de revenir à l'écran précédent : primaryStage est null.");
            return;
        }

        try {
            // Charger le fichier FXML pour la vue Choix Matière
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/choix_matiere.fxml"));
            Parent root = loader.load();
            LOGGER.info("FXMLLoader successfully loaded choix_matiere.fxml");

            // Récupérer le contrôleur associé à la vue chargée
            ChoixMatiereController choixMatiereController = loader.getController();
            choixMatiereController.setCreateurId(createurId);

            // Mettre à jour la scène avec choix_matiere.fxml
            primaryStage.setScene(new Scene(root));
            primaryStage.show(); // Afficher la nouvelle scène
            LOGGER.info("Navigating back to choix_matiere.fxml");

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Erreur lors du chargement de choix_matiere.fxml", e);
            showErrorAlert("Erreur", "Impossible de charger la page précédente : " + e.getMessage());
        }
    }

    /**
     * Affiche une alerte d'erreur à l'utilisateur.
     */
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}