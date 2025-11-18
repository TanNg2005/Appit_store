package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<Product> productList;
    private List<Product> fullProductList;
    private GridAdapter adapter;
    private TextView cartBadgeTextView; // Biến cho badge

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupToolbar();
        setupDrawer();
        updateNavHeader();

        recyclerView = findViewById(R.id.product_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        
        productList = new ArrayList<>();
        fullProductList = new ArrayList<>();
        adapter = new GridAdapter(this, productList);
        recyclerView.setAdapter(adapter);

        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cập nhật badge mỗi khi quay lại màn hình chính
        if (cartBadgeTextView != null) {
            updateCartBadge();
        }
    }

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Appit Store");
    }

    private void setupDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void loadProducts() {
        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fullProductList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Product product = document.toObject(Product.class);
                            product.setId(document.getId());
                            fullProductList.add(product);
                        }
                        filter("");
                    } else {
                        Toast.makeText(MainActivity.this, "Error loading products.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void filter(String query) {
        productList.clear();
        if (query.isEmpty()) {
            productList.addAll(fullProductList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            for (Product product : fullProductList) {
                if (product.getTitle().toLowerCase().contains(lowerCaseQuery)) {
                    productList.add(product);
                    continue;
                }
                if (product.getTags() != null) {
                    for (String tag : product.getTags()) {
                        if (tag.toLowerCase().contains(lowerCaseQuery)) {
                            productList.add(product);
                            break;
                        }
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        
        // --- Thiết lập Badge cho giỏ hàng ---
        final MenuItem cartItem = menu.findItem(R.id.action_cart);
        View actionView = cartItem.getActionView();
        cartBadgeTextView = actionView.findViewById(R.id.cart_badge);

        actionView.setOnClickListener(v -> onOptionsItemSelected(cartItem));

        updateCartBadge();
        // -------------------------------------

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filter(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filter(newText);
                return true;
            }
        });

        return true;
    }

    public void updateCartBadge() {
        int totalItemCount = CartManager.getInstance().getTotalItemCount();
        if (cartBadgeTextView != null) {
            if (totalItemCount > 0) {
                cartBadgeTextView.setText(String.valueOf(totalItemCount));
                cartBadgeTextView.setVisibility(View.VISIBLE);
            } else {
                cartBadgeTextView.setVisibility(View.GONE);
            }
        }
    }

    private void updateNavHeader() {
        View headerView = navigationView.getHeaderView(0);
        TextView navHeaderEmail = headerView.findViewById(R.id.nav_header_email);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navHeaderEmail.setText(currentUser.getEmail());
        } else {
            navHeaderEmail.setText("Vui lòng đăng nhập");
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_cart) {
            startActivity(new Intent(this, CartActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            // Already home
        } else if (id == R.id.nav_cart) {
            startActivity(new Intent(this, CartActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_logout) {
            logoutUser();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logoutUser() {
        mAuth.signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}