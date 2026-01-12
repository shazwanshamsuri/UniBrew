package com.example.unibrew;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class AdminCafeAdapter extends RecyclerView.Adapter<AdminCafeAdapter.CafeViewHolder> {

    private Context context;
    private List<Cafe> cafeList;
    private FirebaseFirestore db;

    public AdminCafeAdapter(Context context, List<Cafe> cafeList) {
        this.context = context;
        this.cafeList = cafeList;
        this.db = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CafeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Link to the "item_admin_cafe.xml" design we just created
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_cafe, parent, false);
        return new CafeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CafeViewHolder holder, int position) {
        Cafe cafe = cafeList.get(position);

        holder.tvName.setText(cafe.getName());
        holder.tvDesc.setText(cafe.getDescription());

        if (cafe.getImageUrl() != null && !cafe.getImageUrl().isEmpty()) {
            Glide.with(context).load(cafe.getImageUrl()).into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_cute_camera);
        }

        // --- UPDATED: CLICKING DELETE SHOWS THE WARNING FIRST ---
        holder.btnDelete.setOnClickListener(v -> {
            showDeleteWarning(cafe, position);
        });
    }
    private void showDeleteWarning(Cafe cafe, int position) {
        // Create the warning box
        new android.app.AlertDialog.Builder(context)
                .setTitle("Delete Cafe?")
                .setMessage("Are you sure you want to delete '" + cafe.getName() + "'? This cannot be undone.")
                .setPositiveButton("Yes, Delete", (dialog, which) -> {
                    // Only delete if they click YES
                    deleteCafe(cafe, position);
                })
                .setNegativeButton("Cancel", null) // Do nothing if they click Cancel
                .show();
    }
    private void deleteCafe(Cafe cafe, int position) {
        // Safety Check: Ensure ID exists
        if (cafe.getId() == null) {
            Toast.makeText(context, "Error: Cannot delete cafe without ID", Toast.LENGTH_SHORT).show();
            return;
        }

        // Delete from Firestore
        db.collection("cafes").document(cafe.getId())
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Success! Now remove it from the screen immediately
                    cafeList.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, cafeList.size());
                    Toast.makeText(context, "Cafe Deleted!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return cafeList.size();
    }

    // --- ViewHolder Class ---
    public static class CafeViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDesc;
        ImageView ivImage, btnDelete;

        public CafeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvCafeName);
            tvDesc = itemView.findViewById(R.id.tvCafeDesc);
            ivImage = itemView.findViewById(R.id.ivCafeImage);
            btnDelete = itemView.findViewById(R.id.btnDeleteCafe);
        }
    }
}