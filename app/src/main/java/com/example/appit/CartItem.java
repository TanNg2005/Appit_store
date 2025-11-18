package com.example.appit;

public class CartItem {
    private Product product;
    private int quantity;
    private boolean selected;

    public CartItem(Product product) {
        this.product = product;
        this.quantity = 1;
        this.selected = false;
    }

    // SỬA LỖI: Thêm constructor để nhận số lượng từ database
    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.selected = true; // Mặc định chọn sẵn khi vào giỏ hàng
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void incrementQuantity() {
        this.quantity++;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
