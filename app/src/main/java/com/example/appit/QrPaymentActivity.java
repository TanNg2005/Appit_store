package com.example.appit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import java.util.Locale;

public class QrPaymentActivity extends AppCompatActivity {

    private ImageView qrCodeImageView;
    private TextView countdownTimerTextView;
    private CountDownTimer visualCountDownTimer;

    private String orderId;
    private double totalAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_payment);

        qrCodeImageView = findViewById(R.id.qr_code_image);
        countdownTimerTextView = findViewById(R.id.countdown_timer);

        // Get data from CartActivity
        totalAmount = getIntent().getDoubleExtra("TOTAL_AMOUNT", 0.0);
        orderId = getIntent().getStringExtra("ORDER_ID");

        // Tải ảnh QR thật từ URL
        loadQrFromUrl();
        
        // Bắt đầu đồng hồ đếm ngược trực quan
        startVisualCountdown();

        // SỬA: Biến mã QR thành nút bấm ẩn để giả lập quét thành công
        qrCodeImageView.setOnClickListener(v -> {
            proceedToSuccessScreen();
        });
    }

    private void loadQrFromUrl() {
        String qrUrl = "https://img.vietqr.io/image/HDB-999991000160605-print.png";
        Glide.with(this)
                .load(qrUrl)
                .into(qrCodeImageView);
    }

    private void proceedToSuccessScreen() {
        // Dừng các bộ đếm giờ để tránh rò rỉ bộ nhớ
        if (visualCountDownTimer != null) {
            visualCountDownTimer.cancel();
        }

        // Kiểm tra activity còn hoạt động không trước khi chuyển
        if (!isFinishing() && !isDestroyed()) {
            Intent intent = new Intent(QrPaymentActivity.this, PaymentSuccessActivity.class);
            intent.putExtra("ORDER_ID", orderId);
            intent.putExtra("TOTAL_AMOUNT", totalAmount);
            startActivity(intent);
            finish();
        }
    }

    private void startVisualCountdown() {
        visualCountDownTimer = new CountDownTimer(300000, 1000) { // Đếm ngược 5 phút
            @Override
            public void onTick(long millisUntilFinished) {
                long seconds = millisUntilFinished / 1000;
                countdownTimerTextView.setText(String.format(Locale.getDefault(), "Mã sẽ hết hạn trong: %02d:%02d", seconds / 60, seconds % 60));
            }

            @Override
            public void onFinish() {
                countdownTimerTextView.setText("Mã đã hết hạn");
                qrCodeImageView.setOnClickListener(null); // Vô hiệu hóa nút ẩn khi hết hạn
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (visualCountDownTimer != null) {
            visualCountDownTimer.cancel();
        }
    }
}
