package com.example.projet;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.RadioGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Применяем тему до super.onCreate
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
            case "dark":   rgTheme.check(R.id.rb_dark);   break;
            case "light":  rgTheme.check(R.id.rb_light);  break;
            default:       rgTheme.check(R.id.rb_system); break;
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

        // ── Язык ──
        RadioGroup rgLang = findViewById(R.id.rg_language);
        rgLang.check("en".equals(Prefs.language(this)) ? R.id.rb_en : R.id.rb_ru);
        rgLang.setOnCheckedChangeListener((group, checkedId) -> {
            String lang = (checkedId == R.id.rb_en) ? "en" : "ru";
            prefs.edit().putString(Prefs.KEY_LANGUAGE, lang).apply();
            LocaleHelper.setLocale(this, lang);
            // Перезапускаем активити чтобы язык применился
            recreate();
        });

        // ── Рандомайзер ──
        SwitchMaterial swCrazy = findViewById(R.id.sw_crazy);
        swCrazy.setChecked(Prefs.crazyMode(this));
        swCrazy.setOnCheckedChangeListener((btn, checked) ->
                prefs.edit().putBoolean(Prefs.KEY_CRAZY_MODE, checked).apply());
    }

    private void applyTheme(String theme) {
        switch (theme) {
            case "dark":   AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES); break;
            case "light":  AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);  break;
            default:       AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM); break;
        }
    }
}
