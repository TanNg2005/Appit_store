package com.example.appit;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class OrderMonitorService extends Service {

    private static final String TAG = "OrderMonitorService";
    private static final String CHANNEL_ID = "OrderMonitorChannel";
    private static final int NOTIFICATION_ID = 123;
    private ListenerRegistration orderListener;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createForegroundNotification());
        startListeningForOrders();
    }

    private void startListeningForOrders() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // Lắng nghe thay đổi trong collection "orders" của user hiện tại
        orderListener = FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.w(TAG, "Listen failed.", e);
                        return;
                    }

                    if (snapshots != null) {
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            // Chỉ quan tâm đến đơn hàng bị sửa đổi (Modified) - tức là trạng thái thay đổi
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                Order order = dc.getDocument().toObject(Order.class);
                                sendOrderUpdateNotification(order);
                            }
                        }
                    }
                });
    }

    private void sendOrderUpdateNotification(Order order) {
        String title = "Cập nhật đơn hàng";
        String message = "Đơn hàng " + order.getItems().get(0).getProductName() + "... đang ở trạng thái: " + order.getStatus();

        Intent intent = new Intent(this, OrderActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            // Dùng ID khác nhau (dựa trên time) để không ghi đè thông báo cũ
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private Notification createForegroundNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Appit Store")
                .setContentText("Đang theo dõi trạng thái đơn hàng của bạn...")
                .setSmallIcon(R.drawable.ic_notifications)
                .setPriority(NotificationCompat.PRIORITY_LOW); // Low priority để không làm phiền

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Order Monitor Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Nếu service bị giết, tự động khởi động lại
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (orderListener != null) {
            orderListener.remove();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}