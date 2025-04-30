package models;

import java.time.LocalDateTime;

public class Exercice {
    private final int id;
    private String titre; // Made mutable for updates
    private String description; // Made mutable for updates
    private final LocalDateTime dateCreation;
    private final int matiereId;
    private final int createurId;
    private Utilisateur createur; // Reference to the creator
    private String matiereNom; // Name of the matiere/subject

    public Exercice(int id, String titre, String description,
                    LocalDateTime dateCreation, int matiereId, int createurId) {
        this.id = id;
        this.titre = titre;
        this.description = description;
        this.dateCreation = dateCreation;
        this.matiereId = matiereId;
        this.createurId = createurId;
        this.createur = null; // To be set later
        this.matiereNom = ""; // To be set later
    }

    // Getters
    public int getId() { return id; }
    public String getTitre() { return titre; }
    public String getDescription() { return description; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public int getMatiereId() { return matiereId; }
    public int getCreateurId() { return createurId; }
    public Utilisateur getCreateur() { return createur; }
    public String getMatiereNom() { return matiereNom; }

    // Setters
    public void setTitre(String titre) { this.titre = titre; }
    public void setDescription(String description) { this.description = description; }
    public void setCreateur(Utilisateur createur) { this.createur = createur; }
    public void setMatiereNom(String matiereNom) { this.matiereNom = matiereNom; }
}
