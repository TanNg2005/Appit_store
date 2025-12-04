package com.example.appit;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends BaseActivity {

    private static final String TAG = "ProfileActivity";

    private CircleImageView profileImageView;
    private TextView userNameTextView, userEmailTextView;
    private DocumentReference userRef;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());
        
        // Setup Views
        profileImageView = findViewById(R.id.profile_image);
        userNameTextView = findViewById(R.id.text_user_name);
        userEmailTextView = findViewById(R.id.text_user_email);
        MaterialButton editProfileButton = findViewById(R.id.btn_edit_profile);
        MaterialButton logoutButton = findViewById(R.id.btn_logout);
        
        // Menu items
        findViewById(R.id.menu_my_orders).setOnClickListener(v -> startActivity(new Intent(this, OrderActivity.class)));
        findViewById(R.id.menu_favorites).setOnClickListener(v -> startActivity(new Intent(this, WishlistActivity.class)));
        findViewById(R.id.menu_address).setOnClickListener(v -> startActivity(new Intent(this, AddressBookActivity.class)));
        
        // Cài đặt: Chế độ tối & Ngôn ngữ đã bị loại bỏ theo yêu cầu
        // Nếu muốn hiển thị lại, hãy khôi phục code trong layout và ở đây.

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            userRef = FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid());
            loadUserProfile();
        } else {
            Toast.makeText(this, "Lỗi: Người dùng chưa đăng nhập!", Toast.LENGTH_LONG).show();
            finish();
        }

        editProfileButton.setOnClickListener(v -> startActivity(new Intent(this, EditProfileActivity.class)));
        logoutButton.setOnClickListener(v -> logoutUser());
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
                        userNameTextView.setText(user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : "Người dùng Appit");
                        userEmailTextView.setText(user.getEmail());

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
    
    // Language dialog code removed since feature is disabled in UI
    /*
    private void showLanguageDialog() {
        new AlertDialog.Builder(this)
            .setTitle(R.string.language_change_title)
            .setItems(R.array.languages_array, (dialog, which) -> {
                String selectedLanguage = which == 1 ? "en" : "vi";
                if (!selectedLanguage.equals(LocaleHelper.getLanguage(this))) {
                     LocaleHelper.setLocale(this, selectedLanguage);
                     recreate();
                }
            })
            .show();
    }
    */

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