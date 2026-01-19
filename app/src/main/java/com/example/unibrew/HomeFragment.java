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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
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
    private boolean isFirstLoad = true; // Flag to control sound

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

    // --- KEY FIX: USE onResume() ---
    // This runs EVERY TIME the screen becomes visible
    @Override
    public void onResume() {
        super.onResume();
        loadData();
    }
    // --------------------------------

    private void loadData() {
        db.collection("cafes").get().addOnSuccessListener(queryDocumentSnapshots -> {
            list.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                Cafe cafe = doc.toObject(Cafe.class);
                cafe.setId(doc.getId()); // Essential for clicks to work
                list.add(cafe);
            }
            adapter.notifyDataSetChanged();

            // Play sound only the first time app opens, not every time you come back
            if (isFirstLoad) {
                playWelcomeSound();
                isFirstLoad = false;
            }

            checkNearbyCafes();
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

                // Updates the "0.2 km" text
                adapter.updateUserLocation(location);

                // Proximity Notification Logic
                for (Cafe cafe : list) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            location.getLatitude(), location.getLongitude(),
                            cafe.getLatitude(), cafe.getLongitude(),
                            results
                    );

                    float distanceInMeters = results[0];

                    if (distanceInMeters < 250) {
                        NotificationHelper.sendProximityNotification(requireContext(), cafe.getName(), cafe.getId());
                        break;
                    }
                }
            }
        });
    }
}