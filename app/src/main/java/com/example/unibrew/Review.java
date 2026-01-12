package com.example.unibrew;

import com.google.firebase.firestore.DocumentId;

public class Review {

    // 1. The ID of the review document (for deleting)
    @DocumentId
    private String reviewId;

    // 2. These names MUST match the keys in CafeDetailActivity.submitReview()
    private String cafeId;
    private String cafeName;
    private String userName; // Matches "userName"
    private String comment;  // Matches "comment"
    private String photoUrl; // Matches "photoUrl"
    private float rating;    // Matches "rating"

    public Review() {} // Required empty constructor

    // --- Getters and Setters ---
    public String getReviewId() { return reviewId; }
    public void setReviewId(String reviewId) { this.reviewId = reviewId; }

    public String getCafeId() { return cafeId; }
    public void setCafeId(String cafeId) { this.cafeId = cafeId; }

    public String getCafeName() { return cafeName; }
    public void setCafeName(String cafeName) { this.cafeName = cafeName; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
}