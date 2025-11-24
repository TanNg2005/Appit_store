package com.example.appit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
        
        // Thêm đuôi VND
        String priceWithUnit = product.getPrice();
        if (!priceWithUnit.toLowerCase().contains("vnd")) {
             priceWithUnit += " VND";
        }
        holder.price.setText(priceWithUnit);
        
        holder.quantity.setText(String.valueOf(cartItem.getQuantity()));

        Glide.with(context).load(product.getThumbnail()).into(holder.image);

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(cartItem.isSelected());
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            cartItem.setSelected(isChecked);
            if (context instanceof CartActivity) {
                ((CartActivity) context).updateTotalPrice();
            }
        });

        // Logic nút Tăng số lượng
        holder.increaseButton.setOnClickListener(v -> {
            int currentQuantity = cartItem.getQuantity();
            if (currentQuantity < product.getStock()) {
                cartManager.updateQuantity(product, currentQuantity + 1);
            } else {
                Toast.makeText(context, "Số lượng đã đạt tối đa trong kho", Toast.LENGTH_SHORT).show();
            }
        });

        // Logic nút Giảm số lượng
        holder.decreaseButton.setOnClickListener(v -> {
            int currentQuantity = cartItem.getQuantity();
            if (currentQuantity > 1) {
                cartManager.updateQuantity(product, currentQuantity - 1);
            } 
        });

        // Logic nút Xóa sản phẩm
        holder.removeButton.setOnClickListener(v -> {
            cartManager.removeProduct(product);
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView name, price, quantity;
        ImageButton removeButton, increaseButton, decreaseButton;
        CheckBox checkBox;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.cart_item_image);
            name = itemView.findViewById(R.id.cart_item_name);
            price = itemView.findViewById(R.id.cart_item_price);
            quantity = itemView.findViewById(R.id.text_quantity); 
            removeButton = itemView.findViewById(R.id.btn_remove_from_cart);
            increaseButton = itemView.findViewById(R.id.btn_increase_quantity); 
            decreaseButton = itemView.findViewById(R.id.btn_decrease_quantity);
            checkBox = itemView.findViewById(R.id.cart_item_checkbox);
        }
    }
}
