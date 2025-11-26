package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CartActivity extends AppCompatActivity {

    private static final String TAG = "CartActivity";
    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private TextView totalPriceTextView;
    private CartManager cartManager;
    private List<CartItem> cartItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartManager = CartManager.getInstance();

        Toolbar toolbar = findViewById(R.id.cart_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Giỏ hàng");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.cart_recycler_view);
        totalPriceTextView = findViewById(R.id.cart_total_price);
        Button btnProceedToPayment = findViewById(R.id.btn_proceed_to_payment);
        Button btnViewOrders = findViewById(R.id.btn_view_orders); // Nút xem đơn hàng đã đặt

        setupRecyclerView();
        loadCartItems();

        // Nút Xem đơn hàng đã đặt
        if (btnViewOrders != null) {
            btnViewOrders.setOnClickListener(v -> {
                startActivity(new Intent(CartActivity.this, OrderActivity.class));
            });
        }

        btnProceedToPayment.setOnClickListener(v -> {
            Log.d(TAG, "Nút thanh toán đã được bấm");
            
            List<CartItem> selectedItems = cartManager.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Hiển thị thông báo đang xử lý để người dùng biết app đang chạy
            Toast.makeText(this, "Đang kiểm tra thông tin...", Toast.LENGTH_SHORT).show();

            // Kiểm tra thông tin người dùng trước khi tạo đơn
            checkUserInfoAndCreateOrder(selectedItems);
        });
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CartAdapter(this, cartItems);
        recyclerView.setAdapter(adapter);
    }

    private void loadCartItems() {
        cartManager.loadCartItems(items -> {
            cartItems.clear();
            cartItems.addAll(items);
            adapter.notifyDataSetChanged();
            updateTotalPrice();
        });
    }

    private void checkUserInfoAndCreateOrder(List<CartItem> selectedItems) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Lỗi: Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Log.d(TAG, "Bắt đầu kiểm tra thông tin user: " + currentUser.getUid());

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = null;
                    if (documentSnapshot.exists()) {
                        user = documentSnapshot.toObject(User.class);
                    }

                    // SỬA LỖI: Nếu user chưa tồn tại trong Firestore (do tài khoản cũ) hoặc load lỗi
                    if (user == null) {
                        Log.w(TAG, "User document chưa tồn tại hoặc rỗng");
                        showMissingInfoRedirect();
                        return;
                    }

                    // Kiểm tra các trường bắt buộc: Số điện thoại và Địa chỉ giao hàng
                    boolean hasPhone = user.getPhone() != null && !user.getPhone().isEmpty();
                    boolean hasAddress = false;
                    if (user.getShippingAddresses() != null && !user.getShippingAddresses().isEmpty()) {
                        // Kiểm tra xem có ít nhất một địa chỉ hợp lệ
                         for (User.ShippingAddress addr : user.getShippingAddresses()) {
                             if (addr.getStreet() != null && !addr.getStreet().isEmpty()) {
                                 hasAddress = true;
                                 break;
                             }
                         }
                    }
                    
                    Log.d(TAG, "Thông tin user: hasPhone=" + hasPhone + ", hasAddress=" + hasAddress);

                    if (hasPhone && hasAddress) {
                        // Đủ thông tin -> Tiến hành tạo đơn hàng
                        createOrder(selectedItems, user);
                    } else {
                        // Thiếu thông tin -> Chuyển hướng
                        showMissingInfoRedirect();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi Firestore check user info", e);
                    Toast.makeText(this, "Lỗi kết nối: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void showMissingInfoRedirect() {
        Toast.makeText(this, "Vui lòng cập nhật SĐT và Địa chỉ để đặt hàng", Toast.LENGTH_LONG).show();
        
        // Chuyển hướng sang trang ProfileActivity để điền thông tin
        Intent intent = new Intent(CartActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void createOrder(List<CartItem> selectedItems, User user) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) return;
        
        Log.d(TAG, "Đang tạo đơn hàng...");
        // Toast.makeText(this, "Đang tạo đơn hàng...", Toast.LENGTH_SHORT).show();

        String orderId = UUID.randomUUID().toString();
        double totalAmount = cartManager.calculateTotalPrice();

        Order order = new Order();
        order.setUserId(currentUser.getUid());
        order.setCustomerName(user.getDisplayName()); // Lấy tên từ user đã load
        order.setTotalPrice(totalAmount);
        order.setStatus("Chờ xác nhận");
        
        // SỬA LỖI: Bổ sung ngày đặt hàng và địa chỉ giao hàng
        order.setOrderDate(new Date());
        
        if (user.getShippingAddresses() != null && !user.getShippingAddresses().isEmpty()) {
            User.ShippingAddress selectedAddr = null;
            // Ưu tiên địa chỉ mặc định
            for (User.ShippingAddress addr : user.getShippingAddresses()) {
                if (addr.isDefault()) {
                    selectedAddr = addr;
                    break;
                }
            }
            // Nếu không có mặc định, lấy cái đầu tiên
            if (selectedAddr == null) {
                selectedAddr = user.getShippingAddresses().get(0);
            }
            order.setShippingAddress(selectedAddr);
        }

        List<Order.OrderItem> orderItems = new ArrayList<>();
        for(CartItem item : selectedItems){
            Order.OrderItem orderItem = new Order.OrderItem();
            
            // SỬA LỖI: Lấy ID sản phẩm chính xác (ưu tiên Document ID, fallback sang Numeric ID)
            String productId = item.getProduct().getDocumentId();
            if (productId == null || productId.isEmpty()) {
                productId = String.valueOf(item.getProduct().getId());
            }
            orderItem.setProductId(productId);
            
            orderItem.setProductName(item.getProduct().getTitle());
            orderItem.setProductPrice(item.getProduct().getPrice());
            orderItem.setThumbnailUrl(item.getProduct().getThumbnail());
            orderItem.setQuantity(item.getQuantity());
            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        FirebaseFirestore.getInstance().collection("orders").document(orderId).set(order)
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Tạo đơn hàng thành công: " + orderId);
                
                // Gọi phương thức xóa sản phẩm đã mua
                cartManager.clearPurchasedItems(selectedItems);
                Intent intent = new Intent(this, QrPaymentActivity.class);
                intent.putExtra("TOTAL_AMOUNT", totalAmount);
                intent.putExtra("ORDER_ID", orderId);
                startActivity(intent);
                // Không finish CartActivity để user có thể back lại nếu muốn
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Lỗi tạo đơn hàng", e);
                // SỬA: Hiển thị dialog lỗi rõ ràng để user biết lý do thất bại
                new AlertDialog.Builder(this)
                        .setTitle("Lỗi tạo đơn hàng")
                        .setMessage("Không thể tạo đơn hàng. Chi tiết: " + e.getMessage())
                        .setPositiveButton("OK", null)
                        .show();
            });
    }

    public void updateTotalPrice() {
        double totalPrice = cartManager.calculateTotalPrice();
        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        totalPriceTextView.setText(format.format(totalPrice) + " VND");
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
