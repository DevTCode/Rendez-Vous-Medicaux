package com.example.rendezvousmedicaux;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import java.sql.*;
import java.io.IOException;
import java.util.regex.Pattern;

public class LoginController {
    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private Button loginButton;

    private Connection cnx;

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
    );

    public void initialize() {
        try {
            cnx = DBconnection.getConnection();
        } catch (SQLException e) {
            showError("Erreur de connexion à la base de données: " + e.getMessage());
        }

        // Ajouter les listeners pour la validation en temps réel
        emailField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateInput();
        });

        passwordField.textProperty().addListener((observable, oldValue, newValue) -> {
            validateInput();
        });
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validation des champs
        if (!validateFields()) {
            return;
        }

        try {
            // Requête SQL pour vérifier les identifiants et récupérer le rôle
            String query = """
                SELECT user_id, role, full_name 
                FROM users 
                WHERE email = ? AND password = ?
            """;

            PreparedStatement pstmt = cnx.prepareStatement(query);
            pstmt.setString(1, email);
            pstmt.setString(2, password);  // Dans un cas réel, utiliser un hachage du mot de passe

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                int userId = rs.getInt("user_id");
                String role = rs.getString("role");
                String fullName = rs.getString("full_name");

                // Rediriger vers la bonne interface selon le rôle
                redirectToUserDashboard(userId, role, fullName);
            } else {
                showError("Email ou mot de passe incorrect");
            }

        } catch (SQLException e) {
            showError("Erreur lors de la connexion: " + e.getMessage());
        }
    }

    private boolean validateFields() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Vérification des champs vides
        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return false;
        }

        // Validation du format de l'email
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            showError("Format d'email invalide");
            return false;
        }

        // Validation de la longueur du mot de passe
        if (password.length() < 3) {  // Dans un cas réel, exiger un mot de passe plus long
            showError("Le mot de passe doit contenir au moins 3 caractères");
            return false;
        }

        clearError();
        return true;
    }

    private void validateInput() {
        loginButton.setDisable(
                emailField.getText().trim().isEmpty() ||
                        passwordField.getText().isEmpty()
        );
    }

    private void redirectToUserDashboard(int userId, String role, String fullName) {
        try {
            String fxmlFile;
            Object controller;

            // Déterminer quel fichier FXML charger selon le rôle
            FXMLLoader loader;
            AnchorPane dashboardRoot;

            if (role.equals("patient")) {
                fxmlFile = "PatientDashboard.fxml";
                loader = new FXMLLoader(getClass().getResource(fxmlFile));
                dashboardRoot = loader.load();

                PatientDashboardController patientController = loader.getController();
                patientController.initData(userId, fullName);  // Initialiser les données du patient

                controller = patientController;
            } else if (role.equals("medecin")) {
                fxmlFile = "DoctorDashboard.fxml";
                loader = new FXMLLoader(getClass().getResource(fxmlFile));
                dashboardRoot = loader.load();

                DoctorDashboardController doctorController = loader.getController();
                doctorController.initData(userId, fullName);  // Initialiser les données du médecin

                controller = doctorController;
            } else {
                showError("Rôle non reconnu");
                return;
            }

            // Obtenir la fenêtre actuelle
            Stage currentStage = (Stage) loginButton.getScene().getWindow();

            // Créer et afficher la nouvelle scène
            Scene scene = new Scene(dashboardRoot);
            currentStage.setScene(scene);
            currentStage.setTitle("Dashboard - " + fullName);
            currentStage.setResizable(true);
            currentStage.show();

        } catch (IOException e) {
            showError("Erreur lors du chargement de l'interface: " + e.getMessage());
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: red;");
    }

    private void clearError() {
        errorLabel.setText("");
    }

    public void closeConnection() {
        try {
            if (cnx != null && !cnx.isClosed()) {
                cnx.close();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
        }
    }
    }
