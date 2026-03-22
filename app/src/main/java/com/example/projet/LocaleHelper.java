package com.example.projet;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LocaleHelper {

    public static void setLocale(Context context, String lang) {
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);
        res.updateConfiguration(config, res.getDisplayMetrics());
    }

    public static Context applyLocale(Context context) {
        String lang = Prefs.language(context);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        Configuration config = new Configuration(context.getResources().getConfiguration());
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }
}
