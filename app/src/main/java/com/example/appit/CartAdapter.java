package com.example.appit;

import android.content.Context;
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

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private final Context context;
    private final List<CartItem> cartItems;
    private final CartManager cartManager = CartManager.getInstance();

    public CartAdapter(Context context, List<CartItem> cartItems) {
        this.context = context;
        this.cartItems = cartItems;
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.cart_item, parent, false);
        return new CartViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem cartItem = cartItems.get(position);
        Product product = cartItem.getProduct();

        holder.name.setText(product.getTitle());
        holder.price.setText(product.getPrice());
        holder.quantity.setText("Số lượng: " + cartItem.getQuantity());

        Glide.with(context)
                .load(product.getThumbnail())
                .into(holder.image);

        holder.removeButton.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition != RecyclerView.NO_POSITION) {
                CartItem itemToRemove = cartItems.get(currentPosition);
                cartManager.removeProduct(itemToRemove.getProduct());
                notifyItemRemoved(currentPosition);
                notifyItemRangeChanged(currentPosition, cartItems.size());
                
                // Cập nhật lại tổng tiền và badge trên MainActivity
                if (context instanceof CartActivity) {
                    ((CartActivity) context).updateTotalPrice();
                }
                if (context instanceof OrderActivity) {
                    // Không cần làm gì ở màn hình hóa đơn
                } else {
                     // Cập nhật badge ở MainActivity
                    if(context instanceof MainActivity) {
                       ((MainActivity) context).updateCartBadge();
                    }    
                } 
            }
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price, quantity;
        ImageButton removeButton;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.cart_item_image);
            name = itemView.findViewById(R.id.cart_item_name);
            price = itemView.findViewById(R.id.cart_item_price);
            quantity = itemView.findViewById(R.id.cart_item_quantity);
            removeButton = itemView.findViewById(R.id.btn_remove_from_cart);
        }
    }
}
