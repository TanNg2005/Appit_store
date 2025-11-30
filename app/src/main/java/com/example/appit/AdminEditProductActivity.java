package com.example.appit;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class AdminEditProductActivity extends AppCompatActivity {

    private TextInputEditText title, description, price, stock, brand, category, thumbnail, discount;
    private FirebaseFirestore db;
    private String productId;
    private Product currentProduct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_product);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.admin_edit_product_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        title = findViewById(R.id.edit_text_product_title);
        description = findViewById(R.id.edit_text_product_description);
        price = findViewById(R.id.edit_text_product_price);
        stock = findViewById(R.id.edit_text_product_stock);
        brand = findViewById(R.id.edit_text_product_brand);
        category = findViewById(R.id.edit_text_product_category);
        thumbnail = findViewById(R.id.edit_text_product_thumbnail);
        discount = findViewById(R.id.edit_text_product_discount); // Initialize discount field
        Button saveButton = findViewById(R.id.btn_save_product);

        productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId != null) {
            getSupportActionBar().setTitle("Sửa sản phẩm");
            loadProductDetails();
        } else {
            getSupportActionBar().setTitle("Thêm sản phẩm mới");
        }

        saveButton.setOnClickListener(v -> saveProduct());
    }

    private void loadProductDetails() {
        db.collection("products").document(productId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                currentProduct = documentSnapshot.toObject(Product.class);
                if (currentProduct != null) {
                    title.setText(currentProduct.getTitle());
                    description.setText(currentProduct.getDescription());
                    price.setText(currentProduct.getPrice());
                    stock.setText(String.valueOf(currentProduct.getStock()));
                    brand.setText(currentProduct.getBrand());
                    category.setText(currentProduct.getCategory());
                    thumbnail.setText(currentProduct.getThumbnail());
                    discount.setText(String.valueOf(currentProduct.getDiscountPercentage())); // Set discount value
                }
            }
        });
    }

    private void saveProduct() {
        String titleStr = title.getText().toString().trim();
        String descStr = description.getText().toString().trim();
        String priceStr = price.getText().toString().trim();
        int stockInt = 0;
        try {
             stockInt = Integer.parseInt(stock.getText().toString().trim());
        } catch (NumberFormatException e) {
             stockInt = 0;
        }
        
        String brandStr = brand.getText().toString().trim();
        String categoryStr = category.getText().toString().trim();
        String thumbStr = thumbnail.getText().toString().trim();
        
        double discountDouble = 0.0;
        try {
             discountDouble = Double.parseDouble(discount.getText().toString().trim());
        } catch (NumberFormatException e) {
             discountDouble = 0.0;
        }

        DocumentReference productRef;
        if (productId != null) {
            productRef = db.collection("products").document(productId);
        } else {
            productRef = db.collection("products").document();
        }
        
        if (currentProduct == null) {
            currentProduct = new Product();
        }

        currentProduct.setTitle(titleStr);
        currentProduct.setDescription(descStr);
        currentProduct.setPrice(priceStr);
        currentProduct.setStock(stockInt);
        currentProduct.setBrand(brandStr);
        currentProduct.setCategory(categoryStr);
        currentProduct.setThumbnail(thumbStr);
        currentProduct.setDiscountPercentage(discountDouble); // Save discount value

        productRef.set(currentProduct)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(AdminEditProductActivity.this, "Đã lưu sản phẩm", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(AdminEditProductActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
