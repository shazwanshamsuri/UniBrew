package com.example.unibrew;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_full);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        findViewById(R.id.btnSwitchToList).setOnClickListener(v -> finish());
        findViewById(R.id.btnBackToList).setOnClickListener(v -> finish());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // 1. Check permissions immediately when map is ready
        checkPermissionAndEnableLocation();

        // 2. Load Cafe Markers (Standard Logic)
        loadCafeMarkers();

        // 3. Click Listener
        mMap.setOnMarkerClickListener(marker -> {
            Cafe cafe = (Cafe) marker.getTag();
            if (cafe != null) {
                Intent intent = new Intent(MapActivity.this, CafeDetailActivity.class);
                intent.putExtra("cafeId", cafe.getId());
                intent.putExtra("cafeName", cafe.getName());
                intent.putExtra("cafeDesc", cafe.getDescription());
                intent.putExtra("cafeImageUrl", cafe.getImageUrl());
                intent.putExtra("cafeLat", cafe.getLatitude());
                intent.putExtra("cafeLng", cafe.getLongitude());
                startActivity(intent);
            }
            return false;
        });
    }

    // --- NEW: LOGIC TO ASK AND ZOOM ---

    private void checkPermissionAndEnableLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            // Permission is already granted -> Turn on Blue Dot
            enableUserLocation();
        } else {
            // Permission missing -> Ask user for it!
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mMap.setMyLocationEnabled(true);

        // GET LOCATION AND ZOOM
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                LatLng userLatLang = new LatLng(location.getLatitude(), location.getLongitude());
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLang, 15f));
            } else {
                Toast.makeText(this, "Waiting for GPS...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // This handles the result when user clicks "Allow" or "Deny"
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // User clicked Allow -> Now we can zoom!
                enableUserLocation();
            } else {
                Toast.makeText(this, "Permission denied. Cannot show location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadCafeMarkers() {
        db.collection("cafes").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Cafe cafe = doc.toObject(Cafe.class);
                if (cafe != null) {
                    // Fix: Ensure ID is set so clicking works
                    cafe.setId(doc.getId());

                    LatLng loc = new LatLng(cafe.getLatitude(), cafe.getLongitude());
                    mMap.addMarker(new MarkerOptions().position(loc).title(cafe.getName())).setTag(cafe);
                }
            }
        });
    }
}