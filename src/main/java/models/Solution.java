package models;

import java.time.LocalDateTime;

public class Solution {
    private int id;
    private String contenu;
    private LocalDateTime dateCreation;
    private int exerciceId;
    private int auteurId;
    private String auteurNom; // Pour l'affichage seulement

    public Solution() {
        this.id = 0;
        this.contenu = "";
        this.dateCreation = LocalDateTime.now();
        this.exerciceId = 0;
        this.auteurId = 0;
    }

    public Solution(int id, String contenu, LocalDateTime dateCreation,
                    int exerciceId, int auteurId) {
        this.id = id;
        this.contenu = contenu;
        this.dateCreation = dateCreation;
        this.exerciceId = exerciceId;
        this.auteurId = auteurId;
    }

    // Getters
    public int getId() { return id; }
    public String getContenu() { return contenu; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public int getExerciceId() { return exerciceId; }
    public int getAuteurId() { return auteurId; }
    public String getAuteurNom() { return auteurNom; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setContenu(String contenu) { this.contenu = contenu; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public void setExerciceId(int exerciceId) { this.exerciceId = exerciceId; }
    public void setAuteurId(int auteurId) { this.auteurId = auteurId; }
    public void setAuteurNom(String nom) { this.auteurNom = nom; }

    @Override
    public String toString() {
        return String.format("%s - %s", auteurNom, dateCreation.toLocalDate());
    }
}
