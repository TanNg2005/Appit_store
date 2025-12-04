package com.example.appit;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import me.relex.circleindicator.CircleIndicator3;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CATEGORY = 1001;

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
    private TextView cartBadgeTextView;

    // Banner components
    private ViewPager2 bannerViewPager;
    private CircleIndicator3 bannerIndicator;
    private Handler bannerHandler;
    private Runnable bannerRunnable;

    // Nav Filter Buttons
    private MaterialButton btnNavFeatured, btnNavSale, btnNavNew, btnNavVoucher, btnNavFavorite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi động Service theo dõi đơn hàng
        startService(new Intent(this, OrderMonitorService.class));

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        setupToolbar();
        setupDrawer();
        updateNavHeader();
        checkAdminStatus();

        // Init Handler here to ensure it's associated with the main looper
        bannerHandler = new Handler(Looper.getMainLooper());

        // Setup Banner
        setupBanner();

        // Setup Nav Buttons
        setupNavButtons();

        recyclerView = findViewById(R.id.product_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        
        productList = new ArrayList<>();
        fullProductList = new ArrayList<>();
        adapter = new GridAdapter(this, productList);
        recyclerView.setAdapter(adapter);

        loadProducts();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    if (productList.size() != fullProductList.size()) {
                        filterByText("");
                        // Reset Nav Buttons visually to "Featured"
                        if (btnNavFeatured != null) updateNavButtonState(btnNavFeatured);
                        Toast.makeText(MainActivity.this, "Đã xóa bộ lọc", Toast.LENGTH_SHORT).show();
                    } else {
                        setEnabled(false);
                        getOnBackPressedDispatcher().onBackPressed();
                    }
                }
            }
        });
    }

    private void setupNavButtons() {
        btnNavFeatured = findViewById(R.id.btn_nav_featured);
        btnNavSale = findViewById(R.id.btn_nav_sale);
        btnNavNew = findViewById(R.id.btn_nav_new);
        btnNavVoucher = findViewById(R.id.btn_nav_voucher);
        btnNavFavorite = findViewById(R.id.btn_nav_favorite);

        View.OnClickListener navClickListener = v -> {
            MaterialButton btn = (MaterialButton) v;
            updateNavButtonState(btn);
            
            int id = v.getId();
            if (id == R.id.btn_nav_featured) {
                filterByFeatured();
            } else if (id == R.id.btn_nav_sale) {
                filterBySale();
            } else if (id == R.id.btn_nav_new) {
                filterByNew();
            } else if (id == R.id.btn_nav_voucher) {
                Toast.makeText(this, "Voucher functionality coming soon!", Toast.LENGTH_SHORT).show();
            } else if (id == R.id.btn_nav_favorite) {
                filterByHighRating();
            }
        };

        if (btnNavFeatured != null) btnNavFeatured.setOnClickListener(navClickListener);
        if (btnNavSale != null) btnNavSale.setOnClickListener(navClickListener);
        if (btnNavNew != null) btnNavNew.setOnClickListener(navClickListener);
        if (btnNavVoucher != null) btnNavVoucher.setOnClickListener(navClickListener);
        if (btnNavFavorite != null) btnNavFavorite.setOnClickListener(navClickListener);
        
        // Set initial state
        if (btnNavFeatured != null) updateNavButtonState(btnNavFeatured);
    }

    private void updateNavButtonState(MaterialButton selectedButton) {
        MaterialButton[] buttons = {btnNavFeatured, btnNavSale, btnNavNew, btnNavVoucher, btnNavFavorite};
        
        // Using ContextCompat logic for colors to avoid deprecated method warnings
        int colorPrimary = androidx.core.content.ContextCompat.getColor(this, R.color.purple_500);
        int colorWhite = androidx.core.content.ContextCompat.getColor(this, R.color.white);
        
        for (MaterialButton btn : buttons) {
            if (btn == null) continue; 
            if (btn == selectedButton) {
                btn.setBackgroundTintList(ColorStateList.valueOf(colorPrimary));
                btn.setTextColor(colorWhite);
                btn.setStrokeWidth(0);
            } else {
                btn.setBackgroundTintList(ColorStateList.valueOf(Color.TRANSPARENT));
                btn.setTextColor(colorPrimary);
                btn.setStrokeColor(ColorStateList.valueOf(colorPrimary));
                btn.setStrokeWidth(3); 
            }
        }
    }

    private void filterByFeatured() {
        productList.clear();
        // Sort by Sales (minimumOrderQuantity) descending for "Featured" as requested
        List<Product> sorted = new ArrayList<>(fullProductList);
        sorted.sort((p1, p2) -> Integer.compare(p2.getMinimumOrderQuantity(), p1.getMinimumOrderQuantity()));
        
        productList.addAll(sorted);
        adapter.notifyDataSetChanged();
        
        TextView textWelcome = findViewById(R.id.textWelcome);
        if(textWelcome != null) textWelcome.setText("Sản phẩm nổi bật");
    }

    private void filterByHighRating() {
        productList.clear();
        // Filter products with rating > 4.0
        List<Product> filtered = fullProductList.stream()
            .filter(p -> p.getRating() > 4.0)
            .collect(Collectors.toList());
            
        // Sort by Rating descending
        filtered.sort((p1, p2) -> Double.compare(p2.getRating(), p1.getRating()));
        
        productList.addAll(filtered);
        adapter.notifyDataSetChanged();
        
        TextView textWelcome = findViewById(R.id.textWelcome);
        if(textWelcome != null) textWelcome.setText("Sản phẩm yêu thích");
    }

    private void filterBySale() {
        productList.clear();
        List<Product> filtered = fullProductList.stream()
            .filter(p -> p.getDiscountPercentage() > 0)
            .collect(Collectors.toList());
        productList.addAll(filtered);
        adapter.notifyDataSetChanged();
        
        TextView textWelcome = findViewById(R.id.textWelcome);
        if(textWelcome != null) textWelcome.setText("Đang giảm giá");
    }

    private void filterByNew() {
        productList.clear();
        List<Product> sorted = new ArrayList<>(fullProductList);
        // Sort by ID descending (assuming newer products have higher IDs)
        sorted.sort((p1, p2) -> {
             Long id1 = p1.getId() != null ? p1.getId() : 0L;
             Long id2 = p2.getId() != null ? p2.getId() : 0L;
             return id2.compareTo(id1);
        });
        productList.addAll(sorted);
        adapter.notifyDataSetChanged();
        
        TextView textWelcome = findViewById(R.id.textWelcome);
        if(textWelcome != null) textWelcome.setText("Sản phẩm mới");
    }

    private void setupBanner() {
        bannerViewPager = findViewById(R.id.banner_view_pager);
        bannerIndicator = findViewById(R.id.banner_indicator);

        List<Integer> bannerImages = Arrays.asList(
                R.drawable.banner_1,
                R.drawable.banner_2,
                R.drawable.banner_3,
                R.drawable.banner_4,
                R.drawable.banner_5
        );

        BannerAdapter bannerAdapter = new BannerAdapter(this, bannerImages);
        bannerViewPager.setAdapter(bannerAdapter);
        bannerIndicator.setViewPager(bannerViewPager);

        // Auto scroll logic fixed to avoid memory issues
        bannerRunnable = new Runnable() {
            @Override
            public void run() {
                if (bannerViewPager.getAdapter() != null) {
                    int currentItem = bannerViewPager.getCurrentItem();
                    int totalItems = bannerViewPager.getAdapter().getItemCount();
                    
                    if (totalItems > 0) {
                        int nextItem = (currentItem + 1) % totalItems;
                        bannerViewPager.setCurrentItem(nextItem, true);
                    }
                    
                    bannerHandler.postDelayed(this, 3000); // 3 seconds
                }
            }
        };
        
        bannerViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                bannerHandler.removeCallbacks(bannerRunnable);
                bannerHandler.postDelayed(bannerRunnable, 3000);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenForUnreadNotifications();
        CartManager.getInstance().loadCartItems(items -> {
            updateCartBadge();
        });
        updateNavHeader();
        
        // Resume banner auto scroll
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable); // Xóa callback cũ để tránh bị duplicate
            bannerHandler.postDelayed(bannerRunnable, 3000);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (notificationListener != null) {
            notificationListener.remove();
        }
        // Pause banner auto scroll
        if (bannerHandler != null && bannerRunnable != null) {
            bannerHandler.removeCallbacks(bannerRunnable);
        }
    }

    // ... (Rest of the methods remain unchanged)
    
    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Appit Store");
        }
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
                if (documentSnapshot.exists() && Boolean.TRUE.equals(documentSnapshot.getBoolean("isAdmin"))) {
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
                        // Initial filter (Featured)
                        filterByFeatured(); 
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                    }
                });
    }

    private void filterByText(String query) {
        productList.clear();
        if (query == null || query.isEmpty()) {
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

    private void filterByCategory(String category) {
        productList.clear();
        if (category == null || category.isEmpty()) {
            productList.addAll(fullProductList);
        } else {
            List<Product> filteredList = fullProductList.stream()
                .filter(p -> p.getCategory() != null && p.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
            productList.addAll(filteredList);
        }
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "Đang hiển thị: " + category, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CATEGORY && resultCode == RESULT_OK && data != null) {
            String selectedCategory = data.getStringExtra("SELECTED_CATEGORY");
            if (selectedCategory != null) {
                filterByCategory(selectedCategory);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        
        final MenuItem notificationItem = menu.findItem(R.id.action_notification);
        View notificationActionView = notificationItem.getActionView();
        notificationBadgeTextView = notificationActionView.findViewById(R.id.notification_badge);

        notificationActionView.setOnClickListener(v -> onOptionsItemSelected(notificationItem));
        listenForUnreadNotifications();

        final MenuItem cartItem = menu.findItem(R.id.action_cart);
        View cartActionView = cartItem.getActionView();
        cartBadgeTextView = cartActionView.findViewById(R.id.cart_badge);
        
        cartActionView.setOnClickListener(v -> {
            startActivity(new Intent(this, CartActivity.class));
        });
        
        CartManager.getInstance().loadCartItems(items -> {
            updateCartBadge();
        });

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
    
    private void updateCartBadge() {
        if (cartBadgeTextView != null) {
            int cartCount = CartManager.getInstance().getTotalItemCount();
            if (cartCount > 0) {
                cartBadgeTextView.setText(String.valueOf(cartCount));
                cartBadgeTextView.setVisibility(View.VISIBLE);
            } else {
                cartBadgeTextView.setVisibility(View.GONE);
            }
        }
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
                                if (doc.contains("isRead") && Boolean.FALSE.equals(doc.getBoolean("isRead"))) {
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
        TextView navHeaderName = headerView.findViewById(R.id.nav_header_name);
        TextView navHeaderEmail = headerView.findViewById(R.id.nav_header_email);
        
        navHeaderName.setText("APPIT STORE");
        
        FirebaseUser currentUser = mAuth.getCurrentUser();
        
        if (currentUser != null) {
            String authDisplayName = currentUser.getDisplayName();
            if (authDisplayName != null && !authDisplayName.isEmpty()) {
                 navHeaderEmail.setText(authDisplayName);
            } else {
                 db.collection("users").document(currentUser.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                       if (documentSnapshot.exists()) {
                           if (documentSnapshot.contains("displayName")) {
                               String dbDisplayName = documentSnapshot.getString("displayName");
                               if (dbDisplayName != null && !dbDisplayName.isEmpty()) {
                                   navHeaderEmail.setText(dbDisplayName);
                                   return;
                               }
                           }
                           
                           String firstName = documentSnapshot.getString("firstName");
                           String lastName = documentSnapshot.getString("lastName");
                           String fullName = "";
                           
                           if (firstName != null) fullName += firstName;
                           if (lastName != null) {
                               if (!fullName.isEmpty()) fullName += " ";
                               fullName += lastName;
                           }
                           
                           if (!fullName.isEmpty()) {
                               navHeaderEmail.setText(fullName);
                           } else {
                               navHeaderEmail.setText(currentUser.getEmail());
                           }
                       } else {
                           navHeaderEmail.setText(currentUser.getEmail());
                       }
                    })
                    .addOnFailureListener(e -> {
                        navHeaderEmail.setText(currentUser.getEmail());
                    });
            }
        } else {
            navHeaderEmail.setText("Vui lòng đăng nhập");
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
            filterByText("");
        } else if (id == R.id.nav_category) {
            // startActivityForResult is deprecated but kept for minimal changes as requested
            // Ideally should use ActivityResultLauncher
            startActivityForResult(new Intent(this, CategoryActivity.class), REQUEST_CATEGORY);
        } else if (id == R.id.nav_cart) {
            startActivity(new Intent(this, CartActivity.class));
        } else if (id == R.id.nav_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
        } else if (id == R.id.nav_filter_advanced) {
            startActivity(new Intent(this, FilterActivity.class));
        } else if (id == R.id.nav_admin_orders) {
            startActivity(new Intent(this, AdminOrdersActivity.class));
        } else if (id == R.id.nav_admin_products) {
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