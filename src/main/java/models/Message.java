
package models;

import java.time.LocalDateTime;

public class Message {
    private final int id;
    private final String contenu;
    private final LocalDateTime dateEnvoi;
    private final int exerciceId;
    private final int auteurId;
    private String auteurNom;

    public Message(int id, String contenu, LocalDateTime dateEnvoi,
                   int exerciceId, int auteurId) {
        this.id = id;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
        this.exerciceId = exerciceId;
        this.auteurId = auteurId;
    }

    // Getters
    public int getId() { return id; }
    public String getContenu() { return contenu; }
    public LocalDateTime getDateEnvoi() { return dateEnvoi; }
    public int getExerciceId() { return exerciceId; }
    public int getAuteurId() { return auteurId; }
    public String getAuteurNom() { return auteurNom; }

    // Setter
    public void setAuteurNom(String nom) {
        this.auteurNom = nom;
    }

    public String formatPourChat() {
        return String.format("[%s] %s: %s",
                dateEnvoi.toLocalTime(), auteurNom, contenu);
    }
}