package models;

import java.util.ArrayList;
import java.util.List;

public class Utilisateur {
    private int id;
    private String email;
    private String motDePasse;
    private String role;
    private List<Exercice> exercices; // List of exercises created by this user

    public Utilisateur(int id, String email, String motDePasse, String role) {
        this.id = id;
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.exercices = new ArrayList<>();
    }

    // Constructeur sans ID (utile pour insertion)
    public Utilisateur(String email, String motDePasse, String role) {
        this.email = email;
        this.motDePasse = motDePasse;
        this.role = role;
        this.exercices = new ArrayList<>();
    }

    // Getters et setters
    public int getId() { return id; }
    public String getEmail() { return email; }
    public String getMotDePasse() { return motDePasse; }
    public String getRole() { return role; }
    public List<Exercice> getExercices() { return exercices; }

    public void setId(int id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public void setRole(String role) { this.role = role; }
    public void setExercices(List<Exercice> exercices) { this.exercices = exercices; }

    // Add an exercise to the user's list
    public void addExercice(Exercice exercice) {
        this.exercices.add(exercice);
    }
}