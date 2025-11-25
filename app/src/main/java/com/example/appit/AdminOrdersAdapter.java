package com.example.appit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class AdminOrdersAdapter extends RecyclerView.Adapter<AdminOrdersAdapter.OrderViewHolder> {

    private final Context context;
    private final List<Order> orderList;
    private final OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(Order order);
    }

    public AdminOrdersAdapter(Context context, List<Order> orderList, OnOrderClickListener listener) {
        this.context = context;
        this.orderList = orderList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.admin_order_item, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        holder.bind(order, listener, context);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, customerName, orderDate, totalPrice, orderStatus, cancelReason;
        ImageView productThumb;
        TextView productName, itemQuantity, moreItems;
        View productInfoContainer; // Container để set listener

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.text_order_id);
            customerName = itemView.findViewById(R.id.text_customer_name);
            orderDate = itemView.findViewById(R.id.text_order_date);
            totalPrice = itemView.findViewById(R.id.text_total_price);
            orderStatus = itemView.findViewById(R.id.text_order_status);
            cancelReason = itemView.findViewById(R.id.text_cancel_reason);
            
            // Ánh xạ các view mới cho thông tin sản phẩm
            productThumb = itemView.findViewById(R.id.img_product_thumb);
            productName = itemView.findViewById(R.id.text_product_name);
            itemQuantity = itemView.findViewById(R.id.text_item_quantity_single);
            moreItems = itemView.findViewById(R.id.text_more_items);
            
            // Tìm container chứa thông tin sản phẩm (cần thêm ID vào layout XML nếu chưa có)
            // Trong layout admin_order_item.xml hiện tại, product info nằm trong một LinearLayout ngang
            // Để dễ dàng, ta có thể gán click cho ảnh và tên sản phẩm, hoặc tìm parent view
            // Tạm thời gán click cho từng thành phần hiển thị sản phẩm
        }

        public void bind(final Order order, final OnOrderClickListener listener, Context context) {
            orderId.setText("ID: " + order.getDocumentId());
            customerName.setText("Khách hàng: " + order.getCustomerName());

            if (order.getOrderDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                orderDate.setText("Ngày đặt: " + sdf.format(order.getOrderDate()));
            }
            
            // Hiển thị thông tin sản phẩm
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                Order.OrderItem firstItem = order.getItems().get(0);
                productName.setText(firstItem.getProductName());
                itemQuantity.setText("x" + firstItem.getQuantity());
                
                if (firstItem.getThumbnailUrl() != null && !firstItem.getThumbnailUrl().isEmpty()) {
                    Glide.with(context)
                         .load(firstItem.getThumbnailUrl())
                         .placeholder(R.drawable.shoe_placeholder)
                         .into(productThumb);
                } else {
                    productThumb.setImageResource(R.drawable.shoe_placeholder);
                }

                if (order.getItems().size() > 1) {
                    moreItems.setVisibility(View.VISIBLE);
                    moreItems.setText("Xem thêm " + (order.getItems().size() - 1) + " sản phẩm...");
                    
                    // Xử lý click "Xem thêm" -> Hiển thị danh sách đầy đủ
                    moreItems.setOnClickListener(v -> showFullOrderItemsDialog(context, order));
                } else {
                    moreItems.setVisibility(View.GONE);
                }
                
                // Xử lý click vào sản phẩm đơn lẻ -> Mở chi tiết sản phẩm
                View.OnClickListener productClickListener = v -> {
                    Intent intent = new Intent(context, ProductDetailActivity.class);
                    // Cần logic để chuyển đổi ID từ String (trong OrderItem) sang Long (nếu Product dùng Long)
                    // Hoặc truyền documentId
                    // Ở đây ta truyền product object giả lập với thông tin có sẵn
                    Product product = new Product();
                    try {
                        product.setId(Long.parseLong(firstItem.getProductId()));
                    } catch (Exception e) {
                         product.setDocumentId(firstItem.getProductId());
                    }
                    product.setTitle(firstItem.getProductName());
                    product.setThumbnail(firstItem.getThumbnailUrl());
                    intent.putExtra("product", product);
                    // Nếu có document ID thì tốt hơn
                    if (firstItem.getProductId() != null && !firstItem.getProductId().matches("\\d+")) {
                         intent.putExtra("PRODUCT_ID", firstItem.getProductId());
                    }
                    
                    context.startActivity(intent);
                };
                
                productThumb.setOnClickListener(productClickListener);
                productName.setOnClickListener(productClickListener);
                
            } else {
                productName.setText("Không có thông tin sản phẩm");
                itemQuantity.setText("");
                moreItems.setVisibility(View.GONE);
                productThumb.setImageResource(R.drawable.shoe_placeholder);
            }

            // Hiển thị lý do hủy nếu có
            if (order.getCancelReason() != null && !order.getCancelReason().isEmpty()) {
                cancelReason.setText("Lý do hủy: " + order.getCancelReason());
                cancelReason.setVisibility(View.VISIBLE);
            } else {
                cancelReason.setVisibility(View.GONE);
            }
            
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            totalPrice.setText(format.format(order.getTotalPrice()));

            orderStatus.setText(order.getStatus());
            setStatusColor(order.getStatus());

            // Xóa listener khỏi itemView để không mở dialog khi click vào toàn bộ card
            itemView.setOnClickListener(null);
            
            // CHỈ GÁN LISTENER CHO orderStatus
            orderStatus.setOnClickListener(v -> listener.onOrderClick(order));
        }
        
        private void showFullOrderItemsDialog(Context context, Order order) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("Chi tiết đơn hàng (" + order.getItems().size() + " sản phẩm)");

            // Tạo RecyclerView để hiển thị danh sách
            RecyclerView recyclerView = new RecyclerView(context);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            // Sử dụng lại OrderItemsAdapter hoặc tạo adapter đơn giản mới
            // Giả sử đã có OrderItemsAdapter từ trước (dùng trong OrderHistory)
            OrderItemsAdapter itemsAdapter = new OrderItemsAdapter(context, order.getItems());
            recyclerView.setAdapter(itemsAdapter);
            
            // Thêm padding cho đẹp
            recyclerView.setPadding(16, 16, 16, 16);
            
            builder.setView(recyclerView);
            builder.setPositiveButton("Đóng", null);
            builder.show();
        }

        private void setStatusColor(String status) {
            int color = Color.GRAY;
            switch (status) {
                case "Chờ xác nhận":
                    color = Color.parseColor("#FFC107"); // Amber
                    break;
                case "Đã thanh toán":
                    color = Color.parseColor("#4CAF50"); // Green
                    break;
                case "Đang giao hàng":
                    color = Color.parseColor("#2196F3"); // Blue
                    break;
                case "Đã nhận hàng":
                    color = Color.parseColor("#BDBDBD"); // Grey
                    break;
                case "Đã hủy":
                    color = Color.parseColor("#F44336"); // Red
                    break;
                case "Yêu cầu hủy":
                    color = Color.parseColor("#FF5722"); // Deep Orange
                    break;
            }
            orderStatus.getBackground().setTint(color);
        }
    }
}
