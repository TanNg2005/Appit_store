package com.example.appit;

import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;
import java.util.List;

public class Order {

    @Exclude
    private String documentId;

    private String userId;
    private String customerName;
    private List<OrderItem> items;
    private double totalPrice;
    private String status;
    private User.ShippingAddress shippingAddress;
    private String cancelReason; // Thêm trường lý do hủy
    
    @ServerTimestamp
    private Date orderDate;

    public Order() {}

    @Exclude
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public User.ShippingAddress getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(User.ShippingAddress shippingAddress) { this.shippingAddress = shippingAddress; }

    public Date getOrderDate() { return orderDate; }
    public void setOrderDate(Date orderDate) { this.orderDate = orderDate; }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    // Nested class for items within an order
    public static class OrderItem {
        private String productId;
        private String productName;
        private String productPrice;
        private String thumbnailUrl;
        private int quantity;

        public OrderItem() {}

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getProductPrice() { return productPrice; }
        public void setProductPrice(String productPrice) { this.productPrice = productPrice; }

        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
    }
}
