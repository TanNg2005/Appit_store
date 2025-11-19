package com.example.appit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

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
        
        // SỬA LỖI: Thay đổi sự kiện click cho nút sửa địa chỉ
        editAddressButton.setOnClickListener(v -> showEditAddressDialog());
        
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
                            currentAddress = new User.ShippingAddress(); // Tạo mới nếu chưa có
                            currentAddress.setDefault(true);
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
            addressEditText.setText(String.format("%s, %s, %s", 
                currentAddress.getStreet(), 
                currentAddress.getDistrict(), 
                currentAddress.getCity()));
        } else {
            addressEditText.setText("");
        }
    }

    private void showEditAddressDialog() {
        if (currentAddress == null) currentAddress = new User.ShippingAddress();

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_edit_address, null);
        builder.setView(dialogView);

        TextInputEditText editStreet = dialogView.findViewById(R.id.edit_street);
        TextInputEditText editDistrict = dialogView.findViewById(R.id.edit_district);
        TextInputEditText editCity = dialogView.findViewById(R.id.edit_city);

        // Điền dữ liệu cũ
        editStreet.setText(currentAddress.getStreet());
        editDistrict.setText(currentAddress.getDistrict());
        editCity.setText(currentAddress.getCity());

        builder.setPositiveButton("Lưu tạm thời", (dialog, which) -> {
            currentAddress.setStreet(editStreet.getText().toString().trim());
            currentAddress.setDistrict(editDistrict.getText().toString().trim());
            currentAddress.setCity(editCity.getText().toString().trim());
            updateAddressDisplay();
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }

    private void saveUserProfile() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        
        userRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    user.setDisplayName(name);
                    user.setPhone(phone);

                    // Cập nhật địa chỉ vào danh sách
                    List<User.ShippingAddress> addresses = user.getShippingAddresses();
                    if (addresses == null) addresses = new ArrayList<>();
                    
                    if (currentAddress != null) {
                        // Nếu danh sách trống, thêm mới. Nếu có rồi, thay thế cái mặc định
                        if (addresses.isEmpty()) {
                            currentAddress.setRecipientName(name); // Cập nhật tên người nhận
                            currentAddress.setPhone(phone); // Cập nhật sđt người nhận
                            addresses.add(currentAddress);
                        } else {
                            // Tìm và cập nhật địa chỉ mặc định
                            for (int i = 0; i < addresses.size(); i++) {
                                if (addresses.get(i).isDefault()) {
                                    currentAddress.setRecipientName(name);
                                    currentAddress.setPhone(phone);
                                    addresses.set(i, currentAddress);
                                    break;
                                }
                            }
                        }
                    }
                    user.setShippingAddresses(addresses);

                    userRef.set(user)
                        .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Hồ sơ đã được cập nhật", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }
        });
    }

    private void uploadProfileImage(Uri imageUri) {
        if (imageUri != null) {
            storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String imageUrl = uri.toString();
                    userRef.update("profileImageUrl", imageUrl);
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
