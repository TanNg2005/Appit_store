package com.example.appit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private static final String TAG = "ProfileActivity";

    private EditText nameEditText, emailEditText, phoneEditText, addressEditText;
    private CircleImageView profileImageView;
    private DocumentReference userRef;
    private StorageReference storageRef;
    private User.ShippingAddress currentAddress; // Lưu địa chỉ hiện tại đang sửa

    private final ActivityResultLauncher<String> mGetContent = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::uploadProfileImage
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        nameEditText = findViewById(R.id.edit_text_name);
        emailEditText = findViewById(R.id.edit_text_email);
        phoneEditText = findViewById(R.id.edit_text_phone);
        addressEditText = findViewById(R.id.edit_text_address);
        profileImageView = findViewById(R.id.profile_image);

        Button saveButton = findViewById(R.id.btn_save_profile);
        Button logoutButton = findViewById(R.id.btn_logout);
        ImageButton editNameButton = findViewById(R.id.btn_edit_name);
        ImageButton editPhoneButton = findViewById(R.id.btn_edit_phone);
        ImageButton editAddressButton = findViewById(R.id.btn_edit_address);
        ImageButton editImageButton = findViewById(R.id.btn_edit_profile_image);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid());
            storageRef = FirebaseStorage.getInstance().getReference().child("profile_images").child(currentUser.getUid());
            loadUserProfile();
        } else {
            Toast.makeText(this, "Lỗi: Người dùng chưa đăng nhập!", Toast.LENGTH_LONG).show();
            finish();
        }

        saveButton.setOnClickListener(v -> saveUserProfile());
        logoutButton.setOnClickListener(v -> logoutUser());

        editNameButton.setOnClickListener(v -> toggleEdit(nameEditText));
        editPhoneButton.setOnClickListener(v -> toggleEdit(phoneEditText));
        
        // Cho phép sửa địa chỉ trực tiếp tại đây luôn để thuận tiện
        editAddressButton.setOnClickListener(v -> toggleEdit(addressEditText));
        
        editImageButton.setOnClickListener(v -> mGetContent.launch("image/*"));
    }

    private void loadUserProfile() {
        userRef.addSnapshotListener((snapshot, e) -> {
            if (e != null) {
                Log.e(TAG, "Lỗi khi lắng nghe dữ liệu hồ sơ.", e);
                return;
            }
            if (snapshot != null && snapshot.exists()) {
                try {
                    User user = snapshot.toObject(User.class);
                    if (user != null) {
                        nameEditText.setText(user.getDisplayName());
                        emailEditText.setText(user.getEmail());
                        phoneEditText.setText(user.getPhone());

                        // Xử lý địa chỉ
                        if (user.getShippingAddresses() != null && !user.getShippingAddresses().isEmpty()) {
                            // Tìm địa chỉ mặc định, nếu không có thì lấy cái đầu tiên
                            currentAddress = user.getShippingAddresses().stream()
                                    .filter(User.ShippingAddress::isDefault).findFirst()
                                    .orElse(user.getShippingAddresses().get(0));
                        } else {
                            currentAddress = null;
                        }
                        updateAddressDisplay();

                        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                            Glide.with(this).load(user.getProfileImageUrl()).placeholder(R.drawable.ic_profile).into(profileImageView);
                        } else {
                            profileImageView.setImageResource(R.drawable.ic_profile);
                        }
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Lỗi khi đọc dữ liệu hồ sơ", ex);
                }
            }
        });
    }

    private void updateAddressDisplay() {
        if (currentAddress != null && currentAddress.getStreet() != null) {
            // Nếu có đầy đủ thông tin thì format đẹp
            String addressStr = currentAddress.getStreet();
            if (currentAddress.getDistrict() != null && !currentAddress.getDistrict().isEmpty()) {
                addressStr += ", " + currentAddress.getDistrict();
            }
            if (currentAddress.getCity() != null && !currentAddress.getCity().isEmpty()) {
                addressStr += ", " + currentAddress.getCity();
            }
            addressEditText.setText(addressStr);
        } else if (currentAddress != null) {
            addressEditText.setText("Địa chỉ chưa đầy đủ");
        } else {
            // Nếu chưa có địa chỉ thì để trống để người dùng nhập
             if (addressEditText.getText().toString().isEmpty()) {
                 addressEditText.setText("");
                 addressEditText.setHint("Nhập địa chỉ nhận hàng tại đây");
             }
        }
    }

    private void saveUserProfile() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String addressInput = addressEditText.getText().toString().trim();

        if (phone.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (addressInput.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập địa chỉ nhận hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("displayName", name);
        updates.put("phone", phone);
        
        // Lưu địa chỉ
        // Tạo một danh sách địa chỉ mới chứa địa chỉ vừa nhập
        User.ShippingAddress newAddress = new User.ShippingAddress();
        newAddress.setStreet(addressInput);
        newAddress.setDistrict(""); // Tạm thời để trống
        newAddress.setCity("");     // Tạm thời để trống
        newAddress.setDefault(true);
        
        updates.put("shippingAddresses", Arrays.asList(newAddress));
        
        // Sử dụng set với merge để tạo document nếu chưa có (fix lỗi user cũ)
        userRef.set(updates, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Toast.makeText(ProfileActivity.this, "Hồ sơ đã được cập nhật thành công!", Toast.LENGTH_SHORT).show();
                // Ẩn bàn phím
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (getCurrentFocus() != null) {
                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                }
                // Quay lại màn hình trước (CartActivity) nếu cần
                // finish(); 
            })
            .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void uploadProfileImage(Uri imageUri) {
        if (imageUri != null) {
            storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("profileImageUrl", imageUrl);
                    userRef.set(updates, SetOptions.merge());
                }));
        }
    }

    private void toggleEdit(EditText editText) {
        editText.setEnabled(!editText.isEnabled());
        if (editText.isEnabled()) {
            editText.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
            editText.setSelection(editText.getText().length());
        }
    }

    private void logoutUser() {
        CartManager.destroyInstance();
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
