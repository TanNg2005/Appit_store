package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class CartActivity extends AppCompatActivity {

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

        setupRecyclerView();
        loadCartItems();

        btnProceedToPayment.setOnClickListener(v -> {
            List<CartItem> selectedItems = cartManager.getSelectedItems();
            if (selectedItems.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ít nhất một sản phẩm để thanh toán", Toast.LENGTH_SHORT).show();
                return;
            }
            createOrder(selectedItems);
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

    private void createOrder(List<CartItem> selectedItems) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if(currentUser == null) return;

        String orderId = UUID.randomUUID().toString();
        double totalAmount = cartManager.calculateTotalPrice();

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    Order order = new Order();
                    order.setUserId(currentUser.getUid());
                    order.setCustomerName(user.getDisplayName());
                    order.setTotalPrice(totalAmount);
                    order.setStatus("Chờ xác nhận");

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
                            intent.putExtra("TOTAL_AMOUNT", totalAmount);
                            intent.putExtra("ORDER_ID", orderId);
                            startActivity(intent);
                        });
                }
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
