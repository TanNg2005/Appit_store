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

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private CartAdapter adapter;
    private TextView totalPriceTextView;
    private CartManager cartManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        cartManager = CartManager.getInstance();

        // --- Setup Toolbar ---
        Toolbar toolbar = findViewById(R.id.cart_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Giỏ hàng");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // --- Setup Views ---
        recyclerView = findViewById(R.id.cart_recycler_view);
        totalPriceTextView = findViewById(R.id.cart_total_price);
        Button btnProceedToPayment = findViewById(R.id.btn_proceed_to_payment);

        // --- Setup RecyclerView ---
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        // SỬA: Truyền đúng List<CartItem>
        adapter = new CartAdapter(this, cartManager.getCartItems()); 
        recyclerView.setAdapter(adapter);

        updateTotalPrice();

        // --- Proceed to Payment Button ---
        btnProceedToPayment.setOnClickListener(v -> {
            double totalAmount = cartManager.calculateTotalPrice();
            if (totalAmount == 0) {
                Toast.makeText(this, "Giỏ hàng của bạn đang trống", Toast.LENGTH_SHORT).show();
                return;
            }

            String orderId = UUID.randomUUID().toString();

            Intent intent = new Intent(this, QrPaymentActivity.class);
            intent.putExtra("TOTAL_AMOUNT", totalAmount);
            intent.putExtra("ORDER_ID", orderId);
            startActivity(intent);
        });
    }

    // Cập nhật lại giỏ hàng mỗi khi quay lại màn hình này
    @Override
    protected void onResume() {
        super.onResume();
        adapter.notifyDataSetChanged();
        updateTotalPrice();
    }

    public void updateTotalPrice() {
        double totalPrice = cartManager.calculateTotalPrice();
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        totalPriceTextView.setText(format.format(totalPrice));
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
