package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private TextView totalPriceTextView;
    private TextView discountPriceTextView;
    private TextInputEditText voucherEditText;
    private CartManager cartManager;
    private List<CartItem> cartItems = new ArrayList<>();
    private FirebaseFirestore db;
    
    private Voucher appliedVoucher = null;
    private double currentDiscountAmount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartManager = CartManager.getInstance();
        db = FirebaseFirestore.getInstance();
        
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        recyclerView = findViewById(R.id.cart_recycler_view);
        totalPriceTextView = findViewById(R.id.cart_total_price);
        discountPriceTextView = findViewById(R.id.cart_discount_price);
        voucherEditText = findViewById(R.id.voucher_edit_text);
        MaterialButton btnApplyVoucher = findViewById(R.id.btn_apply_voucher);
        Button btnProceedToPayment = findViewById(R.id.btn_proceed_to_payment);

        setupRecyclerView();
        loadCartItems();

        btnApplyVoucher.setOnClickListener(v -> applyVoucher());

        btnProceedToPayment.setOnClickListener(v -> {
            List<CartItem> selectedItems = cartManager.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
                return;
            }
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
    
    private void applyVoucher() {
        String voucherCode = voucherEditText.getText().toString().trim();
        if (voucherCode.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập mã voucher", Toast.LENGTH_SHORT).show();
            return;
        }
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để sử dụng voucher", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Reset current voucher
        appliedVoucher = null;
        currentDiscountAmount = 0;
        updateTotalPrice();

        db.collection("vouchers")
                .whereEqualTo("code", voucherCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        // Lấy voucher đầu tiên tìm thấy (mã phải là duy nhất)
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Voucher voucher = document.toObject(Voucher.class);
                            
                            // Tính tổng tiền các món ĐƯỢC CHỌN để kiểm tra điều kiện
                            double selectedTotal = cartManager.calculateTotalPrice();
                            
                            // CHECK VALIDATION with UserId
                            String validationMsg = voucher.getValidationMessage(selectedTotal, user.getUid());
                            
                            if ("VALID".equals(validationMsg)) {
                                appliedVoucher = voucher;
                                Toast.makeText(this, "Áp dụng mã giảm giá thành công!", Toast.LENGTH_SHORT).show();
                                updateTotalPrice();
                            } else {
                                Toast.makeText(this, validationMsg, Toast.LENGTH_SHORT).show();
                            }
                            return; // Chỉ xử lý 1 voucher
                        }
                    } else {
                        Toast.makeText(this, "Mã voucher không tồn tại", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi kiểm tra voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkUserInfoAndCreateOrder(List<CartItem> selectedItems) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshot.toObject(User.class);
                    if (user != null) {
                        // Kiểm tra các trường bắt buộc: Số điện thoại và Địa chỉ giao hàng
                        boolean hasPhone = user.getPhone() != null && !user.getPhone().isEmpty();
                        boolean hasAddress = false;
                        if (user.getShippingAddresses() != null && !user.getShippingAddresses().isEmpty()) {
                            // Kiểm tra xem có ít nhất một địa chỉ hợp lệ (ví dụ: có tên đường)
                             for (User.ShippingAddress addr : user.getShippingAddresses()) {
                                 if (addr.getStreet() != null && !addr.getStreet().isEmpty()) {
                                     hasAddress = true;
                                     break;
                                 }
                             }
                        }

                        if (hasPhone && hasAddress) {
                            // Đủ thông tin -> Tiến hành tạo đơn hàng
                            createOrder(selectedItems, user);
                        } else {
                            // Thiếu thông tin -> Hiển thị thông báo và điều hướng
                            showMissingInfoDialog();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi kiểm tra thông tin người dùng", Toast.LENGTH_SHORT).show();
                });
    }

    private void showMissingInfoDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Thông tin chưa đầy đủ")
                .setMessage("Bạn cần cập nhật Số điện thoại và Địa chỉ giao hàng để tiến hành đặt hàng.")
                .setPositiveButton("Cập nhật ngay", (dialog, which) -> {
                    // Chuyển hướng sang trang ProfileActivity để điền thông tin
                    Intent intent = new Intent(CartActivity.this, ProfileActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void createOrder(List<CartItem> selectedItems, User user) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) return;

        String orderId = UUID.randomUUID().toString();
        
        // Tổng tiền hàng (chưa trừ giảm giá)
        double subTotal = cartManager.calculateTotalPrice();
        // Tính toán lại giảm giá (để đảm bảo chính xác lúc đặt hàng)
        double discount = 0;
        if (appliedVoucher != null) {
             discount = appliedVoucher.calculateDiscount(subTotal);
             
             // CẬP NHẬT VOUCHER: Tăng usedCount và thêm userId vào usedBy
             incrementVoucherUsage(appliedVoucher.getCode(), currentUser.getUid());
        }
        
        double tempTotal = subTotal - discount;
        double finalTotal = tempTotal < 0 ? 0 : tempTotal;

        Order order = new Order();
        order.setUserId(currentUser.getUid());
        order.setCustomerName(user.getDisplayName()); 
        order.setTotalPrice(finalTotal); // Lưu tổng tiền SAU KHI giảm
        order.setStatus("Chờ xác nhận");
        // Lưu thông tin voucher vào đơn hàng (nếu cần thiết kế thêm trường này trong Order model)
        // order.setVoucherCode(appliedVoucher != null ? appliedVoucher.getCode() : null);
        // order.setDiscountAmount(discount);

        List<Order.OrderItem> orderItems = new ArrayList<>();
        for(CartItem item : selectedItems){
            Order.OrderItem orderItem = new Order.OrderItem();
            orderItem.setProductId(String.valueOf(item.getProduct().getId()));
            orderItem.setProductName(item.getProduct().getTitle());
            orderItem.setProductPrice(item.getProduct().getPrice());
            orderItem.setThumbnailUrl(item.getProduct().getThumbnail());
            orderItem.setQuantity(item.getQuantity());
            orderItems.add(orderItem);
        }
        order.setItems(orderItems);

        FirebaseFirestore.getInstance().collection("orders").document(orderId).set(order)
            .addOnSuccessListener(aVoid -> {
                // Gọi phương thức xóa sản phẩm đã mua
                cartManager.clearPurchasedItems(selectedItems);
                Intent intent = new Intent(this, QrPaymentActivity.class);
                intent.putExtra("TOTAL_AMOUNT", finalTotal);
                intent.putExtra("ORDER_ID", orderId);
                startActivity(intent);
            });
    }
    
    private void incrementVoucherUsage(String voucherCode, String userId) {
        // Tìm document ID của voucher để update
        db.collection("vouchers").whereEqualTo("code", voucherCode).get()
            .addOnSuccessListener(snapshots -> {
                for(QueryDocumentSnapshot doc : snapshots) {
                     // Cập nhật atomic: tăng biến đếm và thêm phần tử vào mảng
                     db.collection("vouchers").document(doc.getId())
                       .update(
                           "usedCount", FieldValue.increment(1),
                           "usedBy", FieldValue.arrayUnion(userId)
                       );
                }
            });
    }

    public void updateTotalPrice() {
        double subTotal = cartManager.calculateTotalPrice();
        
        // Re-validate voucher logic when cart changes (e.g. item removed or quantity changed)
        if (appliedVoucher != null) {
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            String userId = user != null ? user.getUid() : null;
            
            String validation = appliedVoucher.getValidationMessage(subTotal, userId);
            if (!"VALID".equals(validation)) {
                appliedVoucher = null;
                currentDiscountAmount = 0;
                Toast.makeText(this, "Voucher không còn đủ điều kiện: " + validation, Toast.LENGTH_SHORT).show();
            } else {
                currentDiscountAmount = appliedVoucher.calculateDiscount(subTotal);
            }
        } else {
            currentDiscountAmount = 0;
        }

        double finalTotal = subTotal - currentDiscountAmount;
        if (finalTotal < 0) finalTotal = 0;
        
        NumberFormat format = NumberFormat.getInstance(new Locale("vi", "VN"));
        
        totalPriceTextView.setText(format.format(finalTotal) + " " + getString(R.string.currency_unit));
        
        if (currentDiscountAmount > 0) {
            discountPriceTextView.setText("-" + format.format(currentDiscountAmount) + " " + getString(R.string.currency_unit));
            discountPriceTextView.setVisibility(View.VISIBLE);
        } else {
            discountPriceTextView.setText("-0" + getString(R.string.currency_unit));
            // discountPriceTextView.setVisibility(View.GONE); // Hoặc ẩn đi nếu muốn
        }
    }
}