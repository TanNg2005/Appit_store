package com.example.appit;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AdminEditVoucherActivity extends AppCompatActivity {

    private TextInputEditText etCode, etTitle, etDesc, etValue, etMaxDiscount, etMinPurchase, etUsageLimit;
    private RadioGroup rgDiscountType;
    private RadioButton rbPercent, rbAmount;
    private Button btnStartDate, btnEndDate, btnSave, btnCancel;
    private TextView tvDateRange;
    private SwitchMaterial switchActive;

    private Date startDate, endDate;
    private FirebaseFirestore db;
    private String voucherCodeToEdit;
    private String documentIdToEdit; // Real document ID if editing

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_voucher);

        db = FirebaseFirestore.getInstance();
        
        initViews();
        
        voucherCodeToEdit = getIntent().getStringExtra("VOUCHER_CODE");
        if (voucherCodeToEdit != null) {
            loadVoucherData(voucherCodeToEdit);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Chỉnh sửa Voucher");
            }
            etCode.setEnabled(false); // Cannot change code when editing
        } else {
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("Thêm Voucher mới");
            }
        }
    }

    private void initViews() {
        etCode = findViewById(R.id.et_voucher_code);
        etTitle = findViewById(R.id.et_voucher_title);
        etDesc = findViewById(R.id.et_voucher_desc);
        etValue = findViewById(R.id.et_discount_value);
        etMaxDiscount = findViewById(R.id.et_max_discount);
        etMinPurchase = findViewById(R.id.et_min_purchase);
        etUsageLimit = findViewById(R.id.et_usage_limit);
        
        rgDiscountType = findViewById(R.id.rg_discount_type);
        rbPercent = findViewById(R.id.rb_percent);
        rbAmount = findViewById(R.id.rb_amount);
        
        btnStartDate = findViewById(R.id.btn_start_date);
        btnEndDate = findViewById(R.id.btn_end_date);
        btnSave = findViewById(R.id.btn_save_voucher);
        btnCancel = findViewById(R.id.btn_cancel_voucher);
        
        tvDateRange = findViewById(R.id.tv_date_range);
        switchActive = findViewById(R.id.switch_active);

        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));
        
        btnSave.setOnClickListener(v -> saveVoucher());
        btnCancel.setOnClickListener(v -> finish()); // Go back to previous screen
    }

    private void showDatePicker(boolean isStartDate) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePickerDialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                if (isStartDate) {
                    startDate = calendar.getTime();
                    btnStartDate.setText(formatDate(startDate));
                } else {
                    endDate = calendar.getTime();
                    // Set end date to end of day
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    endDate = calendar.getTime();
                    btnEndDate.setText(formatDate(endDate));
                }
                updateDateRangeText();
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private String formatDate(Date date) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date);
    }

    private void updateDateRangeText() {
        if (startDate != null && endDate != null) {
            tvDateRange.setText(formatDate(startDate) + " - " + formatDate(endDate));
        }
    }

    private void loadVoucherData(String code) {
        db.collection("vouchers")
            .whereEqualTo("code", code)
            .get()
            .addOnSuccessListener(snapshots -> {
                if (!snapshots.isEmpty()) {
                    QueryDocumentSnapshot doc = (QueryDocumentSnapshot) snapshots.getDocuments().get(0);
                    Voucher voucher = doc.toObject(Voucher.class);
                    documentIdToEdit = doc.getId();
                    
                    etCode.setText(voucher.getCode());
                    etTitle.setText(voucher.getTitle());
                    etDesc.setText(voucher.getDescription());
                    
                    if ("AMOUNT".equals(voucher.getDiscountType())) {
                        rbAmount.setChecked(true);
                    } else {
                        rbPercent.setChecked(true);
                    }
                    
                    etValue.setText(String.valueOf(voucher.getValue()));
                    etMaxDiscount.setText(String.valueOf(voucher.getMaxDiscount()));
                    etMinPurchase.setText(String.valueOf(voucher.getMinPurchase()));
                    etUsageLimit.setText(String.valueOf(voucher.getUsageLimit()));
                    
                    startDate = voucher.getStartDate();
                    endDate = voucher.getEndDate();
                    
                    if (startDate != null) btnStartDate.setText(formatDate(startDate));
                    if (endDate != null) btnEndDate.setText(formatDate(endDate));
                    updateDateRangeText();
                    
                    switchActive.setChecked(voucher.isActive());
                }
            });
    }

    private void saveVoucher() {
        String code = etCode.getText().toString().trim().toUpperCase();
        String title = etTitle.getText().toString().trim();
        String desc = etDesc.getText().toString().trim();
        
        if (code.isEmpty()) {
            etCode.setError("Cần nhập mã code");
            return;
        }
        
        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setTitle(title);
        voucher.setDescription(desc);
        
        voucher.setDiscountType(rbPercent.isChecked() ? "PERCENT" : "AMOUNT");
        
        try {
            voucher.setValue(Double.parseDouble(etValue.getText().toString().trim()));
        } catch (Exception e) { voucher.setValue(0.0); }
        
        try {
            voucher.setMaxDiscount(Double.parseDouble(etMaxDiscount.getText().toString().trim()));
        } catch (Exception e) { voucher.setMaxDiscount(0.0); }
        
        try {
            voucher.setMinPurchase(Double.parseDouble(etMinPurchase.getText().toString().trim()));
        } catch (Exception e) { voucher.setMinPurchase(0.0); }
        
        try {
            voucher.setUsageLimit(Integer.parseInt(etUsageLimit.getText().toString().trim()));
        } catch (Exception e) { voucher.setUsageLimit(0); }
        
        voucher.setStartDate(startDate);
        voucher.setEndDate(endDate);
        voucher.setActive(switchActive.isChecked());
        
        if (documentIdToEdit != null) {
            // Update existing
            db.collection("vouchers").document(documentIdToEdit)
               .update(
                   "title", voucher.getTitle(),
                   "description", voucher.getDescription(),
                   "discountType", voucher.getDiscountType(),
                   "discountValue", voucher.getValue(),
                   "maxDiscountAmount", voucher.getMaxDiscount(),
                   "minOrderAmount", voucher.getMinPurchase(),
                   "usageLimit", voucher.getUsageLimit(),
                   "startDate", voucher.getStartDate(),
                   "endDate", voucher.getEndDate(),
                   "isActive", voucher.isActive()
               )
               .addOnSuccessListener(aVoid -> {
                   Toast.makeText(this, "Đã cập nhật voucher", Toast.LENGTH_SHORT).show();
                   finish();
               })
               .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
               
        } else {
            // Create new
            voucher.setUsedCount(0);
            db.collection("vouchers")
                .add(voucher)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Đã thêm voucher mới", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }
}