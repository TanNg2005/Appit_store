package com.example.appit;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends BaseActivity {

    private SwitchMaterial themeSwitch, promoSwitch, orderSwitch, newProductSwitch;
    private SharedPreferences sharedPreferences;
    private Spinner languageSpinner;
    private View btnChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        findViewById(R.id.btn_back).setOnClickListener(v -> onBackPressed());

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);

        initThemeSwitch();
        initLanguageSpinner();
        initNotificationSwitches();
        initAccountSettings();
    }

    private void initThemeSwitch() {
        themeSwitch = findViewById(R.id.theme_switch);
        themeSwitch.setChecked(sharedPreferences.getBoolean("dark_mode", false));
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });
    }

    private void initLanguageSpinner() {
        languageSpinner = findViewById(R.id.language_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.languages_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageSpinner.setAdapter(adapter);

        String currentLanguage = LocaleHelper.getLanguage(this);
        if (currentLanguage.equals("en")) {
            languageSpinner.setSelection(1);
        } else {
            languageSpinner.setSelection(0);
        }

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedLanguage = position == 1 ? "en" : "vi";
                if (!selectedLanguage.equals(LocaleHelper.getLanguage(SettingsActivity.this))) {
                    showLanguageChangeDialog(selectedLanguage);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void initNotificationSwitches() {
        promoSwitch = findViewById(R.id.promo_notification_switch);
        orderSwitch = findViewById(R.id.order_notification_switch);
        newProductSwitch = findViewById(R.id.new_product_notification_switch);

        promoSwitch.setChecked(sharedPreferences.getBoolean("promo_notification", true));
        orderSwitch.setChecked(sharedPreferences.getBoolean("order_notification", true));
        newProductSwitch.setChecked(sharedPreferences.getBoolean("new_product_notification", true));

        promoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sharedPreferences.edit().putBoolean("promo_notification", isChecked).apply());
        orderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sharedPreferences.edit().putBoolean("order_notification", isChecked).apply());
        newProductSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> 
            sharedPreferences.edit().putBoolean("new_product_notification", isChecked).apply());
    }
    
    private void initAccountSettings() {
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private void showLanguageChangeDialog(String language) {
        new AlertDialog.Builder(this, R.style.AppAlertDialog)
                .setTitle(R.string.language_change_title)
                .setMessage(R.string.language_change_message)
                .setPositiveButton(R.string.language_change_confirm, (dialog, which) -> {
                    LocaleHelper.setLocale(SettingsActivity.this, language);
                    finishAffinity(); // Restart app to apply changes fully
                })
                .setNegativeButton(R.string.language_change_cancel, (dialog, which) -> {
                    // Reset spinner to current language
                    String currentLanguage = LocaleHelper.getLanguage(SettingsActivity.this);
                    languageSpinner.setSelection(currentLanguage.equals("en") ? 1 : 0);
                })
                .show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        builder.setView(view);
        
        TextInputEditText etCurrentPass = view.findViewById(R.id.et_current_password);
        TextInputEditText etNewPass = view.findViewById(R.id.et_new_password);
        TextInputEditText etConfirmPass = view.findViewById(R.id.et_confirm_password);
        
        builder.setPositiveButton("Đổi mật khẩu", null);
        builder.setNegativeButton("Hủy", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
        
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String currentPass = etCurrentPass.getText().toString();
            String newPass = etNewPass.getText().toString();
            String confirmPass = etConfirmPass.getText().toString();
            
            if (TextUtils.isEmpty(currentPass)) {
                etCurrentPass.setError("Cần nhập mật khẩu hiện tại");
                return;
            }
            if (TextUtils.isEmpty(newPass) || newPass.length() < 6) {
                etNewPass.setError("Mật khẩu mới phải từ 6 ký tự trở lên");
                return;
            }
            if (!newPass.equals(confirmPass)) {
                etConfirmPass.setError("Mật khẩu xác nhận không khớp");
                return;
            }
            
            changePassword(currentPass, newPass, dialog);
        });
    }
    
    private void changePassword(String currentPass, String newPass, AlertDialog dialog) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            // Re-authenticate user
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPass);
            
            user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Password verified, now update
                    user.updatePassword(newPass)
                        .addOnSuccessListener(aVoid1 -> {
                            Toast.makeText(SettingsActivity.this, "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(SettingsActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(SettingsActivity.this, "Mật khẩu hiện tại không đúng", Toast.LENGTH_SHORT).show();
                });
        } else {
            Toast.makeText(this, "Lỗi xác thực người dùng", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}