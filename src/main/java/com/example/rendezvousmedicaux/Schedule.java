package com.example.rendezvousmedicaux;

import java.time.LocalTime;

public class Schedule {
    private int scheduleId;
    private String day;
    private LocalTime startTime;
    private LocalTime endTime;

    public Schedule(int scheduleId, String day, LocalTime startTime, LocalTime endTime) {
        this.scheduleId = scheduleId;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    // Getters and Setters
    public int getScheduleId() { return scheduleId; }
    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public LocalTime getEndTime() { return endTime; }
    public void setEndTime(LocalTime endTime) { this.endTime = endTime; }
}
