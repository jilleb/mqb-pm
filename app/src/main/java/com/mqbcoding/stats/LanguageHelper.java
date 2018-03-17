package com.mqbcoding.stats;

import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LanguageHelper {
    public static void changeLocale(Resources res, String locale){

        Configuration config;
        config = new Configuration(res.getConfiguration());

        switch (locale){
            case "ru":
                config.locale = new Locale("ru");
                break;
            case "nl":
                config.locale = new Locale("nl");
                break;
            case "de":
                config.locale = new Locale("de");
                break;


        }
        res.updateConfiguration(config, res.getDisplayMetrics());
        //reload files from assets directory
    }
}
