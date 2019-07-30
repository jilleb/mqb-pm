package com.mqbcoding.stats;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class GeocodeLocationService extends Service {

    private static final String TAG = "GeocodeLocationService";

    // Minimum interval in ms between GPS location updates
    private static final int LOCATION_INTERVAL = 2500;
    // Distance threshold in meters, above which GPS location update will be fired
    private static final float LOCATION_DISTANCE = 10f;
    // Minimum Distance threshold in meters above which we will query geocoding (and update client)
    private static final float DISTANCE_THRESHOLD = 10f;
    // Geocoding thread sleep time in ms, which means minimum interval time between geocoding query
    private static final int GEOCODING_INTERVAL = 5000;
    // Start delay in ms of geocoding thread,
    // I think it's not needed to start thread just after starting service
    private static final int GEOCODING_DELAY = 250;

    private final IBinder mBinder = new LocalBinder();
    private IGeocodeResult mListener;
    private LocationManager mLocationManager = null;
    private Geocoder geocoder;
    private LocationListener[] mLocationListeners = new LocationListener[]{
            new LocationListener(LocationManager.GPS_PROVIDER),
            new LocationListener(LocationManager.NETWORK_PROVIDER)
    };
    private Timer geocodingTimer;
    private Location mLastLocation;
    private Location mLastDecodedLocation;

    public interface IGeocodeResult {
        void onNewGeocodeResult(Address result);
    }

    class LocalBinder extends Binder {
        GeocodeLocationService getService() {
            return GeocodeLocationService.this;
        }
    }

    private class LocationListener implements android.location.LocationListener {

        LocationListener(String provider) {
            Log.e(TAG, "LocationListener " + provider);
            mLastLocation = new Location(provider);
        }

        @Override
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: " + location);
            mLastLocation.set(location);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.e(TAG, "onProviderDisabled: " + provider);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.e(TAG, "onProviderEnabled: " + provider);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.e(TAG, "onStatusChanged: " + provider);
        }
    }

    public GeocodeLocationService() {
        super();
    }

    @Override
    public void onCreate() {
        Log.e(TAG, "onCreate");
        initializeLocationManager();
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[1]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "network provider does not exist, " + ex.getMessage());
        }
        try {
            mLocationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
                    mLocationListeners[0]);
        } catch (java.lang.SecurityException ex) {
            Log.i(TAG, "fail to request location update, ignore", ex);
        } catch (IllegalArgumentException ex) {
            Log.d(TAG, "gps provider does not exist " + ex.getMessage());
        }
        geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        initializeTimerTask();
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "onDestroy");
        geocodingTimer.cancel();
        if (mLocationManager != null) {
            for (LocationListener mLocationListener : mLocationListeners) {
                try {
                    mLocationManager.removeUpdates(mLocationListener);
                } catch (Exception ex) {
                    Log.i(TAG, "fail to remove location listners, ignore", ex);
                }
            }
        }
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_NOT_STICKY;
    }


    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
        return mBinder;
    }

    private void initializeLocationManager() {
        Log.e(TAG, "initializeLocationManager");
        if (mLocationManager == null) {
            mLocationManager = (LocationManager)
                    getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void initializeTimerTask() {
        if (geocodingTimer == null) {
            geocodingTimer = new Timer();
            geocodingTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (mLastDecodedLocation == null || mLastDecodedLocation.distanceTo(mLastLocation) > DISTANCE_THRESHOLD) {
                        try {
                            List<Address> addresses = geocoder.getFromLocation(
                                    mLastLocation.getLatitude(), mLastLocation.getLongitude(), 1);
                            if (mListener != null && addresses != null && addresses.size() > 0) {
                                mListener.onNewGeocodeResult(addresses.get(0));
                                // Only save if we successfully send result to client
                                mLastDecodedLocation = mLastLocation;
                            }
                        } catch (IOException e) {
                            Log.e(TAG, "Service Not Available");
                        }
                    }
                }
            }, GEOCODING_DELAY, GEOCODING_INTERVAL);
        }
    }

    public void setOnNewGeocodeListener(IGeocodeResult listener) {
        mListener = listener;
    }
}
