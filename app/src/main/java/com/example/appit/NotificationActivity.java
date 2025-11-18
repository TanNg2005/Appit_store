package com.example.appit;

import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";
    private RecyclerView recyclerView;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.notification_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerView = findViewById(R.id.recycler_view_notifications);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificationAdapter(this, notificationList);
        recyclerView.setAdapter(adapter);

        loadNotifications();
    }

    private void loadNotifications() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        // SỬA LỖI: Bỏ sắp xếp phía server để không cần tạo chỉ mục
        db.collection("notifications")
                .whereEqualTo("userId", currentUser.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e(TAG, "Lỗi khi lắng nghe thông báo.", e);
                        Toast.makeText(NotificationActivity.this, "Lỗi: Không thể tải thông báo.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    notificationList.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            try {
                                Notification notification = doc.toObject(Notification.class);
                                notification.setDocumentId(doc.getId());
                                notificationList.add(notification);
                            } catch (Exception ex) {
                                Log.e(TAG, "Lỗi khi chuyển đổi dữ liệu thông báo: " + doc.getId(), ex);
                            }
                        }
                        
                        // Sắp xếp thủ công trên điện thoại
                        Collections.sort(notificationList, (o1, o2) -> {
                            if (o1.getTimestamp() == null || o2.getTimestamp() == null) return 0;
                            return o2.getTimestamp().compareTo(o1.getTimestamp());
                        });
                    }
                    adapter.notifyDataSetChanged();
                    if(notificationList.isEmpty()){
                        Toast.makeText(this, "Bạn chưa có thông báo nào.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
