package com.example.appit;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VoucherListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private VoucherAdapter adapter;
    private List<Voucher> voucherList;
    private FirebaseFirestore db;
    private boolean showSavedOnly = false;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voucher_list);

        showSavedOnly = getIntent().getBooleanExtra("SHOW_SAVED_ONLY", false);
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
        }

        setupToolbar();

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recycler_view_vouchers);
        emptyView = findViewById(R.id.empty_view);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        voucherList = new ArrayList<>();
        adapter = new VoucherAdapter(voucherList);
        recyclerView.setAdapter(adapter);

        loadVouchers();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            String title = showSavedOnly ? "Voucher đã lưu" : "Kho Voucher";
            getSupportActionBar().setTitle(title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadVouchers() {
        db.collection("vouchers")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                voucherList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Voucher voucher = document.toObject(Voucher.class);
                    // Gán ID nếu chưa có
                    if (voucher.getCode() == null || voucher.getCode().isEmpty()) {
                        voucher.setCode(document.getId());
                    }
                    
                    boolean isUsed = currentUserId != null && voucher.getUsedBy().contains(currentUserId);
                    boolean isSaved = currentUserId != null && voucher.getSavedBy().contains(currentUserId);

                    // Logic lọc hiển thị
                    if (showSavedOnly) {
                        // Nếu xem "Đã lưu": Chỉ hiện voucher đã lưu VÀ chưa dùng
                        if (isSaved && !isUsed) {
                            voucherList.add(voucher);
                        }
                    } else {
                        // Nếu xem "Kho Voucher": Hiện tất cả voucher active mà chưa dùng
                        // (Có thể hiện cả voucher đã lưu để user biết)
                        if (!isUsed) {
                            voucherList.add(voucher);
                        }
                    }
                }
                adapter.notifyDataSetChanged();
                
                if (voucherList.isEmpty()) {
                    if (emptyView != null) {
                        emptyView.setText(showSavedOnly ? "Bạn chưa lưu voucher nào" : "Hiện chưa có voucher nào");
                        emptyView.setVisibility(View.VISIBLE);
                    }
                    recyclerView.setVisibility(View.GONE);
                } else {
                    if (emptyView != null) emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi tải voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private class VoucherAdapter extends RecyclerView.Adapter<VoucherAdapter.VoucherViewHolder> {

        private List<Voucher> vouchers;

        public VoucherAdapter(List<Voucher> vouchers) {
            this.vouchers = vouchers;
        }

        @NonNull
        @Override
        public VoucherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_voucher, parent, false);
            return new VoucherViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull VoucherViewHolder holder, int position) {
            Voucher voucher = vouchers.get(position);
            holder.bind(voucher);
        }

        @Override
        public int getItemCount() {
            return vouchers.size();
        }

        class VoucherViewHolder extends RecyclerView.ViewHolder {
            TextView code, description, expiry, condition;
            MaterialButton btnAction;

            public VoucherViewHolder(@NonNull View itemView) {
                super(itemView);
                code = itemView.findViewById(R.id.tv_voucher_code);
                description = itemView.findViewById(R.id.tv_voucher_desc);
                expiry = itemView.findViewById(R.id.tv_voucher_expiry);
                condition = itemView.findViewById(R.id.tv_voucher_condition);
                btnAction = itemView.findViewById(R.id.btn_copy_code); // Tái sử dụng button ID cũ
            }

            public void bind(Voucher voucher) {
                code.setText(voucher.getCode());
                description.setText(voucher.getDescription());
                
                DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                String expiryText = "Hạn dùng: ";
                if (voucher.getEndDate() != null) {
                    expiryText += dateFormat.format(voucher.getEndDate());
                } else {
                    expiryText += "Vô thời hạn";
                }
                expiry.setText(expiryText);

                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                String conditionText = "Đơn tối thiểu: " + currencyFormat.format(voucher.getMinPurchase());
                condition.setText(conditionText);

                // Kiểm tra trạng thái đã lưu
                boolean isSaved = currentUserId != null && voucher.getSavedBy().contains(currentUserId);

                if (showSavedOnly) {
                    // Ở màn hình "Đã lưu", nút hành động có thể là "Dùng ngay" (chuyển sang Cart) hoặc "Copy"
                    // Ở đây ta để "Copy" để user dùng sau
                    btnAction.setText("Dùng ngay");
                    btnAction.setEnabled(true);
                    btnAction.setOnClickListener(v -> {
                        // Chuyển sang CartActivity để dùng
                        // (Hoặc logic khác tùy yêu cầu)
                        Toast.makeText(VoucherListActivity.this, "Đã sao chép mã: " + voucher.getCode(), Toast.LENGTH_SHORT).show();
                         android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("Voucher Code", voucher.getCode());
                        clipboard.setPrimaryClip(clip);
                    });
                } else {
                    // Ở màn hình "Kho Voucher"
                    if (isSaved) {
                        btnAction.setText("Đã lưu");
                        btnAction.setEnabled(false);
                        // Màu xám hoặc style khác để thể hiện đã lưu
                        btnAction.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                    } else {
                        btnAction.setText("Lưu");
                        btnAction.setEnabled(true);
                        // Màu accent
                        btnAction.setBackgroundColor(getResources().getColor(R.color.purple_500)); // Sử dụng màu chủ đạo
                        
                        btnAction.setOnClickListener(v -> {
                            if (currentUserId == null) {
                                Toast.makeText(VoucherListActivity.this, "Vui lòng đăng nhập để lưu voucher", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            saveVoucher(voucher);
                        });
                    }
                }
            }
            
            private void saveVoucher(Voucher voucher) {
                // Tìm document ID của voucher (vì code có thể không phải là document ID)
                // Tuy nhiên trong loadVouchers ta đã setCode bằng docId nếu thiếu, 
                // nhưng để chính xác khi update ta nên query lại hoặc lưu docId vào object Voucher
                
                db.collection("vouchers")
                    .whereEqualTo("code", voucher.getCode())
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for(QueryDocumentSnapshot doc : snapshots) {
                            db.collection("vouchers").document(doc.getId())
                                .update("savedBy", FieldValue.arrayUnion(currentUserId))
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(VoucherListActivity.this, "Đã lưu voucher vào kho", Toast.LENGTH_SHORT).show();
                                    // Reload lại list để cập nhật trạng thái nút bấm
                                    loadVouchers();
                                })
                                .addOnFailureListener(e -> Toast.makeText(VoucherListActivity.this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    });
            }
        }
    }
}