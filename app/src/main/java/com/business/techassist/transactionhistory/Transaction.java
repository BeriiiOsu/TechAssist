package com.business.techassist.transactionhistory;

import com.google.firebase.Timestamp;
import java.util.Date;
import com.google.firebase.Timestamp;

public class Transaction {
    private double amount;
    private String description;
    private String status;
    private String type;
    private Timestamp timestamp;

    public Transaction() {}

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Timestamp getTimestamp() { return timestamp; }
    
    // Single setter method that handles both Timestamp and Long values
    public void setTimestamp(Object timestampObj) {
        if (timestampObj == null) {
            this.timestamp = null;
        } else if (timestampObj instanceof Timestamp) {
            this.timestamp = (Timestamp) timestampObj;
        } else if (timestampObj instanceof Long) {
            this.timestamp = new Timestamp(new Date((Long) timestampObj));
        }
    }
}