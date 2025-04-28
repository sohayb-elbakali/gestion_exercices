package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class DetailExerciceController {

    @FXML
    private Label titreLabel;

    @FXML
    private TextArea descriptionArea;

    // Méthode pour initialiser les champs si nécessaire
    @FXML
    public void initialize() {
        // Initialisation de base, facultatif
        titreLabel.setText("Titre par défaut");
        descriptionArea.setText("Description par défaut de l'exercice.");
    }

    // Méthode pour charger les détails d'un exercice
    public void setExerciceDetails(String titre, String description) {
        titreLabel.setText(titre);
        descriptionArea.setText(description);
    }
}

