package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import utils.DatabaseConnection;

import java.io.InputStream;
import java.io.PushbackInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<String> roleComboBox;

    @FXML
    public void initialize() {
        roleComboBox.getItems().addAll("Étudiant", "Professeur");
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText();
        String password = passwordField.getText();
        String role = roleComboBox.getValue();


        try (Connection conn = DatabaseConnection.getConnection()) {
            String sql = "SELECT * FROM utilisateur WHERE email = ? AND mot_de_passe = ? AND role = ?";
            PreparedStatement stmt = conn.prepareStatement(sql);
            stmt.setString(1, email);
            stmt.setString(2, password);
            stmt.setString(3, role);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                int userId = rs.getInt("id"); // ✅ récupérer l'id de l'utilisateur connecté

                // Chargement sécurisé du FXML
                try (InputStream fxmlStream = getClass().getResourceAsStream("/fxml/choix_matiere.fxml")) {
                    // Vérification et suppression du BOM si présent
                    PushbackInputStream pushbackStream = new PushbackInputStream(fxmlStream, 3);
                    byte[] bom = new byte[3];
                    if (pushbackStream.read(bom) == 3
                            && bom[0] == (byte)0xEF
                            && bom[1] == (byte)0xBB
                            && bom[2] == (byte)0xBF) {
                        System.out.println("BOM détecté et ignoré");
                    } else {
                        pushbackStream.unread(bom);
                    }

                    FXMLLoader loader = new FXMLLoader();
                    Parent root = loader.load(pushbackStream);

                    // ✅ Récupérer le contrôleur
                    ChoixMatiereController choixMatiereController = loader.getController();
                    choixMatiereController.setCreateurId(userId); // ✅ envoyer l'id au choix_matiere
                    Scene scene = new Scene(root);
                    Stage stage = new Stage();
                    stage.setScene(scene);
                    stage.setTitle("Choix de la matière");
                    stage.show();

                    ((Stage) emailField.getScene().getWindow()).close();
                }
            } else {
                showAlert("Identifiants incorrects");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur: " + e.getMessage());
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }
}
