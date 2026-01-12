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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // This sets the channel to IMPORTANCE_LOW to remove the "noise"
        NotificationHelper.createNotificationChannel(requireContext());

        db = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        rv = view.findViewById(R.id.rvCafeList);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        list = new ArrayList<>();
        adapter = new CafeAdapter(list);
        rv.setAdapter(adapter);

        loadData();

        view.findViewById(R.id.fabSwitchToMap).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), MapActivity.class));
        });

        return view;
    }

    private void loadData() {
        db.collection("cafes").get().addOnSuccessListener(queryDocumentSnapshots -> {
            list.clear();
            for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                // 1. Convert Firestore data to Cafe object
                Cafe cafe = doc.toObject(Cafe.class);

                // --- FIX STARTS HERE ---
                // 2. Manually grab the Document ID and save it into the object
                // This ensures "cafeId" is not null when you click the item!
                cafe.setId(doc.getId());
                // --- FIX ENDS HERE ---

                list.add(cafe);
            }
            adapter.notifyDataSetChanged();

            // 1. PLAY YOUR CUSTOM ENTRANCE SOUND
            playWelcomeSound();

            // 2. CHECK FOR NEARBY CAFES
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
                for (Cafe cafe : list) {
                    float[] results = new float[1];
                    Location.distanceBetween(
                            location.getLatitude(), location.getLongitude(),
                            cafe.getLatitude(), cafe.getLongitude(),
                            results
                    );

                    float distanceInMeters = results[0];

                    if (distanceInMeters < 1000) {
                        NotificationHelper.sendProximityNotification(requireContext(), cafe);
                        break;
                    }
                }
            }
        });
    }
}