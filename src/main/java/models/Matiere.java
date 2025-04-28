package models;

public class Matiere {
    private int id;
    private String nom;

    public Matiere(int id, String nom) {
        this.id = id;
        this.nom = nom;
    }

    public Matiere(String nom) {
        this.nom = nom;
    }

    // Getters et setters
    public int getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    @Override
    public String toString() {
        return nom;
    }
}

