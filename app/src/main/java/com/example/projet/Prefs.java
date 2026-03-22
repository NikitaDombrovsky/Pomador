package com.example.projet;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {
    private static final String NAME = "app_prefs";

    public static final String KEY_THEME       = "theme";       // dark / light / system
    public static final String KEY_SHOW_NOTIF  = "show_notif";
    public static final String KEY_SHOW_TASKS  = "show_tasks";
    public static final String KEY_LANGUAGE    = "language";    // ru / en
    public static final String KEY_CRAZY_MODE  = "crazy_mode";

    public static SharedPreferences get(Context ctx) {
        return ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String  theme(Context c)      { return get(c).getString(KEY_THEME, "system"); }
    public static boolean showNotif(Context c)  { return get(c).getBoolean(KEY_SHOW_NOTIF, true); }
    public static boolean showTasks(Context c)  { return get(c).getBoolean(KEY_SHOW_TASKS, true); }
    public static String  language(Context c)   { return get(c).getString(KEY_LANGUAGE, "ru"); }
    public static boolean crazyMode(Context c)  { return get(c).getBoolean(KEY_CRAZY_MODE, false); }
}
