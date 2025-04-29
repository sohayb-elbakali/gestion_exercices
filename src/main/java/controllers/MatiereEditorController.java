package controllers;

import dao.MatiereDAO;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import models.Matiere;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Controller for adding and editing matiere (subject)
 */
public class MatiereEditorController {
    private static final Logger LOGGER = Logger.getLogger(MatiereEditorController.class.getName());
    
    @FXML private TextField nomField;
    @FXML private Button submitButton;
    @FXML private Label statusLabel;
    
    private Matiere currentMatiere;
    private boolean isEditing = false;
    private final MatiereDAO matiereDAO = new MatiereDAO();
    
    /**
     * Initialize the controller
     */
    @FXML
    public void initialize() {
        // Set default values
        if (statusLabel != null) {
            statusLabel.setText("");
        }
        
        // Configure button text based on mode
        if (submitButton != null) {
            submitButton.textProperty().bind(
                javafx.beans.binding.Bindings.when(
                    javafx.beans.binding.Bindings.createBooleanBinding(() -> isEditing)
                ).then("Modifier").otherwise("Ajouter")
            );
        }
    }
    
    /**
     * Setup the form for adding a new matiere
     */
    public void setupForAdding() {
        this.isEditing = false;
        clearForm();
    }
    
    /**
     * Setup the form for editing an existing matiere
     */
    public void setupForEditing(Matiere matiere) {
        this.currentMatiere = matiere;
        this.isEditing = true;
        
        if (nomField != null) {
            nomField.setText(matiere.getNom());
        }
    }
    
    /**
     * Clear the form fields
     */
    private void clearForm() {
        if (nomField != null) {
            nomField.clear();
        }
        
        if (statusLabel != null) {
            statusLabel.setText("");
        }
    }
    
    /**
     * Handle form submission (add/edit)
     */
    @FXML
    private void handleSubmit() {
        if (!validateForm()) {
            return;
        }
        
        if (isEditing) {
            updateMatiere();
        } else {
            addMatiere();
        }
    }
    
    /**
     * Validate form inputs
     */
    private boolean validateForm() {
        String nom = nomField.getText().trim();
        
        if (nom.isEmpty()) {
            showStatus("Le nom de la matière est obligatoire.", true);
            return false;
        }
        
        // Check if matiere with this name already exists (for new matiere only)
        if (!isEditing && matiereDAO.matiereExists(nom)) {
            showStatus("Une matière avec ce nom existe déjà.", true);
            return false;
        }
        
        return true;
    }
    
    /**
     * Add a new matiere
     */
    private void addMatiere() {
        try {
            String nom = nomField.getText().trim();
            Matiere matiere = new Matiere(nom);
            
            Matiere createdMatiere = matiereDAO.addMatiereAndReturn(matiere);
            
            if (createdMatiere != null) {
                showStatus("Matière ajoutée avec succès!", false);
                
                // Pass the newly created matiere back to the parent window
                Stage currentStage = (Stage) nomField.getScene().getWindow();
                currentStage.setUserData(createdMatiere);
                
                // Close the window after a short delay
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                pause.setOnFinished(e -> currentStage.close());
                pause.play();
            } else {
                // Fall back to old method if the new one fails
                boolean success = matiereDAO.addMatiere(matiere);
                if (success) {
                    showStatus("Matière ajoutée avec succès!", false);
                    
                    // Mark that an item was added
                    Stage currentStage = (Stage) nomField.getScene().getWindow();
                    currentStage.setUserData(Boolean.TRUE);
                    
                    // Close the window after a short delay
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                    pause.setOnFinished(e -> currentStage.close());
                    pause.play();
                } else {
                    showStatus("Erreur lors de l'ajout de la matière.", true);
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding matiere", e);
            showStatus("Erreur: " + e.getMessage(), true);
        }
    }
    
    /**
     * Update an existing matiere
     */
    private void updateMatiere() {
        try {
            String nom = nomField.getText().trim();
            
            // Check if the name is actually changed
            if (currentMatiere.getNom().equals(nom)) {
                // No changes made, just close the form
                closeForm();
                return;
            }
            
            // Check if another matiere with this name already exists
            if (matiereDAO.matiereExists(nom)) {
                showStatus("Une matière avec ce nom existe déjà.", true);
                return;
            }
            
            currentMatiere.setNom(nom);
            
            boolean success = matiereDAO.updateMatiere(currentMatiere);
            
            if (success) {
                showStatus("Matière modifiée avec succès!", false);
                
                // Pass the updated matiere back to the parent
                Stage currentStage = (Stage) nomField.getScene().getWindow();
                currentStage.setUserData(currentMatiere);
                
                // Close the window after a short delay
                javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
                pause.setOnFinished(e -> currentStage.close());
                pause.play();
            } else {
                showStatus("Erreur lors de la modification de la matière.", true);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating matiere", e);
            showStatus("Erreur: " + e.getMessage(), true);
        }
    }
    
    /**
     * Cancel form editing
     */
    @FXML
    private void handleCancel() {
        closeForm();
    }
    
    /**
     * Close the current form
     */
    private void closeForm() {
        Stage stage = (Stage) nomField.getScene().getWindow();
        if (stage != null) {
            stage.close();
        }
    }
    
    /**
     * Show a status message
     */
    private void showStatus(String message, boolean isError) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            statusLabel.setStyle("-fx-text-fill: " + (isError ? "red" : "green"));
        }
    }
} 
