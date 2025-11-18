package com.example.appit;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.bind(order, listener);
    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView orderId, customerName, orderDate, totalPrice, orderStatus;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            orderId = itemView.findViewById(R.id.text_order_id);
            customerName = itemView.findViewById(R.id.text_customer_name);
            orderDate = itemView.findViewById(R.id.text_order_date);
            totalPrice = itemView.findViewById(R.id.text_total_price);
            orderStatus = itemView.findViewById(R.id.text_order_status);
        }

        public void bind(final Order order, final OnOrderClickListener listener) {
            orderId.setText("ID: " + order.getDocumentId());
            customerName.setText("Khách hàng: " + order.getCustomerName());

            if (order.getOrderDate() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                orderDate.setText("Ngày đặt: " + sdf.format(order.getOrderDate()));
            }
            
            NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
            totalPrice.setText("Tổng tiền: " + format.format(order.getTotalPrice()));

            orderStatus.setText(order.getStatus());
            setStatusColor(order.getStatus());

            itemView.setOnClickListener(v -> listener.onOrderClick(order));
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
                case "Đã hoàn thành":
                    color = Color.parseColor("#BDBDBD"); // Grey
                    break;
                case "Đã hủy":
                    color = Color.parseColor("#F44336"); // Red
                    break;
            }
            orderStatus.getBackground().setTint(color);
        }
    }
}
