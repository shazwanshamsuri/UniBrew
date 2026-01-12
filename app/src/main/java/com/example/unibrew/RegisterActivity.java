package com.example.unibrew;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore; // UPDATED: Using Firestore
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etUsername;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ImageView ivProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db; // UPDATED: Firestore Instance
    private Uri imageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance(); // UPDATED: Initialize Firestore

        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        etUsername = findViewById(R.id.etRegUsername);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        ivProfile = findViewById(R.id.ivProfileReg);

        // 1. Logic to Pick Image
        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 100);
        });

        // 2. Logic to Register
        btnRegister.setOnClickListener(v -> registerUser());

        // 3. Go to Login
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    // Handle Image Pick Result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            ivProfile.setImageURI(imageUri);
        }
    }

    private void registerUser() {
        String email = etEmail.getText().toString();
        String pass = etPassword.getText().toString();
        String username = etUsername.getText().toString();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(pass) || TextUtils.isEmpty(username)) {
            Toast.makeText(this, "All fields are required!", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Creating Account...", Toast.LENGTH_SHORT).show();

        // 1. Create User in Auth
        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();

                        // Check if user uploaded a picture
                        if (imageUri != null) {
                            uploadProfileImage(user, username, email);
                        } else {
                            // No image? Set default
                            updateUserProfileAndSaveToFirestore(user, username, email, null);
                        }
                    } else {
                        Toast.makeText(RegisterActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void uploadProfileImage(FirebaseUser user, String username, String email) {
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("profile_images/" + user.getUid() + ".jpg");

        ref.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                // Now we have the URL, update profile AND save to Firestore
                updateUserProfileAndSaveToFirestore(user, username, email, uri);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUserProfileAndSaveToFirestore(FirebaseUser user, String username, String email, Uri photoUrl) {
        // 1. Update the Auth Profile (For login handling)
        UserProfileChangeRequest.Builder request = new UserProfileChangeRequest.Builder()
                .setDisplayName(username);

        String photoString = null;
        if (photoUrl != null) {
            request.setPhotoUri(photoUrl);
            photoString = photoUrl.toString();
        }

        String finalPhotoString = photoString;
        user.updateProfile(request.build()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                // 2. NOW SAVE TO FIRESTORE (Matches your Console)
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("name", username);
                userMap.put("email", email);
                userMap.put("role", "user"); // Helpful for your Admin later

                if (finalPhotoString != null) {
                    userMap.put("imageURL", finalPhotoString);
                }

                // Saves to "users" collection -> document "UID"
                db.collection("users").document(user.getUid())
                        .set(userMap)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(RegisterActivity.this, "Registration Success!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(RegisterActivity.this, "Failed to save details: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
            }
        });
    }
}