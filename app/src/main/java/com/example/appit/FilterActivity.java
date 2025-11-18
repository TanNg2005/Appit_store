package com.example.appit;

import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private List<Product> allProducts = new ArrayList<>();
    private GridAdapter adapter;
    private RecyclerView recyclerView;

    private ChipGroup brandChipGroup, categoryChipGroup;
    private RatingBar ratingBar;
    private TextView resultsCountTextView;

    private Set<String> selectedBrands = new HashSet<>();
    private Set<String> selectedCategories = new HashSet<>();
    private float selectedRating = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filter);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.filter_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Lọc sản phẩm");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        brandChipGroup = findViewById(R.id.chipgroup_brand);
        categoryChipGroup = findViewById(R.id.chipgroup_category);
        ratingBar = findViewById(R.id.rating_bar_filter);
        recyclerView = findViewById(R.id.recycler_view_filtered_products);
        resultsCountTextView = findViewById(R.id.text_filter_results_count);
        Button applyFilterButton = findViewById(R.id.btn_apply_filter);
        Button clearFilterButton = findViewById(R.id.btn_clear_all_filters);

        adapter = new GridAdapter(this, new ArrayList<>());
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);

        applyFilterButton.setOnClickListener(v -> applyFilters());
        clearFilterButton.setOnClickListener(v -> clearFilters());

        loadAllProductsAndSetupFilters();
    }

    private void loadAllProductsAndSetupFilters() {
        db.collection("products").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                allProducts.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Product product = document.toObject(Product.class);
                    product.setDocumentId(document.getId());
                    allProducts.add(product);
                }
                setupFilterChips();
                applyFilters();
            }
        });
    }

    private void setupFilterChips() {
        List<String> brands = allProducts.stream().map(Product::getBrand).distinct().collect(Collectors.toList());
        setupChipGroup(brandChipGroup, brands, selectedBrands);

        List<String> categories = allProducts.stream().map(Product::getCategory).distinct().collect(Collectors.toList());
        setupChipGroup(categoryChipGroup, categories, selectedCategories);
    }

    // Sửa lại hoàn toàn logic để thêm nút "Tất cả"
    private void setupChipGroup(ChipGroup chipGroup, List<String> items, Set<String> selectedItems) {
        chipGroup.removeAllViews();

        // 1. Tạo và thêm nút "Tất cả"
        Chip allChip = createChoiceChip("Tất cả", chipGroup);
        allChip.setId(View.generateViewId());
        chipGroup.addView(allChip);

        // 2. Tạo và thêm các nút còn lại
        List<Chip> itemChips = new ArrayList<>();
        for (String item : items) {
            Chip chip = createChoiceChip(item, chipGroup);
            chip.setId(View.generateViewId());
            chipGroup.addView(chip);
            itemChips.add(chip);
        }

        // 3. Cài đặt listener cho nút "Tất cả"
        allChip.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedItems.clear();
                for (Chip otherChip : itemChips) {
                    otherChip.setChecked(false);
                }
                buttonView.setTypeface(null, Typeface.BOLD);
            }
        });

        // 4. Cài đặt listener cho các nút còn lại
        for (Chip chip : itemChips) {
            chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                String itemText = buttonView.getText().toString();
                if (isChecked) {
                    selectedItems.add(itemText);
                    buttonView.setTypeface(null, Typeface.BOLD);
                    allChip.setChecked(false);
                    allChip.setTypeface(null, Typeface.NORMAL);
                } else {
                    selectedItems.remove(itemText);
                    buttonView.setTypeface(null, Typeface.NORMAL);
                    if (selectedItems.isEmpty()) {
                        allChip.setChecked(true);
                    }
                }
            });
        }
        
        // 5. Đặt trạng thái ban đầu
        allChip.setChecked(true);
    }

    private Chip createChoiceChip(String text, ViewGroup parent) {
        Chip chip = (Chip) LayoutInflater.from(this).inflate(R.layout.chip_choice, parent, false);
        chip.setText(text);
        return chip;
    }

    private void applyFilters() {
        selectedRating = ratingBar.getRating();

        List<Product> filteredList = allProducts.stream()
                .filter(p -> selectedBrands.isEmpty() || selectedBrands.contains(p.getBrand()))
                .filter(p -> selectedCategories.isEmpty() || selectedCategories.contains(p.getCategory()))
                .filter(p -> p.getRating() >= selectedRating)
                .collect(Collectors.toList());

        adapter = new GridAdapter(this, filteredList);
        recyclerView.setAdapter(adapter);
        resultsCountTextView.setText(String.format("Tìm thấy %d kết quả", filteredList.size()));
    }

    private void clearFilters() {
        selectedBrands.clear();
        selectedCategories.clear();
        setupFilterChips(); // Re-setup to select "All"
        ratingBar.setRating(0);
        applyFilters();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
