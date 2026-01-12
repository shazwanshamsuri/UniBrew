package com.example.unibrew;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.AlertDialog; // Import for the Popup

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.UserViewHolder> {

    private Context context;
    private List<User> userList;
    private FirebaseFirestore db;

    public AdminUserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);

        // 1. Set Text Data
        holder.tvName.setText(user.getName());
        holder.tvEmail.setText(user.getEmail());

        // Show role (Admin or User)
        if (user.getRole() != null) {
            holder.tvRole.setText("Role: " + user.getRole());
        } else {
            holder.tvRole.setText("Role: User");
        }

        // 2. Load Profile Image
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(context).load(user.getProfileImageUrl()).into(holder.ivProfile);
        } else {
            holder.ivProfile.setImageResource(R.drawable.ic_cute_camera); // Fallback
        }

        // 3. DELETE BUTTON WITH WARNING
        holder.btnDelete.setOnClickListener(v -> {
            showDeleteWarning(user, position);
        });
    }

    // --- THE WARNING POPUP ---
    private void showDeleteWarning(User user, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete User?")
                .setMessage("Are you sure you want to remove " + user.getName() + "?\n(They will lose access immediately)")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    deleteUser(user, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --- THE ACTUAL DELETE LOGIC ---
    private void deleteUser(User user, int position) {
        if (user.getUserId() == null) {
            Toast.makeText(context, "Error: User ID missing", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("users").document(user.getUserId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    userList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, userList.size());
                    Toast.makeText(context, "User Removed Successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // --- ViewHolder Class ---
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole;
        ImageView ivProfile, btnDelete;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvUserName);
            tvEmail = itemView.findViewById(R.id.tvUserEmail);
            tvRole = itemView.findViewById(R.id.tvUserRole);
            ivProfile = itemView.findViewById(R.id.ivUserProfile);
            btnDelete = itemView.findViewById(R.id.btnDeleteUser);
        }
    }
}