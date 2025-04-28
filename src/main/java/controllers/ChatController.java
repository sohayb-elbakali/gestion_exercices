package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import models.Exercice;
import utils.DatabaseConnection;

import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Timer;
import java.util.TimerTask;

public class ChatController {
    @FXML
    private TextArea chatArea;
    @FXML private TextField messageField;

    private Exercice exercice;
    private Timer chatUpdater;

    public void setExercice(Exercice exercice) {
        this.exercice = exercice;
        loadMessages();
        startChatUpdates();
    }

    private void loadMessages() {
        String sql = "SELECT m.contenu, u.nom, m.date_envoi FROM message m " +
                "JOIN utilisateur u ON m.auteur_id = u.id " +
                "WHERE m.exercice_id = ? ORDER BY m.date_envoi";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, exercice.getId());
            ResultSet rs = stmt.executeQuery();

            StringBuilder chatContent = new StringBuilder();
            while (rs.next()) {
                chatContent.append(String.format("[%s] %s: %s\n",
                        rs.getTimestamp("date_envoi").toLocalDateTime(),
                        rs.getString("nom"),
                        rs.getString("contenu")));
            }

            chatArea.setText(chatContent.toString());
        } catch (SQLException e) {
            showErrorAlert("Erreur de chargement du chat");
        }
    }

    private void showErrorAlert(String erreurDeChargementDuChat) {

    }

    @FXML
    private void handleSendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            // Envoyer le message à la base de données
            messageField.list();
        }
    }

    private void startChatUpdates() {
        chatUpdater = new Timer();
        chatUpdater.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> loadMessages());
            }
        }, 0, 5000); // Mise à jour toutes les 5 secondes
    }
}
