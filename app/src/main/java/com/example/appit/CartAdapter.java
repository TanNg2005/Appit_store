package com.example.appit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

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

        // Logic hiển thị giá có giảm giá
        double priceValue = 0;
        try {
            String cleanPrice = product.getPrice().replaceAll("[^\\d]", "");
            if (!cleanPrice.isEmpty()) {
                priceValue = Double.parseDouble(cleanPrice);
            }
        } catch (Exception e) {
            priceValue = 0;
        }

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

        if (product.getDiscountPercentage() > 0) {
            double discountedPrice = priceValue * (1 - product.getDiscountPercentage() / 100);
            // Làm tròn đến hàng nghìn
            long roundedDiscountedPrice = Math.round(discountedPrice / 1000) * 1000;
            
            holder.price.setText(formatter.format(roundedDiscountedPrice) + " VND");
            // Có thể thêm logic hiển thị giá gốc gạch ngang nếu layout hỗ trợ, 
            // nhưng hiện tại chỉ cần hiển thị giá đúng để thanh toán.
        } else {
            holder.price.setText(formatter.format(priceValue) + " VND");
        }
        
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

        // Bấm vào item -> chuyển sang trang chi tiết sản phẩm
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            // Truyền cả object product nếu có thể, hoặc ID document
            if (product.getDocumentId() != null) {
                intent.putExtra("PRODUCT_ID", product.getDocumentId());
            }
            intent.putExtra("product", product);
            context.startActivity(intent);
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
