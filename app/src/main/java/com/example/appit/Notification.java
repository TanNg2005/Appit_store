package com.example.appit;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.PropertyName;

import java.util.Date;

public class Notification {
    @Exclude
    private String documentId;

    private String userId;
    private String title;
    private String message;
    private boolean isRead; // This field name matches "isRead" in Firestore
    private Date timestamp;

    public Notification() {}

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    // SỬA LỖI: Đổi tên getter/setter để khớp với quy tắc của Firestore
    @PropertyName("isRead")
    public boolean isRead() { return isRead; }

    @PropertyName("isRead")
    public void setRead(boolean read) { isRead = read; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }
}
