package com.example.appit;

import android.animation.Animator;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class PaymentSuccessActivity extends AppCompatActivity {

    private String orderId;
    private double totalAmount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_success);

        // --- SỬA: Lấy dữ liệu từ Deeplink hoặc Intent thông thường ---
        handleIntentData();

        LottieAnimationView animationView = findViewById(R.id.lottie_animation_view);

        animationView.addAnimatorListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) { }

            @Override
            public void onAnimationEnd(Animator animation) {
                // Animation has finished, now open the OrderActivity
                Intent intent = new Intent(PaymentSuccessActivity.this, OrderActivity.class);
                intent.putExtra("ORDER_ID", orderId); // Truyền dữ liệu đã lấy
                intent.putExtra("TOTAL_AMOUNT", totalAmount); // Truyền dữ liệu đã lấy
                startActivity(intent);
                finish(); // Close this activity so the user can't go back to it
            }

            @Override
            public void onAnimationCancel(Animator animation) { }

            @Override
            public void onAnimationRepeat(Animator animation) { }
        });
    }

    private void handleIntentData() {
        Intent intent = getIntent();
        Uri data = intent.getData();

        if (data != null && "myapp".equals(data.getScheme())) { // Được mở từ deeplink
            orderId = data.getQueryParameter("orderId");
            String amountStr = data.getQueryParameter("amount");
            try {
                if (amountStr != null) {
                    totalAmount = Double.parseDouble(amountStr);
                }
            } catch (NumberFormatException e) {
                totalAmount = 0.0;
            }
        } else { // Được mở từ một Intent thông thường (dự phòng)
            orderId = intent.getStringExtra("ORDER_ID");
            totalAmount = intent.getDoubleExtra("TOTAL_AMOUNT", 0.0);
        }
    }
}
