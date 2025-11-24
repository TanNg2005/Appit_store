package com.example.appit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final Context context;
    private final List<Product.Review> reviews;

    public ReviewAdapter(Context context, List<Product.Review> reviews) {
        this.context = context;
        this.reviews = reviews;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Product.Review review = reviews.get(position);

        holder.userName.setText(review.getReviewerName() != null ? review.getReviewerName() : "Ẩn danh");
        holder.ratingBar.setRating(review.getRating());
        holder.comment.setText(review.getComment());

        if (review.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.date.setText(sdf.format(review.getTimestamp()));
        } else {
            holder.date.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return reviews != null ? reviews.size() : 0;
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView userName, comment, date;
        RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.review_user_name);
            comment = itemView.findViewById(R.id.review_comment);
            date = itemView.findViewById(R.id.review_date);
            ratingBar = itemView.findViewById(R.id.review_rating_bar);
        }
    }
}
