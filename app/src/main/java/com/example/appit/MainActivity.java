package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar toolbar;
    private RecyclerView recyclerView;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    private List<Product> productList;
    private List<Product> fullProductList;
    private GridAdapter adapter;
    private TextView notificationBadgeTextView;
    private ListenerRegistration notificationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupToolbar();
        setupDrawer();
        updateNavHeader();
        checkAdminStatus();

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
        listenForUnreadNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notificationListener != null) {
            notificationListener.remove();
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

    private void checkAdminStatus() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            DocumentReference userRef = db.collection("users").document(currentUser.getUid());
            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && documentSnapshot.contains("isAdmin") && documentSnapshot.getBoolean("isAdmin") == true) {
                    navigationView.getMenu().setGroupVisible(R.id.admin_panel_group, true);
                }
            });
        }
    }

    private void loadProducts() {
        db.collection("products")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        fullProductList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Product product = document.toObject(Product.class);
                                product.setDocumentId(document.getId()); 
                                fullProductList.add(product);
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document to Product object: " + document.getId(), e);
                            }
                        }
                        filterByText("");
                        adapter.notifyDataSetChanged();
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    private void filterByText(String query) {
        productList.clear();
        if (query.isEmpty()) {
            productList.addAll(fullProductList);
        } else {
            String lowerCaseQuery = query.toLowerCase();
            List<Product> filteredList = fullProductList.stream()
                .filter(p -> p.getTitle().toLowerCase().contains(lowerCaseQuery) || 
                             (p.getTags() != null && p.getTags().stream().anyMatch(tag -> tag.toLowerCase().contains(lowerCaseQuery))))
                .collect(Collectors.toList());
            productList.addAll(filteredList);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        
        final MenuItem notificationItem = menu.findItem(R.id.action_notification);
        View actionView = notificationItem.getActionView();
        notificationBadgeTextView = actionView.findViewById(R.id.notification_badge);

        actionView.setOnClickListener(v -> onOptionsItemSelected(notificationItem));

        listenForUnreadNotifications();

        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterByText(query);
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterByText(newText);
                return true;
            }
        });
        return true;
    }

    private void listenForUnreadNotifications() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) return;

        if (notificationListener != null) notificationListener.remove();

        notificationListener = db.collection("notifications")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }
                    if (snapshots != null) {
                        int unreadCount = 0;
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                if (doc.contains("isRead") && doc.getBoolean("isRead") == false) {
                                    unreadCount++;
                                }
                            } catch (Exception ex){
                                Log.e(TAG, "Error checking 'isRead' status for notification: " + doc.getId(), ex);
                            }
                        }
                        updateNotificationBadge(unreadCount);
                    }
                });
    }

    public void updateNotificationBadge(int count) {
        if (notificationBadgeTextView != null) {
            if (count > 0) {
                notificationBadgeTextView.setText(String.valueOf(count));
                notificationBadgeTextView.setVisibility(View.VISIBLE);
            } else {
                notificationBadgeTextView.setVisibility(View.GONE);
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
        if (id == R.id.action_notification) {
            startActivity(new Intent(this, NotificationActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_home) {
            // Do nothing
        } else if (id == R.id.nav_cart) {
            startActivity(new Intent(this, CartActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_filter_advanced) {
            startActivity(new Intent(this, FilterActivity.class));
        } else if (id == R.id.nav_admin_orders) {
            startActivity(new Intent(this, AdminOrdersActivity.class));
        } else if (id == R.id.nav_admin_products) {
            // SỬA LỖI: Mở màn hình quản lý sản phẩm
            startActivity(new Intent(this, AdminProductsActivity.class));
        } else if (id == R.id.nav_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_logout) {
            logoutUser();
        }

        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }

    private void logoutUser() {
        if (notificationListener != null) {
            notificationListener.remove();
        }
        CartManager.destroyInstance(); 
        mAuth.signOut();
        Intent intent = new Intent(MainActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
