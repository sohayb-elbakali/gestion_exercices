// ExerciceController.java
package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import models.Exercice;
import models.Matiere;
import utils.DatabaseConnection;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ExerciceController {
    @FXML private TableView<Exercice> exerciceTable;
    @FXML private TableColumn<Exercice, String> titreColumn;
    @FXML private TableColumn<Exercice, LocalDateTime> dateColumn;

    private int matiereId;
    private int createurId; // PAS de valeur fixe ici

    private final ObservableList<Exercice> exerciceList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        titreColumn.setCellValueFactory(new PropertyValueFactory<>("titre"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(item));
            }
        });
    }

    public void setMatiere(Matiere matiere) {
        this.matiereId = matiere.getId();
        loadExercices();
    }

    public void setCreateurId(int id) {
        this.createurId = id;
    }

    @FXML
    private void handleAddExercice() {
        System.out.println("createurId : " + createurId);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ajout_exercice.fxml"));
            Parent root = loader.load();

            AjoutExerciceController controller = loader.getController();
            controller.setMatiereId(matiereId);
            controller.setCreateurId(createurId); // ON ENVOIE LE BON ID

            Stage dialog = new Stage();
            dialog.setTitle("Ajouter un exercice");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(new Scene(root));
            dialog.showAndWait();

            loadExercices();
        } catch (IOException e) {
            showAlert("Erreur lors de l'ouverture de la fenêtre: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadExercices() {
        exerciceList.clear();
        String sql = "SELECT * FROM exercice WHERE matiere_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, matiereId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("date_creation");
                LocalDateTime dateCreation = timestamp != null ? timestamp.toLocalDateTime() : LocalDateTime.now();

                exerciceList.add(new Exercice(
                        rs.getInt("id"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        dateCreation,
                        rs.getInt("matiere_id"),
                        rs.getInt("createur_id")
                ));
            }

            exerciceTable.setItems(exerciceList);
        } catch (SQLException e) {
            showAlert("Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleOpenChat() {
        Exercice selected = exerciceTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/chat.fxml"));
                Parent root = loader.load();

                Stage stage = new Stage();
                stage.setTitle("Chat - " + selected.getTitre());
                stage.setScene(new Scene(root));
                stage.show();
            } catch (IOException e) {
                showAlert("Erreur lors de l'ouverture du chat: " + e.getMessage());
            }
        } else {
            showAlert("Veuillez sélectionner un exercice pour ouvrir le chat");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleVoirDetails() {
        Exercice selected = exerciceTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/detail_exercice.fxml"));
                Parent root = loader.load();

                DetailExerciceController controller = loader.getController();
                controller.setExerciceDetails(selected.getTitre(), selected.getDescription());

                Stage stage = new Stage();
                stage.setTitle("Détails de l'exercice");
                stage.setScene(new Scene(root));
                stage.initModality(Modality.APPLICATION_MODAL);
                stage.show();

            } catch (IOException e) {
                showAlert("Erreur lors de l'ouverture des détails : " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            showAlert("Veuillez sélectionner un exercice pour voir ses détails.");
        }

    }
}
