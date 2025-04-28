package models;

import java.time.LocalDateTime;

public class Solution {
    private final int id;
    private final String contenu;
    private final LocalDateTime dateCreation;
    private final int exerciceId;
    private final int auteurId;
    private String auteurNom; // Pour l'affichage seulement

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

    // Setter pour auteurNom
    public void setAuteurNom(String nom) {
        this.auteurNom = nom;
    }

    @Override
    public String toString() {
        return String.format("%s - %s", auteurNom, dateCreation.toLocalDate());
    }
}