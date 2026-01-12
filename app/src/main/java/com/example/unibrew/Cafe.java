package com.example.unibrew;

import com.google.firebase.firestore.DocumentId;

public class Cafe {

    @DocumentId
    private String cafeId;

    private String name, description, imageUrl;
    private double latitude, longitude;
    private float rating; // Added this because your screenshots show star ratings

    public Cafe() {}

    // --- FIX: Add this method to solve the 'cannot find symbol' error ---
    public void setId(String id) {
        this.cafeId = id;
    }
    // ------------------------------------------------------------------

    // Keep these as well
    public String getId() { return cafeId; }
    public String getCafeId() { return cafeId; }
    public void setCafeId(String cafeId) { this.cafeId = cafeId; }

    // Getters and Setters for other fields
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
}