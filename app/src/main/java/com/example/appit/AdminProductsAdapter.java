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

public class AdminProductsAdapter extends RecyclerView.Adapter<AdminProductsAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Product> productList;
    private final ProductInteractionListener listener;

    public interface ProductInteractionListener {
        void onEditProduct(Product product);
        void onDeleteProduct(Product product);
    }

    public AdminProductsAdapter(Context context, List<Product> productList, ProductInteractionListener listener) {
        this.context = context;
        this.productList = productList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.admin_product_item, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = productList.get(position);
        holder.bind(product, listener);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImage;
        TextView productTitle, productPrice, productStock;
        ImageButton editButton, deleteButton;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            productImage = itemView.findViewById(R.id.product_image);
            productTitle = itemView.findViewById(R.id.product_title);
            productPrice = itemView.findViewById(R.id.product_price);
            productStock = itemView.findViewById(R.id.product_stock);
            editButton = itemView.findViewById(R.id.btn_edit_product);
            deleteButton = itemView.findViewById(R.id.btn_delete_product);
        }

        public void bind(final Product product, final ProductInteractionListener listener) {
            productTitle.setText(product.getTitle());
            productPrice.setText("Giá: " + product.getPrice());
            productStock.setText("Kho: " + product.getStock());

            Glide.with(itemView.getContext())
                    .load(product.getThumbnail())
                    .placeholder(R.drawable.shoe_placeholder)
                    .into(productImage);

            editButton.setOnClickListener(v -> listener.onEditProduct(product));
            deleteButton.setOnClickListener(v -> listener.onDeleteProduct(product));
        }
    }
}
