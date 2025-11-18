package com.example.appit;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private final Context context;
    private final List<Notification> notificationList;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public NotificationAdapter(Context context, List<Notification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.notification_item, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notificationList.get(position);

        holder.title.setText(notification.getTitle());
        holder.message.setText(notification.getMessage());

        if (notification.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.timestamp.setText(sdf.format(notification.getTimestamp()));
        } else {
            holder.timestamp.setText("");
        }

        // SỬA LỖI: Gọi đúng phương thức isRead()
        if (notification.isRead()) {
            holder.title.setTypeface(null, Typeface.NORMAL);
            holder.message.setTypeface(null, Typeface.NORMAL);
            holder.unreadIndicator.setVisibility(View.GONE);
        } else {
            holder.title.setTypeface(null, Typeface.BOLD);
            holder.message.setTypeface(null, Typeface.BOLD);
            holder.unreadIndicator.setVisibility(View.VISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (!notification.isRead()) {
                db.collection("notifications").document(notification.getDocumentId())
                        .update("isRead", true);

                // SỬA LỖI: Gọi đúng phương thức setRead()
                notification.setRead(true);
                notifyItemChanged(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView title, message, timestamp;
        ImageView unreadIndicator;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.notification_title);
            message = itemView.findViewById(R.id.notification_message);
            timestamp = itemView.findViewById(R.id.notification_timestamp);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
        }
    }
}
