package com.example.appit;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.util.List;

public class OrderItemsAdapter extends RecyclerView.Adapter<OrderItemsAdapter.ViewHolder> {

    private final Context context;
    private final List<Order.OrderItem> items;

    public OrderItemsAdapter(Context context, List<Order.OrderItem> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.order_item_detail, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order.OrderItem item = items.get(position);

        holder.name.setText(item.getProductName());
        holder.price.setText(item.getProductPrice());
        holder.quantity.setText("x" + item.getQuantity());

        Glide.with(context).load(item.getThumbnailUrl()).into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            Product product = new Product();
            try {
                // Try parsing as Long ID first
                product.setId(Long.parseLong(item.getProductId()));
            } catch (Exception e) {
                // If not a number, treat as document ID
                product.setDocumentId(item.getProductId());
            }
            product.setTitle(item.getProductName());
            product.setThumbnail(item.getThumbnailUrl());
            product.setPrice(item.getProductPrice());
            
            intent.putExtra("product", product);
            // Nếu ID là dạng chuỗi (document ID), truyền thêm vào extra PRODUCT_ID để đảm bảo
            if (item.getProductId() != null && !item.getProductId().matches("\\d+")) {
                 intent.putExtra("PRODUCT_ID", item.getProductId());
            }
            
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price, quantity;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.order_item_image);
            name = itemView.findViewById(R.id.order_item_name);
            price = itemView.findViewById(R.id.order_item_price);
            quantity = itemView.findViewById(R.id.order_item_quantity);
        }
    }
}
