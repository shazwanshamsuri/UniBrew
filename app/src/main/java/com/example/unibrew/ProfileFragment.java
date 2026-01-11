package com.example.unibrew;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

import java.util.HashMap;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private ImageView ivProfile;
    private EditText etUsername, etEmail, etNewPass, etCurrentPass;
    private TextView tvTitle;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private Uri imageUri;

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

        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 101);
        });

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

                String pUrl = doc.getString("profileImageUrl");
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

        // 1. If only changing Name/Photo, skip authentication
        if (!isEmailChanged && !isPasswordChanged) {
            if (imageUri != null) {
                uploadImageAndSaveData();
            } else {
                saveProfileData(null);
            }
            return;
        }

        // 2. If changing Email/Password, require Current Password
        if (currentPassword.isEmpty()) {
            etCurrentPass.setError("Required to change Email or Password");
            return;
        }

        // 3. Re-authenticate
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

    // --- REWRITTEN METHOD: CHAINS THE UPDATES SAFELY ---
    private void saveProfileData(String photoUrl) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String newPassword = etNewPass.getText().toString().trim();
        String newEmail = etEmail.getText().toString().trim();
        boolean isEmailChanged = !newEmail.isEmpty() && !newEmail.equals(user.getEmail());

        // Step A: Define the Final Database Save action
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
            if (photoUrl != null) updates.put("profileImageUrl", photoUrl);

            db.collection("users").document(user.getUid())
                    .set(updates, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        updatePastReviews(oldName, newName);
                        tvTitle.setText(newName);

                        // Clear Sensitive Fields
                        etCurrentPass.setText("");
                        etNewPass.setText("");
                        etEmail.setText("");
                        if (user.getEmail() != null) etEmail.setHint(user.getEmail());

                        Toast.makeText(getActivity(), "Profile Updated Successfully!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "DB Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        };

        // Step B: Execute the Updates in a Chain (Password -> Email -> DB)

        // 1. Update Password First (if needed)
        if (!newPassword.isEmpty()) {
            user.updatePassword(newPassword).addOnSuccessListener(aVoid -> {
                // Password done. Now check Email.
                if (isEmailChanged) {
                    user.updateEmail(newEmail).addOnSuccessListener(v -> saveToDatabase.run())
                            .addOnFailureListener(e -> Toast.makeText(getActivity(), "Email failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                } else {
                    saveToDatabase.run();
                }
            }).addOnFailureListener(e -> Toast.makeText(getActivity(), "Password failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
        // 2. Or Update Email Only (if Password wasn't changed)
        else if (isEmailChanged) {
            user.updateEmail(newEmail).addOnSuccessListener(v -> saveToDatabase.run())
                    .addOnFailureListener(e -> Toast.makeText(getActivity(), "Email failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        }
        // 3. Or Just Save Database (if neither changed)
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 101 && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            Glide.with(this).load(imageUri).circleCrop().into(ivProfile);
        }
    }
}