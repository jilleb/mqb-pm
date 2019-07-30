package com.mqbcoding.stats;

import android.app.Notification;
import android.app.Service;
import android.app.UiModeManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import androidx.annotation.CallSuper;

public abstract class CarModeService extends Service {

    private boolean mForegroundStarted;

    @Override
    @CallSuper
    public void onCreate() {
        super.onCreate();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UiModeManager.ACTION_EXIT_CAR_MODE);
        registerReceiver(mBroadcastReceiver, intentFilter);

    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mBroadcastReceiver);
        super.onDestroy();
    }



    @Override
    @CallSuper
    public int onStartCommand(Intent intent, int flags, int startId) {
        UiModeManager uiModeManager = (UiModeManager) getSystemService(Context.UI_MODE_SERVICE);
        assert uiModeManager != null;
        if (uiModeManager.getCurrentModeType() != Configuration.UI_MODE_TYPE_CAR) {
            // Car mode ended before onStartCommand was delivered.
            stopSelf();
        }

        if (!mForegroundStarted) {
            try {
                Notification notification = buildNotification();
                if (notification != null) {
                    // This is required on Oreo so that the service is not destroyed when the app is
                    // in background.

                    startForeground(getNotificationId(), notification);

                }
                mForegroundStarted = true;
            } catch (Exception e) {
                e.printStackTrace();
                mForegroundStarted = false;
            }
        }

        return START_STICKY;
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UiModeManager.ACTION_EXIT_CAR_MODE.equals(intent.getAction())) {
                stopSelf();
            }
        }
    };

    protected abstract int getNotificationId();
    protected abstract Notification buildNotification();
}