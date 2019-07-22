package com.mqbcoding.stats;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.SystemClock;
import androidx.annotation.RequiresApi;
import android.util.Log;
@RequiresApi(26)
public class CarModeActiveMonitoringReceiver extends BroadcastReceiver {
    public static final String TAG = "CarModeAMR";
    private static final long INTERVAL_MS = 15000;
    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(TAG, "intent: " + intent);
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        if (uiModeManager != null && uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_CAR) {
            try {
                context.startForegroundService(new Intent(context, CarStatsService.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        scheduleAlarm(context);
    }
    private void scheduleAlarm(Context context) {
        Intent intent = new Intent(context, CarModeActiveMonitoringReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        assert mgr != null;
        mgr.setWindow(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + INTERVAL_MS / 2,
                INTERVAL_MS, alarmIntent);
    }
}