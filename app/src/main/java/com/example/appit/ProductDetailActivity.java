package com.example.appit;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";
    private FirebaseFirestore db;
    private Product currentProduct;
    private ReviewAdapter reviewAdapter;
    private final List<Product.Review> reviewList = new ArrayList<>();
    private Button btnAddReview;
    private boolean isFavorite = false; // Trạng thái yêu thích

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        Button btnAddToCart = findViewById(R.id.btnAddToCart);
        btnAddReview = findViewById(R.id.btn_add_review);
        ImageButton btnAddToWishlist = findViewById(R.id.btnAddToWishlist);
        
        RecyclerView reviewsRecyclerView = findViewById(R.id.reviews_recycler_view);
        reviewsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        reviewAdapter = new ReviewAdapter(this, reviewList);
        reviewsRecyclerView.setAdapter(reviewAdapter);

        btnAddToCart.setOnClickListener(v -> {
            if (currentProduct != null) {
                CartManager.getInstance().addProduct(currentProduct);
                Toast.makeText(this, "Đã thêm vào giỏ hàng", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không thể thêm sản phẩm, vui lòng thử lại", Toast.LENGTH_SHORT).show();
            }
        });

        // Logic xử lý nút yêu thích
        btnAddToWishlist.setOnClickListener(v -> {
            if (currentProduct == null) return;
            toggleWishlist(btnAddToWishlist);
        });

        btnAddReview.setOnClickListener(v -> showAddReviewDialog());

        String documentId = getIntent().getStringExtra("PRODUCT_ID");
        if (documentId != null && !documentId.isEmpty()) {
            loadProductDetails(documentId);
            checkCanReview(documentId);
            checkIfFavorite(documentId, btnAddToWishlist);
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
                                currentProduct.setDocumentId(documentSnapshot.getId());
                                populateUI(currentProduct);
                                
                                // Load reviews
                                if (currentProduct.getReviews() != null) {
                                    reviewList.clear();
                                    reviewList.addAll(currentProduct.getReviews());
                                    reviewAdapter.notifyDataSetChanged();
                                }
                            }
                        } catch (Exception e) {
                             Log.e(TAG, "Error parsing product object", e);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading product details", e);
                });
    }

    private void checkCanReview(String productDocId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            btnAddReview.setVisibility(View.GONE);
            return;
        }

        db.collection("orders")
            .whereEqualTo("userId", currentUser.getUid())
            .whereEqualTo("status", "Đã hoàn thành")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                boolean hasBought = false;
                if (!queryDocumentSnapshots.isEmpty()) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        for (Order.OrderItem item : order.getItems()) {
                            if (currentProduct != null && item.getProductId().equals(String.valueOf(currentProduct.getId()))) {
                                hasBought = true;
                                break;
                            }
                        }
                        if (hasBought) break;
                    }
                }
                
                if (hasBought) {
                    btnAddReview.setVisibility(View.VISIBLE);
                } else {
                    btnAddReview.setVisibility(View.GONE);
                }
            });
    }
    
    // Kiểm tra sản phẩm có trong danh sách yêu thích không
    private void checkIfFavorite(String productId, ImageButton btnWishlist) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid())
            .collection("wishlist").document(productId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    isFavorite = true;
                    btnWishlist.setImageResource(R.drawable.ic_favorite); // Tim đặc
                } else {
                    isFavorite = false;
                    btnWishlist.setImageResource(R.drawable.ic_favorite_border); // Tim rỗng
                }
            });
    }

    // Thêm hoặc xóa khỏi danh sách yêu thích
    private void toggleWishlist(ImageButton btnWishlist) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show();
            return;
        }

        String productId = currentProduct.getDocumentId();
        if (isFavorite) {
            // Xóa khỏi wishlist
            db.collection("users").document(currentUser.getUid())
                .collection("wishlist").document(productId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    isFavorite = false;
                    btnWishlist.setImageResource(R.drawable.ic_favorite_border);
                    Toast.makeText(this, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                });
        } else {
            // Thêm vào wishlist
            db.collection("users").document(currentUser.getUid())
                .collection("wishlist").document(productId)
                .set(currentProduct) // Lưu thông tin sản phẩm vào subcollection wishlist
                .addOnSuccessListener(aVoid -> {
                    isFavorite = true;
                    btnWishlist.setImageResource(R.drawable.ic_favorite);
                    Toast.makeText(this, "Đã thêm vào yêu thích", Toast.LENGTH_SHORT).show();
                });
        }
    }

    private void showAddReviewDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_add_review, null);
        builder.setView(view);

        RatingBar ratingBar = view.findViewById(R.id.dialog_rating_bar);
        TextInputEditText commentInput = view.findViewById(R.id.dialog_review_comment);

        builder.setPositiveButton("Gửi đánh giá", (dialog, which) -> {
            float rating = ratingBar.getRating();
            String comment = "";
            if (commentInput.getText() != null) {
                comment = commentInput.getText().toString();
            }
            submitReview(rating, comment);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void submitReview(float rating, String comment) {
        if (currentProduct == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        db.collection("users").document(currentUser.getUid()).get()
            .addOnSuccessListener(documentSnapshot -> {
                User user = documentSnapshot.toObject(User.class);
                String userName = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "User";
                
                Product.Review newReview = new Product.Review();
                newReview.setRating((int) rating);
                newReview.setComment(comment);
                newReview.setReviewerName(userName);
                newReview.setTimestamp(new Date());
                
                if (currentProduct.getReviews() == null) {
                    currentProduct.setReviews(new ArrayList<>());
                }
                currentProduct.getReviews().add(newReview);
                
                double totalRating = 0;
                for (Product.Review r : currentProduct.getReviews()) {
                    totalRating += r.getRating();
                }
                double avgRating = totalRating / currentProduct.getReviews().size();
                
                db.collection("products").document(currentProduct.getDocumentId())
                    .update(
                        "reviews", currentProduct.getReviews(),
                        "rating", avgRating
                    )
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Cảm ơn bạn đã đánh giá!", Toast.LENGTH_SHORT).show();
                        loadProductDetails(currentProduct.getDocumentId()); 
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi khi gửi đánh giá", Toast.LENGTH_SHORT).show());
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
        
        // SỬA: Ánh xạ các view RatingBar và TextView rating mới
        RatingBar ratingBarIndicator = findViewById(R.id.ratingBarIndicator);
        TextView ratingTextIndicator = findViewById(R.id.ratingTextIndicator);
        
        // Hiển thị số lượng đã bán
        TextView textSoldQuantity = findViewById(R.id.productSoldQuantity);
        if (textSoldQuantity != null) {
            textSoldQuantity.setText("Đã bán: " + product.getMinimumOrderQuantity());
        }
        
        TextView textDimensions = findViewById(R.id.productDimensions);
        TextView textWarranty = findViewById(R.id.productWarranty);
        TextView textShipping = findViewById(R.id.productShipping);
        TextView textReturn = findViewById(R.id.productReturn);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(product.getTitle());
        }
        textName.setText(product.getTitle());
        
        // Set giá trị cho Rating Bar
        ratingBarIndicator.setRating((float) product.getRating());
        ratingTextIndicator.setText(String.format(java.util.Locale.US, "(%.1f)", product.getRating()));
        
        String priceWithUnit = product.getPrice();
        if (!priceWithUnit.toLowerCase().contains("vnd")) {
             priceWithUnit += " VND";
        }
        textPrice.setText(priceWithUnit);
        
        textDescription.setText(product.getDescription());

        if (product.getImages() != null && !product.getImages().isEmpty()) {
            Glide.with(this).load(product.getImages().get(0)).into(imageView);
        } else {
            Glide.with(this).load(product.getThumbnail()).into(imageView);
        }

        textBrand.setText("Thương hiệu: " + product.getBrand());
        textCategory.setText("Danh mục: " + product.getCategory());
        textStock.setText("Trong kho: " + product.getStock());
        
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
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}
