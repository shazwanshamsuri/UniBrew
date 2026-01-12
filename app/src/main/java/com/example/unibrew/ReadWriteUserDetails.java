package com.example.unibrew;

public class ReadWriteUserDetails {
    public String username, email, imageURL;

    // Empty constructor is required for Firebase
    public ReadWriteUserDetails() {};

    public ReadWriteUserDetails(String textUsername, String textEmail, String textImageURL) {
        this.username = textUsername;
        this.email = textEmail;
        this.imageURL = textImageURL;
    }
}