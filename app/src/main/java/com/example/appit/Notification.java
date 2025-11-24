package com.example.appit;

import com.google.firebase.firestore.Exclude;
import java.util.Date;

public class Notification {
    @Exclude
    private String documentId;
    private String title;
    private String message;
    private String type; // "PROMO", "ORDER", "SYSTEM", "ORDER_COMPLETED"
    private Date timestamp;
    private boolean isRead;
    private String userId;
    private String orderId; // Optional: Link to specific order for review

    public Notification() {}

    public Notification(String title, String message, String type) {
        this.title = title;
        this.message = message;
        this.type = type;
        this.timestamp = new Date();
        this.isRead = false;
    }
    
    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
}
