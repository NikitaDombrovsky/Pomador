package com.example.projet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

public class TimerService extends Service {

    public static final String ACTION_START  = "com.example.projet.TIMER_START";
    public static final String ACTION_STOP   = "com.example.projet.TIMER_STOP";
    public static final String EXTRA_DURATION_MS = "duration_ms";

    // Broadcast отправляемый в MainActivity каждую секунду
    public static final String ACTION_TICK   = "com.example.projet.TIMER_TICK";
    public static final String EXTRA_REMAINING = "remaining_ms";

    private static final String CHANNEL_ID = "timer_channel";
    private static final int NOTIF_ID = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long endTimeMs = 0;
    private boolean running = false;

    private final Runnable tickRunnable = new Runnable() {
        @Override
        public void run() {
            long remaining = endTimeMs - System.currentTimeMillis();
            if (remaining <= 0) {
                remaining = 0;
                running = false;
                updateNotification(0);
                sendTickBroadcast(0);
                stopSelf();
                return;
            }
            updateNotification(remaining);
            sendTickBroadcast(remaining);
            handler.postDelayed(this, 500);
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        if (ACTION_START.equals(intent.getAction())) {
            long durationMs = intent.getLongExtra(EXTRA_DURATION_MS, 0);
            if (durationMs <= 0) { stopSelf(); return START_NOT_STICKY; }

            endTimeMs = System.currentTimeMillis() + durationMs;
            running = true;

            createNotificationChannel();
            startForeground(NOTIF_ID, buildNotification(durationMs));
            handler.removeCallbacks(tickRunnable);
            handler.post(tickRunnable);

        } else if (ACTION_STOP.equals(intent.getAction())) {
            stopTimer();
        }

        return START_NOT_STICKY;
    }

    private void stopTimer() {
        running = false;
        handler.removeCallbacks(tickRunnable);
        sendTickBroadcast(-1); // -1 = сигнал остановки для MainActivity
        stopForeground(true);
        stopSelf();
    }

    private void sendTickBroadcast(long remainingMs) {
        Intent broadcast = new Intent(ACTION_TICK);
        broadcast.putExtra(EXTRA_REMAINING, remainingMs);
        sendBroadcast(broadcast);
    }

    private void updateNotification(long remainingMs) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(NOTIF_ID, buildNotification(remainingMs));
    }

    private Notification buildNotification(long remainingMs) {
        String timeStr = formatTime(remainingMs);

        // PendingIntent — открыть приложение по тапу
        Intent openApp = new Intent(this, MainActivity.class);
        openApp.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent openPending = PendingIntent.getActivity(
                this, 0, openApp,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // PendingIntent для кнопки Стоп
        Intent stopIntent = new Intent(this, TimerService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
                this, 0, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.notif_title))
                .setContentText(getString(R.string.notif_remaining, timeStr))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(false)
                .setContentIntent(openPending)
                .addAction(android.R.drawable.ic_media_pause, getString(R.string.notif_stop), stopPending)
                .build();
    }

    private String formatTime(long ms) {
        if (ms < 0) ms = 0;
        long totalSec = ms / 1000;
        long hours   = totalSec / 3600;
        long minutes = (totalSec % 3600) / 60;
        long seconds = totalSec % 60;
        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(tickRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Таймер", NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Обратный отсчёт");
            channel.setSound(null, null);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }
}
