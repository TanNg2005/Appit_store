package com.example.appit;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.util.List;
import java.text.NumberFormat;
import java.util.Locale;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> productList;

    public GridAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.grid_item, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.nameView.setText(product.getTitle());

        // Format price to currency (VND)
        double priceValue = 0;
        try {
            if (product.getPrice() != null) {
                // Loại bỏ tất cả các ký tự không phải số (giữ lại số 0-9)
                // Ví dụ: "3.500.000" -> "3500000"
                String cleanPrice = product.getPrice().replaceAll("[^\\d]", "");
                if(!cleanPrice.isEmpty()) {
                     priceValue = Double.parseDouble(cleanPrice);
                }
            }
        } catch (NumberFormatException e) {
            priceValue = 0;
        }

        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));

        if (product.getDiscountPercentage() > 0) {
            // Có giảm giá
            double discountedPrice = priceValue * (1 - product.getDiscountPercentage() / 100);
            
            // Làm tròn đến hàng nghìn
            // Ví dụ: 123150 -> 123000
            long roundedDiscountedPrice = Math.round(discountedPrice / 1000) * 1000;
            
            // Giá hiện tại (màu đỏ)
            holder.priceView.setText(formatter.format(roundedDiscountedPrice) + "đ");
            
            // Giá gốc (gạch ngang) - Giá gốc có thể cũng cần hiển thị đẹp
            holder.originalPriceView.setText(formatter.format(priceValue) + "đ");
            holder.originalPriceView.setPaintFlags(holder.originalPriceView.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.originalPriceView.setVisibility(View.VISIBLE);
            
            // Badge % giảm
            holder.badgeDiscount.setText("-" + (int)product.getDiscountPercentage() + "%");
            holder.badgeDiscount.setVisibility(View.VISIBLE);
        } else {
            // Không giảm giá
            holder.priceView.setText(formatter.format(priceValue) + "đ");
            holder.originalPriceView.setVisibility(View.GONE);
            holder.badgeDiscount.setVisibility(View.GONE);
        }

        // SET ĐÁNH GIÁ
        // Check if rating is 0, maybe default to something or hide?
        // For now just showing whatever is in model
        if (product.getRating() > 0) {
             holder.ratingView.setText(String.format(Locale.US, "%.1f", product.getRating()));
        } else {
             holder.ratingView.setText("5.0"); // Default visually
        }
        
        // Số lượng đã bán (Dùng tạm field minimumOrderQuantity hoặc một field khác nếu có)
        // Giả lập hiển thị "Đã bán 1k+" nếu chưa có field 'sold'
        if (product.getMinimumOrderQuantity() > 0) {
             holder.soldView.setText("Đã bán " + product.getMinimumOrderQuantity());
        } else {
             holder.soldView.setText("Đã bán 0");
        }

        Glide.with(context)
                .load(product.getThumbnail())
                .placeholder(R.drawable.shoe_placeholder)
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.getDocumentId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameView;
        TextView priceView;
        TextView originalPriceView;
        TextView badgeDiscount;
        TextView ratingView; 
        TextView soldView;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.gridImage);
            nameView = itemView.findViewById(R.id.gridText);
            priceView = itemView.findViewById(R.id.gridPrice);
            
            // New views
            originalPriceView = itemView.findViewById(R.id.gridOriginalPrice);
            badgeDiscount = itemView.findViewById(R.id.badgeDiscount);
            ratingView = itemView.findViewById(R.id.gridRatingText);
            soldView = itemView.findViewById(R.id.gridSoldText);
        }
    }
}