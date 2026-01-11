package com.example.unibrew;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import android.Manifest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AddCafeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private EditText etName, etDesc;
    private ImageView ivPreview;
    private Button btnSave;
    private FirebaseFirestore db;
    private GoogleMap mMap;
    private Uri imageUri;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_cafe);

        // Initialize the location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etCafeName);
        etDesc = findViewById(R.id.etCafeDesc);
        ivPreview = findViewById(R.id.ivCafePreview);
        btnSave = findViewById(R.id.btnSaveCafe);

        // Pick Image Logic
        ivPreview.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(intent, 100);
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_picker);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnSave.setOnClickListener(v -> saveCafeProcess());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            ivPreview.setImageURI(imageUri);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            // 1. Enable the blue dot and "Find Me" button
            mMap.setMyLocationEnabled(true);

            // 2. Fetch current location and move camera automatically
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                } else {
                    // Fallback if location is null (e.g., GPS turned off)
                    LatLng uitm = new LatLng(3.0698, 101.5037);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uitm, 15f));
                }
            });
        } else {
            // Request permission if not already granted
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1001) { // This matches the code used in your request
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission GRANTED: Trigger the location fetch again
                onMapReady(mMap);
            } else {
                // Permission DENIED: Show a friendly message or explanation
                Toast.makeText(this, "Location denied. Map will stay at default location.", Toast.LENGTH_LONG).show();

                // If the permission is critical, you can show a dialog or disable features
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    // User checked "Don't ask again"
                    showSettingsDialog();
                }
            }
        }
    }

    // Optional: Guide them to settings if they permanently denied it
    private void showSettingsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Permission Needed")
                .setMessage("Location permission is required for this feature. Please enable it in app settings.")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    private void saveCafeProcess() {
        String name = etName.getText().toString();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name required");
            return;
        }

        Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
        btnSave.setEnabled(false);

        if (imageUri != null) {
            uploadImageAndSave(name);
        } else {
            saveCafeToFirestore(name, null);
        }
    }

    private void uploadImageAndSave(String name) {
        String filename = UUID.randomUUID().toString();
        StorageReference ref = FirebaseStorage.getInstance().getReference().child("cafe_images/" + filename);

        ref.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    ref.getDownloadUrl().addOnSuccessListener(uri -> {
                        saveCafeToFirestore(name, uri.toString());
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show();
                    btnSave.setEnabled(true);
                });
    }

    private void saveCafeToFirestore(String name, String imageUrl) {
        String desc = etDesc.getText().toString();
        LatLng center = mMap.getCameraPosition().target;

        Map<String, Object> cafe = new HashMap<>();
        cafe.put("name", name);
        cafe.put("description", desc);
        cafe.put("latitude", center.latitude);
        cafe.put("longitude", center.longitude);
        cafe.put("addedBy", FirebaseAuth.getInstance().getCurrentUser().getEmail());

        if (imageUrl != null) {
            cafe.put("imageUrl", imageUrl);
        }

        db.collection("cafes").add(cafe)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Cafe Saved!", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }
}