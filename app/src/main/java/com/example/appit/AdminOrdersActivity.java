package com.example.appit;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class AdminOrdersActivity extends BaseActivity implements AdminOrdersAdapter.OnOrderClickListener {

    private static final String TAG = "AdminOrdersActivity";
    private RecyclerView recyclerView;
    private AdminOrdersAdapter adapter;
    private List<Order> allOrders = new ArrayList<>();
    private List<Order> filteredOrders = new ArrayList<>();
    private FirebaseFirestore db;
    private Spinner statusSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_orders);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.admin_orders_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.nav_manage_orders);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_view_admin_orders);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        statusSpinner = findViewById(R.id.spinner_order_status);
        setupSpinner();

        loadAllOrders();
    }

    private void setupSpinner() {
        List<String> statusOptions = Arrays.asList("Tất cả", "Chờ xác nhận", "Đã thanh toán", "Đang giao hàng", "Đã hoàn thành", "Đã hủy");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(spinnerAdapter);

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterOrders(parent.getItemAtPosition(position).toString());
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadAllOrders() {
        db.collection("orders")
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    allOrders.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Order order = doc.toObject(Order.class);
                        order.setDocumentId(doc.getId());
                        allOrders.add(order);
                    }
                    filterOrders(statusSpinner.getSelectedItem().toString());
                });
    }

    private void filterOrders(String status) {
        filteredOrders.clear();
        if (status.equals("Tất cả")) {
            filteredOrders.addAll(allOrders);
        } else {
            for (Order order : allOrders) {
                if (order.getStatus().equals(status)) {
                    filteredOrders.add(order);
                }
            }
        }
        adapter = new AdminOrdersAdapter(this, filteredOrders, this);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onOrderClick(Order order) {
        showUpdateStatusDialog(order);
    }

    private void showUpdateStatusDialog(Order order) {
        List<String> statusOptions = Arrays.asList("Chờ xác nhận", "Đã thanh toán", "Đang giao hàng", "Đã hoàn thành", "Đã hủy");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cập nhật trạng thái đơn hàng");
        builder.setItems(statusOptions.toArray(new String[0]), (dialog, which) -> {
            String newStatus = statusOptions.get(which);
            updateOrderStatus(order, newStatus);
        });
        builder.show();
    }

    private void updateOrderStatus(Order order, String newStatus) {
        db.collection("orders").document(order.getDocumentId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật trạng thái", Toast.LENGTH_SHORT).show();
                    if (newStatus.equals("Đã thanh toán") && !order.getStatus().equals("Đã thanh toán")) {
                        updateStock(order);
                    }
                });
    }

    private void updateStock(Order order) {
        WriteBatch batch = db.batch();
        List<String> productIds = new ArrayList<>();
        for (Order.OrderItem item : order.getItems()) {
            productIds.add(item.getProductId());
        }

        db.collection("products").whereIn("id", productIds).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot productSnapshots = task.getResult();
                for (QueryDocumentSnapshot productDoc : productSnapshots) {
                    for (Order.OrderItem item : order.getItems()) {
                        if (String.valueOf(productDoc.getLong("id")).equals(item.getProductId())) {
                            batch.update(productDoc.getReference(), "stock", FieldValue.increment(-item.getQuantity()));
                        }
                    }
                }
                batch.commit().addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi cập nhật kho", e);
                });
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
