package com.example.unibrew;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat; // IMPORTANT for permissions

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
    private FusedLocationProviderClient fusedLocationClient; // The tool to find GPS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_view);

        // Initialize Database
        db = FirebaseFirestore.getInstance();

        // Initialize Location Tool
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // 1. Setup the Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_full);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        // 2. Button Logic
        findViewById(R.id.btnSwitchToList).setOnClickListener(v -> finish());
        findViewById(R.id.btnBackToList).setOnClickListener(v -> finish());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        // --- NEW: Check Permissions & Enable Blue Dot ---
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            // 1. Turn on the Blue Dot
            mMap.setMyLocationEnabled(true);

            // 2. Get Current Location & Move Camera
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    LatLng currentLoc = new LatLng(location.getLatitude(), location.getLongitude());
                    // Zoom level 15 is good for streets
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 15f));
                }
            });
        }

        // --- EXISTING: Load Cafe Markers ---
        db.collection("cafes").get().addOnSuccessListener(queryDocumentSnapshots -> {
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                // Ensure your Cafe class handles nulls gracefully
                Cafe cafe = doc.toObject(Cafe.class);
                if (cafe != null) {
                    LatLng loc = new LatLng(cafe.getLatitude(), cafe.getLongitude());
                    // We save the WHOLE cafe object in the tag now, so we can pass ID later if needed
                    mMap.addMarker(new MarkerOptions().position(loc).title(cafe.getName())).setTag(cafe);
                }
            }
        });

        // Click Marker to go to Detail Page
        mMap.setOnMarkerClickListener(marker -> {
            Cafe cafe = (Cafe) marker.getTag();
            if (cafe != null) {
                Intent intent = new Intent(MapActivity.this, CafeDetailActivity.class);
                // Pass all the data your Detail Page needs
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
}