package com.example.unibrew;

import com.google.firebase.firestore.DocumentId; // <--- IMPORTANT IMPORT

public class Cafe {

    // 1. This special annotation automatically grabs the random Document ID
    @DocumentId
    private String cafeId;

    private String name, description, imageUrl;
    private double latitude, longitude;

    // Required empty constructor for Firebase
    public Cafe() {}

    // 2. This is the Getter method your Adapter is screaming for!
    public String getCafeId() { return cafeId; }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}