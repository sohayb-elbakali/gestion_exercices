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
import models.Solution;
import utils.DatabaseConnection;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class MesSolutionsController {

    @FXML private TableView<Solution> solutionTable;
    @FXML private TableColumn<Solution, String> contenuColumn;
    @FXML private TableColumn<Solution, LocalDateTime> dateColumn;
    @FXML private TableColumn<Solution, Void> actionsColumn;

    private int createurId;
    private final ObservableList<Solution> solutionList = FXCollections.observableArrayList();

    public void setCreateurId(int createurId) {
        this.createurId = createurId;
        loadSolutions();
    }

    @FXML
    public void initialize() {
        contenuColumn.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));

        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        item.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            }
        });

        actionsColumn.setCellFactory(param -> createActionButtons());
    }

    private void loadSolutions() {
        solutionList.clear();
        String sql = "SELECT s.id, s.contenu, s.date_creation, s.exercice_id " +
                "FROM solution s " +
                "JOIN exercice e ON s.exercice_id = e.id " +
                "WHERE e.createur_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, createurId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                solutionList.add(new Solution(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        rs.getInt("exercice_id"),
                        0 // auteurId n'est pas utilisé ici car il est déduit
                ));
            }

            solutionTable.setItems(solutionList);

        } catch (SQLException e) {
            showErrorAlert("Erreur SQL", "Impossible de charger les solutions: " + e.getMessage());
        }
    }

    private TableCell<Solution, Void> createActionButtons() {
        return new TableCell<>() {
            private final Button modifyButton = new Button("Modifier");
            private final Button deleteButton = new Button("Supprimer");

            {
                modifyButton.setOnAction(event -> {
                    Solution solution = getTableView().getItems().get(getIndex());
                    openModifyDialog(solution);
                });

                deleteButton.setOnAction(event -> {
                    Solution solution = getTableView().getItems().get(getIndex());
                    confirmAndDeleteSolution(solution);
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

    private void openModifyDialog(Solution solution) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/modifier_solution.fxml"));
            Parent root = loader.load();

            ModifierSolutionController controller = loader.getController();
            controller.setSolution(solution);

            Stage stage = new Stage();
            stage.setTitle("Modifier la solution");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            loadSolutions(); // Rafraîchir après modification

        } catch (IOException e) {
            showErrorAlert("Erreur", "Impossible d'ouvrir l'éditeur: " + e.getMessage());
        }
    }

    private void confirmAndDeleteSolution(Solution solution) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer cette solution ?");
        confirmation.setContentText("Cette action est irréversible.");

        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "DELETE FROM solution WHERE id = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, solution.getId());
                stmt.executeUpdate();
                loadSolutions(); // Rafraîchir après suppression

            } catch (SQLException e) {
                showErrorAlert("Erreur SQL", "Échec de la suppression: " + e.getMessage());
            }
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
