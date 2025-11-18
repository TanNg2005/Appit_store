package com.example.appit;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";

    private EditText nameEditText, emailEditText, phoneEditText, addressEditText;
    private CircleImageView profileImageView;
    private DocumentReference userRef;
    private StorageReference storageRef;

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
        getSupportActionBar().setTitle(R.string.nav_account);
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
                    nameEditText.setText(snapshot.getString("displayName"));
                    emailEditText.setText(snapshot.getString("email"));
                    phoneEditText.setText(snapshot.getString("phone"));

                    if (snapshot.contains("shippingAddresses")) {
                        // Logic đọc địa chỉ an toàn hơn
                    }

                    String imageUrl = snapshot.getString("profileImageUrl");
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_profile).into(profileImageView);
                    } else {
                        profileImageView.setImageResource(R.drawable.ic_profile);
                    }
                } catch (Exception ex) {
                    Log.e(TAG, "Lỗi khi đọc dữ liệu hồ sơ", ex);
                    Toast.makeText(ProfileActivity.this, "Dữ liệu hồ sơ không hợp lệ, đang cố gắng sửa...", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void saveUserProfile() {
        String name = nameEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        
        userRef.update("displayName", name, "phone", phone)
            .addOnSuccessListener(aVoid -> Toast.makeText(ProfileActivity.this, "Hồ sơ đã được cập nhật", Toast.LENGTH_SHORT).show())
            .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
