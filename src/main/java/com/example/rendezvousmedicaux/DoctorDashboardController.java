package com.example.rendezvousmedicaux;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class DoctorDashboardController {
    @FXML private Label doctorNameLabel;
    @FXML private TableView<Appointment> appointmentsTable;
    @FXML private TableColumn<Appointment, LocalDate> dateColumn;
    @FXML private TableColumn<Appointment, LocalTime> timeColumn;
    @FXML private TableColumn<Appointment, String> patientColumn;
    @FXML private TableColumn<Appointment, String> statusColumn;

    @FXML private DatePicker appointmentDateFilter;
    @FXML private ComboBox<String> statusComboBox;

    @FXML private TableView<Schedule> schedulesTable;
    @FXML private TableColumn<Schedule, String> dayColumn;
    @FXML private TableColumn<Schedule, LocalTime> startTimeColumn;
    @FXML private TableColumn<Schedule, LocalTime> endTimeColumn;

    @FXML private ComboBox<String> dayComboBox;
    @FXML private TextField startTimeField;
    @FXML private TextField endTimeField;

    private int doctorId;
    private Connection conn;
    private final Map<String, Integer> dayToNumber = new HashMap<>();
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    @FXML
    public void initialize() {
        setupDayMapping();
        setupTableColumns();
        setupStatusComboBox();
        setupDayComboBox();

        appointmentDateFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
            loadAppointments();
        });
    }

    public void initData(int userId, String fullName) {
        this.doctorId = userId;
        this.doctorNameLabel.setText("Dr. " + fullName);

        setupDatabase();
        loadAppointments();
        loadSchedules();
    }

    private void setupDatabase() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DBconnection.getConnection();
            }
        } catch (SQLException e) {
            showError("Erreur de connexion à la base de données: " + e.getMessage());
        }
    }

    private void setupDayMapping() {
        dayToNumber.put("Lundi", 1);
        dayToNumber.put("Mardi", 2);
        dayToNumber.put("Mercredi", 3);
        dayToNumber.put("Jeudi", 4);
        dayToNumber.put("Vendredi", 5);
        dayToNumber.put("Samedi", 6);
        dayToNumber.put("Dimanche", 7);
    }

    private void setupTableColumns() {
        // Configuration des colonnes de la table des rendez-vous
        dateColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getDate()));
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

        timeColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getTime()));
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

        patientColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPatientName()));
        statusColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        // Configuration des colonnes de la table des horaires
        dayColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDay()));

        startTimeColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getStartTime()));
        startTimeColumn.setCellFactory(column -> new TableCell<>() {
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

        endTimeColumn.setCellValueFactory(data -> new SimpleObjectProperty<>(data.getValue().getEndTime()));
        endTimeColumn.setCellFactory(column -> new TableCell<>() {
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
    }

    private void setupStatusComboBox() {
        statusComboBox.setItems(FXCollections.observableArrayList(
                "SCHEDULED", "COMPLETED", "CANCELLED"
        ));
    }

    private void setupDayComboBox() {
        dayComboBox.setItems(FXCollections.observableArrayList(
                "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
        ));
    }

    private void loadAppointments() {
        checkAndRestoreConnection();
        try {
            String query = """
            SELECT a.*, u.full_name as patient_name 
            FROM appointments a 
            JOIN users u ON a.patient_id = u.user_id 
            WHERE a.doctor_id = ? 
        """;

            if (appointmentDateFilter.getValue() != null) {
                query += " AND a.appointment_date = ?";
            }

            query += " ORDER BY a.appointment_date, a.appointment_time";

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, doctorId);

            if (appointmentDateFilter.getValue() != null) {
                pstmt.setDate(2, java.sql.Date.valueOf(appointmentDateFilter.getValue()));
            }

            ResultSet rs = pstmt.executeQuery();
            ObservableList<Appointment> appointments = FXCollections.observableArrayList();

            while (rs.next()) {
                appointments.add(new Appointment(
                        rs.getInt("appointment_id"),
                        rs.getDate("appointment_date").toLocalDate(),
                        rs.getTime("appointment_time").toLocalTime(),
                        rs.getString("patient_name"),
                        rs.getString("status"),
                        true  // indique que c'est pour le docteur
                ));
            }

            appointmentsTable.setItems(appointments);

            // Ajouter un message si la liste est vide
            if (appointments.isEmpty()) {
                showInfo("Aucun rendez-vous trouvé pour cette période");
            }

        } catch (SQLException e) {
            showError("Erreur lors du chargement des rendez-vous: " + e.getMessage());
        }
    }

    @FXML
    private void handleUpdateStatus() {
        Appointment selectedAppointment = appointmentsTable.getSelectionModel().getSelectedItem();
        String newStatus = statusComboBox.getValue();

        if (selectedAppointment == null) {
            showError("Veuillez sélectionner un rendez-vous");
            return;
        }

        if (newStatus == null) {
            showError("Veuillez sélectionner un nouveau statut");
            return;
        }

        try {
            String query = "UPDATE appointments SET status = ? WHERE appointment_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setString(1, newStatus);
            pstmt.setInt(2, selectedAppointment.getAppointmentId());
            pstmt.executeUpdate();

            showInfo("Statut mis à jour avec succès");
            loadAppointments();

        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour du statut: " + e.getMessage());
        }
    }

    private void loadSchedules() {
        try {
            String query = """
                SELECT * FROM doctor_schedules 
                WHERE doctor_id = ? 
                ORDER BY day_of_week
            """;

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, doctorId);
            ResultSet rs = pstmt.executeQuery();

            ObservableList<Schedule> schedules = FXCollections.observableArrayList();

            while (rs.next()) {
                String day = dayToNumber.entrySet().stream()
                        .filter(entry -> {
                            try {
                                return entry.getValue() == rs.getInt("day_of_week");
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .findFirst()
                        .map(Map.Entry::getKey)
                        .orElse("Inconnu");

                schedules.add(new Schedule(
                        rs.getInt("schedule_id"),
                        day,
                        rs.getTime("start_time").toLocalTime(),
                        rs.getTime("end_time").toLocalTime()
                ));
            }

            schedulesTable.setItems(schedules);

        } catch (SQLException e) {
            showError("Erreur lors du chargement des horaires: " + e.getMessage());
        }
    }

    @FXML
    private void handleAddSchedule() {
        if (!validateScheduleInput()) return;

        try {
            String query = """
                INSERT INTO doctor_schedules (doctor_id, day_of_week, start_time, end_time) 
                VALUES (?, ?, ?, ?)
            """;

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, doctorId);
            pstmt.setInt(2, dayToNumber.get(dayComboBox.getValue()));
            pstmt.setTime(3, Time.valueOf(LocalTime.parse(startTimeField.getText(), timeFormatter)));
            pstmt.setTime(4, Time.valueOf(LocalTime.parse(endTimeField.getText(), timeFormatter)));

            pstmt.executeUpdate();

            showInfo("Horaire ajouté avec succès");
            clearScheduleFields();
            loadSchedules();

        } catch (SQLException e) {
            showError("Erreur lors de l'ajout de l'horaire: " + e.getMessage());
        }
    }

    private boolean hasAppointmentsOnDay(int dayOfWeek) {
        try {
            String query = """
            SELECT a.* 
            FROM appointments a 
            WHERE a.doctor_id = ? 
            AND EXTRACT(DOW FROM appointment_date) = ? 
            AND a.status != 'CANCELLED'
            LIMIT 1
        """;

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, doctorId);

            int pgDayOfWeek = (dayOfWeek % 7);
            pstmt.setInt(2, pgDayOfWeek);

            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            showError("Erreur lors de la vérification des rendez-vous: " + e.getMessage());
            return true;
        }
    }

    @FXML
    private void handleUpdateSchedule() {
        Schedule selectedSchedule = schedulesTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null) {
            showError("Veuillez sélectionner un horaire à modifier");
            return;
        }

        if (!validateScheduleInput()) return;

        int selectedDayNumber = dayToNumber.get(dayComboBox.getValue());
        if (hasAppointmentsOnDay(selectedDayNumber)) {
            showError("Impossible de modifier les horaires car il existe des rendez-vous programmés pour ce jour");
            return;
        }

        try {
            String query = """
                UPDATE doctor_schedules 
                SET day_of_week = ?, start_time = ?, end_time = ? 
                WHERE schedule_id = ?
            """;

            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, selectedDayNumber);
            pstmt.setTime(2, Time.valueOf(LocalTime.parse(startTimeField.getText(), timeFormatter)));
            pstmt.setTime(3, Time.valueOf(LocalTime.parse(endTimeField.getText(), timeFormatter)));
            pstmt.setInt(4, selectedSchedule.getScheduleId());

            pstmt.executeUpdate();

            showInfo("Horaire mis à jour avec succès");
            clearScheduleFields();
            loadSchedules();

        } catch (SQLException e) {
            showError("Erreur lors de la mise à jour de l'horaire: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteSchedule() {
        Schedule selectedSchedule = schedulesTable.getSelectionModel().getSelectedItem();
        if (selectedSchedule == null) {
            showError("Veuillez sélectionner un horaire à supprimer");
            return;
        }

        // Récupérer le jour de la semaine de l'horaire sélectionné
        int dayNumber = dayToNumber.get(selectedSchedule.getDay());

        // Vérifier s'il y a des rendez-vous programmés ce jour-là
        if (hasAppointmentsOnDay(dayNumber)) {
            showError("Impossible de supprimer l'horaire car il existe des rendez-vous programmés pour ce jour");
            return;
        }

        try {
            String query = "DELETE FROM doctor_schedules WHERE schedule_id = ?";
            PreparedStatement pstmt = conn.prepareStatement(query);
            pstmt.setInt(1, selectedSchedule.getScheduleId());
            pstmt.executeUpdate();

            showInfo("Horaire supprimé avec succès");
            clearScheduleFields();
            loadSchedules();

        } catch (SQLException e) {
            showError("Erreur lors de la suppression de l'horaire: " + e.getMessage());
        }
    }

    private boolean validateScheduleInput() {
        if (dayComboBox.getValue() == null) {
            showError("Veuillez sélectionner un jour");
            return false;
        }

        try {
            LocalTime startTime = LocalTime.parse(startTimeField.getText(), timeFormatter);
            LocalTime endTime = LocalTime.parse(endTimeField.getText(), timeFormatter);

            if (endTime.isBefore(startTime)) {
                showError("L'heure de fin doit être après l'heure de début");
                return false;
            }
        } catch (DateTimeException e) {
            showError("Format d'heure invalide. Utilisez le format HH:mm");
            return false;
        }

        return true;
    }

    private void clearScheduleFields() {
        dayComboBox.setValue(null);
        startTimeField.clear();
        endTimeField.clear();
    }

    @FXML
    private void handleLogout() {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }

            // Charger la page de connexion avec le bon chemin
            FXMLLoader loader = new FXMLLoader(getClass().getResource("Login.fxml"));
            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) doctorNameLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            showError("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void checkAndRestoreConnection() {
        try {
            if (conn == null || conn.isClosed()) {
                conn = DBconnection.getConnection();
            }
        } catch (SQLException e) {
            showError("Erreur de connexion à la base de données: " + e.getMessage());
        }
    }
}
