package com.example.appit;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RevenueActivity extends AppCompatActivity {

    private static final String TAG = "RevenueActivity";
    private BarChart barChart;
    private PieChart pieChart;
    private TextView textTotalRevenue;
    private FirebaseFirestore db;

    // Maps to store aggregated data
    private Map<String, Double> monthlyRevenueMap = new HashMap<>();
    private Map<String, Double> categoryRevenueMap = new HashMap<>();
    private Map<String, String> productIdToCategoryMap = new HashMap<>();
    private double totalRevenue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue);

        db = FirebaseFirestore.getInstance();

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Thống kê doanh thu");
        }

        barChart = findViewById(R.id.barChart);
        pieChart = findViewById(R.id.pieChart);
        textTotalRevenue = findViewById(R.id.text_total_revenue);

        loadData();
    }

    private void loadData() {
        // Step 1: Load all products to map productId -> category
        db.collection("products").get()
            .addOnSuccessListener(productSnapshots -> {
                for (DocumentSnapshot doc : productSnapshots) {
                    String category = doc.getString("category");
                    if (category != null) {
                        // 1. Map theo Document ID (ID của Firebase)
                        productIdToCategoryMap.put(doc.getId(), category);
                        
                        // 2. Map theo field "id" bên trong document (nếu có)
                        Object internalId = doc.get("id");
                        if (internalId != null) {
                            productIdToCategoryMap.put(String.valueOf(internalId), category);
                        }
                    }
                }
                // Step 2: After loading products, load orders
                loadOrders();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading products", e);
                Toast.makeText(this, "Lỗi tải dữ liệu sản phẩm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void loadOrders() {
        db.collection("orders").get()
            .addOnSuccessListener(orderSnapshots -> {
                processOrderData(orderSnapshots);
                updateCharts();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading orders", e);
                Toast.makeText(this, "Lỗi tải dữ liệu đơn hàng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private void processOrderData(com.google.firebase.firestore.QuerySnapshot snapshots) {
        monthlyRevenueMap.clear();
        categoryRevenueMap.clear();
        totalRevenue = 0;

        SimpleDateFormat monthFormat = new SimpleDateFormat("MM/yyyy", Locale.getDefault());

        for (QueryDocumentSnapshot doc : snapshots) {
            try {
                Order order = doc.toObject(Order.class);
                String status = order.getStatus();

                // Chỉ tính các đơn hàng có trạng thái hợp lệ
                if (status == null || 
                   (!status.equalsIgnoreCase("Đã thanh toán") && 
                    !status.equalsIgnoreCase("Đã nhận hàng") && 
                    !status.equalsIgnoreCase("Đang giao hàng"))) {
                    continue;
                }

                double orderTotal = order.getTotalPrice();
                
                // Nếu totalPrice = 0, thử tính lại từ items
                if (orderTotal == 0 && order.getItems() != null) {
                    for (Order.OrderItem item : order.getItems()) {
                        double itemPrice = parsePrice(item.getProductPrice());
                        orderTotal += itemPrice * item.getQuantity();
                    }
                }

                totalRevenue += orderTotal;

                // Tính doanh thu theo tháng
                if (order.getOrderDate() != null) {
                    String monthKey = monthFormat.format(order.getOrderDate());
                    monthlyRevenueMap.put(monthKey, monthlyRevenueMap.getOrDefault(monthKey, 0.0) + orderTotal);
                }

                // Tính doanh thu theo danh mục
                if (order.getItems() != null) {
                    for (Order.OrderItem item : order.getItems()) {
                        String productId = item.getProductId();
                        String category = "Khác";
                        
                        if (productId != null && productIdToCategoryMap.containsKey(productId)) {
                            category = productIdToCategoryMap.get(productId);
                        }
                        
                        // Tính giá trị của từng item
                        double itemPrice = parsePrice(item.getProductPrice());
                        double itemTotal = itemPrice * item.getQuantity();
                        
                        categoryRevenueMap.put(category, categoryRevenueMap.getOrDefault(category, 0.0) + itemTotal);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing order: " + doc.getId(), e);
            }
        }
        
        // Hiển thị tổng doanh thu
        java.text.NumberFormat currencyFormat = java.text.NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        textTotalRevenue.setText(currencyFormat.format(totalRevenue));
    }
    
    private double parsePrice(String priceStr) {
        if (priceStr == null || priceStr.isEmpty()) return 0;
        try {
            String cleanStr = priceStr.replaceAll("[^\\d]", "");
            if (cleanStr.isEmpty()) return 0;
            return Double.parseDouble(cleanStr);
        } catch (NumberFormatException e) {
            Log.e(TAG, "Error parsing price: " + priceStr, e);
            return 0;
        }
    }

    private void updateCharts() {
        if (monthlyRevenueMap.isEmpty() && categoryRevenueMap.isEmpty()) {
            barChart.setNoDataText("Chưa có dữ liệu doanh thu");
            pieChart.setNoDataText("Chưa có dữ liệu danh mục");
            barChart.invalidate();
            pieChart.invalidate();
            return;
        }
        setupBarChart();
        setupPieChart();
    }

    private void setupBarChart() {
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        List<String> sortedMonths = new ArrayList<>(monthlyRevenueMap.keySet());
        Collections.sort(sortedMonths);

        int index = 0;
        for (String month : sortedMonths) {
            entries.add(new BarEntry(index, monthlyRevenueMap.get(month).floatValue()));
            labels.add(month);
            index++;
        }

        if (entries.isEmpty()) return;

        BarDataSet dataSet = new BarDataSet(entries, "Doanh thu (VNĐ)");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(12f);

        BarData barData = new BarData(dataSet);
        barChart.setData(barData);
        
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);

        barChart.getDescription().setEnabled(false);
        barChart.animateY(1000);
        barChart.invalidate();
    }

    private void setupPieChart() {
        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryRevenueMap.entrySet()) {
            if (entry.getValue() > 0) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }
        }

        if (entries.isEmpty()) return;

        PieDataSet dataSet = new PieDataSet(entries, "Danh mục");
        
        // Kết hợp nhiều bảng màu để đa dạng hơn
        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.VORDIPLOM_COLORS) colors.add(c);
        for (int c : ColorTemplate.JOYFUL_COLORS) colors.add(c);
        for (int c : ColorTemplate.COLORFUL_COLORS) colors.add(c);
        for (int c : ColorTemplate.LIBERTY_COLORS) colors.add(c);
        for (int c : ColorTemplate.PASTEL_COLORS) colors.add(c);
        dataSet.setColors(colors);

        // Cấu hình hiển thị Text ra bên ngoài
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        
        // Đường kẻ chỉ dẫn
        dataSet.setValueLinePart1OffsetPercentage(80.f);
        dataSet.setValueLinePart1Length(0.3f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setValueTextColor(Color.BLACK); // Màu chữ đen
        dataSet.setValueTextSize(11f);

        PieData pieData = new PieData(dataSet);
        
        // Formatter rút gọn số tiền (ví dụ: 1.5M, 500k)
        pieData.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                if (value >= 1000000) {
                    return String.format(Locale.getDefault(), "%.1fM", value / 1000000);
                } else if (value >= 1000) {
                     return String.format(Locale.getDefault(), "%.0fk", value / 1000);
                }
                return String.format(Locale.getDefault(), "%.0f", value);
            }
        });

        pieChart.setData(pieData);
        
        // Cấu hình PieChart
        pieChart.setEntryLabelColor(Color.BLACK); // Màu tên danh mục
        pieChart.setEntryLabelTextSize(11f);
        pieChart.setExtraOffsets(20.f, 0.f, 20.f, 0.f); // Thêm khoảng trắng xung quanh để chứa text
        
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("Doanh thu\ntheo loại");
        pieChart.setCenterTextSize(14f);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
