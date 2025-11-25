package com.example.appit;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AdminOrdersActivity extends BaseActivity implements AdminOrdersAdapter.OnOrderClickListener {

    private static final String TAG = "AdminOrdersActivity";
    private RecyclerView recyclerView;
    private AdminOrdersAdapter adapter;
    private List<Order> allOrders = new ArrayList<>();
    private List<Order> filteredOrders = new ArrayList<>();
    private FirebaseFirestore db;
    private Spinner statusSpinner;
    private TabLayout tabLayout;
    private LinearLayout filterContainer;

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
        
        tabLayout = findViewById(R.id.tab_layout_admin_orders);
        filterContainer = findViewById(R.id.layout_filter_container);
        statusSpinner = findViewById(R.id.spinner_order_status);
        
        setupSpinner();
        setupTabLayout();

        loadAllOrders();
    }
    
    private void setupTabLayout() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateListBasedOnTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                updateListBasedOnTab(tab.getPosition());
            }
        });
    }
    
    private void updateListBasedOnTab(int tabPosition) {
        if (tabPosition == 0) {
            // Tab Đơn hàng chung
            filterContainer.setVisibility(View.VISIBLE);
            filterOrders(statusSpinner.getSelectedItem().toString());
        } else {
            // Tab Yêu cầu hủy
            filterContainer.setVisibility(View.GONE);
            filterCancellationRequests();
        }
    }
    
    private void filterCancellationRequests() {
        filteredOrders.clear();
        for (Order order : allOrders) {
            if ("Yêu cầu hủy".equals(order.getStatus())) {
                filteredOrders.add(order);
            }
        }
        
        // Show badge count on tab if possible, or just update list
        if (tabLayout.getTabAt(1) != null && tabLayout.getTabAt(1).getBadge() != null) {
             tabLayout.getTabAt(1).getBadge().setNumber(filteredOrders.size());
        } else if (tabLayout.getTabAt(1) != null && filteredOrders.size() > 0) {
             tabLayout.getTabAt(1).getOrCreateBadge().setNumber(filteredOrders.size());
        } else if (tabLayout.getTabAt(1) != null) {
             tabLayout.getTabAt(1).removeBadge();
        }
        
        adapter = new AdminOrdersAdapter(this, filteredOrders, this);
        recyclerView.setAdapter(adapter);
    }

    private void setupSpinner() {
        List<String> statusOptions = Arrays.asList("Tất cả", "Chờ xác nhận", "Đã thanh toán", "Đang giao hàng", "Đã nhận hàng", "Đã hủy");
        // Removed "Yêu cầu hủy" from spinner since it has its own tab now
        
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statusOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        statusSpinner.setAdapter(spinnerAdapter);

        statusSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (tabLayout.getSelectedTabPosition() == 0) {
                    filterOrders(parent.getItemAtPosition(position).toString());
                }
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
                    // Refresh current view
                    updateListBasedOnTab(tabLayout.getSelectedTabPosition());
                    
                    // Update badge for cancellation requests even if on other tab
                    long cancelCount = allOrders.stream().filter(o -> "Yêu cầu hủy".equals(o.getStatus())).count();
                    if (tabLayout.getTabAt(1) != null) {
                        if (cancelCount > 0) {
                            tabLayout.getTabAt(1).getOrCreateBadge().setNumber((int) cancelCount);
                        } else {
                            tabLayout.getTabAt(1).removeBadge();
                        }
                    }
                });
    }

    private void filterOrders(String status) {
        filteredOrders.clear();
        if (status.equals("Tất cả")) {
            // Exclude "Yêu cầu hủy" from general list if needed, or keep them?
            // Usually if they are separated, we might want to exclude them or keep them.
            // Let's keep them but filtered out if user selects specific status other than ALL.
            // Actually, let's show everything EXCEPT maybe "Yêu cầu hủy" to avoid duplication, 
            // OR show everything. Let's show everything for now in "Tất cả".
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
        // KHÔNG CẦN LÀM GÌ KHI CLICK VÀO ITEM - CHỈ XỬ LÝ KHI CLICK STATUS (đã xử lý trong Adapter nếu cần)
        // HOẶC:
        // Nếu bạn muốn khi click vào item sẽ mở trang chi tiết đơn hàng cho admin (nếu có), hoặc hiện dialog
        // Hiện tại Adapter đang gọi listener này khi click vào itemView.
        
        // Nếu click vào toàn bộ item, ta có thể show dialog chi tiết hoặc không làm gì
        // Nhưng yêu cầu của bạn là "chỉ khi bấm vào trạng thái đơn hàng thì mới sửa trạng thái"
        
        // Vậy ta sẽ kiểm tra xem view được click có phải là status không, NHƯNG listener này được gọi cho toàn bộ item view.
        
        // GIẢI PHÁP: 
        // 1. Sửa Adapter: Bỏ setOnClickListener cho itemView, chỉ set cho textOrderStatus.
        // 2. Ở đây chỉ show dialog khi được gọi từ Adapter (lúc này Adapter chỉ gọi khi click status).
        
        // Tạm thời ở đây vẫn giữ logic show dialog, nhưng ta sẽ sửa Adapter để chỉ gọi listener khi click status.
        if ("Yêu cầu hủy".equals(order.getStatus())) {
            showProcessCancellationDialog(order);
        } else {
            showStandardStatusDialog(order);
        }
    }

    // Dialog xử lý yêu cầu hủy đơn
    private void showProcessCancellationDialog(Order order) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xử lý yêu cầu hủy");
        builder.setMessage("Khách hàng muốn hủy đơn hàng này. Bạn muốn xử lý thế nào?");
        
        builder.setIcon(R.drawable.ic_notifications);

        // Nút Đồng ý hủy
        builder.setPositiveButton("Đồng ý hủy", (dialog, which) -> {
            updateOrderStatus(order, "Đã hủy");
        });

        // Nút Từ chối -> Cho phép chọn lại trạng thái để khôi phục
        builder.setNegativeButton("Từ chối hủy", (dialog, which) -> {
            showStandardStatusDialog(order); 
        });

        builder.setNeutralButton("Đóng", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // Dialog chọn trạng thái thông thường
    private void showStandardStatusDialog(Order order) {
        List<String> statusOptions = Arrays.asList("Chờ xác nhận", "Đã thanh toán", "Đang giao hàng", "Đã nhận hàng", "Đã hủy");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Cập nhật trạng thái đơn hàng");
        
        builder.setItems(statusOptions.toArray(new String[0]), (dialog, which) -> {
            String newStatus = statusOptions.get(which);
            updateOrderStatus(order, newStatus);
        });
        builder.show();
    }

    private void updateOrderStatus(Order order, String newStatus) {
        boolean isCompleting = newStatus.equals("Đã nhận hàng") && !order.getStatus().equals("Đã nhận hàng");
        
        boolean isRestoringStock = newStatus.equals("Đã hủy") && !order.getStatus().equals("Đã hủy");
        
        boolean isDeductingStock = newStatus.equals("Đã thanh toán") && order.getStatus().equals("Chờ xác nhận");

        db.collection("orders").document(order.getDocumentId())
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã cập nhật: " + newStatus, Toast.LENGTH_SHORT).show();
                    
                    if (isDeductingStock) {
                        updateStock(order, -1); 
                    }
                    
                    if (isRestoringStock) {
                        updateStock(order, 1); 
                    }
                    
                    if (order.getStatus().equals("Yêu cầu hủy")) {
                        if (newStatus.equals("Đã hủy")) {
                            sendCancellationNotification(order, true); 
                        } else {
                            sendCancellationNotification(order, false); 
                        }
                    }
                    
                    if (isCompleting) {
                        sendReviewNotification(order);
                    }
                });
    }
    
    private void sendCancellationNotification(Order order, boolean isAccepted) {
         Notification notification = new Notification(
            "Phản hồi yêu cầu hủy",
            isAccepted ? "Yêu cầu hủy đơn hàng của bạn đã được chấp nhận." : "Yêu cầu hủy đơn hàng của bạn đã bị từ chối. Đơn hàng sẽ tiếp tục được xử lý.",
            "SYSTEM"
        );
        notification.setUserId(order.getUserId());
        notification.setRead(false);
        notification.setTimestamp(new java.util.Date());
        notification.setOrderId(order.getDocumentId()); 

        db.collection("notifications").add(notification);
    }
    
    private void sendReviewNotification(Order order) {
        Notification notification = new Notification(
            "Đơn hàng đã hoàn thành",
            "Đơn hàng của bạn đã được giao. Hãy đánh giá sản phẩm để nhận ưu đãi!",
            "ORDER_COMPLETED"
        );
        notification.setUserId(order.getUserId());
        notification.setRead(false);
        notification.setTimestamp(new java.util.Date());
        notification.setOrderId(order.getDocumentId()); 

        db.collection("notifications").add(notification)
            .addOnFailureListener(e -> Log.e(TAG, "Lỗi gửi thông báo đánh giá", e));
    }

    private void updateStock(Order order, int direction) {
        WriteBatch batch = db.batch();
        List<String> productIds = new ArrayList<>();
        if (order.getItems() == null) return;
        
        for (Order.OrderItem item : order.getItems()) {
            productIds.add(item.getProductId());
        }
        
        List<Long> numericIds = new ArrayList<>();
        for (String id : productIds) {
            try {
                 numericIds.add(Long.parseLong(id));
            } catch (NumberFormatException e) {}
        }
        if (numericIds.isEmpty()) return;

        db.collection("products").whereIn("id", numericIds).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                QuerySnapshot productSnapshots = task.getResult();
                for (QueryDocumentSnapshot productDoc : productSnapshots) {
                    for (Order.OrderItem item : order.getItems()) {
                        if (String.valueOf(productDoc.getLong("id")).equals(item.getProductId())) {
                            long change = item.getQuantity() * direction;
                            batch.update(productDoc.getReference(), "stock", FieldValue.increment(change));
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
