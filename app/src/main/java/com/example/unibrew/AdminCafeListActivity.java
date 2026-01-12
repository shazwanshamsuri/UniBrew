package com.example.unibrew;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminCafeListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminCafeAdapter adapter;
    private List<Cafe> cafeList;
    private FirebaseFirestore db;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_cafe_list);

        // 1. Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // 2. Link Variables to XML
        recyclerView = findViewById(R.id.rvAdminCafeList);
        btnBack = findViewById(R.id.btnBack);

        // 3. Setup RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        cafeList = new ArrayList<>();
        adapter = new AdminCafeAdapter(this, cafeList);
        recyclerView.setAdapter(adapter);

        // 4. Load Data from Firestore
        loadCafesFromFirestore();

        // 5. Back Button Logic
        btnBack.setOnClickListener(v -> finish());
    }

    private void loadCafesFromFirestore() {
        db.collection("cafes").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        cafeList.clear(); // Clear old list to avoid duplicates
                        List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();

                        for (DocumentSnapshot d : list) {
                            Cafe cafe = d.toObject(Cafe.class);

                            // --- FORCE THE ID HERE ---
                            if (cafe != null) {
                                cafe.setId(d.getId()); // Manually grab the ID from the document
                                cafeList.add(cafe);
                            }
                        }
                        // Notify adapter to refresh the screen
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "No cafes found in database.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}