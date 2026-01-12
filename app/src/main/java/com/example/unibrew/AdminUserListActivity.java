package com.example.unibrew;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class AdminUserListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AdminUserAdapter adapter;
    private List<User> userList;
    private FirebaseFirestore db;
    private ImageView btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_user_list);

        // 1. Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // 2. Link Variables
        recyclerView = findViewById(R.id.rvAdminUserList);
        btnBack = findViewById(R.id.btnBack);

        if (recyclerView == null) {
            Toast.makeText(this, "Error: RecyclerView not found", Toast.LENGTH_LONG).show();
            return;
        }

        // 3. Setup RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        userList = new ArrayList<>();
        adapter = new AdminUserAdapter(this, userList);
        recyclerView.setAdapter(adapter);

        // 4. Load Data
        loadUsersFromFirestore();

        // 5. Back Button
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }
    }

    private void loadUsersFromFirestore() {
        db.collection("users").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        userList.clear();
                        List<DocumentSnapshot> list = queryDocumentSnapshots.getDocuments();

                        for (DocumentSnapshot d : list) {
                            User user = d.toObject(User.class);

                            if (user != null) {
                                // --- SAFETY FILTER ---
                                // If this is the Admin account, SKIP it!
                                if ("admin@gmail.com".equals(user.getEmail())) {
                                    continue;
                                }
                                // ---------------------

                                user.setUserId(d.getId());
                                userList.add(user);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "No users found.", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading users: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}