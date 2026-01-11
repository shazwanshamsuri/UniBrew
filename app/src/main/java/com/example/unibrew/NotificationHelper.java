package com.example.unibrew;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationHelper {

    private static final String CHANNEL_ID = "nearby_cafe_channel";
    private static final String CHANNEL_NAME = "Nearby Cafes";
    private static final String CHANNEL_DESC = "Notifications when a cafe is nearby";

    // Call this once in MainActivity onCreate to set up the channel
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // We set importance to LOW to stop the default system sound
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(CHANNEL_DESC);
            channel.setSound(null, null); // Explicitly remove system sound

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // Call this to actually show the notification
    public static void sendProximityNotification(Context context, Cafe cafe) {
        // 1. Create intent to open the Detail Page when clicked
        Intent intent = new Intent(context, CafeDetailActivity.class);
        intent.putExtra("cafeId", cafe.getCafeId());
        intent.putExtra("cafeName", cafe.getName());
        intent.putExtra("cafeDesc", cafe.getDescription());
        intent.putExtra("cafeImageUrl", cafe.getImageUrl());
        intent.putExtra("cafeLat", cafe.getLatitude());
        intent.putExtra("cafeLng", cafe.getLongitude());

        // PendingIntent is a "wrapper" that allows the notification to open your app
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                cafe.getName().hashCode(), // Unique ID per cafe so they don't overwrite
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 2. Build the Notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_cute_camera) // Use your existing icon or change this!
                .setContentTitle("Nearby Coffee Spot! ☕")
                .setContentText("You are near " + cafe.getName() + ". Check it out!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent) // Add the click action
                .setAutoCancel(true); // Remove notification when clicked

        // 3. Show it!
        try {
            NotificationManagerCompat.from(context).notify(cafe.getName().hashCode(), builder.build());
        } catch (SecurityException e) {
            // Android 13+ requires POST_NOTIFICATIONS permission, handled in Activity
        }
    }
}