package com.example.projet;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Chronometer;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final long ONE_SECOND_MS = 1_000L;
    private static final long ONE_MINUTE_MS = 60_000L;
    private static final String CHANNEL_ID = "timer_channel";
    private static final int NOTIF_ID_DONE = 2;
    private static final int REQUEST_NOTIF_PERMISSION = 100;

    private Chronometer timer;
    private MaterialButton startResetButton;
    private View tasksSection;
    private long currentDurationMs;
    private long startedDurationMs;
    private boolean isRunning;

    // Рандомайзер
    private final Handler crazyHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final Runnable crazyRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning && Prefs.crazyMode(MainActivity.this)) {
                // Случайное время от 1 сек до 10 минут
                currentDurationMs = (long)(random.nextInt(600) + 1) * ONE_SECOND_MS;
                updateTimerDisplay();
                crazyHandler.postDelayed(this, ONE_SECOND_MS);
            }
        }
    };

    private final List<TaskAdapter.Task> taskList = new ArrayList<>();
    private TaskAdapter taskAdapter;

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long remaining = intent.getLongExtra(TimerService.EXTRA_REMAINING, -1);
            if (remaining == -1) {
                if (isRunning) {
                    timer.stop();
                    isRunning = false;
                    currentDurationMs = startedDurationMs;
                    updateTimerDisplay();
                    startResetButton.setText(getString(R.string.btn_start));
                    crazyHandler.removeCallbacks(crazyRunnable);
                    if (Prefs.crazyMode(MainActivity.this)) crazyHandler.post(crazyRunnable);
                }
                return;
            }
            if (remaining == 0 && isRunning) {
                timer.stop();
                isRunning = false;
                currentDurationMs = 0L;
                updateTimerDisplay();
                startResetButton.setText(getString(R.string.btn_start));
                sendFinishedNotification();
                crazyHandler.removeCallbacks(crazyRunnable);
                if (Prefs.crazyMode(MainActivity.this)) crazyHandler.post(crazyRunnable);
            }
        }
    };

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(LocaleHelper.applyLocale(base));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeSetting();
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        createNotificationChannel();
        requestNotificationPermission();

        IntentFilter filter = new IntentFilter(TimerService.ACTION_TICK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(tickReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(tickReceiver, filter);
        }

        timer = findViewById(R.id.timer);
        startResetButton = findViewById(R.id.start_reset_button);
        tasksSection = findViewById(R.id.tasks_section);

        currentDurationMs = 0L;
        startedDurationMs = 0L;
        isRunning = false;
        updateTimerDisplay();

        findViewById(R.id.add_minute_button).setOnClickListener(v -> adjustTimer(ONE_MINUTE_MS));
        findViewById(R.id.subtract_minute_button).setOnClickListener(v -> adjustTimer(-ONE_MINUTE_MS));
        findViewById(R.id.add_second_button).setOnClickListener(v -> adjustTimer(ONE_SECOND_MS));
        findViewById(R.id.subtract_second_button).setOnClickListener(v -> adjustTimer(-ONE_SECOND_MS));
        startResetButton.setOnClickListener(v -> toggleTimer());
        findViewById(R.id.settings_button).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        RecyclerView recyclerView = findViewById(R.id.tasks_recycler);
        taskAdapter = new TaskAdapter(taskList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(taskAdapter);
        findViewById(R.id.add_task_button).setOnClickListener(v -> showAddTaskDialog());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Применяем настройки при возврате из Settings
        applyVisibilitySettings();
        if (Prefs.crazyMode(this) && !isRunning) {
            crazyHandler.removeCallbacks(crazyRunnable);
            crazyHandler.post(crazyRunnable);
        } else {
            crazyHandler.removeCallbacks(crazyRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(tickReceiver);
        crazyHandler.removeCallbacks(crazyRunnable);
    }

    private void applyVisibilitySettings() {
        // Список дел
        if (tasksSection != null) {
            tasksSection.setVisibility(Prefs.showTasks(this) ? View.VISIBLE : View.GONE);
        }
    }

    private void applyThemeSetting() {
        switch (Prefs.theme(this)) {
            case "dark":  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            case "light": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);  break;
            default:      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }

    private void adjustTimer(long deltaMs) {
        if (isRunning) return;
        currentDurationMs = Math.max(0L, currentDurationMs + deltaMs);
        updateTimerDisplay();
    }

    private void toggleTimer() {
        if (isRunning) {
            Intent stopIntent = new Intent(this, TimerService.class);
            stopIntent.setAction(TimerService.ACTION_STOP);
            startService(stopIntent);
            timer.stop();
            isRunning = false;
            currentDurationMs = startedDurationMs;
            updateTimerDisplay();
            startResetButton.setText(getString(R.string.btn_start));
            crazyHandler.removeCallbacks(crazyRunnable);
            if (Prefs.crazyMode(this)) crazyHandler.post(crazyRunnable);
            return;
        }

        if (currentDurationMs <= 0) return;

        crazyHandler.removeCallbacks(crazyRunnable);
        startedDurationMs = currentDurationMs;
        timer.setBase(SystemClock.elapsedRealtime() + currentDurationMs);
        timer.start();
        isRunning = true;
        startResetButton.setText(getString(R.string.btn_reset));

        if (Prefs.showNotif(this)) {
            Intent startIntent = new Intent(this, TimerService.class);
            startIntent.setAction(TimerService.ACTION_START);
            startIntent.putExtra(TimerService.EXTRA_DURATION_MS, currentDurationMs);
            startForegroundService(startIntent);
        }
    }

    private void updateTimerDisplay() {
        timer.stop();
        timer.setBase(SystemClock.elapsedRealtime() + currentDurationMs);
        if (currentDurationMs == 0L) timer.setText("00:00");
    }

    private void sendFinishedNotification() {
        if (!Prefs.showNotif(this)) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) return;
        }
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(getString(R.string.notif_done_title))
                .setContentText(getString(R.string.notif_done_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);
        NotificationManagerCompat.from(this).notify(NOTIF_ID_DONE, builder.build());
    }

    private void showAddTaskDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_task, null);
        TextInputEditText inputTitle = dialogView.findViewById(R.id.input_title);
        TextInputEditText inputDesc = dialogView.findViewById(R.id.input_description);

        new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(getString(R.string.btn_add), (dialog, which) -> {
                    String title = inputTitle.getText() != null
                            ? inputTitle.getText().toString().trim() : "";
                    String desc = inputDesc.getText() != null
                            ? inputDesc.getText().toString().trim() : "";
                    if (!title.isEmpty()) {
                        taskList.add(new TaskAdapter.Task(title, desc));
                        taskAdapter.notifyItemInserted(taskList.size() - 1);
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Таймер", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Обратный отсчёт");
            channel.setSound(null, null);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIF_PERMISSION);
            }
        }
    }
}
