package com.example.appit;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class WishlistAdapter extends RecyclerView.Adapter<WishlistAdapter.WishlistViewHolder> {

    private final Context context;
    private final List<Product> wishlistProducts;
    private final OnItemActionListener listener;

    public interface OnItemActionListener {
        void onRemoveClick(Product product);
        void onItemClick(Product product);
    }

    public WishlistAdapter(Context context, List<Product> wishlistProducts, OnItemActionListener listener) {
        this.context = context;
        this.wishlistProducts = wishlistProducts;
        this.listener = listener;
    }

    @NonNull
    @Override
    public WishlistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_wishlist, parent, false);
        return new WishlistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WishlistViewHolder holder, int position) {
        Product product = wishlistProducts.get(position);

        holder.name.setText(product.getTitle());
        
        String priceWithUnit = product.getPrice();
        if (!priceWithUnit.toLowerCase().contains("vnd")) {
             priceWithUnit += " VND";
        }
        holder.price.setText(priceWithUnit);

        Glide.with(context)
                .load(product.getThumbnail())
                .placeholder(R.drawable.shoe_placeholder)
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> listener.onItemClick(product));
        holder.removeButton.setOnClickListener(v -> listener.onRemoveClick(product));
    }

    @Override
    public int getItemCount() {
        return wishlistProducts.size();
    }

    static class WishlistViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price;
        ImageButton removeButton;

        public WishlistViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.wishlist_image);
            name = itemView.findViewById(R.id.wishlist_name);
            price = itemView.findViewById(R.id.wishlist_price);
            removeButton = itemView.findViewById(R.id.btn_remove_wishlist);
        }
    }
}
