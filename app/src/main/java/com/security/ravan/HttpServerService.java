package com.security.ravan;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class HttpServerService extends Service {

    private static final String TAG = "HttpServerService";
    private static final String CHANNEL_ID = "RavanServerChannel";
    private static final int NOTIFICATION_ID = 1;

    private RavanHttpServer server;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;

        if ("START".equals(action)) {
            startForeground(NOTIFICATION_ID, createNotification());
            startServer();
        } else if ("STOP".equals(action)) {
            stopServer();
            stopForeground(true);
            stopSelf();
        }

        return START_STICKY;
    }

    private void startServer() {
        try {
            if (server == null || !server.isAlive()) {
                server = new RavanHttpServer(this, 8080);
                server.start();
                Log.d(TAG, "HTTP Server started on port 8080");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start server", e);
        }
    }

    private void stopServer() {
        try {
            if (server != null) {
                server.stop();
                server = null;
                Log.d(TAG, "HTTP Server stopped");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping server", e);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Ravan Security Server",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("HTTP Server running for device security");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE);

        String ipv6 = MainActivity.getIPv6Address();
        String contentText = ipv6 != null
                ? "Server running at http://[" + ipv6 + "]:8080"
                : "Server running on port 8080";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("🛡️ Ravan Security Active")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopServer();
        super.onDestroy();
    }
}
