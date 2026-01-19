package com.example.unibrew;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Locale;

public class CafeAdapter extends RecyclerView.Adapter<CafeAdapter.ViewHolder> {

    private List<Cafe> cafeList;
    private Location userLocation; // We store the user's GPS location here
    private int lastPosition = -1;

    public CafeAdapter(List<Cafe> cafeList) {
        this.cafeList = cafeList;
    }

    // --- NEW: METHOD TO RECEIVE GPS LOCATION ---
    public void updateUserLocation(Location location) {
        this.userLocation = location;
        notifyDataSetChanged(); // Refresh the list to show new distances
    }
    // -------------------------------------------

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cafe_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cafe cafe = cafeList.get(position);
        Context context = holder.itemView.getContext();

        holder.name.setText(cafe.getName());
        holder.desc.setText(cafe.getDescription());

        // --- 1. SET REAL RATING ---
        // If rating is 0, we can show "New" or just 0.0
        if (cafe.getRating() > 0) {
            holder.rating.setText(String.format(Locale.getDefault(), "★ %.1f", cafe.getRating()));
        } else {
            holder.rating.setText("New");
        }

        // --- 2. CALCULATE REAL DISTANCE ---
        if (userLocation != null) {
            float[] results = new float[1];
            // Math to calculate distance between User and Cafe
            Location.distanceBetween(
                    userLocation.getLatitude(), userLocation.getLongitude(),
                    cafe.getLatitude(), cafe.getLongitude(),
                    results);

            float distanceInKm = results[0] / 1000;
            holder.distance.setText(String.format(Locale.getDefault(), "%.1f km away", distanceInKm));
        } else {
            holder.distance.setText("... km");
        }

        Glide.with(context)
                .load(cafe.getImageUrl())
                .placeholder(R.drawable.ic_cute_camera)
                .centerCrop()
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CafeDetailActivity.class);
            intent.putExtra("cafeId", cafe.getId());
            intent.putExtra("cafeName", cafe.getName());
            intent.putExtra("cafeDesc", cafe.getDescription());
            intent.putExtra("cafeImageUrl", cafe.getImageUrl());
            intent.putExtra("cafeLat", cafe.getLatitude());
            intent.putExtra("cafeLng", cafe.getLongitude());
            context.startActivity(intent);
        });

        if (position > lastPosition) {
            holder.itemView.startAnimation(AnimationUtils.loadAnimation(context, R.anim.item_slide_up));
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() { return cafeList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, desc, rating, distance;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tvCardName);
            desc = itemView.findViewById(R.id.tvCardDesc);
            rating = itemView.findViewById(R.id.tvCardRating); // Added this
            distance = itemView.findViewById(R.id.tvCardDistance); // Added this
            image = itemView.findViewById(R.id.ivCardImage);
        }
    }
}