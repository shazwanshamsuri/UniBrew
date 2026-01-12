package com.example.unibrew;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.ReviewViewHolder> {

    private List<Review> reviewList;
    private OnReviewDeleteListener listener;

    public interface OnReviewDeleteListener {
        void onReviewDelete(Review review);
    }

    public AdminReviewAdapter(List<Review> reviewList, OnReviewDeleteListener listener) {
        this.reviewList = reviewList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin_review, parent, false);
        return new ReviewViewHolder(v);
    }

    // Inside AdminReviewAdapter.java
    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviewList.get(position);

        holder.tvCafeName.setText(review.getCafeName());
        holder.tvUserName.setText(review.getUserName());

        // MAKE SURE THIS MATCHES YOUR NEW REVIEW CLASS:
        holder.tvComment.setText(review.getComment());

        holder.tvRating.setText(review.getRating() + "★");

        holder.btnDelete.setOnClickListener(v -> listener.onReviewDelete(review));
    }

    @Override
    public int getItemCount() { return reviewList.size(); }

    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvCafeName, tvUserName, tvComment, tvRating;
        ImageButton btnDelete;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCafeName = itemView.findViewById(R.id.tvAdminRevCafe);
            tvUserName = itemView.findViewById(R.id.tvAdminRevUser);
            tvComment = itemView.findViewById(R.id.tvAdminRevComment);
            tvRating = itemView.findViewById(R.id.tvAdminRevRating);
            btnDelete = itemView.findViewById(R.id.btnDeleteReview);
        }
    }
}