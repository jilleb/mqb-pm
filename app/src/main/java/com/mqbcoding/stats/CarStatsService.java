package com.mqbcoding.stats;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.github.martoreto.aauto.vex.CarStatsClient;

import java.io.File;

public class CarStatsService extends Service {
    private static final String TAG = "CarStatsService";

    private CarStatsClient mStatsClient;
    private CarStatsLogger mStatsLogger;
    private OilTempMonitor mOilTempMonitor;
    private WheelStateMonitor mWheelStateMonitor;

    private final IBinder mBinder = new CarStatsBinder();

    @SuppressWarnings("unused")
    public class CarStatsBinder extends Binder {
        CarStatsClient getStatsClient() {
            return mStatsClient;
        }
        OilTempMonitor getOilTempMonitor() {
            return mOilTempMonitor;
        }
        WheelStateMonitor getWheelStateMonitor() {
            return mWheelStateMonitor;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service starting...");

        mStatsClient = new CarStatsClient(this);

        mStatsLogger = new CarStatsLogger(this, mStatsClient, new Handler());
        mStatsLogger.registerListener(mStatsLoggerListener);
        mStatsClient.registerListener(mStatsLogger);

        mOilTempMonitor = new OilTempMonitor(this, new Handler());
        mStatsClient.registerListener(mOilTempMonitor);

        mWheelStateMonitor = new WheelStateMonitor(this, new Handler());
        mStatsClient.registerListener(mWheelStateMonitor);

        mStatsClient.start();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service stopping.");

        if (mStatsLogger != null) {
            mStatsLogger.close();
            mStatsLogger = null;
        }
        if (mOilTempMonitor != null) {
            mOilTempMonitor.close();
            mOilTempMonitor = null;
        }

        mStatsClient.stop();
        mStatsClient = null;

        super.onDestroy();
    }

    private final CarStatsLogger.Listener mStatsLoggerListener = new CarStatsLogger.Listener() {
        @Override
        public void onLogFileComplete(File logFile) {
            LogUploadService.schedule(CarStatsService.this, logFile);
        }
    };
}
