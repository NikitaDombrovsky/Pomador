package com.example.projet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private final Handler stopHandler = new Handler(Looper.getMainLooper());
    private TextView tvRingtoneName;

    private final ActivityResultLauncher<Intent> ringtonePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    String uriStr = (uri != null) ? uri.toString() : "";
                    Prefs.get(this).edit().putString(Prefs.KEY_RINGTONE, uriStr).apply();
                    updateRingtoneName(uriStr);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyTheme(Prefs.theme(this));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(getString(R.string.settings_title));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        SharedPreferences prefs = Prefs.get(this);

        // ── Тема ──
        RadioGroup rgTheme = findViewById(R.id.rg_theme);
        switch (Prefs.theme(this)) {
            case "dark":  rgTheme.check(R.id.rb_dark);   break;
            case "light": rgTheme.check(R.id.rb_light);  break;
            default:      rgTheme.check(R.id.rb_system); break;
        }
        rgTheme.setOnCheckedChangeListener((group, checkedId) -> {
            String theme;
            if (checkedId == R.id.rb_dark)       theme = "dark";
            else if (checkedId == R.id.rb_light) theme = "light";
            else                                  theme = "system";
            prefs.edit().putString(Prefs.KEY_THEME, theme).apply();
            applyTheme(theme);
        });

        // ── Уведомление ──
        SwitchMaterial swNotif = findViewById(R.id.sw_notif);
        swNotif.setChecked(Prefs.showNotif(this));
        swNotif.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(Prefs.KEY_SHOW_NOTIF, checked).apply());

        // ── Список дел ──
        SwitchMaterial swTasks = findViewById(R.id.sw_tasks);
        swTasks.setChecked(Prefs.showTasks(this));
        swTasks.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(Prefs.KEY_SHOW_TASKS, checked).apply());



        // ── Рандомайзер ──
        SwitchMaterial swCrazy = findViewById(R.id.sw_crazy);
        swCrazy.setChecked(Prefs.crazyMode(this));
        swCrazy.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(Prefs.KEY_CRAZY_MODE, checked).apply());

        // ── Мелодия ──
        tvRingtoneName = findViewById(R.id.tv_ringtone_name);
        updateRingtoneName(Prefs.ringtoneUri(this));

        MaterialButton btnPick = findViewById(R.id.btn_pick_ringtone);
        btnPick.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            String saved = Prefs.ringtoneUri(this);
            if (!saved.isEmpty()) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(saved));
            }
            ringtonePicker.launch(intent);
        });

        // ── Громкость ──
        TextView tvVolVal = findViewById(R.id.tv_volume_value);
        SeekBar sbVolume = findViewById(R.id.sb_volume);
        int savedVol = Prefs.volume(this);
        sbVolume.setProgress(savedVol);
        tvVolVal.setText(savedVol + "%");
        sbVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                tvVolVal.setText(progress + "%");
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    float vol = progress / 100f;
                    mediaPlayer.setVolume(vol, vol);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {
                prefs.edit().putInt(Prefs.KEY_VOLUME, sb.getProgress()).apply();
            }
        });
    }

    private void updateRingtoneName(String uriStr) {
        if (uriStr == null || uriStr.isEmpty()) {
            tvRingtoneName.setText(getString(R.string.settings_ringtone_none));
            return;
        }
        try {
            android.media.Ringtone r = RingtoneManager.getRingtone(this, Uri.parse(uriStr));
            tvRingtoneName.setText(r != null ? r.getTitle(this) : uriStr);
        } catch (Exception e) {
            tvRingtoneName.setText(uriStr);
        }
    }

    private void previewRingtone(String uriStr) {
        stopPreview();
        try {
            float vol = Prefs.volume(this) / 100f;
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            mediaPlayer.setDataSource(this, Uri.parse(uriStr));
            mediaPlayer.setVolume(vol, vol);
            mediaPlayer.prepare();
            mediaPlayer.start();
            // Останавливаем через 5 секунд
            stopHandler.postDelayed(this::stopPreview, 5_000);
        } catch (Exception e) {
            stopPreview();
        }
    }

    private void stopPreview() {
        stopHandler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
            } catch (Exception ignored) {}
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        stopPreview();
        super.onDestroy();
    }

    private void applyTheme(String theme) {
        switch (theme) {
            case "dark":  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            case "light": AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);  break;
            default:      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }
}
