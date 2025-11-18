package com.example.appit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private CircleImageView profileImageView;
    private EditText etFirstName, etLastName, etEmail;
    private Button btnSaveChanges, btnLogout;
    private TextView tvChangeAvatar;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private StorageReference storageRef;

    private Uri selectedImageUri; // To hold the new avatar URI

    // Activity Result Launcher for picking an image
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null && result.getData().getData() != null) {
                    selectedImageUri = result.getData().getData();
                    // Show the new image immediately
                    profileImageView.setImageURI(selectedImageUri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storageRef = FirebaseStorage.getInstance().getReference();

        setupToolbar();
        bindViews();
        loadUserData();
        setupClickListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Hồ sơ");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void bindViews() {
        profileImageView = findViewById(R.id.profile_image);
        tvChangeAvatar = findViewById(R.id.tv_change_avatar);
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        etEmail = findViewById(R.id.et_email);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        btnLogout = findViewById(R.id.btn_logout);
    }

    private void setupClickListeners() {
        View.OnClickListener changeAvatarListener = v -> openGallery();
        profileImageView.setOnClickListener(changeAvatarListener);
        tvChangeAvatar.setOnClickListener(changeAvatarListener);

        btnSaveChanges.setOnClickListener(v -> saveChanges());
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void loadUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        etEmail.setText(currentUser.getEmail());

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        etFirstName.setText(doc.getString("firstName"));
                        etLastName.setText(doc.getString("lastName"));
                        String avatarUrl = doc.getString("avatarUrl");
                        if (avatarUrl != null && !avatarUrl.isEmpty()) {
                            Glide.with(this).load(avatarUrl).into(profileImageView);
                        } else {
                            profileImageView.setImageResource(android.R.drawable.sym_def_app_icon);
                        }
                    } else {
                         profileImageView.setImageResource(android.R.drawable.sym_def_app_icon);
                    }
                });
    }

    private void saveChanges() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (firstName.isEmpty() || lastName.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ họ và tên", Toast.LENGTH_SHORT).show();
            return;
        }

        // If a new image was selected, upload it first
        if (selectedImageUri != null) {
            uploadImageAndSaveChanges(firstName, lastName);
        } else {
            // Otherwise, just save the names
            updateFirestore(firstName, lastName, null);
        }
    }

    private void uploadImageAndSaveChanges(String firstName, String lastName) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        final StorageReference fileRef = storageRef.child("avatars/" + user.getUid() + ".jpg");

        fileRef.putFile(selectedImageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    String avatarUrl = uri.toString();
                    updateFirestore(firstName, lastName, avatarUrl);
                }))
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Lỗi tải ảnh lên", Toast.LENGTH_SHORT).show());
    }

    private void updateFirestore(String firstName, String lastName, String avatarUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("firstName", firstName);
        userData.put("lastName", lastName);
        if (avatarUrl != null) {
            userData.put("avatarUrl", avatarUrl);
        }

        db.collection("users").document(user.getUid()).set(userData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(ProfileActivity.this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    finish(); // Go back to the previous screen
                })
                .addOnFailureListener(e -> Toast.makeText(ProfileActivity.this, "Lỗi cập nhật hồ sơ", Toast.LENGTH_SHORT).show());
    }

    private void logoutUser() {
        mAuth.signOut();
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
