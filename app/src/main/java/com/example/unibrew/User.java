package com.example.unibrew;

import com.google.firebase.firestore.DocumentId;

public class User {

    // 1. This grabs the Firestore Document ID automatically
    @DocumentId
    private String userId;

    private String name;
    private String email;
    private String profileImageUrl;
    private String role; // "admin" or "user"

    // 2. Required Empty Constructor for Firebase
    public User() {}

    // 3. Constructor for creating new users manually (Optional but good to have)
    public User(String name, String email, String role) {
        this.name = name;
        this.email = email;
        this.role = role;
    }

    // --- GETTERS ---
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public String getRole() { return role; }

    // --- SETTERS ---
    // This is the CRITICAL one for fixing the "Delete" bug
    public void setUserId(String userId) { this.userId = userId; }

    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setRole(String role) { this.role = role; }
}