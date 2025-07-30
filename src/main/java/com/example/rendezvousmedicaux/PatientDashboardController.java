package com.example.rendezvousmedicaux;
import javafx.beans.property.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class PatientDashboardController {
    @FXML private Label patientNameLabel;
    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, LocalDate> dateColumn;
    @FXML private TableColumn<Appointment, LocalTime> timeColumn;
    @FXML private TableColumn<Appointment, String> doctorColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;

    @FXML private ComboBox<Doctor> doctorComboBox;
    @FXML private DatePicker appointmentDatePicker;
    @FXML private TextField appointmentTimeField;  // Nouveau TextField pour l'heure
    @FXML private Label bookingErrorLabel;
    @FXML private ListView<String> remindersList;

    private int patientId;
    private Connection cnx;
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public void initData(int userId, String fullName) {
        try {
            if (cnx == null || cnx.isClosed()) {
                cnx = DBconnection.getConnection();
            }
            this.patientId = userId;
            this.patientNameLabel.setText(fullName);

            // Recharger toutes les données nécessaires
            loadAppointments();
            loadDoctors();
            loadReminders();

            // Afficher le popup des rendez-vous à venir
            showUpcomingAppointmentsPopup();

        } catch (SQLException e) {
            showError("Erreur de connexion à la base de données: " + e.getMessage());
        }
    }

    @FXML
    public void initialize() {
        // Configuration des colonnes
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("time"));
        doctorColumn.setCellValueFactory(new PropertyValueFactory<>("doctorName"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Ajout des CellFactory pour le formatage des dates et heures
        dateColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                if (empty || date == null) {
                    setText(null);
                } else {
                    setText(date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });

        timeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(LocalTime time, boolean empty) {
                super.updateItem(time, empty);
                if (empty || time == null) {
                    setText(null);
                } else {
                    setText(time.format(timeFormatter));
                }
            }
        });

        // Configuration des listeners
        doctorComboBox.setOnAction(e -> handleDoctorSelection());
        appointmentDatePicker.setOnAction(e -> handleDateSelection());
    }

    private void loadPatientInfo() {
        try {
            String query = "SELECT full_name FROM users WHERE user_id = ? AND role = 'patient'";
            PreparedStatement pstmt = cnx.prepareStatement(query);
            pstmt.setInt(1, patientId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                patientNameLabel.setText(rs.getString("full_name"));
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des informations patient: " + e.getMessage());
        }
    }

    private void loadAppointments() {
        checkAndRestoreConnection();
        try {
            String query = """
                SELECT a.*, u.full_name as doctor_name 
                FROM appointments a 
                JOIN users u ON a.doctor_id = u.user_id 
                WHERE a.patient_id = ? 
                ORDER BY a.appointment_date, a.appointment_time
            """;

            PreparedStatement pstmt = cnx.prepareStatement(query);
            pstmt.setInt(1, patientId);
            ResultSet rs = pstmt.executeQuery();

            ObservableList<Appointment> appointments = FXCollections.observableArrayList();
            while (rs.next()) {
                appointments.add(new Appointment(
                        rs.getInt("appointment_id"),
                        rs.getDate("appointment_date").toLocalDate(),
                        rs.getTime("appointment_time").toLocalTime(),
                        rs.getString("doctor_name"),
                        rs.getString("status")
                ));
            }
            appointmentsTable.setItems(appointments);
        } catch (SQLException e) {
            showError("Erreur lors du chargement des rendez-vous: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelAppointment() {
        checkAndRestoreConnection();
        Appointment selectedAppointment = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selectedAppointment == null) {
            showError("Veuillez sélectionner un rendez-vous à annuler");
            return;
        }

        try {
            String query = "UPDATE appointments SET status = 'CANCELLED' WHERE appointment_id = ?";
            PreparedStatement pstmt = cnx.prepareStatement(query);
            pstmt.setInt(1, selectedAppointment.getAppointmentId());
            pstmt.executeUpdate();

            loadAppointments();
            showInfo("Le rendez-vous a été annulé avec succès");
        } catch (SQLException e) {
            showError("Erreur lors de l'annulation du rendez-vous: " + e.getMessage());
        }
    }

    private void loadDoctors() {
        checkAndRestoreConnection();
        try {
            String query = "SELECT user_id, full_name FROM users WHERE role = 'medecin'";
            PreparedStatement pstmt = cnx.prepareStatement(query);
            ResultSet rs = pstmt.executeQuery();

            ObservableList<Doctor> doctors = FXCollections.observableArrayList();
            while (rs.next()) {
                doctors.add(new Doctor(
                        rs.getInt("user_id"),
                        rs.getString("full_name")
                ));
            }
            doctorComboBox.setItems(doctors);
        } catch (SQLException e) {
            showError("Erreur lors du chargement des médecins: " + e.getMessage());
        }
    }

    private void handleDoctorSelection() {
        checkAndRestoreConnection();
        // Reset des champs date et heure
        appointmentDatePicker.setValue(null);
        appointmentTimeField.clear();
        appointmentTimeField.setDisable(false); // Réactiver le champ
        appointmentTimeField.setPromptText("Saisir l'heure (HH:mm)");

        // Configurer les restrictions de date
        appointmentDatePicker.setDayCellFactory(dp -> new DateCell() {
            @Override
            public void updateItem(LocalDate date, boolean empty) {
                super.updateItem(date, empty);
                setDisable(empty || date.compareTo(LocalDate.now()) < 0);
            }
        });
    }

    private void handleDateSelection() {
        checkAndRestoreConnection();
        Doctor selectedDoctor = doctorComboBox.getValue();
        LocalDate selectedDate = appointmentDatePicker.getValue();

        if (selectedDoctor == null || selectedDate == null) {
            return;
        }

        try {
            String query = """
            SELECT start_time, end_time 
            FROM doctor_schedules 
            WHERE doctor_id = ? AND day_of_week = ?
        """;

            PreparedStatement pstmt = cnx.prepareStatement(query);
            pstmt.setInt(1, selectedDoctor.getId());
            pstmt.setInt(2, selectedDate.getDayOfWeek().getValue());
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                LocalTime startTime = rs.getTime("start_time").toLocalTime();
                LocalTime endTime = rs.getTime("end_time").toLocalTime();

                // Réactiver le champ et mettre à jour le placeholder
                appointmentTimeField.setDisable(false);
                appointmentTimeField.setPromptText(String.format("Entre %s et %s",
                        startTime.format(timeFormatter),
                        endTime.format(timeFormatter)));
            } else {
                appointmentTimeField.setDisable(true);
                appointmentTimeField.clear();
                appointmentTimeField.setPromptText("Aucun horaire disponible ce jour");
            }
        } catch (SQLException e) {
            showError("Erreur lors du chargement des horaires: " + e.getMessage());
            appointmentTimeField.setDisable(true);
            appointmentTimeField.clear();
        }
    }

    @FXML
    private void handleBookAppointment() {
        checkAndRestoreConnection();
        // Récupération des valeurs
        Doctor selectedDoctor = doctorComboBox.getValue();
        LocalDate selectedDate = appointmentDatePicker.getValue();
        String timeStr = appointmentTimeField.getText().trim();

        // Validation des champs
        if (selectedDoctor == null || selectedDate == null || timeStr.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        // Validation du format de l'heure
        LocalTime selectedTime;
        try {
            selectedTime = LocalTime.parse(timeStr, timeFormatter);
        } catch (DateTimeParseException e) {
            showError("Format d'heure invalide. Utilisez le format HH:mm");
            return;
        }

        try {
            // Vérifier les horaires du médecin
            String scheduleQuery = """
                SELECT start_time, end_time 
                FROM doctor_schedules 
                WHERE doctor_id = ? AND day_of_week = ?
            """;

            PreparedStatement scheduleStmt = cnx.prepareStatement(scheduleQuery);
            scheduleStmt.setInt(1, selectedDoctor.getId());
            scheduleStmt.setInt(2, selectedDate.getDayOfWeek().getValue());
            ResultSet scheduleRs = scheduleStmt.executeQuery();

            if (scheduleRs.next()) {
                LocalTime startTime = scheduleRs.getTime("start_time").toLocalTime();
                LocalTime endTime = scheduleRs.getTime("end_time").toLocalTime();

                // Vérifier que l'heure est dans la plage horaire du médecin
                if (selectedTime.isBefore(startTime) || selectedTime.isAfter(endTime)) {
                    showError("L'heure doit être entre " + startTime.format(timeFormatter) +
                            " et " + endTime.format(timeFormatter));
                    return;
                }

                // Vérifier si le créneau est déjà pris
                String checkQuery = """
                    SELECT COUNT(*) 
                    FROM appointments 
                    WHERE doctor_id = ? 
                    AND appointment_date = ? 
                    AND appointment_time = ? 
                    AND status != 'CANCELLED'
                """;

                PreparedStatement checkStmt = cnx.prepareStatement(checkQuery);
                checkStmt.setInt(1, selectedDoctor.getId());
                checkStmt.setDate(2, java.sql.Date.valueOf(selectedDate));
                checkStmt.setTime(3, java.sql.Time.valueOf(selectedTime));
                ResultSet checkRs = checkStmt.executeQuery();

                if (checkRs.next() && checkRs.getInt(1) > 0) {
                    showError("Ce créneau est déjà pris");
                    return;
                }

                // Créer le rendez-vous
                String insertQuery = """
                    INSERT INTO appointments (patient_id, doctor_id, appointment_date, 
                                           appointment_time, status, created_at) 
                    VALUES (?, ?, ?, ?, 'SCHEDULED', CURRENT_TIMESTAMP)
                """;

                PreparedStatement insertStmt = cnx.prepareStatement(insertQuery);
                insertStmt.setInt(1, patientId);
                insertStmt.setInt(2, selectedDoctor.getId());
                insertStmt.setDate(3, java.sql.Date.valueOf(selectedDate));
                insertStmt.setTime(4, java.sql.Time.valueOf(selectedTime));
                insertStmt.executeUpdate();

                // Recharger les données
                loadAppointments();
                loadReminders();

                // Réinitialiser les champs
                doctorComboBox.setValue(null);
                appointmentDatePicker.setValue(null);
                appointmentTimeField.clear();

                showInfo("Le rendez-vous a été pris avec succès");
            } else {
                showError("Le médecin n'a pas d'horaires définis pour ce jour");
            }
        } catch (SQLException e) {
            showError("Erreur lors de la prise de rendez-vous: " + e.getMessage());
        }
    }

    private void loadReminders() {
        checkAndRestoreConnection();
        try {
            String query = """
                SELECT a.appointment_date, a.appointment_time, u.full_name as doctor_name 
                FROM appointments a 
                JOIN users u ON a.doctor_id = u.user_id 
                WHERE a.patient_id = ? AND a.status = 'SCHEDULED' 
                AND a.appointment_date >= CURRENT_DATE 
                ORDER BY a.appointment_date, a.appointment_time 
                LIMIT 5
            """;

            PreparedStatement pstmt = cnx.prepareStatement(query);
            pstmt.setInt(1, patientId);
            ResultSet rs = pstmt.executeQuery();

            ObservableList<String> reminders = FXCollections.observableArrayList();
            while (rs.next()) {
                LocalDate date = rs.getDate("appointment_date").toLocalDate();
                LocalTime time = rs.getTime("appointment_time").toLocalTime();
                String doctorName = rs.getString("doctor_name");

                reminders.add(String.format(
                        "Rendez-vous le %s à %s avec Dr. %s",
                        date.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")),
                        doctorName
                ));
            }
            remindersList.setItems(reminders);
        } catch (SQLException e) {
            showError("Erreur lors du chargement des rappels: " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout() {
        checkAndRestoreConnection();
        try {
            if (cnx != null && !cnx.isClosed()) {
                cnx.close();
            }

            // Charger la page de connexion
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/rendezvousmedicaux/login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) patientNameLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            showError("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    private void showError(String message) {
        bookingErrorLabel.setText(message);
        bookingErrorLabel.setStyle("-fx-text-fill: red;");
    }

    private void showInfo(String message) {
        bookingErrorLabel.setText(message);
        bookingErrorLabel.setStyle("-fx-text-fill: green;");
    }

    private void checkAndRestoreConnection() {
        try {
            if (cnx == null || cnx.isClosed()) {
                cnx = DBconnection.getConnection();
            }
        } catch (SQLException e) {
            showError("Erreur de connexion à la base de données: " + e.getMessage());
        }
    }

    private void showUpcomingAppointmentsPopup() {
        try {
            String query = """
            SELECT a.appointment_date, a.appointment_time, u.full_name as doctor_name 
            FROM appointments a 
            JOIN users u ON a.doctor_id = u.user_id 
            WHERE a.patient_id = ? 
            AND a.status = 'SCHEDULED' 
            AND a.appointment_date >= CURRENT_DATE 
            AND a.appointment_date <= CURRENT_DATE + INTERVAL '7 days'
            ORDER BY a.appointment_date, a.appointment_time
        """;

            PreparedStatement pstmt = cnx.prepareStatement(query);
            pstmt.setInt(1, patientId);
            ResultSet rs = pstmt.executeQuery();

            // Créer la fenêtre pop-up
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Rappel de vos rendez-vous à venir");

            VBox content = new VBox(10);
            content.setPadding(new Insets(20));
            content.setAlignment(Pos.CENTER);

            Label titleLabel = new Label("Vos prochains rendez-vous");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
            content.getChildren().add(titleLabel);

            boolean hasAppointments = false;
            while (rs.next()) {
                hasAppointments = true;
                LocalDate date = rs.getDate("appointment_date").toLocalDate();
                LocalTime time = rs.getTime("appointment_time").toLocalTime();
                String doctorName = rs.getString("doctor_name");

                Label appointmentLabel = new Label(String.format(
                        "Le %s à %s avec Dr. %s",
                        date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                        time.format(DateTimeFormatter.ofPattern("HH:mm")),
                        doctorName
                ));
                appointmentLabel.setStyle("-fx-text-fill: #2c3e50;");
                content.getChildren().add(appointmentLabel);
            }

            if (!hasAppointments) {
                Label noAppointmentLabel = new Label("Aucun rendez-vous prévu pour la semaine à venir");
                noAppointmentLabel.setStyle("-fx-text-fill: #7f8c8d;");
                content.getChildren().add(noAppointmentLabel);
            }

            Button closeButton = new Button("Fermer");
            closeButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
            closeButton.setOnAction(e -> popupStage.close());
            content.getChildren().add(closeButton);

            Scene scene = new Scene(content);
            popupStage.setScene(scene);
            popupStage.show();

        } catch (SQLException e) {
            showError("Erreur lors du chargement des rappels: " + e.getMessage());
        }
    }
}

// Classes utilitaires
class Doctor {
    private final int id;
    private final String name;

    public Doctor(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() { return id; }
    public String getName() { return name; }

    @Override
    public String toString() {
        return name;
    }
}

