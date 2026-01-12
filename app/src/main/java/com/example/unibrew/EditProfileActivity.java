package com.example.unibrew;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends AppCompatActivity {

    private ImageView ivProfile;
    private EditText etUsername;
    private Button btnSave, btnLogout;
    private Uri imageUri;
    private FirebaseAuth mAuth;
    private FirebaseUser user;
    private FirebaseFirestore db;

    // --- NEW: Camera & Gallery Constants ---
    private static final int REQUEST_IMAGE_PICK = 200;
    private static final int REQUEST_IMAGE_CAPTURE = 201;
    private static final int REQUEST_PERMISSION_CAMERA = 202;
    private String currentPhotoPath; // Stores where the camera saved the photo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        user = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

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

        // 2. CRITICAL CHANGE: This opens the Popup Dialog instead of just Gallery
        ivProfile.setOnClickListener(v -> showImageSourceOptions());

        // 3. Save Changes
        btnSave.setOnClickListener(v -> updateProfile());

        // 4. Logout Logic
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(EditProfileActivity.this, LoginActivity.class));
            finishAffinity();
        });
    }

    // --- NEW: The Dialog Box Logic ---
    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Change Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Option 0: Take Photo
                checkCameraPermissionAndOpen();
            } else {
                // Option 1: Gallery
                openGallery();
            }
        });
        builder.show();
    }

    // --- NEW: Permission Check ---
    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
        } else {
            dispatchTakePictureIntent();
        }
    }

    // --- NEW: Open Camera ---
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                // Securely get the URI for the file
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.unibrew.fileprovider", // Make sure this matches AndroidManifest.xml
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // --- NEW: Create Temp File ---
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // --- NEW: Open Gallery ---
    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    // --- UPDATED: Handle Results for Both Camera & Gallery ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                // User picked from Gallery
                imageUri = data.getData();
                ivProfile.setImageURI(imageUri);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // User took a photo with Camera
                File f = new File(currentPhotoPath);
                imageUri = Uri.fromFile(f);
                ivProfile.setImageURI(imageUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- SAVING LOGIC (Same as before) ---
    private void updateProfile() {
        String newName = etUsername.getText().toString();

        if (newName.isEmpty()) {
            Toast.makeText(this, "Username cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "Updating...", Toast.LENGTH_SHORT).show();

        if (imageUri != null) {
            uploadImageAndUpdate(newName);
        } else {
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
        UserProfileChangeRequest.Builder request = new UserProfileChangeRequest.Builder()
                .setDisplayName(name);

        if (photoUri != null) {
            request.setPhotoUri(photoUri);
        }

        user.updateProfile(request.build()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Map<String, Object> updates = new HashMap<>();
                updates.put("name", name);

                if (photoUri != null) {
                    updates.put("imageURL", photoUri.toString());
                }

                db.collection("users").document(user.getUid())
                        .update(updates)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(EditProfileActivity.this, "Profile Updated!", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            db.collection("users").document(user.getUid()).set(updates);
                            finish();
                        });
            }
        });
    }
}