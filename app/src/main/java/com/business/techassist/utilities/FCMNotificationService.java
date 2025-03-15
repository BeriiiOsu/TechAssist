package com.business.techassist.utilities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.business.techassist.R;
import com.business.techassist.SplashActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMNotificationService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        if (!remoteMessage.getData().isEmpty()) {
            String title = remoteMessage.getData().get("title");
            String body = remoteMessage.getData().get("body");
            String userID = remoteMessage.getData().get("userID");

            sendNotification(title, body, userID);
        }
    }

    private void sendNotification(String title, String message, String userID) {
        Intent intent = new Intent(this, SplashActivity.class);
        intent.putExtra("userID", userID);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "default_channel")
                .setSmallIcon(R.drawable.notifications_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "default_channel",
                    "Default Channel",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        notificationManager.notify(0, builder.build());
    }
}
