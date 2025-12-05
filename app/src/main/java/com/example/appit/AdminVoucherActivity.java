package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class AdminVoucherActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private AdminVoucherAdapter adapter;
    private List<Voucher> voucherList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_voucher);

        setupToolbar();
        
        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recycler_view_vouchers);
        emptyView = findViewById(R.id.empty_view);
        FloatingActionButton fabAdd = findViewById(R.id.fab_add_voucher);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        voucherList = new ArrayList<>();
        adapter = new AdminVoucherAdapter(voucherList);
        recyclerView.setAdapter(adapter);

        fabAdd.setOnClickListener(v -> {
            startActivity(new Intent(AdminVoucherActivity.this, AdminEditVoucherActivity.class));
        });

        loadVouchers();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý Voucher");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void loadVouchers() {
        db.collection("vouchers")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                voucherList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    Voucher voucher = document.toObject(Voucher.class);
                    // Use document ID as code if not set, or ensure we have the doc ID for updates
                    // Here we assume code is unique and used as ID or we store docId separately if needed
                    // For simplicity in this adapter, let's assume we might need document ID for deletion
                    // But since we don't have a docId field in Voucher class yet (except implicitly), 
                    // we'll rely on 'code' query if needed or best practice: add docId to model.
                    // However, let's just pass the object. Ideally Voucher model should have setDocumentId.
                    
                    // Let's set the code from ID if null, similar to VoucherListActivity
                    if (voucher.getCode() == null || voucher.getCode().isEmpty()) {
                        voucher.setCode(document.getId());
                    }
                    
                    voucherList.add(voucher);
                }
                adapter.notifyDataSetChanged();
                
                if (voucherList.isEmpty()) {
                    emptyView.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    emptyView.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi tải voucher: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadVouchers(); // Reload list when returning from Edit screen
    }

    private class AdminVoucherAdapter extends RecyclerView.Adapter<AdminVoucherAdapter.ViewHolder> {

        private List<Voucher> vouchers;

        public AdminVoucherAdapter(List<Voucher> vouchers) {
            this.vouchers = vouchers;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_voucher, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Voucher voucher = vouchers.get(position);
            holder.bind(voucher);
        }

        @Override
        public int getItemCount() {
            return vouchers.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView code, desc, status;
            ImageButton btnEdit, btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                code = itemView.findViewById(R.id.tv_voucher_code);
                desc = itemView.findViewById(R.id.tv_voucher_desc);
                status = itemView.findViewById(R.id.tv_voucher_status);
                btnEdit = itemView.findViewById(R.id.btn_edit);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }

            public void bind(Voucher voucher) {
                code.setText(voucher.getCode());
                desc.setText(voucher.getDescription());
                
                if (voucher.isActive()) {
                    status.setText("Active");
                    status.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    status.setText("Inactive");
                    status.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                }

                btnEdit.setOnClickListener(v -> {
                    Intent intent = new Intent(AdminVoucherActivity.this, AdminEditVoucherActivity.class);
                    // Pass the voucher code or ID to edit
                    // Since we don't have a dedicated ID field and used 'code' as ID in some places,
                    // passing 'code' is safer if it's the document ID. 
                    // Let's assume document ID = code for simplicity or we query by code.
                    // Ideally we should have passed document ID.
                    // Let's use the code and query it in EditActivity.
                    intent.putExtra("VOUCHER_CODE", voucher.getCode());
                    startActivity(intent);
                });

                btnDelete.setOnClickListener(v -> {
                    new AlertDialog.Builder(AdminVoucherActivity.this)
                        .setTitle("Xóa Voucher")
                        .setMessage("Bạn có chắc muốn xóa voucher " + voucher.getCode() + " không?")
                        .setPositiveButton("Xóa", (dialog, which) -> deleteVoucher(voucher))
                        .setNegativeButton("Hủy", null)
                        .show();
                });
            }

            private void deleteVoucher(Voucher voucher) {
                // We need to find the document to delete. 
                // Best effort: query by code.
                db.collection("vouchers")
                    .whereEqualTo("code", voucher.getCode())
                    .get()
                    .addOnSuccessListener(snapshots -> {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            db.collection("vouchers").document(doc.getId()).delete()
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(AdminVoucherActivity.this, "Đã xóa voucher", Toast.LENGTH_SHORT).show();
                                    loadVouchers();
                                });
                        }
                    })
                    .addOnFailureListener(e -> Toast.makeText(AdminVoucherActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }
}