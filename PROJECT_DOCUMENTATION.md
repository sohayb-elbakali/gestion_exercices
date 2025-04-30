# Gestion Exercice - Project Documentation

## Table of Contents
- [Overview](#overview)
- [Project Structure](#project-structure)
- [Models](#models)
- [Data Access Objects (DAO)](#data-access-objects-dao)
- [Controllers](#controllers)
- [Utilities](#utilities)
- [Resources (FXML, CSS, Images)](#resources-fxml-css-images)

---

## Overview
`gestion_exercice` is a JavaFX-based application for creating, solving, and managing exercises. It follows an MVC pattern:
- **Models:** Data entities representing domain objects.
- **DAOs:** Classes handling database interactions (CRUD operations).
- **Controllers:** JavaFX controllers managing UI logic and user interactions.
- **Utilities:** Shared helper classes (e.g., database connection).

---

## Project Structure

```
gestion_exercice/
├─ src/main/java/
│  ├─ models/         # Domain entities
│  ├─ dao/            # Data Access Objects
│  ├─ controllers/    # JavaFX controllers for UI
│  └─ utils/          # Utility classes (e.g., DB connection)
├─ src/main/resources/
│  ├─ fxml/           # FXML view definitions
│  ├─ css/            # Stylesheets
│  └─ images/         # Static images
└─ PROJECT_DOCUMENTATION.md  # This documentation
```

---

## Models
Each model class represents a database table or domain concept:

### `Utilisateur.java`
- Fields: `id`, `email`, `motDePasse`, `role`, `nom`, `exercices` (list of created exercises)
- Constructors: With and without `id` for easy insertion
- Methods:
  - Getters/Setters for all fields
  - `addExercice(Exercice exercice)`: Associate an exercise

### `Exercice.java`
- Fields: `id`, `titre`, `description`, `dateCreation`, `matiereId`, `createurId`, `createur`, `matiereNom`
- Methods:
  - Getters for all fields
  - Setters for mutable properties (`titre`, `description`, `createur`, `matiereNom`)

### `Solution.java`
- Fields: `id`, `contenu`, `dateCreation`, `exerciceId`, `auteurId`, `auteurNom`
- Methods:
  - Getters/Setters
  - `toString()`: Formats as `auteurNom - date`

---

## Data Access Objects (DAO)
DAOs handle SQL queries and map result sets to model objects.

### `UtilisateurDAO.java`
- Methods:
  - `findByEmailAndPasswordAndRole(...)`: Authenticate user
  - `getById(int id)`: Retrieve user by ID
  - `addUtilisateur(Utilisateur user)`: Insert new user (with fallback if `nom` column missing)
  - `userExists(String email)`: Check duplicate email
  - `getAllUsers()`: List all users (admin view)
  - `deleteUser(int id)`: Remove user by ID

### `ExerciceDAO.java`
- Methods:
  - `getExercicesByMatiere(int matiereId)`
  - `getExercicesByCreateur(int createurId)`
  - `getAllExercices()`
  - `getExerciceById(int id)`
  - `addExercice(Exercice exercice)` / `addExerciceAndReturn(...)`
  - `updateExercice(Exercice exercice)`
  - `deleteExercice(int id)`: Also cascades delete to associated solutions within a transaction

### `SolutionDAO.java`
- Methods:
  - `getSolutionsByExercice(int exerciceId)`
  - `getSolutionsByAuteur(int auteurId)`
  - `getSolutionsByCreateur(int createurId)`
  - `getSolutionById(int id)`
  - `addSolution(Solution solution)` / `addSolutionAndReturn(...)`
  - `updateSolution(Solution solution)`
  - `deleteSolution(int id)`

---

## Controllers
Controllers bind UI components to application logic. Each controller corresponds to an FXML view.

### `RegisterController.java`
- Manages user registration form
- Validates inputs, checks for existing email, calls `UtilisateurDAO.addUtilisateur(...)`

### `UtilisateurController.java`
- Displays current user info and manages login/logout

### `UserManagementController.java`
- Admin-only view to list, view, add, and delete users
- Uses `UtilisateurDAO.getAllUsers()`, `deleteUser(...)` and opens `RegisterController` form

### `MatiereController.java`
- CRUD operations for subjects (matières)

### `ExerciceController.java`
- Displays exercises by subject or creator
- Opens editor dialogs for create/edit/delete exercises

### `SolutionController.java`
- Lists and manages solutions for a given exercise
- Allows adding, editing, and deleting solutions

---

## Utilities

### `DatabaseConnection.java`
- Provides a singleton method `getConnection()` to obtain a JDBC `Connection`.
- Reads configuration (URL, credentials) from properties or environment.

---

## Resources (FXML, CSS, Images)
- FXML files define view layouts for each controller.
- Stylesheets (`css/`) apply consistent styling.
- Images (`images/`) used in the UI (icons, logos).

---

*End of Documentation* 
