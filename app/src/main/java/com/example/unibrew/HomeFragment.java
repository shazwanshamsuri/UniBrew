package com.example.unibrew;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private RecyclerView rv;
    private CafeAdapter adapter;
    private List<Cafe> list;
    private FirebaseFirestore db;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isFirstLoad = true;

    // Request Code for permission
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        NotificationHelper.createNotificationChannel(requireContext());

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        rv = view.findViewById(R.id.rvCafeList);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        list = new ArrayList<>();
        adapter = new CafeAdapter(list);
        rv.setAdapter(adapter);

        view.findViewById(R.id.fabSwitchToMap).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MapActivity.class));
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Check permission immediately when screen loads
        checkPermissionAndLoadData();
    }

    private void checkPermissionAndLoadData() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // IF PERMISSION MISSING: ASK FOR IT
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            // IF GRANTED: LOAD DATA
            loadData();
        }
    }

    // Handle the user's choice (Allow vs Deny)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadData(); // They said yes! Load data.
            } else {
                Toast.makeText(getContext(), "Location needed for distance", Toast.LENGTH_SHORT).show();
                // We still load data, but distance will remain "... km"
                loadCafesFromFirestore();
            }
        }
    }

    private void loadData() {
        // 1. Load the list from Firestore
        loadCafesFromFirestore();

        // 2. Try to get GPS Location
        checkNearbyCafes();
    }

    private void loadCafesFromFirestore() {
        db.collection("cafes").get().addOnSuccessListener(queryDocumentSnapshots -> {
            list.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Cafe cafe = doc.toObject(Cafe.class);
                cafe.setId(doc.getId());
                list.add(cafe);
            }
            adapter.notifyDataSetChanged();

            if (isFirstLoad) {
                playWelcomeSound();
                isFirstLoad = false;
            }
        });
    }

    private void playWelcomeSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(getContext(), R.raw.welcome);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.reset();
                    mp.release();
                });
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkNearbyCafes() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                // SUCCESS: We found the location!
                adapter.updateUserLocation(location);

                for (Cafe cafe : list) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            location.getLatitude(), location.getLongitude(),
                            cafe.getLatitude(), cafe.getLongitude(),
                            results
                    );
                    if (results[0] < 250) {
                        NotificationHelper.sendProximityNotification(requireContext(), cafe.getName(), cafe.getId());
                        break;
                    }
                }
            } else {
                // FAILURE: GPS is on, but hasn't found a signal yet.
                // This is common inside buildings.
            }
        });
    }
}