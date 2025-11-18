package com.example.appit;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";
    private FirebaseFirestore db;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Button btnAddToCart = findViewById(R.id.btnAddToCart);
        btnAddToCart.setOnClickListener(v -> {
            if (currentProduct != null) {
                CartManager.getInstance().addProduct(currentProduct);
                Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không thể thêm sản phẩm, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            }
        });

        // The ID passed from MainActivity/GridAdapter should be the Firestore Document ID
        String documentId = getIntent().getStringExtra("PRODUCT_ID");
        if (documentId != null && !documentId.isEmpty()) {
            loadProductDetails(documentId);
        } else {
            Toast.makeText(this, "Error: Không tìm thấy ID sản phẩm", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadProductDetails(String documentId) {
        ProgressBar progressBar = findViewById(R.id.productProgressBar);
        progressBar.setVisibility(View.VISIBLE);

        db.collection("products").document(documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    progressBar.setVisibility(View.GONE);
                    if (documentSnapshot.exists()) {
                        try {
                            currentProduct = documentSnapshot.toObject(Product.class);
                            if (currentProduct != null) {
                                // CORRECT FIX: Set the documentId, not the numeric id.
                                // The numeric 'id' field is already mapped by toObject().
                                currentProduct.setDocumentId(documentSnapshot.getId());
                                populateUI(currentProduct);
                            }
                        } catch (Exception e) {
                             Log.e(TAG, "Error parsing product object", e);
                             Toast.makeText(ProductDetailActivity.this, "Lỗi phân tích dữ liệu sản phẩm", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(ProductDetailActivity.this, "Không tìm thấy sản phẩm", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading product details", e);
                    Toast.makeText(ProductDetailActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void populateUI(Product product) {
        ImageView imageView = findViewById(R.id.productImage);
        TextView textName = findViewById(R.id.productName);
        TextView textPrice = findViewById(R.id.productPrice);
        TextView textDescription = findViewById(R.id.productDescription);
        TextView textBrand = findViewById(R.id.productBrand);
        TextView textCategory = findViewById(R.id.productCategory);
        TextView textStock = findViewById(R.id.productStock);
        TextView textRating = findViewById(R.id.productRating);
        TextView textDimensions = findViewById(R.id.productDimensions);
        TextView textWarranty = findViewById(R.id.productWarranty);
        TextView textShipping = findViewById(R.id.productShipping);
        TextView textReturn = findViewById(R.id.productReturn);

        getSupportActionBar().setTitle(product.getTitle());
        textName.setText(product.getTitle());
        textPrice.setText(product.getPrice());
        textDescription.setText(product.getDescription());

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            Glide.with(this).load(product.getImages().get(0)).into(imageView);
        } else {
            Glide.with(this).load(product.getThumbnail()).into(imageView);
        }

        textBrand.setText("Thương hiệu: " + product.getBrand());
        textCategory.setText("Danh mục: " + product.getCategory());
        textStock.setText("Trong kho: " + product.getStock());
        textRating.setText(String.format(java.util.Locale.US, "Đánh giá: %.2f / 5", product.getRating()));
        textWarranty.setText("Bảo hành: " + product.getWarrantyInformation());
        textShipping.setText("Vận chuyển: " + product.getShippingInformation());
        textReturn.setText("Chính sách đổi trả: " + product.getReturnPolicy());

        if (product.getDimensions() != null) {
            Product.Dimensions dims = product.getDimensions();
            String dimText = String.format(java.util.Locale.US, "Kích thước: %.2f x %.2f x %.2f cm", dims.getWidth(), dims.getHeight(), dims.getDepth());
            textDimensions.setText(dimText);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
