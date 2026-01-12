package com.example.unibrew;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.List;
import android.view.animation.AnimationUtils;

public class CafeAdapter extends RecyclerView.Adapter<CafeAdapter.ViewHolder> {

    private List<Cafe> cafeList;
    private int lastPosition = -1;
    // Constructor
    public CafeAdapter(List<Cafe> cafeList) {
        this.cafeList = cafeList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cafe_card, parent, false);
        return new ViewHolder(v);
    }

    // --- THIS IS THE ONE AND ONLY BIND METHOD YOU NEED ---
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cafe cafe = cafeList.get(position);
        Context context = holder.itemView.getContext();

        // 1. Set Text
        holder.name.setText(cafe.getName());
        holder.desc.setText(cafe.getDescription());

        // 2. Load Image
        Glide.with(context)
                .load(cafe.getImageUrl())
                .placeholder(R.drawable.ic_cute_camera)
                .centerCrop()
                .into(holder.image);

        // 3. Click Listener
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

        // --- NEW: WATERFALL ANIMATION ---
        // Only animate if the item is scrolling into view, not when scrolling back up
        if (position > lastPosition) {
            android.view.animation.Animation animation =
                    AnimationUtils.loadAnimation(context, R.anim.item_slide_up);
            holder.itemView.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public int getItemCount() { return cafeList.size(); }

    // --- YOUR VIEWHOLDER CLASS ---
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name, desc;
        ImageView image;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // These IDs must match your item_cafe_card.xml
            name = itemView.findViewById(R.id.tvCardName);
            desc = itemView.findViewById(R.id.tvCardDesc);
            image = itemView.findViewById(R.id.ivCardImage);
        }
    }
}