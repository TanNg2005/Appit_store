package com.example.appit;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class OrderActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private OrderItemsAdapter adapter;
    private TextView totalPriceTextView, orderStatusTextView, orderDateTextView, shippingAddressTextView;
    private String orderId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);

        Toolbar toolbar = findViewById(R.id.order_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Chi tiết Đơn hàng");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.order_recycler_view);
        totalPriceTextView = findViewById(R.id.order_total_price);
        orderStatusTextView = findViewById(R.id.order_status);
        orderDateTextView = findViewById(R.id.order_date);
        shippingAddressTextView = findViewById(R.id.order_shipping_address);

        orderId = getIntent().getStringExtra("ORDER_ID");

        if (orderId != null && !orderId.isEmpty()) {
            loadOrderDetails();
        } else {
            Toast.makeText(this, "Không tìm thấy thông tin đơn hàng", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadOrderDetails() {
        FirebaseFirestore.getInstance().collection("orders").document(orderId).get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    Order order = documentSnapshot.toObject(Order.class);
                    if (order != null) {
                        displayOrderDetails(order);
                    }
                }
            });
    }

    private void displayOrderDetails(Order order) {
        // Display general order info
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        totalPriceTextView.setText(format.format(order.getTotalPrice()));
        orderStatusTextView.setText("Trạng thái: " + order.getStatus());

        if (order.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            orderDateTextView.setText("Ngày đặt: " + sdf.format(order.getOrderDate()));
        }
        
        if(order.getShippingAddress() != null){
            User.ShippingAddress address = order.getShippingAddress();
            shippingAddressTextView.setText(String.format("Giao đến: %s, %s, %s", address.getStreet(), address.getDistrict(), address.getCity()));
        }

        // Setup RecyclerView for order items
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderItemsAdapter(this, order.getItems() != null ? order.getItems() : new ArrayList<>());
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
