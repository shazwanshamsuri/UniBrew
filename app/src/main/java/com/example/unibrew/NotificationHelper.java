package com.example.unibrew;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Map;

public class NotificationHelper {

    public static final String CHANNEL_ID = "nearby_cafe_channel";
    private static final String CHANNEL_NAME = "Nearby Cafes";
    private static final String CHANNEL_DESC = "Notifications when a cafe is nearby";

    // 1. Create the Channel (Call this in MainActivity onCreate)
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(CHANNEL_DESC);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // 2. Send the "Nearby" Notification
    // Note: We accept a Map or a Cafe object. Adapting for flexibility.
    public static void sendProximityNotification(Context context, String cafeName, String cafeId) {

        // Permission Check (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                return; // Stop if permission missing
            }
        }

        // Create Intent to open the cafe details
        Intent intent = new Intent(context, CafeDetailActivity.class);
        intent.putExtra("cafeId", cafeId);
        intent.putExtra("cafeName", cafeName);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                cafeName.hashCode(), // Unique ID per cafe
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_map) // Uses system map icon
                .setContentTitle("Nearby Coffee Spot! ☕")
                .setContentText("You are near " + cafeName + ". Check it out!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        // Show it
        NotificationManagerCompat.from(context).notify(cafeName.hashCode(), builder.build());
    }
}