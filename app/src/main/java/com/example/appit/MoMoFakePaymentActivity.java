package com.example.appit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MoMoFakePaymentActivity extends AppCompatActivity {

    public static final int REQUEST_CODE_MOMO_FAKE = 999; // Mã request code riêng

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_momo_fake_payment);

        Button btnSuccess = findViewById(R.id.btn_fake_success);
        Button btnFailure = findViewById(R.id.btn_fake_failure);

        btnSuccess.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("status", 0); // 0 = Success
            setResult(RESULT_OK, resultIntent);
            finish(); // Đóng activity và trả kết quả về
        });

        btnFailure.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("status", 1); // 1 = Failure
            resultIntent.putExtra("message", "Người dùng hủy giao dịch");
            setResult(RESULT_OK, resultIntent); // MoMo thật vẫn trả về RESULT_OK dù thất bại
            finish(); // Đóng activity và trả kết quả về
        });
    }
}
