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

public class RelatedProductsAdapter extends RecyclerView.Adapter<RelatedProductsAdapter.ViewHolder> {

    private final Context context;
    private final List<Product> productList;

    public RelatedProductsAdapter(Context context, List<Product> productList) {
        this.context = context;
        this.productList = productList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_related_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = productList.get(position);

        holder.nameView.setText(product.getTitle());
        
        String priceWithUnit = product.getPrice();
        if (!priceWithUnit.toLowerCase().contains("vnd")) {
             priceWithUnit += " VND";
        }
        holder.priceView.setText(priceWithUnit);
        
        holder.ratingView.setText(String.format("%.1f", product.getRating()));

        Glide.with(context)
                .load(product.getThumbnail())
                .placeholder(R.drawable.shoe_placeholder)
                .into(holder.imageView);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ProductDetailActivity.class);
            // Pass full object if available, otherwise just ID logic handled in Activity
            if (product.getDocumentId() != null) {
                 intent.putExtra("PRODUCT_ID", product.getDocumentId());
            }
            // Also pass serializable object as backup/primary
            intent.putExtra("product", product);
            
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView nameView;
        TextView priceView;
        TextView ratingView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.relatedProductImage);
            nameView = itemView.findViewById(R.id.relatedProductName);
            priceView = itemView.findViewById(R.id.relatedProductPrice);
            ratingView = itemView.findViewById(R.id.relatedProductRating);
        }
    }
}
