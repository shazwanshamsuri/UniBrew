package com.example.unibrew;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private ImageView ivProfile;
    private EditText etUsername, etEmail, etNewPass, etCurrentPass;
    private TextView tvTitle;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Uri imageUri;

    // --- NEW CONSTANTS FOR CAMERA ---
    private static final int REQUEST_IMAGE_PICK = 101;
    private static final int REQUEST_IMAGE_CAPTURE = 102;
    private static final int REQUEST_PERMISSION_CAMERA = 103;
    private String currentPhotoPath;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ivProfile = view.findViewById(R.id.ivEditProfile);
        etUsername = view.findViewById(R.id.etEditUsername);
        etEmail = view.findViewById(R.id.etEditEmail);
        etNewPass = view.findViewById(R.id.etEditPassword);
        etCurrentPass = view.findViewById(R.id.etCurrentPassword);
        tvTitle = view.findViewById(R.id.tvProfileTitle);

        loadUserData();

        // --- UPDATED: Show Popup Dialog ---
        ivProfile.setOnClickListener(v -> showImageSourceOptions());

        view.findViewById(R.id.btnSaveChanges).setOnClickListener(v -> handleSaveChanges());

        view.findViewById(R.id.llSubmitCafe).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), AddCafeActivity.class)));

        view.findViewById(R.id.llLogout).setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
        });

        return view;
    }

    // --- NEW: Dialog for Camera vs Gallery ---
    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpen();
            } else {
                openGallery();
            }
        });
        builder.show();
    }

    private void checkCameraPermissionAndOpen() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_PERMISSION_CAMERA);
        } else {
            dispatchTakePictureIntent();
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (getActivity() != null && takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getContext(), "Error creating file", Toast.LENGTH_SHORT).show();
            }

            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(requireContext(),
                        "com.example.unibrew.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    // --- UPDATED: Handle Results ---
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                // Gallery
                imageUri = data.getData();
                Glide.with(this).load(imageUri).circleCrop().into(ivProfile);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Camera
                File f = new File(currentPhotoPath);
                imageUri = Uri.fromFile(f);
                Glide.with(this).load(imageUri).circleCrop().into(ivProfile);
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
                Toast.makeText(getContext(), "Camera permission needed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- EXISTING SAVE LOGIC (UNCHANGED) ---
    private void loadUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            etEmail.setHint(user.getEmail());
        }

        String uid = mAuth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                String name = doc.getString("name");
                tvTitle.setText(name != null ? name : "User");
                etUsername.setText(name);

                String pUrl = doc.getString("profileImageUrl"); // Matches your saving logic
                // If this is null, try checking "imageURL" just in case old data exists
                if (pUrl == null) pUrl = doc.getString("imageURL");

                if (pUrl != null && isAdded()) {
                    Glide.with(this).load(pUrl).circleCrop().into(ivProfile);
                }
            }
        });
    }

    private void handleSaveChanges() {
        String currentPassword = etCurrentPass.getText().toString();
        String newEmail = etEmail.getText().toString().trim();
        FirebaseUser user = mAuth.getCurrentUser();

        if (user == null) return;

        boolean isEmailChanged = !newEmail.isEmpty() && !newEmail.equals(user.getEmail());
        boolean isPasswordChanged = !etNewPass.getText().toString().isEmpty();

        if (!isEmailChanged && !isPasswordChanged) {
            if (imageUri != null) {
                uploadImageAndSaveData();
            } else {
                saveProfileData(null);
            }
            return;
        }

        if (currentPassword.isEmpty()) {
            etCurrentPass.setError("Required to change Email or Password");
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
        user.reauthenticate(credential).addOnSuccessListener(aVoid -> {
            if (imageUri != null) {
                uploadImageAndSaveData();
            } else {
                saveProfileData(null);
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(getActivity(), "Incorrect Current Password", Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadImageAndSaveData() {
        String uid = mAuth.getUid();
        if (uid == null) return;

        Toast.makeText(getActivity(), "Uploading image...", Toast.LENGTH_SHORT).show();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("profile_images/" + uid + ".jpg");

        ref.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            ref.getDownloadUrl().addOnSuccessListener(uri -> {
                saveProfileData(uri.toString());
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(getActivity(), "Image Upload Failed", Toast.LENGTH_SHORT).show();
        });
    }

    private void saveProfileData(String photoUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String newPassword = etNewPass.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        boolean isEmailChanged = !newEmail.isEmpty() && !newEmail.equals(user.getEmail());

        Runnable saveToDatabase = () -> {
            String newName = etUsername.getText().toString().trim();
            String oldName = tvTitle.getText().toString();

            if (!newName.isEmpty()) {
                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(newName).build();
                user.updateProfile(profileUpdates);
            }

            Map<String, Object> updates = new HashMap<>();
            if (!newName.isEmpty()) updates.put("name", newName);
            // Save as both keys to ensure compatibility with all pages
            if (photoUrl != null) {
                updates.put("profileImageUrl", photoUrl);
                updates.put("imageURL", photoUrl);
            }

            db.collection("users").document(user.getUid())
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        updatePastReviews(oldName, newName);
                        tvTitle.setText(newName);
                        etCurrentPass.setText("");
                        etNewPass.setText("");
                        etEmail.setText("");
                        if (user.getEmail() != null) etEmail.setHint(user.getEmail());
                        Toast.makeText(getActivity(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        };

        if (!newPassword.isEmpty()) {
            user.updatePassword(newPassword).addOnSuccessListener(aVoid -> {
                if (isEmailChanged) {
                    user.updateEmail(newEmail).addOnSuccessListener(v -> saveToDatabase.run())
                            .addOnFailureListener(e -> Toast.makeText(getActivity(), "Email failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    saveToDatabase.run();
                }
            }).addOnFailureListener(e -> Toast.makeText(getActivity(), "Password failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
        else if (isEmailChanged) {
            user.updateEmail(newEmail).addOnSuccessListener(v -> saveToDatabase.run())
                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "Email failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
        else {
            saveToDatabase.run();
        }
    }

    private void updatePastReviews(String oldName, String newName) {
        if (oldName == null || oldName.isEmpty() || oldName.equals(newName)) return;

        db.collectionGroup("reviews").whereEqualTo("user", oldName).get()
                .addOnSuccessListener(querySnapshot -> {
                    for (com.google.firebase.firestore.DocumentSnapshot document : querySnapshot) {
                        document.getReference().update("user", newName);
                    }
                });
    }
}