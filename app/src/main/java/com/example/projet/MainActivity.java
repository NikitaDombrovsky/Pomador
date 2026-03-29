package com.example.projet;

import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
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
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

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
    private View timerControls;
    private TextView labelMode;
    private MaterialButton btnModeTimer, btnModePomodoro;
    private boolean isPomodoroMode = false;
    private static final long POMODORO_MS = 25 * 60 * 1_000L; // 25 минут
    private long currentDurationMs;
    private long startedDurationMs;
    private boolean isRunning;
    private MediaPlayer finishPlayer;

    // Рандомайзер
    private final Handler crazyHandler = new Handler(Looper.getMainLooper());
    private final Random random = new Random();
    private final Runnable crazyRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isRunning && Prefs.crazyMode(MainActivity.this)) {
                // Случайное время от 10 секунд до 1 часа
                long minMs = 10 * ONE_SECOND_MS;
                long maxMs = 3600 * ONE_SECOND_MS;
                currentDurationMs = minMs + (long)(random.nextDouble() * (maxMs - minMs));
                updateTimerDisplay();
                crazyHandler.postDelayed(this, 1); // каждую миллисекунду
            }
        }
    };

    private final List<TaskAdapter.Task> taskList = new ArrayList<>();
    private final List<TaskAdapter.Task> pomodoroList = new ArrayList<>();
    private TaskAdapter taskAdapter;
    private PomodoroAdapter pomodoroAdapter;
    private RecyclerView tasksRecycler;

    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long remaining = intent.getLongExtra(TimerService.EXTRA_REMAINING, -1);

            if (remaining == -1) {
                // Сервис остановлен (кнопка Стоп из уведомления)
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
                // Время вышло
                timer.stop();
                isRunning = false;
                currentDurationMs = 0L;
                updateTimerDisplay();
                startResetButton.setText(getString(R.string.btn_start));
                sendFinishedNotification();
                playFinishSound();
                crazyHandler.removeCallbacks(crazyRunnable);
                if (Prefs.crazyMode(MainActivity.this)) crazyHandler.post(crazyRunnable);
                return;
            }

            // Сервис работал пока приложение было закрыто — восстанавливаем UI
            if (!isRunning && remaining > 0) {
                isRunning = true;
                startedDurationMs = remaining;
                currentDurationMs = remaining;
                timer.setBase(android.os.SystemClock.elapsedRealtime() + remaining);
                timer.start();
                startResetButton.setText(getString(R.string.btn_reset));
                crazyHandler.removeCallbacks(crazyRunnable);
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
        timerControls = findViewById(R.id.timer_controls);
        labelMode = findViewById(R.id.label_mode);
        btnModeTimer = findViewById(R.id.btn_mode_timer);
        btnModePomodoro = findViewById(R.id.btn_mode_pomodoro);

        currentDurationMs = Prefs.timerMs(this);
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

        btnModeTimer.setOnClickListener(v -> setMode(false));
        btnModePomodoro.setOnClickListener(v -> setMode(true));

        tasksRecycler = findViewById(R.id.tasks_recycler);

        // Адаптер обычного режима
        taskAdapter = new TaskAdapter(taskList);

        // Адаптер Помодоро
        pomodoroAdapter = new PomodoroAdapter(pomodoroList);
        ItemTouchHelper.Callback callback = new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                pomodoroAdapter.onItemMoved(vh.getAdapterPosition(), target.getAdapterPosition());
                return true;
            }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int dir) {}
            @Override public boolean isLongPressDragEnabled() { return false; }
        };
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(tasksRecycler);
        pomodoroAdapter.setTouchHelper(touchHelper);
        pomodoroAdapter.setOnFirstTaskChangedListener(tomatoes -> {
            if (isPomodoroMode && !isRunning) {
                currentDurationMs = tomatoes > 0 ? tomatoes * POMODORO_MS : POMODORO_MS;
                updateTimerDisplay();
            }
        });

        tasksRecycler.setLayoutManager(new LinearLayoutManager(this));
        tasksRecycler.setAdapter(taskAdapter); // по умолчанию обычный режим
        loadTasks();
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
        applyVisibilitySettings();

        // Синхронизируем с сервисом если он ещё работает
        Intent query = new Intent(this, TimerService.class);
        query.setAction(TimerService.ACTION_QUERY);
        startService(query);

        if (Prefs.crazyMode(this) && !isRunning) {
            crazyHandler.removeCallbacks(crazyRunnable);
            crazyHandler.post(crazyRunnable);
        } else if (!isRunning) {
            crazyHandler.removeCallbacks(crazyRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Сохраняем время таймера (только если не запущен)
        if (!isRunning) {
            Prefs.get(this).edit().putLong(Prefs.KEY_TIMER_MS, currentDurationMs).apply();
        }
        saveTasks();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(tickReceiver);
        crazyHandler.removeCallbacks(crazyRunnable);
        if (finishPlayer != null) { finishPlayer.release(); finishPlayer = null; }
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

    private void setMode(boolean pomodoro) {
        if (isPomodoroMode == pomodoro) return;
        isPomodoroMode = pomodoro;

        // Плавная анимация кнопок управления временем
        if (pomodoro) {
            timerControls.animate().alpha(0f).setDuration(250)
                    .withEndAction(() -> timerControls.setVisibility(View.GONE)).start();
        } else {
            timerControls.setVisibility(View.VISIBLE);
            timerControls.animate().alpha(1f).setDuration(250).start();
        }

        // Меняем адаптер — никакого переиспользования ViewHolder между режимами
        tasksRecycler.setAdapter(pomodoro ? pomodoroAdapter : taskAdapter);

        if (pomodoro) {
            labelMode.setText(getString(R.string.mode_pomodoro));
            currentDurationMs = (!pomodoroList.isEmpty())
                    ? pomodoroList.get(0).tomatoes * POMODORO_MS
                    : POMODORO_MS;
        } else {
            labelMode.setText(getString(R.string.label_timer));
            currentDurationMs = Prefs.timerMs(this);
        }
        if (!isRunning) updateTimerDisplay();
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
                        TaskAdapter.Task task = new TaskAdapter.Task(title, desc);
                        if (isPomodoroMode) {
                            pomodoroList.add(task);
                            pomodoroAdapter.notifyItemInserted(pomodoroList.size() - 1);
                            // Если первая задача — обновляем таймер
                            if (pomodoroList.size() == 1 && !isRunning) {
                                currentDurationMs = task.tomatoes * POMODORO_MS;
                                updateTimerDisplay();
                            }
                        } else {
                            taskList.add(task);
                            taskAdapter.notifyItemInserted(taskList.size() - 1);
                        }
                    }
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void playFinishSound() {
        String uriStr = Prefs.ringtoneUri(this);
        if (uriStr == null || uriStr.isEmpty()) return;
        try {
            if (finishPlayer != null) { finishPlayer.release(); finishPlayer = null; }
            float vol = Prefs.volume(this) / 100f;
            finishPlayer = new MediaPlayer();
            finishPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            finishPlayer.setDataSource(this, Uri.parse(uriStr));
            finishPlayer.setVolume(vol, vol);
            finishPlayer.prepare();
            finishPlayer.start();
            finishPlayer.setOnCompletionListener(mp -> {
                mp.release();
                finishPlayer = null;
            });
        } catch (Exception e) {
            if (finishPlayer != null) { finishPlayer.release(); finishPlayer = null; }
        }
    }

    private void saveTasks() {
        try {
            Prefs.get(this).edit()
                    .putString(Prefs.KEY_TASKS_JSON, serializeList(taskList))
                    .putString("pomodoro_tasks_json", serializeList(pomodoroList))
                    .apply();
        } catch (Exception ignored) {}
    }

    private String serializeList(List<TaskAdapter.Task> list) throws Exception {
        org.json.JSONArray arr = new org.json.JSONArray();
        for (TaskAdapter.Task t : list) {
            org.json.JSONObject obj = new org.json.JSONObject();
            obj.put("title", t.title);
            obj.put("desc", t.description);
            obj.put("status", t.status.name());
            obj.put("tomatoes", t.tomatoes);
            arr.put(obj);
        }
        return arr.toString();
    }

    private void loadTasks() {
        taskList.addAll(deserializeList(Prefs.tasksJson(this)));
        pomodoroList.addAll(deserializeList(
                Prefs.get(this).getString("pomodoro_tasks_json", "[]")));
        taskAdapter.notifyDataSetChanged();
        pomodoroAdapter.notifyDataSetChanged();
    }

    private List<TaskAdapter.Task> deserializeList(String json) {
        List<TaskAdapter.Task> result = new ArrayList<>();
        try {
            org.json.JSONArray arr = new org.json.JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                org.json.JSONObject obj = arr.getJSONObject(i);
                TaskAdapter.Task t = new TaskAdapter.Task(
                        obj.getString("title"), obj.getString("desc"));
                try { t.status = TaskAdapter.Status.valueOf(obj.getString("status")); }
                catch (Exception e) { t.status = TaskAdapter.Status.PENDING; }
                t.tomatoes = obj.optInt("tomatoes", 1);
                result.add(t);
            }
        } catch (Exception ignored) {}
        return result;
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
