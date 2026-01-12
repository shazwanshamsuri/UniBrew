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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AddCafeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private EditText etName, etDesc;
    // --- LINK FEATURE VARIABLES ---
    private EditText etMapLink;
    private Button btnApplyLink;
    // -----------------------------

    private ImageView ivPreview;
    private Button btnSave;
    private FirebaseFirestore db;
    private GoogleMap mMap;
    private Uri imageUri;
    private FusedLocationProviderClient fusedLocationClient;

    // --- NEW CAMERA CONSTANTS ---
    private static final int REQUEST_IMAGE_PICK = 200;
    private static final int REQUEST_IMAGE_CAPTURE = 201;
    private static final int REQUEST_PERMISSION_CAMERA = 202;
    private static final int REQUEST_PERMISSION_LOCATION = 1001;
    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_cafe);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        db = FirebaseFirestore.getInstance();

        etName = findViewById(R.id.etCafeName);
        etDesc = findViewById(R.id.etCafeDesc);

        // --- LINK FEATURE INIT ---
        etMapLink = findViewById(R.id.etGoogleMapLink);
        btnApplyLink = findViewById(R.id.btnApplyLink);
        btnApplyLink.setOnClickListener(v -> parseGoogleMapsLink());

        ivPreview = findViewById(R.id.ivCafePreview);
        btnSave = findViewById(R.id.btnSaveCafe);

        // 1. UPDATED: Show Option Dialog (Camera or Gallery)
        ivPreview.setOnClickListener(v -> showImageSourceOptions());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_picker);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnSave.setOnClickListener(v -> saveCafeProcess());
    }

    // --- NEW: Pop-up Dialog ---
    private void showImageSourceOptions() {
        String[] options = {"Take Photo", "Choose from Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Cafe Photo");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                checkCameraPermissionAndOpen();
            } else {
                openGallery();
            }
        });
        builder.show();
    }

    // --- NEW: Check Permission ---
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
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.unibrew.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    // --- NEW: Create File Helper ---
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void openGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    // --- UPDATED: Handle Results (Gallery, Camera, Permission) ---
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_PICK && data != null) {
                imageUri = data.getData();
                ivPreview.setImageURI(imageUri);
            } else if (requestCode == REQUEST_IMAGE_CAPTURE) {
                File f = new File(currentPhotoPath);
                imageUri = Uri.fromFile(f);
                ivPreview.setImageURI(imageUri);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Handle Location Permission
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onMapReady(mMap);
            } else {
                Toast.makeText(this, "Location denied.", Toast.LENGTH_LONG).show();
            }
        }

        // Handle Camera Permission
        if (requestCode == REQUEST_PERMISSION_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission needed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- LINK LOGIC (Unchanged) ---
    private void parseGoogleMapsLink() {
        String url = etMapLink.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Paste a Google Maps link first", Toast.LENGTH_SHORT).show();
            return;
        }
        Pattern pattern = Pattern.compile("@(-?\\d+\\.\\d+),(-?\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(url);

        if (matcher.find()) {
            try {
                double lat = Double.parseDouble(matcher.group(1));
                double lng = Double.parseDouble(matcher.group(2));
                LatLng target = new LatLng(lat, lng);
                if (mMap != null) {
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 17f));
                    Toast.makeText(this, "Location Found!", Toast.LENGTH_SHORT).show();
                }
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Error parsing coordinates", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Could not find location. Use full browser link.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f));
                } else {
                    LatLng uitm = new LatLng(3.0698, 101.5037);
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(uitm, 15f));
                }
            });
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSION_LOCATION);
        }
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