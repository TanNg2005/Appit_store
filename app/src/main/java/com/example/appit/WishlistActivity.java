package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class WishlistActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WishlistAdapter adapter;
    private List<Product> wishlistProducts = new ArrayList<>();
    private FirebaseFirestore db;
    private ProgressBar progressBar;
    private TextView emptyText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wishlist);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.wishlist_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.nav_favorite);
        }

        progressBar = findViewById(R.id.wishlist_progress_bar);
        emptyText = findViewById(R.id.text_empty_wishlist);
        recyclerView = findViewById(R.id.wishlist_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new WishlistAdapter(this, wishlistProducts, new WishlistAdapter.OnItemActionListener() {
            @Override
            public void onRemoveClick(Product product) {
                removeFromWishlist(product);
            }

            @Override
            public void onItemClick(Product product) {
                Intent intent = new Intent(WishlistActivity.this, ProductDetailActivity.class);
                intent.putExtra("PRODUCT_ID", product.getDocumentId());
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);

        loadWishlist();
    }

    private void loadWishlist() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(currentUser.getUid())
                .collection("wishlist")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    wishlistProducts.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            Product product = doc.toObject(Product.class);
                            product.setDocumentId(doc.getId()); // Quan trọng để xóa sau này
                            wishlistProducts.add(product);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    if (wishlistProducts.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    } else {
                        emptyText.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.VISIBLE);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(WishlistActivity.this, "Lỗi tải danh sách: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void removeFromWishlist(Product product) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || product.getDocumentId() == null) return;

        db.collection("users").document(currentUser.getUid())
                .collection("wishlist").document(product.getDocumentId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    wishlistProducts.remove(product);
                    adapter.notifyDataSetChanged();
                    
                    if (wishlistProducts.isEmpty()) {
                        emptyText.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi xóa", Toast.LENGTH_SHORT).show());
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload khi quay lại từ màn hình chi tiết (trong trường hợp user bỏ thích ở đó)
        loadWishlist();
    }
}
