package com.mqbcoding.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.text.TextUtils;
import android.util.AttributeSet;

public class TemperaturePreference extends EditTextPreference implements Preference.OnPreferenceChangeListener {

    private boolean conversionNeeded = false;
    private String mText;
    private boolean mTextSet;

    public TemperaturePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setOnPreferenceChangeListener(this);
    }

    public TemperaturePreference(Context context) {
        this(context, null);
    }

    // TODO: Make conversion between celsius and Fahrenheit
    @Override
    public void setText(String text) {
        SharedPreferences sp = getSharedPreferences();
        conversionNeeded = !sp.getBoolean("selectTemperatureUnit", true);
        this.setSummary(text + " °" + (conversionNeeded ? "F" : "C"));
        super.setText(text);

    }

    @Override
    public String getText() {
        return mText;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        this.setSummary(newValue + " °C");
        this.setText(newValue.toString());
        return false;
    }


}
