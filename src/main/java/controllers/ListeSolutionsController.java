package controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import models.Solution;
import utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ListeSolutionsController {
    @FXML private TableView<Solution> solutionTable;
    @FXML private TableColumn<Solution, String> contenuColumn;
    @FXML private TableColumn<Solution, LocalDateTime> dateColumn;
    @FXML private TableColumn<Solution, String> auteurColumn;

    private int exerciceId;
    private final ObservableList<Solution> solutionList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        contenuColumn.setCellValueFactory(new PropertyValueFactory<>("contenu"));
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        auteurColumn.setCellValueFactory(new PropertyValueFactory<>("auteurNom"));

        dateColumn.setCellFactory(column -> new javafx.scene.control.TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null :
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(item));
            }
        });
    }

    public void setExerciceId(int exerciceId) {
        this.exerciceId = exerciceId;
        loadSolutions();
    }
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadSolutions() {
        solutionList.clear();
        String sql = "SELECT s.id, s.contenu, s.date_creation, s.auteur_id, u.email AS auteur_email " +
                "FROM solution s " +
                "JOIN utilisateur u ON s.auteur_id = u.id " +
                "WHERE s.exercice_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, exerciceId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Solution solution = new Solution(
                        rs.getInt("id"),
                        rs.getString("contenu"),
                        rs.getTimestamp("date_creation").toLocalDateTime(),
                        exerciceId,
                        rs.getInt("auteur_id")
                );
                solution.setAuteurNom(rs.getString("auteur_email")); // Utiliser l'email comme nom d'auteur
                solutionList.add(solution);
            }

            solutionTable.setItems(solutionList);
        } catch (SQLException e) {
            showAlert("Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
    }



    }


