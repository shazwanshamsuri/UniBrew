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

    // --- NEW: Call Button ---
    private Button btnCall;
    // ------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cafe_detail);

        db = FirebaseFirestore.getInstance();

        tvName = findViewById(R.id.tvDetailName);
        tvDesc = findViewById(R.id.tvDetailDesc);
        ivCafeImage = findViewById(R.id.ivCafeImage);
        llReviewsContainer = findViewById(R.id.llReviewsContainer);
        rbRating = findViewById(R.id.rbCafeRating);
        etComment = findViewById(R.id.etReviewComment);
        btnSubmitReview = findViewById(R.id.btnSubmitReview);
        Button btnNavigate = findViewById(R.id.btnNavigate);

        // --- NEW: Find Call Button ---
        // You MUST add this ID to your XML: android:id="@+id/btnCall"
        btnCall = findViewById(R.id.btnCall);
        // -----------------------------

        cafeId = getIntent().getStringExtra("cafeId");
        cafeName = getIntent().getStringExtra("cafeName");
        String cafeDesc = getIntent().getStringExtra("cafeDesc");
        String cafeImageUrl = getIntent().getStringExtra("cafeImageUrl");
        double lat = getIntent().getDoubleExtra("cafeLat", 0.0);
        double lng = getIntent().getDoubleExtra("cafeLng", 0.0);

        if (cafeName != null) tvName.setText(cafeName);
        if (cafeDesc != null) tvDesc.setText(cafeDesc);

        if (cafeImageUrl != null && !cafeImageUrl.isEmpty()) {
            Glide.with(this).load(cafeImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .centerCrop().into(ivCafeImage);
        }

        if (cafeId != null) {
            loadReviews(cafeId);
            // --- NEW: Fetch Phone Number ---
            fetchCafeDetails(cafeId);
            // -------------------------------
        } else {
            Toast.makeText(this, "Error: Cafe ID missing", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnSubmitReview.setOnClickListener(v -> submitReview());

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

    // --- NEW: Get Phone Number from Database ---
    private void fetchCafeDetails(String id) {
        db.collection("cafes").document(id).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String phone = documentSnapshot.getString("phoneNumber");
                if (phone != null && !phone.isEmpty()) {
                    setupCallButton(phone);
                } else {
                    // Hide button if no phone number exists
                    if (btnCall != null) btnCall.setVisibility(View.GONE);
                }
            }
        });
    }

    // --- NEW: Setup Call Button ---
    private void setupCallButton(String phone) {
        if (btnCall == null) return;

        btnCall.setVisibility(View.VISIBLE);
        btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone));
            startActivity(intent);
        });
    }

    private void submitReview() {
        String comment = etComment.getText().toString();
        float rating = rbRating.getRating();

        if (comment.isEmpty()) {
            etComment.setError("Please write a comment");
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "You must be logged in!", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(documentSnapshot -> {
                    String currentUserName = documentSnapshot.getString("name");
                    String currentUserPhoto = documentSnapshot.getString("imageURL");

                    if (currentUserName == null) currentUserName = currentUser.getDisplayName();
                    if (currentUserPhoto == null && currentUser.getPhotoUrl() != null) {
                        currentUserPhoto = currentUser.getPhotoUrl().toString();
                    }

                    Map<String, Object> review = new HashMap<>();
                    review.put("cafeId", cafeId);
                    review.put("cafeName", cafeName);
                    review.put("comment", comment);
                    review.put("rating", rating);
                    review.put("userName", currentUserName);
                    review.put("photoUrl", currentUserPhoto);

                    db.collection("reviews").add(review)
                            .addOnSuccessListener(doc -> {
                                Toast.makeText(this, "Review Posted!", Toast.LENGTH_SHORT).show();
                                etComment.setText("");
                                rbRating.setRating(0);
                                loadReviews(cafeId);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Could not fetch user profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadReviews(String currentCafeId) {
        llReviewsContainer.removeAllViews();

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