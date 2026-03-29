package com.example.projet;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String NAME = "app_prefs";

    public static final String KEY_THEME       = "theme";
    public static final String KEY_SHOW_NOTIF  = "show_notif";
    public static final String KEY_SHOW_TASKS  = "show_tasks";
    public static final String KEY_LANGUAGE    = "language";
    public static final String KEY_CRAZY_MODE  = "crazy_mode";
    public static final String KEY_TIMER_MS    = "timer_ms";
    public static final String KEY_TASKS_JSON  = "tasks_json";
    public static final String KEY_RINGTONE    = "ringtone_uri";  // URI рингтона
    public static final String KEY_VOLUME      = "ringtone_vol";  // 0..100

    public static SharedPreferences get(Context ctx) {
        return ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String  theme(Context c)      { return get(c).getString(KEY_THEME, "system"); }
    public static boolean showNotif(Context c)  { return get(c).getBoolean(KEY_SHOW_NOTIF, true); }
    public static boolean showTasks(Context c)  { return get(c).getBoolean(KEY_SHOW_TASKS, true); }
    public static String  language(Context c)   { return get(c).getString(KEY_LANGUAGE, "ru"); }
    public static boolean crazyMode(Context c)  { return get(c).getBoolean(KEY_CRAZY_MODE, false); }
    public static long    timerMs(Context c)    { return get(c).getLong(KEY_TIMER_MS, 0L); }
    public static String  tasksJson(Context c)  { return get(c).getString(KEY_TASKS_JSON, "[]"); }
    public static String  ringtoneUri(Context c){ return get(c).getString(KEY_RINGTONE, ""); }
    public static int     volume(Context c)     { return get(c).getInt(KEY_VOLUME, 80); }
}
