package com.example.appit;

import com.google.firebase.firestore.PropertyName;

import java.util.List;
import java.util.Map;

public class User {
    private String uid;
    private String email;
    private String displayName;
    private String phone;
    private String profileImageUrl;
    private List<ShippingAddress> shippingAddresses;
    private Map<String, Long> cart;
    private List<String> favorites;
    private boolean isAdmin;
    private boolean isLocked;

    public User() {}

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public List<ShippingAddress> getShippingAddresses() { return shippingAddresses; }
    public void setShippingAddresses(List<ShippingAddress> shippingAddresses) { this.shippingAddresses = shippingAddresses; }

    public Map<String, Long> getCart() { return cart; }
    public void setCart(Map<String, Long> cart) { this.cart = cart; }

    public List<String> getFavorites() { return favorites; }
    public void setFavorites(List<String> favorites) { this.favorites = favorites; }

    // Sử dụng @PropertyName để khớp với Firestore
    @PropertyName("isAdmin")
    public boolean isAdmin() { return isAdmin; }

    @PropertyName("isAdmin")
    public void setAdmin(boolean admin) { isAdmin = admin; }

    @PropertyName("isLocked")
    public boolean isLocked() { return isLocked; }

    @PropertyName("isLocked")
    public void setLocked(boolean locked) { isLocked = locked; }

    public static class ShippingAddress {
        private String recipientName;
        private String phone;
        private String street;
        private String district;
        private String city;
        private boolean isDefault;

        public ShippingAddress() {}

        public String getRecipientName() { return recipientName; }
        public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getStreet() { return street; }
        public void setStreet(String street) { this.street = street; }

        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }

        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }

        @PropertyName("isDefault")
        public boolean isDefault() { return isDefault; }

        @PropertyName("isDefault")
        public void setDefault(boolean aDefault) { isDefault = aDefault; }
    }
}
