package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminProductsActivity extends AppCompatActivity implements AdminProductsAdapter.ProductInteractionListener {

    private RecyclerView recyclerView;
    private AdminProductsAdapter adapter;
    private List<Product> productList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_products);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.admin_products_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_view_admin_products);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        FloatingActionButton fab = findViewById(R.id.fab_add_product);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(AdminProductsActivity.this, AdminEditProductActivity.class);
            startActivity(intent);
        });

        loadProducts();
    }

    private void loadProducts() {
        db.collection("products").addSnapshotListener((snapshots, e) -> {
            if (e != null) return;
            productList.clear();
            for (QueryDocumentSnapshot doc : snapshots) {
                Product product = doc.toObject(Product.class);
                product.setDocumentId(doc.getId());
                productList.add(product);
            }
            adapter = new AdminProductsAdapter(this, productList, this);
            recyclerView.setAdapter(adapter);
        });
    }

    @Override
    public void onEditProduct(Product product) {
        Intent intent = new Intent(this, AdminEditProductActivity.class);
        intent.putExtra("PRODUCT_ID", product.getDocumentId());
        startActivity(intent);
    }

    @Override
    public void onDeleteProduct(Product product) {
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    db.collection("products").document(product.getDocumentId()).delete();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
