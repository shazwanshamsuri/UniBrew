package com.example.unibrew;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

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
    private String cafeName, cafeId;

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
        Button btnNavigate = findViewById(R.id.btnNavigate);

        // 2. Get Data from Intent
        cafeId = getIntent().getStringExtra("cafeId");
        cafeName = getIntent().getStringExtra("cafeName");
        String cafeDesc = getIntent().getStringExtra("cafeDesc");
        String cafeImageUrl = getIntent().getStringExtra("cafeImageUrl");
        double lat = getIntent().getDoubleExtra("cafeLat", 0.0);
        double lng = getIntent().getDoubleExtra("cafeLng", 0.0);

        // 3. Set Text & Image
        if (cafeName != null) tvName.setText(cafeName);
        if (cafeDesc != null) tvDesc.setText(cafeDesc);

        if (cafeImageUrl != null && !cafeImageUrl.isEmpty()) {
            Glide.with(this).load(cafeImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop().into(ivCafeImage);
        }

        // 4. Load Reviews (Using the new "Flattened" Query)
        if (cafeId != null) {
            loadReviews(cafeId);
        } else {
            Toast.makeText(this, "Error: Cafe ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 5. Submit Logic
        btnSubmitReview.setOnClickListener(v -> submitReview());

        // 6. Navigation Logic
        if (btnNavigate != null) {
            btnNavigate.setOnClickListener(v -> {
                if (lat != 0.0 && lng != 0.0) {
                    Uri gmmIntentUri = Uri.parse("google.navigation:q=" + lat + "," + lng);
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(mapIntent);
                    } else {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://maps.google.com/maps?daddr=" + lat + "," + lng)));
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

        // --- PREPARE DATA ---
        Map<String, Object> review = new HashMap<>();

        // CRITICAL: We save the Cafe ID so we can find this review later!
        review.put("cafeId", cafeId);
        review.put("cafeName", cafeName);
        review.put("comment", comment);
        review.put("rating", rating);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String name = (currentUser != null && currentUser.getDisplayName() != null) ? currentUser.getDisplayName() : "Anonymous";
        String photo = (currentUser != null && currentUser.getPhotoUrl() != null) ? currentUser.getPhotoUrl().toString() : "";

        // Standardized Keys (Matching your Admin Adapter)
        review.put("userName", name);
        review.put("photoUrl", photo);

        // --- SAVE TO "reviews" COLLECTION (FLAT STRUCTURE) ---
        db.collection("reviews")
                .add(review)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Review Posted!", Toast.LENGTH_SHORT).show();
                    etComment.setText("");
                    rbRating.setRating(0);
                    loadReviews(cafeId); // Refresh list
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReviews(String currentCafeId) {
        llReviewsContainer.removeAllViews();

        // --- NEW QUERY: Get reviews ONLY for this specific Cafe ID ---
        db.collection("reviews")
                .whereEqualTo("cafeId", currentCafeId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        View view = getLayoutInflater().inflate(R.layout.item_review, null);

                        TextView tvUser = view.findViewById(R.id.tvReviewUser);
                        TextView tvComment = view.findViewById(R.id.tvReviewComment);
                        RatingBar rb = view.findViewById(R.id.rbReviewItem);
                        ImageView ivProfile = view.findViewById(R.id.ivReviewProfile);

                        // Use "userName" because we standardized it in submitReview()
                        tvUser.setText(doc.getString("userName"));
                        tvComment.setText(doc.getString("comment"));

                        Double r = doc.getDouble("rating");
                        if (r != null) rb.setRating(r.floatValue());

                        String photo = doc.getString("photoUrl");
                        if (photo != null && !photo.isEmpty()) {
                            Glide.with(this).load(photo).circleCrop().into(ivProfile);
                        }
                        llReviewsContainer.addView(view);
                    }
                });
    }
}