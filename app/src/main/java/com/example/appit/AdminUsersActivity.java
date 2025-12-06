package com.example.appit;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AdminUsersActivity extends BaseActivity {

    private RecyclerView recyclerView;
    private TextView emptyView;
    private TabLayout tabLayout;
    private FloatingActionButton fabAddUser;
    
    private AdminUserAdapter adapter;
    private List<User> allUsers; // Store all users
    private List<User> displayedUsers; // Store filtered users for display
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        setupToolbar();
        
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        
        recyclerView = findViewById(R.id.recycler_view_users);
        emptyView = findViewById(R.id.empty_view);
        tabLayout = findViewById(R.id.tab_layout_users);
        fabAddUser = findViewById(R.id.fab_add_user);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        
        allUsers = new ArrayList<>();
        displayedUsers = new ArrayList<>();
        adapter = new AdminUserAdapter(displayedUsers);
        recyclerView.setAdapter(adapter);

        loadUsers();
        
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                filterUsers(tab.getPosition());
                updateFabVisibility(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        fabAddUser.setOnClickListener(v -> {
            int selectedTab = tabLayout.getSelectedTabPosition();
            // Both tabs use the same dialog logic, just differ in initial role assignment
            showAddUserDialog(selectedTab == 1);
        });
    }
    
    private void updateFabVisibility(int tabPosition) {
        fabAddUser.setVisibility(View.VISIBLE);
        if (tabPosition == 0) {
            fabAddUser.setContentDescription("Thêm tài khoản mới");
        } else {
            fabAddUser.setContentDescription("Thêm Admin");
        }
    }
    
    private void showAddUserDialog(boolean createAsAdmin) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_user, null);
        builder.setView(view);
        
        TextView tvTitle = view.findViewById(R.id.tv_dialog_title);
        tvTitle.setText(createAsAdmin ? "Thêm Admin mới" : "Thêm người dùng mới");
        
        TextInputEditText etEmail = view.findViewById(R.id.et_email);
        TextInputEditText etPassword = view.findViewById(R.id.et_password);
        TextInputEditText etName = view.findViewById(R.id.et_name);
        Button btnCancel = view.findViewById(R.id.btn_cancel_add_user);
        Button btnConfirm = view.findViewById(R.id.btn_confirm_add_user);
        
        // Use custom buttons instead of builder buttons
        AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            view.setBackgroundResource(R.drawable.dialog_background); // Optional: if you have a rounded bg
            // Fallback for background color if drawable missing, to avoid transparent background issues
            view.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white));
        }
        
        dialog.show();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnConfirm.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String name = etName.getText().toString().trim();
            
            if (TextUtils.isEmpty(email)) {
                etEmail.setError("Cần nhập Email");
                return;
            }
            if (TextUtils.isEmpty(password) || password.length() < 6) {
                etPassword.setError("Mật khẩu cần ít nhất 6 ký tự");
                return;
            }
            if (TextUtils.isEmpty(name)) {
                etName.setError("Cần nhập tên hiển thị");
                return;
            }
            
            createUserAccount(email, password, name, createAsAdmin, dialog);
        });
    }
    
    private void createUserAccount(String email, String password, String name, boolean isAdmin, AlertDialog dialog) {
        // Use a secondary Firebase App to create user without logging out current admin
        String secondaryAppName = "SecondaryApp";
        FirebaseApp secondaryApp = null;
        
        try {
            try {
                secondaryApp = FirebaseApp.getInstance(secondaryAppName);
            } catch (IllegalStateException e) {
                // App doesn't exist, create it
                FirebaseOptions options = FirebaseApp.getInstance().getOptions();
                secondaryApp = FirebaseApp.initializeApp(this, options, secondaryAppName);
            }
            
            FirebaseAuth secondaryAuth = FirebaseAuth.getInstance(secondaryApp);
            
            secondaryAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    
                    // Create User object for Firestore
                    User newUser = new User();
                    newUser.setUid(uid);
                    newUser.setEmail(email);
                    newUser.setDisplayName(name);
                    newUser.setAdmin(isAdmin);
                    newUser.setLocked(false);
                    newUser.setCart(new HashMap<>());
                    newUser.setFavorites(new ArrayList<>());
                    newUser.setShippingAddresses(new ArrayList<>());
                    
                    // Save to Firestore using MAIN db instance (admin rights)
                    db.collection("users").document(uid)
                        .set(newUser)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(AdminUsersActivity.this, "Tạo tài khoản thành công", Toast.LENGTH_SHORT).show();
                            
                            // Sign out secondary auth to clean up
                            secondaryAuth.signOut();
                            
                            dialog.dismiss();
                            
                            // Add to local list and refresh
                            allUsers.add(newUser);
                            filterUsers(tabLayout.getSelectedTabPosition());
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(AdminUsersActivity.this, "Lỗi lưu Firestore: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            // Ideally delete the auth user if firestore fails, but keeping simple here
                        });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(AdminUsersActivity.this, "Lỗi tạo Auth: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
                
        } catch (Exception e) {
             Toast.makeText(this, "Lỗi hệ thống: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Quản lý người dùng");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                allUsers.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    User user = document.toObject(User.class);
                    user.setUid(document.getId());
                    allUsers.add(user);
                }
                filterUsers(tabLayout.getSelectedTabPosition());
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi tải danh sách người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
    
    private void filterUsers(int tabPosition) {
        displayedUsers.clear();
        if (tabPosition == 0) {
            // Regular Users (Not Admin)
            for (User user : allUsers) {
                if (!user.isAdmin()) {
                    displayedUsers.add(user);
                }
            }
        } else {
            // Admins
            for (User user : allUsers) {
                if (user.isAdmin()) {
                    displayedUsers.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyView();
    }
    
    private void updateEmptyView() {
        if (displayedUsers.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
        } else {
            emptyView.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    // Helper method to show confirmation dialog with visible buttons
    // This solves the issue of white-on-white buttons in AlertDialog
    private void showConfirmationDialog(String title, String message, String positiveText, View.OnClickListener onPositive) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(positiveText, null); // Will override listener
        builder.setNegativeButton("Hủy", null);
        
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(d -> {
            // Đặt màu cho nút Positive (Đồng ý/Xóa) thành màu xanh dương
            // Sử dụng màu purple_500 từ resources (được định nghĩa là #2962FF - xanh dương)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(AdminUsersActivity.this, R.color.purple_500));
            
            // Đặt màu cho nút Negative (Hủy) - dùng màu xám đậm
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(ContextCompat.getColor(AdminUsersActivity.this, android.R.color.darker_gray));
        });
        
        dialog.show();
        
        // Set listener after show to access buttons (standard practice)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            onPositive.onClick(v);
            dialog.dismiss();
        });
    }

    private class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

        private final List<User> users;

        public AdminUserAdapter(List<User> users) {
            this.users = users;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_user, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = users.get(position);
            holder.bind(user);
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView email, name, statusLocked;
            SwitchMaterial switchAdmin;
            ImageButton btnLock, btnDelete;
            View layoutActions;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                email = itemView.findViewById(R.id.tv_user_email);
                name = itemView.findViewById(R.id.tv_user_name);
                statusLocked = itemView.findViewById(R.id.tv_status_locked);
                switchAdmin = itemView.findViewById(R.id.switch_admin);
                btnLock = itemView.findViewById(R.id.btn_lock_user);
                btnDelete = itemView.findViewById(R.id.btn_delete_user);
                layoutActions = itemView.findViewById(R.id.layout_actions);
            }

            public void bind(User user) {
                email.setText(user.getEmail());
                name.setText(user.getDisplayName() != null ? user.getDisplayName() : "N/A");
                
                boolean isCurrentUser = false;
                if (auth.getCurrentUser() != null) {
                    isCurrentUser = user.getUid().equals(auth.getCurrentUser().getUid());
                }
                
                if (user.isAdmin()) {
                    // Admin View
                    switchAdmin.setVisibility(View.VISIBLE);
                    btnLock.setVisibility(View.GONE);
                    btnDelete.setVisibility(View.GONE);
                    statusLocked.setVisibility(View.GONE); // Admins typically aren't locked
                    
                    switchAdmin.setOnCheckedChangeListener(null);
                    switchAdmin.setChecked(true);
                    
                    // Cannot remove admin rights from self
                    switchAdmin.setEnabled(!isCurrentUser);
                    
                    switchAdmin.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        // Reset switch immediately to avoid visual flicker before confirmation
                        // We handle the logic in the dialog
                        switchAdmin.setOnCheckedChangeListener(null);
                        switchAdmin.setChecked(!isChecked); // Revert to previous state
                        switchAdmin.setOnCheckedChangeListener(this::onCheckedChanged);

                        // Show confirmation dialog
                        confirmUpdateAdminRole(user, isChecked);
                    });
                    
                } else {
                    // Regular User View
                    switchAdmin.setVisibility(View.GONE);
                    btnLock.setVisibility(View.VISIBLE);
                    btnDelete.setVisibility(View.VISIBLE);
                    
                    // Lock status
                    if (user.isLocked()) {
                        statusLocked.setVisibility(View.VISIBLE);
                        btnLock.setImageResource(android.R.drawable.ic_partial_secure); // Unlocked icon or similar
                    } else {
                        statusLocked.setVisibility(View.GONE);
                        btnLock.setImageResource(android.R.drawable.ic_secure);
                    }
                    
                    btnLock.setOnClickListener(v -> confirmToggleUserLock(user));
                    
                    btnDelete.setOnClickListener(v -> confirmDeleteUser(user));
                }
            }

            // Helper for listener reference
            private void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                 int pos = getAdapterPosition();
                 if (pos != RecyclerView.NO_POSITION) {
                     User user = users.get(pos);
                     confirmUpdateAdminRole(user, isChecked);
                 }
            }
            
            private void confirmUpdateAdminRole(User user, boolean isNewStateAdmin) {
                String action = isNewStateAdmin ? "cấp quyền Admin" : "gỡ quyền Admin";
                String message = "Bạn có chắc chắn muốn " + action + " cho tài khoản " + user.getEmail() + " không?";
                
                // Use custom confirmation dialog helper
                showConfirmationDialog("Xác nhận thay đổi quyền", message, "Đồng ý", v -> {
                    updateUserRole(user, isNewStateAdmin);
                });
            }

            private void updateUserRole(User user, boolean isAdmin) {
                db.collection("users").document(user.getUid())
                    .update("isAdmin", isAdmin)
                    .addOnSuccessListener(aVoid -> {
                        user.setAdmin(isAdmin);
                        String message = isAdmin ? "Đã cấp quyền Admin" : "Đã gỡ quyền Admin";
                        Toast.makeText(AdminUsersActivity.this, message, Toast.LENGTH_SHORT).show();
                        // Refresh list to move user to correct tab
                        loadUsers(); 
                    })
                    .addOnFailureListener(e -> {
                        // Revert visual state if failed
                        if (switchAdmin.getVisibility() == View.VISIBLE) {
                             switchAdmin.setOnCheckedChangeListener(null);
                             switchAdmin.setChecked(!isAdmin); 
                             switchAdmin.setOnCheckedChangeListener((btn, checked) -> confirmUpdateAdminRole(user, checked));
                        }
                        Toast.makeText(AdminUsersActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            }
            
            private void confirmToggleUserLock(User user) {
                 boolean newLockState = !user.isLocked();
                 String action = newLockState ? "khóa" : "mở khóa";
                 String message = "Bạn có chắc chắn muốn " + action + " tài khoản " + user.getEmail() + " không?";
                 
                 showConfirmationDialog("Xác nhận " + action + " tài khoản", message, "Đồng ý", v -> {
                     toggleUserLock(user);
                 });
            }
            
            private void toggleUserLock(User user) {
                boolean newLockState = !user.isLocked();
                String message = newLockState ? "Đã khóa tài khoản" : "Đã mở khóa tài khoản";
                
                db.collection("users").document(user.getUid())
                    .update("isLocked", newLockState)
                    .addOnSuccessListener(aVoid -> {
                        user.setLocked(newLockState);
                        notifyItemChanged(getAdapterPosition());
                        Toast.makeText(AdminUsersActivity.this, message, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(AdminUsersActivity.this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
            
            private void confirmDeleteUser(User user) {
                String message = "Bạn có chắc chắn muốn xóa vĩnh viễn tài khoản " + user.getEmail() + " không? Hành động này không thể hoàn tác.";
                showConfirmationDialog("Xóa người dùng", message, "Xóa", v -> {
                    deleteUser(user);
                });
            }
            
            private void deleteUser(User user) {
                db.collection("users").document(user.getUid())
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(AdminUsersActivity.this, "Đã xóa người dùng", Toast.LENGTH_SHORT).show();
                        allUsers.remove(user);
                        filterUsers(tabLayout.getSelectedTabPosition());
                    })
                    .addOnFailureListener(e -> Toast.makeText(AdminUsersActivity.this, "Lỗi xóa: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }
}