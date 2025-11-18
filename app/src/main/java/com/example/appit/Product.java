package com.example.appit;

import com.google.firebase.firestore.Exclude;
import java.util.List;

public class Product {

    @Exclude
    private String id; // Firestore Document ID

    // --- Các trường chính ---
    private String title;
    private String description;
    private String price; // SỬA LỖI: Trả về kiểu String để khớp với Firestore
    private double discountPercentage;
    private double rating;
    private int stock;
    private String brand;
    private String category;
    private String thumbnail;
    private List<String> images;

    // --- Các trường thông tin khác ---
    private String warrantyInformation;
    private String shippingInformation;
    private String availabilityStatus;
    private String returnPolicy;
    private List<String> tags;
    private Dimensions dimensions;
    private List<Review> reviews;

    // Constructor rỗng cần thiết cho Firestore
    public Product() {}

    // --- Getters and Setters ---

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPrice() { return price; } // SỬA LỖI: Trả về kiểu String
    public void setPrice(String price) { this.price = price; } // SỬA LỖI: Tham số là String

    public double getDiscountPercentage() { return discountPercentage; }
    public void setDiscountPercentage(double discountPercentage) { this.discountPercentage = discountPercentage; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public String getWarrantyInformation() { return warrantyInformation; }
    public void setWarrantyInformation(String warrantyInformation) { this.warrantyInformation = warrantyInformation; }

    public String getShippingInformation() { return shippingInformation; }
    public void setShippingInformation(String shippingInformation) { this.shippingInformation = shippingInformation; }

    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }

    public String getReturnPolicy() { return returnPolicy; }
    public void setReturnPolicy(String returnPolicy) { this.returnPolicy = returnPolicy; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public Dimensions getDimensions() { return dimensions; }
    public void setDimensions(Dimensions dimensions) { this.dimensions = dimensions; }

    public List<Review> getReviews() { return reviews; }
    public void setReviews(List<Review> reviews) { this.reviews = reviews; }

    // --- Lớp con cho Dimensions ---
    public static class Dimensions {
        private double width;
        private double height;
        private double depth;

        public Dimensions() {}

        public double getWidth() { return width; }
        public void setWidth(double width) { this.width = width; }

        public double getHeight() { return height; }
        public void setHeight(double height) { this.height = height; }

        public double getDepth() { return depth; }
        public void setDepth(double depth) { this.depth = depth; }
    }

    // --- Lớp con cho Review ---
    public static class Review {
        private int rating;
        private String comment;
        private String reviewerName;

        public Review() {}

        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public String getReviewerName() { return reviewerName; }
        public void setReviewerName(String reviewerName) { this.reviewerName = reviewerName; }
    }
}
