package com.example.appit;

import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Voucher {

    private String code; 
    private String description;
    private String discountType; // PERCENT or AMOUNT
    
    @PropertyName("discountValue")
    private Double value;
    
    @PropertyName("maxDiscountAmount")
    private Double maxDiscount; 
    
    @PropertyName("minOrderAmount")
    private Double minPurchase; 

    @ServerTimestamp
    private Date startDate;
    @ServerTimestamp
    private Date endDate;

    private int usageLimit;
    private int usedCount;
    
    @PropertyName("isActive")
    private boolean active;
    
    private String title;
    
    // DANH SÁCH CÁC USER ID ĐÃ SỬ DỤNG VOUCHER NÀY
    @PropertyName("usedBy")
    private List<String> usedBy;

    // DANH SÁCH CÁC USER ID ĐÃ LƯU VOUCHER NÀY
    @PropertyName("savedBy")
    private List<String> savedBy;

    // Required empty public constructor for Firestore
    public Voucher() {}

    // --- Getters and Setters ---

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
    
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    @PropertyName("discountValue")
    public Double getValue() {
        return value != null ? value : 0.0;
    }

    @PropertyName("discountValue")
    public void setValue(Double value) {
        this.value = value;
    }

    @PropertyName("maxDiscountAmount")
    public Double getMaxDiscount() {
        return maxDiscount != null ? maxDiscount : 0.0;
    }

    @PropertyName("maxDiscountAmount")
    public void setMaxDiscount(Double maxDiscount) {
        this.maxDiscount = maxDiscount;
    }

    @PropertyName("minOrderAmount")
    public Double getMinPurchase() {
        return minPurchase != null ? minPurchase : 0.0;
    }

    @PropertyName("minOrderAmount")
    public void setMinPurchase(Double minPurchase) {
        this.minPurchase = minPurchase;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public int getUsageLimit() {
        return usageLimit;
    }

    public void setUsageLimit(int usageLimit) {
        this.usageLimit = usageLimit;
    }

    public int getUsedCount() {
        return usedCount;
    }

    public void setUsedCount(int usedCount) {
        this.usedCount = usedCount;
    }

    @PropertyName("isActive")
    public boolean isActive() {
        return active;
    }

    @PropertyName("isActive")
    public void setActive(boolean active) {
        this.active = active;
    }

    @PropertyName("usedBy")
    public List<String> getUsedBy() {
        return usedBy != null ? usedBy : new ArrayList<>();
    }

    @PropertyName("usedBy")
    public void setUsedBy(List<String> usedBy) {
        this.usedBy = usedBy;
    }

    @PropertyName("savedBy")
    public List<String> getSavedBy() {
        return savedBy != null ? savedBy : new ArrayList<>();
    }

    @PropertyName("savedBy")
    public void setSavedBy(List<String> savedBy) {
        this.savedBy = savedBy;
    }

    /**
     * Business Logic Methods
     */

    // Check if the voucher is valid (active, date, usage, AND specific user check)
    public String getValidationMessage(double currentPurchase, String currentUserId) {
        Date now = new Date();
        
        if (!isActive()) {
            return "Voucher không hợp lệ hoặc đã bị khóa";
        }
        if (getStartDate() != null && now.before(getStartDate())) {
            return "Voucher chưa có hiệu lực";
        }
        if (getEndDate() != null && now.after(getEndDate())) {
            return "Voucher đã hết hạn";
        }
        // Kiểm tra tổng số lượng (Ví dụ: chỉ có 100 mã)
        if (getUsageLimit() > 0 && getUsedCount() >= getUsageLimit()) {
            return "Voucher đã hết lượt sử dụng (Hết mã)";
        }
        // Kiểm tra xem User này đã dùng chưa
        if (currentUserId != null && getUsedBy().contains(currentUserId)) {
            return "Bạn đã sử dụng mã giảm giá này rồi";
        }
        // Lưu ý: Không kiểm tra "savedBy" ở đây vì voucher có thể dùng ngay mà không cần lưu
        
        if (currentPurchase < getMinPurchase()) {
            return "Đơn hàng chưa đạt giá trị tối thiểu";
        }
        return "VALID";
    }
    
    // Overload method for backwards compatibility (if needed elsewhere without user check)
    public String getValidationMessage(double currentPurchase) {
        return getValidationMessage(currentPurchase, null);
    }

    // Calculate the discount amount
    public double calculateDiscount(double purchaseTotal) {
        // Note: This simple calculation assumes validation passed separately
        double discountAmount = 0;
        double val = getValue();
        
        if ("PERCENT".equals(getDiscountType())) {
            discountAmount = purchaseTotal * (val / 100.0);
            double max = getMaxDiscount();
            if (max > 0 && discountAmount > max) {
                discountAmount = max;
            }
        } else if ("AMOUNT".equals(getDiscountType())) {
            discountAmount = val;
        }
        
        return Math.min(discountAmount, purchaseTotal);
    }
}