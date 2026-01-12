package com.example.unibrew;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

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

public class RegisterActivity extends AppCompatActivity {

    private EditText etEmail, etPassword, etUsername;
    private Button btnRegister;
    private TextView tvLoginLink;
    private ImageView ivProfile;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Uri imageUri;

    // --- NEW CONSTANTS FOR CAMERA ---
    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_PERMISSION_CAMERA = 102;
    private String currentPhotoPath; // To remember where the camera saved the file

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        etUsername = findViewById(R.id.etRegUsername);
        btnRegister = findViewById(R.id.btnRegister);
        tvLoginLink = findViewById(R.id.tvLoginLink);
        ivProfile = findViewById(R.id.ivProfileReg);

        // 1. UPDATED: Show Option Dialog instead of just Gallery
        ivProfile.setOnClickListener(v -> showImageSourceOptions());

        btnRegister.setOnClickListener(v -> registerUser());

        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });
    }

    // --- NEW: Pop-up Dialog to choose Camera or Gallery ---
    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpen();
            } else {
                openGallery();
            }
        });
        builder.show();
    }

    // --- NEW: Camera Permission Check ---
    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
        } else {
            dispatchTakePictureIntent();
        }
    }

    // --- NEW: Logic to Open Camera ---
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                // Get the safe URI from FileProvider
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.unibrew.fileprovider", // MUST MATCH AndroidManifest
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // --- NEW: Create a File to save the photo ---
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath(); // Save path to use later
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    // --- UPDATED: Handle Both Results ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                // Gallery Logic
                imageUri = data.getData();
                ivProfile.setImageURI(imageUri);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Camera Logic
                File f = new File(currentPhotoPath);
                imageUri = Uri.fromFile(f);
                ivProfile.setImageURI(imageUri);
            }
        }
    }

    // --- NEW: Handle Permission Result ---
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission needed to take photos", Toast.LENGTH_SHORT).show();
            }
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

        mAuth.createUserWithEmailAndPassword(email, pass)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (imageUri != null) {
                            uploadProfileImage(user, username, email);
                        } else {
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
                updateUserProfileAndSaveToFirestore(user, username, email, uri);
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
        });
    }

    private void updateUserProfileAndSaveToFirestore(FirebaseUser user, String username, String email, Uri photoUrl) {
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
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("name", username);
                userMap.put("email", email);
                userMap.put("role", "user");

                if (finalPhotoString != null) {
                    userMap.put("imageURL", finalPhotoString);
                }

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