package com.example.unibrew;

import android.content.Intent; // Needed for starting new activities
import android.net.Uri;       // Needed for Google Maps URL
import android.os.Bundle;
import android.view.View;
import android.widget.Button;   // <--- THIS WAS MISSING!
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class CafeDetailActivity extends AppCompatActivity {

    private TextView tvName, tvDesc;
    private ImageView ivCafeImage;
    private FirebaseFirestore db;
    private LinearLayout llReviewsContainer;
    private RatingBar rbRating;
    private EditText etComment;
    private View btnSubmitReview;
    private String cafeName;
    private String cafeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cafe_detail);

        db = FirebaseFirestore.getInstance();

        // 1. Initialize Views
        tvName = findViewById(R.id.tvDetailName);
        tvDesc = findViewById(R.id.tvDetailDesc);
        ivCafeImage = findViewById(R.id.ivCafeImage);
        llReviewsContainer = findViewById(R.id.llReviewsContainer);
        rbRating = findViewById(R.id.rbCafeRating);
        etComment = findViewById(R.id.etReviewComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);

        // --- Initialize Navigation Button ---
        Button btnNavigate = findViewById(R.id.btnNavigate);

        // 2. Get Data from Intent
        cafeId = getIntent().getStringExtra("cafeId");
        cafeName = getIntent().getStringExtra("cafeName");
        String cafeDesc = getIntent().getStringExtra("cafeDesc");
        String cafeImageUrl = getIntent().getStringExtra("cafeImageUrl");

        // Get Coordinates
        double lat = getIntent().getDoubleExtra("cafeLat", 0.0);
        double lng = getIntent().getDoubleExtra("cafeLng", 0.0);

        // 3. Set the Text
        if (cafeName != null) tvName.setText(cafeName);
        if (cafeDesc != null) tvDesc.setText(cafeDesc);

        // 4. Load the Image
        if (cafeImageUrl != null && !cafeImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(cafeImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(ivCafeImage);
        }

        // 5. Load Reviews
        if (cafeId != null) {
            loadReviews(cafeId);
        } else {
            Toast.makeText(this, "Error: Cafe ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 6. Submit Review Logic
        btnSubmitReview.setOnClickListener(v -> submitReview());

        // 7. Navigation Logic
        if (btnNavigate != null) {
            btnNavigate.setOnClickListener(v -> {
                if (lat != 0.0 && lng != 0.0) {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");

                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        // Fallback to browser
                        Uri browserUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + lat + "," + lng);
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, browserUri);
                        startActivity(browserIntent);
                    }
                } else {
                    Toast.makeText(this, "Location not available", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void submitReview() {
        String comment = etComment.getText().toString();
        float rating = rbRating.getRating();

        if (comment.isEmpty()) {
            etComment.setError("Please write a comment");
            return;
        }

        Map<String, Object> review = new HashMap<>();
        review.put("cafeName", cafeName);
        review.put("comment", comment);
        review.put("rating", rating);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String name = (currentUser != null && currentUser.getDisplayName() != null) ? currentUser.getDisplayName() : "Anonymous Student";
        String photo = (currentUser != null && currentUser.getPhotoUrl() != null) ? currentUser.getPhotoUrl().toString() : "";

        review.put("user", name);
        review.put("photoUrl", photo);

        db.collection("reviews").document(cafeId).collection("reviews")
                .add(review)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Review Posted!", Toast.LENGTH_SHORT).show();
                    etComment.setText("");
                    loadReviews(cafeId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to post review", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReviews(String cafeId) {
        llReviewsContainer.removeAllViews();

        db.collection("reviews").document(cafeId).collection("reviews")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        View view = getLayoutInflater().inflate(R.layout.item_review, null);

                        TextView tvUser = view.findViewById(R.id.tvReviewUser);
                        TextView tvComment = view.findViewById(R.id.tvReviewComment);
                        RatingBar rb = view.findViewById(R.id.rbReviewItem);
                        ImageView ivProfile = view.findViewById(R.id.ivReviewProfile);

                        tvUser.setText(doc.getString("user"));
                        tvComment.setText(doc.getString("comment"));

                        Double r = doc.getDouble("rating");
                        if (r != null) rb.setRating(r.floatValue());

                        String photo = doc.getString("photoUrl");
                        if (photo != null && !photo.isEmpty()) {
                            Glide.with(this)
                                    .load(photo)
                                    .placeholder(R.drawable.ic_launcher_background)
                                    .circleCrop()
                                    .into(ivProfile);
                        }
                        llReviewsContainer.addView(view);
                    }
                });
    }
}