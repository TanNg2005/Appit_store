package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;
    private EditText inputEmail, inputPassword, inputFirstName, inputLastName;
    private Button btnRegister, btnGoogleRegister;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private GoogleSignInClient mGoogleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        inputFirstName = findViewById(R.id.firstName);
        inputLastName = findViewById(R.id.lastName);
        inputEmail = findViewById(R.id.email);
        inputPassword = findViewById(R.id.password);
        btnRegister = findViewById(R.id.btnRegister);
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister);
        progressBar = findViewById(R.id.progressBar);

        TextView btnGoToLogin = findViewById(R.id.btnGoToLogin);
        btnGoToLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        });

        // Configure Google Sign In
        // Using the string directly because R.string.default_web_client_id might not be generated yet or properly referenced
        String defaultWebClientId = getString(R.string.default_web_client_id);
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(defaultWebClientId)
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String firstName = inputFirstName.getText().toString().trim();
                String lastName = inputLastName.getText().toString().trim();
                String email = inputEmail.getText().toString().trim();
                String password = inputPassword.getText().toString().trim();

                if (TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName)) {
                    Toast.makeText(getApplicationContext(), "Vui lòng nhập đầy đủ họ và tên!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(email)) {
                    Toast.makeText(getApplicationContext(), "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(password)) {
                    Toast.makeText(getApplicationContext(), "Vui lòng nhập mật khẩu!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (password.length() < 6) {
                    Toast.makeText(getApplicationContext(), "Mật khẩu phải có ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
                    return;
                }

                progressBar.setVisibility(View.VISIBLE);
                
                // 1. Tạo người dùng trong Firebase Authentication
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (!task.isSuccessful()) {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(RegisterActivity.this, "Đăng ký thất bại: " + 
                                            (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"),
                                            Toast.LENGTH_LONG).show();
                                } else {
                                    // 2. Nếu thành công, lấy UID
                                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                                    if (firebaseUser != null) {
                                        String userId = firebaseUser.getUid();
                                        
                                        // Tạo display name: Họ + Tên (Ví dụ: Le Xuan Tai)
                                        String displayName = lastName + " " + firstName;

                                        // Cập nhật profile cho Auth user (để getCurrentUser().getDisplayName() hoạt động ngay)
                                        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                                .setDisplayName(displayName)
                                                .build();
                                        firebaseUser.updateProfile(profileUpdates);

                                        // Lưu vào Firestore
                                        Map<String, Object> user = new HashMap<>();
                                        user.put("firstName", firstName);
                                        user.put("lastName", lastName);
                                        user.put("displayName", displayName); // Lưu thêm trường displayName
                                        user.put("email", email);
                                        user.put("avatarUrl", "");
                                        user.put("isAdmin", false);

                                        db.collection("users").document(userId).set(user)
                                                .addOnSuccessListener(aVoid -> {
                                                    progressBar.setVisibility(View.GONE);
                                                    Toast.makeText(RegisterActivity.this, "Tạo tài khoản thành công!", Toast.LENGTH_SHORT).show();
                                                    finish(); // Quay lại màn hình Login
                                                })
                                                .addOnFailureListener(e -> {
                                                    progressBar.setVisibility(View.GONE);
                                                    Toast.makeText(RegisterActivity.this, "Lỗi khi lưu thông tin: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                }
                            }
                        });
            }
        });
    }

    private void signInWithGoogle() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "Google sign in failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        progressBar.setVisibility(View.VISIBLE);
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            checkAndSaveUserToFirestore(user);
                        } else {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(RegisterActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void checkAndSaveUserToFirestore(FirebaseUser firebaseUser) {
        if (firebaseUser == null) {
            progressBar.setVisibility(View.GONE);
            return;
        }

        db.collection("users").document(firebaseUser.getUid()).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (!document.exists()) {
                                // User doesn't exist in Firestore, save new user
                                saveNewGoogleUser(firebaseUser);
                            } else {
                                // User exists, navigate to MainActivity
                                navigateToMainActivity();
                            }
                        } else {
                            navigateToMainActivity();
                        }
                    }
                });
    }

    private void saveNewGoogleUser(FirebaseUser firebaseUser) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", firebaseUser.getEmail());
        user.put("displayName", firebaseUser.getDisplayName());
        user.put("firstName", firebaseUser.getDisplayName()); // Default first name
        user.put("lastName", ""); // Default empty last name
        user.put("isAdmin", false);

        db.collection("users").document(firebaseUser.getUid())
                .set(user)
                .addOnSuccessListener(aVoid -> navigateToMainActivity())
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Failed to save user data.", Toast.LENGTH_SHORT).show();
                    navigateToMainActivity(); // Navigate anyway
                });
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
