package com.example.appit;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends BaseActivity {

    private SwitchMaterial themeSwitch, promoSwitch, orderSwitch, newProductSwitch;
    private SharedPreferences sharedPreferences;
    private Spinner languageSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.settings_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(R.string.nav_settings);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);

        // --- Theme Switch ---
        themeSwitch = findViewById(R.id.theme_switch);
        themeSwitch.setChecked(sharedPreferences.getBoolean("dark_mode", false));
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("dark_mode", isChecked);
            editor.apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        // --- Language Spinner ---
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // --- Notification Switches ---
        promoSwitch = findViewById(R.id.promo_notification_switch);
        orderSwitch = findViewById(R.id.order_notification_switch);
        newProductSwitch = findViewById(R.id.new_product_notification_switch);

        // Load saved settings
        promoSwitch.setChecked(sharedPreferences.getBoolean("promo_notification", true));
        orderSwitch.setChecked(sharedPreferences.getBoolean("order_notification", true));
        newProductSwitch.setChecked(sharedPreferences.getBoolean("new_product_notification", true));

        // Save settings on change
        promoSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("promo_notification", isChecked).apply();
        });
        orderSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("order_notification", isChecked).apply();
        });
        newProductSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            sharedPreferences.edit().putBoolean("new_product_notification", isChecked).apply();
        });
    }

    private void showLanguageChangeDialog(String language) {
        new AlertDialog.Builder(this, R.style.AppAlertDialog)
                .setTitle(R.string.language_change_title)
                .setMessage(R.string.language_change_message)
                .setPositiveButton(R.string.language_change_confirm, (dialog, which) -> {
                    LocaleHelper.setLocale(SettingsActivity.this, language);
                    finishAffinity();
                })
                .setNegativeButton(R.string.language_change_cancel, (dialog, which) -> {
                    // Reset spinner to current language
                    String currentLanguage = LocaleHelper.getLanguage(SettingsActivity.this);
                    if (currentLanguage.equals("en")) {
                        languageSpinner.setSelection(1);
                    } else {
                        languageSpinner.setSelection(0);
                    }
                })
                .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
