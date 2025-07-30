-- Table des utilisateurs (patients et médecins) 
CREATE TABLE users (
    user_id SERIAL PRIMARY KEY,
    email VARCHAR(100) UNIQUE NOT NULL,
    password VARCHAR(100) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('medecin', 'patient')),
    phone VARCHAR(20)
);

-- Insertion des médecins
INSERT INTO users (email, password, full_name, role, phone) VALUES
('medecin1@gmail.com', '123', 'SOUKRATI', 'medecin', '0611223344'),
('medecin2@email.com', '456', 'ASSIM', 'medecin', '0622334455');

-- Insertion des patients
INSERT INTO users (email, password, full_name, role, phone) VALUES
('patient1@gmail.com', '123', 'Sajda Jinane', 'patient', '0644556677'),
('patient2@gmail.com', '456', 'Ichraq', 'patient', '0655667788');

-- Table des horaires disponibles des médecins
CREATE TABLE doctor_schedules (
    schedule_id SERIAL PRIMARY KEY,
    doctor_id INTEGER REFERENCES users(user_id),
    day_of_week INTEGER CHECK (day_of_week BETWEEN 1 AND 7),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL
);

-- Table des rendez-vous
CREATE TABLE appointments (
    appointment_id SERIAL PRIMARY KEY,
    patient_id INTEGER REFERENCES users(user_id),
    doctor_id INTEGER REFERENCES users(user_id),
    appointment_date DATE NOT NULL,
    appointment_time TIME NOT NULL,
    status VARCHAR(20) CHECK (status IN ('SCHEDULED', 'COMPLETED', 'CANCELLED')),
    reminder_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);



--Test des requêtes:
select * from public."users";

select * from public."doctor_schedules";

select * from public."appointments"

--LoadPatientInfo():
SELECT full_name FROM users WHERE user_id = 4 AND role = 'patient'

--LoadAppointments():
SELECT a.*, u.full_name as doctor_name FROM appointments a JOIN users u ON a.doctor_id = u.user_id 
WHERE a.patient_id = 4 ORDER BY a.appointment_date, a.appointment_time

--HandleCancelAppointment():
UPDATE appointments SET status = 'CANCELLED' WHERE appointment_id = 1

--LoadDoctors():
SELECT user_id, full_name FROM users WHERE role = 'medecin'

--HandleDataSelection():
SELECT start_time, end_time FROM doctor_schedules WHERE doctor_id = ? AND day_of_week = ?
SELECT appointment_time FROM appointments  WHERE doctor_id = ? AND appointment_date = ? AND status != 'CANCELLED'

--HandleBookAppointment():
INSERT INTO appointments (patient_id, doctor_id, appointment_date, appointment_time, status, created_at) 
VALUES (?, ?, ?, ?, 'SCHEDULED', CURRENT_TIMESTAMP)

--LoadReminders():
SELECT a.appointment_date, a.appointment_time, u.full_name as doctor_name FROM appointments a 
JOIN users u ON a.doctor_id = u.user_id 
WHERE a.patient_id = ? AND a.status = 'SCHEDULED' AND a.appointment_date >= CURRENT_DATE 
ORDER BY a.appointment_date, a.appointment_time LIMIT 5

