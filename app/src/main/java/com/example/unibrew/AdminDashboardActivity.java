package com.example.unibrew;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // --- 1. Link Buttons to XML ---
        Button btnManageCafes = findViewById(R.id.btnManageCafes);
        Button btnManageUsers = findViewById(R.id.btnManageUsers);
        Button btnManageReviews = findViewById(R.id.btnManageReviews);
        Button btnLogout = findViewById(R.id.btnAdminLogout);

        // --- 2. Set Click Listeners ---

        // A. Manage Cafes
        btnManageCafes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminDashboardActivity.this, AdminCafeListActivity.class);
                startActivity(intent);
            }
        });

        // B. Manage Users
        btnManageUsers.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminDashboardActivity.this, AdminUserListActivity.class);
                startActivity(intent);
            }
        });

        // C. Manage Reviews
        btnManageReviews.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(AdminDashboardActivity.this, AdminReviewListActivity.class);
                startActivity(intent);
            }
        });

        // D. Log Out
        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                // Go back to Login Page and clear history so they can't click "Back"
                Intent intent = new Intent(AdminDashboardActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}