package com.example.appit;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CartManager {

    private static CartManager instance;
    private final List<CartItem> cartItems = new ArrayList<>();

    private CartManager() {}

    public static synchronized CartManager getInstance() {
        if (instance == null) {
            instance = new CartManager();
        }
        return instance;
    }

    public void addProduct(Product product) {
        // Kiểm tra xem sản phẩm đã có trong giỏ hàng chưa
        for (CartItem item : cartItems) {
            if (item.getProduct().getId().equals(product.getId())) {
                // Nếu có, chỉ tăng số lượng
                item.incrementQuantity();
                return; // Kết thúc
            }
        }
        // Nếu chưa có, thêm một món hàng mới vào giỏ
        cartItems.add(new CartItem(product));
    }

    public void removeProduct(Product product) {
        // Dùng Iterator để tránh lỗi ConcurrentModificationException khi xóa
        Iterator<CartItem> iterator = cartItems.iterator();
        while (iterator.hasNext()) {
            CartItem item = iterator.next();
            if (item.getProduct().getId().equals(product.getId())) {
                iterator.remove();
                return;
            }
        }
    }

    // Trả về danh sách các CartItem
    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void clearCart() {
        cartItems.clear();
    }

    // Tính tổng số lượng tất cả các sản phẩm (để hiển thị trên badge)
    public int getTotalItemCount() {
        int totalCount = 0;
        for (CartItem item : cartItems) {
            totalCount += item.getQuantity();
        }
        return totalCount;
    }

    public double calculateTotalPrice() {
        double total = 0;
        for (CartItem item : cartItems) {
            try {
                String priceString = item.getProduct().getPrice().replaceAll("[^\\d]", "");
                double price = Double.parseDouble(priceString);
                // Nhân giá với số lượng
                total += price * item.getQuantity();
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return total;
    }
}
