package com.business.techassist.admin_utils;

public class SQL_AdminModel {
    String adminID;
    String ratings;
    String name;
    String specialized;
    int yearsExp;
    byte[] image;
    int deviceChecked;
    String schedule;
    String availability;
    String status;
    int completedJobs;

    // No-argument constructor
    public SQL_AdminModel() {
        // Default constructor with no arguments
        this.adminID = "";
        this.name = "";
        this.ratings = "0";
        this.specialized = "";
        this.yearsExp = 0;
        this.image = null;
        this.deviceChecked = 0;
        this.schedule = "";
        this.availability = "";
        this.status = "";
        this.completedJobs = 0;
    }

    public SQL_AdminModel(String adminID, String name, String ratings, String specialized, int yearsExp, byte[] image, int deviceChecked, String schedule, String availability, String status, int completedJobs) {
        this.adminID = adminID;
        this.name = name;
        this.ratings = ratings;
        this.specialized = specialized;
        this.yearsExp = yearsExp;
        this.image = image;
        this.deviceChecked = deviceChecked;
        this.schedule = schedule;
        this.availability = availability;
        this.status = status;
        this.completedJobs = completedJobs;
    }

    public int getCompletedJobs() {
        return completedJobs;
    }

    public void setCompletedJobs(int completedJobs) {
        this.completedJobs = completedJobs;
    }

    public String getAvailability() {
        return availability;
    }

    public void setAvailability(String availability) {
        this.availability = availability;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getDeviceChecked() {
        return deviceChecked;
    }

    public void setDeviceChecked(int deviceChecked) {
        this.deviceChecked = deviceChecked;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getAdminID() {
        return adminID;
    }

    public void setAdminID(String adminID) {
        this.adminID = adminID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRatings() {
        return ratings;
    }

    public byte[] getImage() {
        return image;
    }

    public void setImage(byte[] image) {
        this.image = image;
    }

    public void setRatings(String ratings) {
        this.ratings = ratings;
    }

    public String getSpecialized() {
        return specialized;
    }

    public void setSpecialized(String specialized) {
        this.specialized = specialized;
    }

    public int getYearsExp() {
        return yearsExp;
    }

    public void setYearsExp(int yearsExp) {
        this.yearsExp = yearsExp;
    }
}
