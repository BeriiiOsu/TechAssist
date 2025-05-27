package com.business.techassist.notif;

import com.google.firebase.Timestamp;

public class Notification {
    private String title;
    private String message;
    private Timestamp timestamp;
    private boolean isRead;
    private String type;

    // Empty constructor needed for Firestore
    public Notification() {}

    public Notification(String title, String message, String type) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = Timestamp.now();
        this.isRead = false;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}