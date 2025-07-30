package com.example.rendezvousmedicaux;

import javafx.beans.property.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class Appointment {
    private final IntegerProperty appointmentId;
    private final ObjectProperty<LocalDate> date;
    private final ObjectProperty<LocalTime> time;
    private final StringProperty doctorName;
    private final StringProperty patientName;
    private final StringProperty status;

    // Constructeur pour PatientDashboardController
    public Appointment(int appointmentId, LocalDate date, LocalTime time,
                       String doctorName, String status) {
        this.appointmentId = new SimpleIntegerProperty(appointmentId);
        this.date = new SimpleObjectProperty<>(date);
        this.time = new SimpleObjectProperty<>(time);
        this.doctorName = new SimpleStringProperty(doctorName);
        this.patientName = new SimpleStringProperty(null);
        this.status = new SimpleStringProperty(status);
    }

    // Constructeur pour DoctorDashboardController
    public Appointment(int appointmentId, LocalDate date, LocalTime time,
                       String patientName, String status, boolean isDoctor) {
        this.appointmentId = new SimpleIntegerProperty(appointmentId);
        this.date = new SimpleObjectProperty<>(date);
        this.time = new SimpleObjectProperty<>(time);
        this.doctorName = new SimpleStringProperty(null);
        this.patientName = new SimpleStringProperty(patientName);
        this.status = new SimpleStringProperty(status);
    }

    // Propriétés JavaFX
    public IntegerProperty appointmentIdProperty() { return appointmentId; }
    public ObjectProperty<LocalDate> dateProperty() { return date; }
    public ObjectProperty<LocalTime> timeProperty() { return time; }
    public StringProperty doctorNameProperty() { return doctorName; }
    public StringProperty patientNameProperty() { return patientName; }
    public StringProperty statusProperty() { return status; }

    // Getters standard
    public int getAppointmentId() { return appointmentId.get(); }
    public LocalDate getDate() { return date.get(); }
    public LocalTime getTime() { return time.get(); }
    public String getDoctorName() { return doctorName.get(); }
    public String getPatientName() { return patientName.get(); }
    public String getStatus() { return status.get(); }
}