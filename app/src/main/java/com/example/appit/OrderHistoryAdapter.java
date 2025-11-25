package com.example.appit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class OrderHistoryAdapter extends RecyclerView.Adapter<OrderHistoryAdapter.ViewHolder> {

    private final Context context;
    private final List<Order> orderList;

    public OrderHistoryAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orderList.get(position);

        // 1. Ngày đặt
        if (order.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.orderDate.setText("Ngày đặt: " + sdf.format(order.getOrderDate()));
        } else {
            holder.orderDate.setText("Ngày đặt: N/A");
        }

        // 2. Trạng thái
        holder.status.setText(order.getStatus());
        updateStatusColor(holder.status, order.getStatus());

        // 3. Địa chỉ giao hàng
        if (order.getShippingAddress() != null) {
            User.ShippingAddress addr = order.getShippingAddress();
            String addressStr = String.format("Giao đến: %s, %s, %s", 
                    addr.getStreet(), addr.getDistrict(), addr.getCity());
            holder.shippingAddress.setText(addressStr);
        } else {
            holder.shippingAddress.setText("Giao đến: Chưa cập nhật");
        }

        // 4. Thông tin sản phẩm
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            Order.OrderItem firstItem = order.getItems().get(0);
            holder.productName.setText(firstItem.getProductName());
            
            if (firstItem.getThumbnailUrl() != null && !firstItem.getThumbnailUrl().isEmpty()) {
                Glide.with(context)
                    .load(firstItem.getThumbnailUrl())
                    .placeholder(R.drawable.shoe_placeholder)
                    .into(holder.productImage);
            } else {
                 holder.productImage.setImageResource(R.drawable.shoe_placeholder);
            }

            if (order.getItems().size() > 1) {
                holder.moreItems.setVisibility(View.VISIBLE);
                holder.moreItems.setText("và " + (order.getItems().size() - 1) + " sản phẩm khác");
            } else {
                holder.moreItems.setVisibility(View.GONE);
            }

            View.OnClickListener productClickListener = v -> {
                Intent intent = new Intent(context, ProductDetailActivity.class);
                Product product = new Product();
                try {
                    long pId = Long.parseLong(firstItem.getProductId());
                    product.setId(pId);
                } catch (NumberFormatException e) {
                }
                product.setTitle(firstItem.getProductName());
                product.setThumbnail(firstItem.getThumbnailUrl());
                intent.putExtra("product", product); 
                context.startActivity(intent);
            };
            holder.productImage.setOnClickListener(productClickListener);
            holder.productName.setOnClickListener(productClickListener);
        } else {
             holder.productName.setText("Sản phẩm không xác định");
             holder.productImage.setImageResource(R.drawable.shoe_placeholder);
             holder.productImage.setOnClickListener(null);
             holder.productName.setOnClickListener(null);
        }

        // 5. Tổng tiền
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.totalPrice.setText(format.format(order.getTotalPrice()));

        // 6. Xử lý nút Hủy đơn hàng
        // Cho phép hủy khi: Chờ xác nhận, Đã thanh toán, Đang giao hàng
        String status = order.getStatus();
        if ("Chờ xác nhận".equals(status) || "Đã thanh toán".equals(status) || "Đang giao hàng".equals(status)) {
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> showCancelConfirmationDialog(order, holder.getBindingAdapterPosition()));
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }
    }

    private void updateStatusColor(TextView statusView, String status) {
        if ("Đã nhận hàng".equals(status)) {
            statusView.setTextColor(ContextCompat.getColor(context, R.color.teal_700));
        } else if ("Đã hủy".equals(status)) {
            statusView.setTextColor(Color.RED);
        } else if ("Yêu cầu hủy".equals(status)) {
            statusView.setTextColor(Color.RED);
        } else {
            statusView.setTextColor(ContextCompat.getColor(context, R.color.purple_500));
        }
    }

    private void showCancelConfirmationDialog(Order order, int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        // Sử dụng layout custom cho dialog
        LayoutInflater inflater = LayoutInflater.from(context);
        View dialogView = inflater.inflate(R.layout.dialog_cancel_order, null);
        builder.setView(dialogView);

        RadioGroup radioGroup = dialogView.findViewById(R.id.cancel_reason_group);
        RadioButton radioOther = dialogView.findViewById(R.id.reason_other);
        TextInputLayout inputLayoutOther = dialogView.findViewById(R.id.input_layout_other_reason);
        TextInputEditText inputOther = dialogView.findViewById(R.id.input_other_reason);

        // Xử lý ẩn hiện ô nhập lý do khác
        radioGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.reason_other) {
                inputLayoutOther.setVisibility(View.VISIBLE);
            } else {
                inputLayoutOther.setVisibility(View.GONE);
            }
        });

        builder.setPositiveButton("Gửi yêu cầu", (dialog, which) -> {
             // Sẽ xử lý trong listener riêng để có thể validate
        });
        builder.setNegativeButton("Đóng", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        // Xử lý sự kiện nút Positive sau khi show để ngăn dialog đóng nếu chưa chọn lý do
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId == -1) {
                Toast.makeText(context, "Vui lòng chọn lý do hủy", Toast.LENGTH_SHORT).show();
                return;
            }

            String reason = "";
            RadioButton selectedRadio = dialogView.findViewById(selectedId);
            if (selectedRadio != null) {
                reason = selectedRadio.getText().toString();
            }

            if (selectedId == R.id.reason_other) {
                String otherReason = inputOther.getText() != null ? inputOther.getText().toString().trim() : "";
                if (otherReason.isEmpty()) {
                    inputLayoutOther.setError("Vui lòng nhập lý do");
                    return; // Không đóng dialog
                }
                reason = otherReason;
            }

            // Gửi yêu cầu hủy với lý do
            requestCancelOrder(order, position, reason);
            dialog.dismiss();
        });

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.purple_500));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(context, R.color.text_secondary));
    }

    private void requestCancelOrder(Order order, int position, String reason) {
        // CHỈ GỬI YÊU CẦU, KHÔNG HỦY NGAY
        String newStatus = "Yêu cầu hủy";
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        
        db.collection("orders").document(order.getDocumentId())
                .update("status", newStatus, "cancelReason", reason) // Cập nhật thêm trường lý do
                .addOnSuccessListener(aVoid -> {
                    if (context != null) {
                         Toast.makeText(context, "Đã gửi yêu cầu hủy đơn hàng", Toast.LENGTH_SHORT).show();
                    }
                    
                    // Gửi thông báo cho Admin kèm thông tin đơn hàng
                    sendAdminNotification(order, reason);

                    order.setStatus(newStatus);
                    order.setCancelReason(reason);
                    if (position != RecyclerView.NO_POSITION) {
                        notifyItemChanged(position);
                    } else {
                        notifyDataSetChanged(); 
                    }
                })
                .addOnFailureListener(e -> {
                    if (context != null) {
                        Toast.makeText(context, "Lỗi khi gửi yêu cầu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void sendAdminNotification(Order order, String reason) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Notification notification = new Notification();
        notification.setTitle("Yêu cầu hủy đơn hàng");
        notification.setMessage("Đơn hàng " + order.getDocumentId() + " có yêu cầu hủy. Lý do: " + reason);
        notification.setType("ADMIN_ALERT"); 
        notification.setTimestamp(new Date());
        notification.setRead(false);
        notification.setOrderId(order.getDocumentId());
        notification.setUserId(order.getUserId());
        
        // Thêm thông tin đơn hàng vào Notification
        notification.setOrderCustomerName(order.getCustomerName());
        notification.setOrderTotalPrice(order.getTotalPrice());
        if (order.getOrderDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            notification.setOrderDateStr(sdf.format(order.getOrderDate()));
        }
        
        db.collection("notifications").add(notification);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView orderDate, status, shippingAddress, productName, moreItems, totalPrice;
        ImageView productImage;
        Button btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            orderDate = itemView.findViewById(R.id.history_order_date);
            status = itemView.findViewById(R.id.history_order_status);
            shippingAddress = itemView.findViewById(R.id.history_shipping_address);
            productName = itemView.findViewById(R.id.history_product_name);
            moreItems = itemView.findViewById(R.id.history_more_items);
            totalPrice = itemView.findViewById(R.id.history_total_price);
            productImage = itemView.findViewById(R.id.history_product_image);
            btnCancel = itemView.findViewById(R.id.btn_cancel_order);
        }
    }
}
