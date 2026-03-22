package com.example.projet;

import android.os.SystemClock;
import android.os.Bundle;
import android.widget.Chronometer;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private static final long ONE_SECOND_MS = 1_000L;
    private static final long ONE_MINUTE_MS = 60_000L;

    private Chronometer timer;
    private long currentDurationMs;
    private long startedDurationMs;
    private boolean isRunning;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        timer = findViewById(R.id.timer);
        currentDurationMs = 0L;
        startedDurationMs = 0L;
        isRunning = false;
        updateTimerDisplay();

        findViewById(R.id.add_minute_button).setOnClickListener(v -> adjustTimer(ONE_MINUTE_MS));
        findViewById(R.id.subtract_minute_button).setOnClickListener(v -> adjustTimer(-ONE_MINUTE_MS));
        findViewById(R.id.add_second_button).setOnClickListener(v -> adjustTimer(ONE_SECOND_MS));
        findViewById(R.id.subtract_second_button).setOnClickListener(v -> adjustTimer(-ONE_SECOND_MS));
        findViewById(R.id.start_reset_button).setOnClickListener(v -> toggleTimer());

        timer.setOnChronometerTickListener(chronometer -> {
            if (isRunning && SystemClock.elapsedRealtime() >= chronometer.getBase()) {
                chronometer.stop();
                isRunning = false;
                currentDurationMs = 0L;
                updateTimerDisplay();
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    private void adjustTimer(long deltaMs) {
        if (isRunning) {
            return;
        }

        currentDurationMs = Math.max(0L, currentDurationMs + deltaMs);
        updateTimerDisplay();
    }

    private void toggleTimer() {
        if (isRunning) {
            timer.stop();
            isRunning = false;
            currentDurationMs = startedDurationMs;
            updateTimerDisplay();
            return;
        }

        startedDurationMs = currentDurationMs;
        timer.setBase(SystemClock.elapsedRealtime() + currentDurationMs);
        timer.start();
        isRunning = true;
    }

    private void updateTimerDisplay() {
        timer.stop();
        timer.setBase(SystemClock.elapsedRealtime() + currentDurationMs);

        if (currentDurationMs == 0L) {
            timer.setText("00:00");
        }
    }
}
