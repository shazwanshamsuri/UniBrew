package com.example.unibrew;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class AdminReviewListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminReviewAdapter adapter;
    private List<Review> reviewList;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_review_list);

        // --- 1. ADD THIS BLOCK TO SHOW THE BACK ARROW ---
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Manage Reviews");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true); // This turns on the arrow
        }
        // -----------------------------------------------

        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.rvAdminReviews);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        reviewList = new ArrayList<>();
        adapter = new AdminReviewAdapter(reviewList, review -> {
            deleteReviewFromFirestore(review);
        });

        recyclerView.setAdapter(adapter);
        fetchReviews();
    }

    // --- 2. ADD THIS METHOD TO MAKE THE ARROW WORK ---
    @Override
    public boolean onSupportNavigateUp() {
        finish(); // This closes the page and goes back
        return true;
    }
    // ------------------------------------------------

    private void fetchReviews() {
        db.collection("reviews").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                reviewList.clear();
                for (QueryDocumentSnapshot document : task.getResult()) {
                    Review review = document.toObject(Review.class);
                    review.setReviewId(document.getId());
                    reviewList.add(review);
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteReviewFromFirestore(Review review) {
        db.collection("reviews").document(review.getReviewId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Review deleted successfully", Toast.LENGTH_SHORT).show();
                    fetchReviews();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error deleting review", Toast.LENGTH_SHORT).show());
    }
}