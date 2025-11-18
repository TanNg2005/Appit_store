package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.NumberFormat;
import java.util.Locale;

public class OrderActivity extends AppCompatActivity {

    private TextView orderIdTextView;
    private TextView userEmailTextView;
    private TextView totalAmountTextView;
    private RecyclerView orderItemsRecyclerView;
    private CartAdapter adapter;
    private Button btnFinish;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        // --- Setup Toolbar ---
        Toolbar toolbar = findViewById(R.id.order_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Hóa đơn");

        // --- Get Views ---
        orderIdTextView = findViewById(R.id.order_id);
        userEmailTextView = findViewById(R.id.order_user_email);
        totalAmountTextView = findViewById(R.id.order_total_amount);
        orderItemsRecyclerView = findViewById(R.id.order_items_recycler_view);
        btnFinish = findViewById(R.id.btn_finish);

        // --- Get Data from Intent ---
        String orderId = getIntent().getStringExtra("ORDER_ID");
        double totalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 0.0);

        // --- Display User Info ---
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userEmailTextView.setText(currentUser.getEmail());
        }

        // --- Display Order Info ---
        orderIdTextView.setText(orderId);
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        totalAmountTextView.setText(format.format(totalAmount));

        // --- Display Order Items ---
        orderItemsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        // In this simulation, we get items directly from the cart manager
        adapter = new CartAdapter(this, CartManager.getInstance().getCartItems());
        orderItemsRecyclerView.setAdapter(adapter);

        // --- Finish Button ---
        btnFinish.setOnClickListener(v -> {
            // Clear cart after viewing the order
            CartManager.getInstance().clearCart();

            // Go back to the main activity
            Intent intent = new Intent(OrderActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish(); // Close order activity
        });
    }
}
