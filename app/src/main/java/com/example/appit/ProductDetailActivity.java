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
import android.graphics.Paint;
import java.text.NumberFormat;
import java.util.Locale;

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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";
    private FirebaseFirestore db;
    private Product currentProduct;
    private ReviewAdapter reviewAdapter;
    private RelatedProductsAdapter relatedProductsAdapter;
    private final List<Product.Review> reviewList = new ArrayList<>();
    private final List<Product> relatedProductList = new ArrayList<>();
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

        // Thiết lập RecyclerView cho sản phẩm liên quan
        RecyclerView relatedRecyclerView = findViewById(R.id.related_products_recycler_view);
        relatedRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        relatedProductsAdapter = new RelatedProductsAdapter(this, relatedProductList);
        relatedRecyclerView.setAdapter(relatedProductsAdapter);

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

        // Check for object first, then check for ID
        if (getIntent().hasExtra("product")) {
             try {
                 currentProduct = (Product) getIntent().getSerializableExtra("product");
                 if (currentProduct != null) {
                     populateUI(currentProduct);
                     
                     // If we only have basic info (like ID or partial info), we should try to load full details
                     // First check if we have a document ID (String) or product ID (Long)
                     if (currentProduct.getDocumentId() != null && !currentProduct.getDocumentId().isEmpty()) {
                         loadProductDetails(currentProduct.getDocumentId());
                         checkCanReview(currentProduct.getDocumentId());
                         checkIfFavorite(currentProduct.getDocumentId(), btnAddToWishlist);
                     } else if (currentProduct.getId() != null) {
                         // If we have numeric ID, we need to find the document ID
                         findProductByNumericId(currentProduct.getId(), btnAddToWishlist);
                     }
                 }
             } catch (Exception e) {
                 Log.e(TAG, "Error retrieving product from intent", e);
             }
        }

        String documentId = getIntent().getStringExtra("PRODUCT_ID");
        if (documentId != null && !documentId.isEmpty()) {
            loadProductDetails(documentId);
            checkCanReview(documentId);
            checkIfFavorite(documentId, btnAddToWishlist);
        } else if (currentProduct == null) {
            // Only show error if we didn't successfully load product from object
            Toast.makeText(this, "Error: Không tìm thấy ID sản phẩm", Toast.LENGTH_SHORT).show();
            // finish(); // Don't finish yet, let the UI show empty state or whatever was loaded
        }
    }

    private void findProductByNumericId(Long id, ImageButton btnAddToWishlist) {
        ProgressBar progressBar = findViewById(R.id.productProgressBar);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("products").whereEqualTo("id", id).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                     if (progressBar != null) progressBar.setVisibility(View.GONE);
                     if (!queryDocumentSnapshots.isEmpty()) {
                         QueryDocumentSnapshot document = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                         currentProduct = document.toObject(Product.class);
                         currentProduct.setDocumentId(document.getId());
                         populateUI(currentProduct);
                         
                         checkCanReview(document.getId());
                         checkIfFavorite(document.getId(), btnAddToWishlist);
                         
                         // Load reviews
                         if (currentProduct.getReviews() != null) {
                             reviewList.clear();
                             reviewList.addAll(currentProduct.getReviews());
                             reviewAdapter.notifyDataSetChanged();
                         }
                         
                         // Load related products
                         loadRelatedProducts();
                     }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error finding product by ID", e);
                });
    }

    private void loadProductDetails(String documentId) {
        ProgressBar progressBar = findViewById(R.id.productProgressBar);
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        db.collection("products").document(documentId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
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
                                
                                // Load related products
                                loadRelatedProducts();
                            }
                        } catch (Exception e) {
                             Log.e(TAG, "Error parsing product object", e);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading product details", e);
                });
    }

    private void loadRelatedProducts() {
        if (currentProduct == null || currentProduct.getTags() == null || currentProduct.getTags().isEmpty()) {
            return; // Nếu không có tags thì không tìm sản phẩm liên quan
        }

        List<String> tags = currentProduct.getTags();
        
        // Firestore giới hạn 'array-contains-any' tối đa 10 giá trị. 
        // Nếu tags nhiều hơn 10, chỉ lấy 10 cái đầu tiên.
        List<String> searchTags = tags.size() > 10 ? tags.subList(0, 10) : tags;

        db.collection("products")
                .whereArrayContainsAny("tags", searchTags)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Product> tempList = new ArrayList<>();
                    String currentId = currentProduct.getDocumentId();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        // Loại trừ sản phẩm hiện tại
                        if (!doc.getId().equals(currentId)) {
                            Product p = doc.toObject(Product.class);
                            p.setDocumentId(doc.getId());
                            tempList.add(p);
                        }
                    }

                    // Random và lấy tối đa 4 sản phẩm
                    Collections.shuffle(tempList);
                    relatedProductList.clear();
                    if (tempList.size() > 4) {
                        relatedProductList.addAll(tempList.subList(0, 4));
                    } else {
                        relatedProductList.addAll(tempList);
                    }

                    relatedProductsAdapter.notifyDataSetChanged();

                    // Hiển thị tiêu đề và RecyclerView nếu có sản phẩm
                    TextView titleView = findViewById(R.id.text_related_products);
                    RecyclerView recyclerView = findViewById(R.id.related_products_recycler_view);
                    
                    if (!relatedProductList.isEmpty()) {
                        titleView.setVisibility(View.VISIBLE);
                        recyclerView.setVisibility(View.VISIBLE);
                    } else {
                        titleView.setVisibility(View.GONE);
                        recyclerView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error loading related products", e));
    }

    private void checkCanReview(String productDocId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            if (btnAddReview != null) btnAddReview.setVisibility(View.GONE);
            return;
        }

        db.collection("orders")
            .whereEqualTo("userId", currentUser.getUid())
            .whereEqualTo("status", "Đã nhận hàng")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                boolean hasBought = false;
                if (!queryDocumentSnapshots.isEmpty()) {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Order order = doc.toObject(Order.class);
                        if (order.getItems() != null) {
                            for (Order.OrderItem item : order.getItems()) {
                                if (currentProduct != null && item.getProductId() != null && 
                                    (item.getProductId().equals(String.valueOf(currentProduct.getId())) || 
                                     item.getProductId().equals(currentProduct.getDocumentId()))) {
                                    hasBought = true;
                                    break;
                                }
                            }
                        }
                        if (hasBought) break;
                    }
                }
                
                if (btnAddReview != null) {
                    if (hasBought) {
                        btnAddReview.setVisibility(View.VISIBLE);
                    } else {
                        btnAddReview.setVisibility(View.GONE);
                    }
                }
            });
    }
    
    // Kiểm tra sản phẩm có trong danh sách yêu thích không
    private void checkIfFavorite(String productId, ImageButton btnWishlist) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || btnWishlist == null) return;

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
        if (productId == null && currentProduct.getId() != null) {
            // If we don't have document ID but have numeric ID, maybe query for it first or just warn
             Toast.makeText(this, "Đang tải thông tin sản phẩm...", Toast.LENGTH_SHORT).show();
             return;
        }
        
        if (productId == null) return;

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
        AlertDialog dialog = builder.create();

        RatingBar ratingBar = view.findViewById(R.id.dialog_rating_bar);
        TextInputEditText commentInput = view.findViewById(R.id.dialog_review_comment);
        Button btnCancel = view.findViewById(R.id.btn_cancel_review);
        Button btnSubmit = view.findViewById(R.id.btn_submit_review);

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = "";
            if (commentInput.getText() != null) {
                comment = commentInput.getText().toString();
            }
            submitReview(rating, comment);
            dialog.dismiss();
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void submitReview(float rating, String comment) {
        if (currentProduct == null || currentProduct.getDocumentId() == null) {
             Toast.makeText(this, "Lỗi: Không thể xác định sản phẩm để đánh giá", Toast.LENGTH_SHORT).show();
             return;
        }

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
        if (product == null) return;
        
        ImageView imageView = findViewById(R.id.productImage);
        TextView textName = findViewById(R.id.productName);
        TextView textPrice = findViewById(R.id.productPrice);
        TextView textOriginalPrice = findViewById(R.id.productOriginalPrice); // New view for original price
        TextView textDiscountBadge = findViewById(R.id.productDiscountBadge); // New view for discount badge
        
        TextView textDescription = findViewById(R.id.productDescription);
        TextView textBrand = findViewById(R.id.productBrand);
        TextView textCategory = findViewById(R.id.productCategory);
        TextView textStock = findViewById(R.id.productStock);
        
        RatingBar ratingBarIndicator = findViewById(R.id.ratingBarIndicator);
        TextView ratingTextIndicator = findViewById(R.id.ratingTextIndicator);
        
        TextView textSoldQuantity = findViewById(R.id.productSoldQuantity);
        if (textSoldQuantity != null) {
            textSoldQuantity.setText("Đã bán: " + product.getMinimumOrderQuantity());
        }
        
        TextView textDimensions = findViewById(R.id.productDimensions);
        TextView textWarranty = findViewById(R.id.productWarranty);
        TextView textShipping = findViewById(R.id.productShipping);
        TextView textReturn = findViewById(R.id.productReturn);

        if (getSupportActionBar() != null && product.getTitle() != null) {
            getSupportActionBar().setTitle(product.getTitle());
        }
        if (textName != null) textName.setText(product.getTitle());
        
        if (ratingBarIndicator != null) ratingBarIndicator.setRating((float) product.getRating());
        if (ratingTextIndicator != null) ratingTextIndicator.setText(String.format(java.util.Locale.US, "(%.1f)", product.getRating()));
        
        // Handle Price and Discount
        if (textPrice != null && product.getPrice() != null) {
            double priceValue = 0;
            try {
                String cleanPrice = product.getPrice().replaceAll("[^\\d]", "");
                if (!cleanPrice.isEmpty()) {
                     priceValue = Double.parseDouble(cleanPrice);
                }
            } catch (NumberFormatException e) {
                priceValue = 0;
            }
            
            NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
            
            if (product.getDiscountPercentage() > 0) {
                double discountedPrice = priceValue * (1 - product.getDiscountPercentage() / 100);
                long roundedDiscountedPrice = Math.round(discountedPrice / 1000) * 1000;
                
                textPrice.setText(formatter.format(roundedDiscountedPrice) + "đ");
                textPrice.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                
                if (textOriginalPrice != null) {
                    textOriginalPrice.setText(formatter.format(priceValue) + "đ");
                    textOriginalPrice.setPaintFlags(textOriginalPrice.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                    textOriginalPrice.setVisibility(View.VISIBLE);
                }
                
                if (textDiscountBadge != null) {
                    textDiscountBadge.setText("-" + (int)product.getDiscountPercentage() + "%");
                    textDiscountBadge.setVisibility(View.VISIBLE);
                }
            } else {
                textPrice.setText(formatter.format(priceValue) + "đ");
                textPrice.setTextColor(getResources().getColor(R.color.accent)); // Or default color
                
                if (textOriginalPrice != null) textOriginalPrice.setVisibility(View.GONE);
                if (textDiscountBadge != null) textDiscountBadge.setVisibility(View.GONE);
            }
        }
        
        if (textDescription != null) textDescription.setText(product.getDescription());

        if (imageView != null) {
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                Glide.with(this).load(product.getImages().get(0)).into(imageView);
            } else if (product.getThumbnail() != null) {
                Glide.with(this).load(product.getThumbnail()).into(imageView);
            }
        }

        if (textBrand != null) textBrand.setText("Thương hiệu: " + product.getBrand());
        if (textCategory != null) textCategory.setText("Danh mục: " + product.getCategory());
        if (textStock != null) textStock.setText("Trong kho: " + product.getStock());
        
        if (textWarranty != null) textWarranty.setText("Bảo hành: " + product.getWarrantyInformation());
        if (textShipping != null) textShipping.setText("Vận chuyển: " + product.getShippingInformation());
        if (textReturn != null) textReturn.setText("Chính sách đổi trả: " + product.getReturnPolicy());

        if (textDimensions != null && product.getDimensions() != null) {
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