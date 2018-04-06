package com.mqbcoding.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;

import com.github.martoreto.aauto.vex.CarStatsClient;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPOutputStream;

public class CarStatsLogger implements CarStatsClient.Listener {
    private static final String TAG = "CarStatsLogger";

    private static final DateFormat LOG_FILENAME_DATE_FORMAT =
            new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
    private static final DateFormat JSON_DATE_FORMAT;

    private static final int AUTO_SYNC_TIMEOUT_MS = 60000;

    public static final String PREF_ENABLED = "statsLoggingActive";

    static {
        JSON_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSSSS", Locale.US);
        JSON_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private boolean mIsEnabled;
    private final String mPrefix;
    private Writer mLogWriter;
    private File mLogFile;
    private Collection<Listener> mListeners = new ArrayList<>();
    private Handler mHandler;
    private Gson mGson = new Gson();

    public CarStatsLogger(Context context, Handler handler, String prefix) {
        super();
        mHandler = handler;
        mPrefix = prefix;

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(mPreferencesListener);
        readPreferences(sharedPreferences);
    }

    public CarStatsLogger(Context context, Handler handler) {
        this(context, handler, "car");
    }

    private void readPreferences(SharedPreferences preferences) {
        mIsEnabled = preferences.getBoolean(PREF_ENABLED, true);
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            readPreferences(sharedPreferences);

        }
    };

    public interface Listener {
        void onLogFileComplete(File logFile);
    }

    private void scheduleSyncTimeout() {
        mHandler.removeCallbacks(mSync);
        mHandler.postDelayed(mSync, AUTO_SYNC_TIMEOUT_MS);
    }

    private final Runnable mSync = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Auto-sync");
            close();
        }
    };

    @Override
    public void onNewMeasurements(String provider, Date date, Map<String, Object> values) {
        if (!mIsEnabled) {
            return;
        }
        try {
            createLogStream();
            if (mLogWriter != null) {
                Map<String, Object> o = new HashMap<>();
                o.put("timestamp", JSON_DATE_FORMAT.format(date));
                for (Map.Entry<String, Object> measurement: values.entrySet()) {
                    String key = measurement.getKey().replaceAll("[^a-zA-Z0-9]", "_");
                    o.put(key, measurement.getValue());
                }
                mLogWriter.write(mGson.toJson(o));
                mLogWriter.write('\n');
                scheduleSyncTimeout();
            }
        } catch (Exception e) {
            Log.w(TAG, "Error saving measurements", e);
            close();
        }
    }

    public static File getLogsDir() throws IOException {
        File logsDir = new File(Environment.getExternalStorageDirectory(), "CarLogs");
        if (!logsDir.exists()) {
            if (!logsDir.mkdirs()) {
                Log.e(TAG, "Failed to create CarLogs folder: " + logsDir.getAbsolutePath());
                throw new IOException("Failed to create CarLogs folder");
            }
        }
        return logsDir;
    }

    private synchronized void createLogStream() throws IOException {
        if (mLogWriter != null) {
            return;
        }
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            return;
        }

        String formattedDate = LOG_FILENAME_DATE_FORMAT.format(new Date());
        mLogFile = new File(getLogsDir(), mPrefix + "-" + formattedDate + ".log.gz");
        mLogWriter = new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(mLogFile)),
                StandardCharsets.UTF_8);
        Log.i(TAG, "Started log file: " + mLogFile.getAbsolutePath());
    }

    public synchronized void close() {
        if (mLogWriter != null) {
            try {
                mLogWriter.flush();
                mLogWriter.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing log stream", e);
            }
            for (Listener listener: mListeners) {
                try {
                    listener.onLogFileComplete(mLogFile);
                } catch (Exception e) {
                    Log.e(TAG, "Error from listener", e);
                }
            }
            mLogWriter = null;
            mLogFile = null;
            mHandler.removeCallbacks(mSync);
        }
    }

    public void registerListener(Listener listener) {
        mListeners.add(listener);
    }
}
