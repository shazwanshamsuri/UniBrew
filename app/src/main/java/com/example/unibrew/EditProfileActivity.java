package com.example.unibrew;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore; // UPDATED: Using Firestore
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private EditText etUsername;
    private Button btnSave, btnLogout;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db; // UPDATED: Firestore Instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance(); // UPDATED: Initialize Firestore

        ivProfile = findViewById(R.id.ivEditProfile);
        etUsername = findViewById(R.id.etEditUsername);
        btnSave = findViewById(R.id.btnSaveChanges);
        btnLogout = findViewById(R.id.btnLogout);

        // 1. Load Current Data
        if (user != null) {
            etUsername.setText(user.getDisplayName());
            if (user.getPhotoUrl() != null) {
                Glide.with(this).load(user.getPhotoUrl()).into(ivProfile);
            }
        }

        // 2. Change Photo
        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 200);
        });

        // 3. Save Changes
        btnSave.setOnClickListener(v -> updateProfile());

        // 4. Logout Logic
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(EditProfileActivity.this, LoginActivity.class));
            finishAffinity();
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            ivProfile.setImageURI(imageUri);
        }
    }

    private void updateProfile() {
        String newName = etUsername.getText().toString();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show();

        if (imageUri != null) {
            // Case A: User picked a new photo -> Upload first
            uploadImageAndUpdate(newName);
        } else {
            // Case B: Only changed name
            updateFirebaseProfile(newName, user.getPhotoUrl());
        }
    }

    private void uploadImageAndUpdate(String newName) {
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("profile_images/" + user.getUid() + ".jpg");

        ref.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                updateFirebaseProfile(newName, uri);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFirebaseProfile(String name, Uri photoUri) {
        // 1. Update Authentication (Login) Profile
        UserProfileChangeRequest.Builder request = new UserProfileChangeRequest.Builder()
                .setDisplayName(name);

        if (photoUri != null) {
            request.setPhotoUri(photoUri);
        }

        user.updateProfile(request.build()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {

                // 2. SYNC WITH FIRESTORE
                Map<String, Object> updates = new HashMap<>();

                // IMPORTANT: We use "name" here because that is what we used in RegisterActivity
                updates.put("name", name);

                if (photoUri != null) {
                    updates.put("imageURL", photoUri.toString());
                }

                // Update the document in "users" collection with the current UID
                db.collection("users").document(user.getUid())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(EditProfileActivity.this, "Profile Updated & Synced!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            // If user document doesn't exist for some reason, create it
                            db.collection("users").document(user.getUid()).set(updates);
                            finish();
                        });
            }
        });
    }
}