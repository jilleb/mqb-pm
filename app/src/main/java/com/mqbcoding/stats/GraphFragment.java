package com.mqbcoding.stats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.anastr.speedviewlib.ImageLinearGauge;
import com.github.anastr.speedviewlib.SpeedView;
import com.github.anastr.speedviewlib.Speedometer;
import com.github.anastr.speedviewlib.components.Indicators.ImageIndicator;
import com.github.martoreto.aauto.vex.CarStatsClient;
import com.google.android.apps.auto.sdk.DayNightStyle;
import com.google.android.apps.auto.sdk.StatusBarController;
import com.google.common.collect.ImmutableMap;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class GraphFragment extends CarFragment {
    private final String TAG = "GraphFragment";

    private CarStatsClient mStatsClient;
    private TextView mLastSpeedKmh;
    private GraphView mGraph;
    private LineGraphSeries<DataPoint> mSpeedSeries;
    private double graphLastXValue =5d;
    private SpeedView mSpeedView;

    private static final float DISABLED_ALPHA = 0.8f;

    private Speedometer mOilTemp; //used to be mOutputPower
    private int mAnimationDuration;
    private int lastSpeed;


    public static final float FULL_BRAKE_PRESSURE = 100.0f;

    private Map<String, Object> mLastMeasurements = new HashMap<>();
    private Handler mHandler = new Handler();

    public GraphFragment() {
        // Required empty public constructor
    }

    //@Override
    protected void setupStatusBar(StatusBarController sc) {
        sc.setDayNightStyle(DayNightStyle.FORCE_NIGHT);
        sc.showAppHeader();
        sc.hideBatteryLevel();
        sc.showClock();
        sc.hideConnectivityLevel();
        sc.showMicButton();
        sc.showTitle();
        sc.setTitle("Graphs");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");


        Intent serviceIntent = new Intent(getContext(), CarStatsService.class);
        getContext().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        mAnimationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CarStatsService.CarStatsBinder carStatsBinder = (CarStatsService.CarStatsBinder)iBinder;
            mStatsClient = carStatsBinder.getStatsClient();
            mLastMeasurements = mStatsClient.getMergedMeasurements();
            mStatsClient.registerListener(mCarStatsListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mStatsClient.unregisterListener(mCarStatsListener);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_graph, container, false);


        mLastSpeedKmh = rootView.findViewById(R.id.txtGraphCurrentSpeed);

        mGraph = rootView.findViewById(R.id.graph);
        mSpeedSeries = new LineGraphSeries<>();
        mGraph.addSeries(mSpeedSeries);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinX(0);
        mGraph.getViewport().setMaxX(1000);
        mGraph.getViewport().setMaxY(200);
        mGraph.getViewport().setMinY(0);

        mSpeedView = rootView.findViewById(R.id.speedTest);

        mSpeedView.speedTo(300,60000);



        doUpdate();

        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onActivate");
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onDeactivate");
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "onDestroyView");



        super.onDestroyView();
    }

    @Override
    public void onStop() {
        Log.i(TAG, "onStop");
        super.onStop();
    }

    @Override
    public void onDetach() {
        Log.i(TAG, "onDetach");

        mStatsClient.unregisterListener(mCarStatsListener);
        getContext().unbindService(mServiceConnection);

        super.onDetach();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }

    private final CarStatsClient.Listener mCarStatsListener = new CarStatsClient.Listener() {
        @Override
        public void onNewMeasurements(String provider, Date timestamp, Map<String, Object> values) {
            mLastMeasurements.putAll(values);
            postUpdate();
        }
    };

    private void postUpdate() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                graphLastXValue += 1d;
                mSpeedSeries.appendData(new DataPoint(graphLastXValue, lastSpeed), true, 60);

                doUpdate();

            }

        }, 500 );

    }

    private void doUpdate() {

        Float lastSpeed = (Float) mLastMeasurements.get("vehicleSpeed");
        String speedUnit = (String) mLastMeasurements.get("vehicleSpeed.unit");
        if (lastSpeed != null && speedUnit != null) {
            switch (speedUnit) {
                case "mph":
                    lastSpeed *= 1.60934f;
                    break;
            }
            //lastSpeed = mSpeedView.getCurrentIntSpeed();


//        String speedUnit = "kmh";

            //    String speedtext = Integer.toString(lastSpeed);
            //  mLastSpeedKmh.setText(speedtext + speedUnit);

            // mSpeedSeries.appendData(new DataPoint(graphLastXValue, lastSpeed), true, 10000);

            postUpdate();
        }
    }





/*
        Float oilTemp = (Float) mLastMeasurements.get("oilTemperature");
        if (oilTemp == null) {
            mOilTempTxt.setText("----");
        } else {
            mOilTempTxt.setText(String.format(Locale.US,
                    getContext().getText(R.string.temp_format).toString(), oilTemp));
        }

        //get the oilTemperature unit (degrees C or F)
        String oilTempUnit = (String) mLastMeasurements.get("oilTemperature_unit");
        if (oilTempUnit == null) {
            mOilTemp.setUnit("");
        }else{
            mOilTemp.setUnit(oilTempUnit);

        }

        Float currentOilTemp = (Float) mLastMeasurements.get("oilTemperature");
        if (currentOilTemp == null){
            mOilTemp.speedTo(0);
        } else {
            mOilTemp.speedTo(currentOilTemp == null ? 0.0f : currentOilTemp);
        }
        //


        Float currentChargingPressure = (Float) mLastMeasurements.get(pressureValueQuery);
        if (currentChargingPressure == null){

        } else {
            currentChargingPressure = currentChargingPressure * pressureFactor;
            mChargingPressure.speedTo(currentChargingPressure == null ? 0.0f : currentChargingPressure);
        }


        Float currentOutputTorque = (Float) mLastMeasurements.get("currentTorque");
        mOutputTorque.speedTo(currentOutputTorque == null ? 0.0f : currentOutputTorque);

        Float brakePressure = (Float) mLastMeasurements.get("brakePressure");
        Float accelPos = (Float) mLastMeasurements.get("acceleratorPosition");

        if (brakePressure != null && accelPos != null) {
            float normalizedBrakePressure = Math.min(Math.max(0.0f, brakePressure / FULL_BRAKE_PRESSURE), 1.0f);
            boolean isBraking = normalizedBrakePressure > 0;
            mBrakeAccel.setRotation(isBraking ? 180.0f : 0.0f);
            //noinspection deprecation
            mBrakeAccel.setProgressTintList(ColorStateList.valueOf(getContext().getResources()
                    .getColor(isBraking ? R.color.car_accent: R.color.car_primary)));
            mBrakeAccel.setProgress((int) ((isBraking ? normalizedBrakePressure : accelPos) * 10000));
        } else {
            mBrakeAccel.setProgress(0);
        }

        // Footer

        Boolean reverseGear = (Boolean) mLastMeasurements.get("reverseGear.engaged");
        Boolean parkingBrake = (Boolean) mLastMeasurements.get("parkingBrake.engaged");
        String currentGear = (String) mLastMeasurements.get("currentGear");
        if (parkingBrake != null && parkingBrake) {
            currentGear = "Park";
        } else if (reverseGear != null && reverseGear) {
            currentGear = "Reverse";
        }
        for (ImmutableMap.Entry<String, View> gear : mGearViews.entrySet()) {
            gear.getValue().setSelected(currentGear != null && currentGear.equals(gear.getKey()));
        }

        // Right panel

        Float oilTempT = (Float) mLastMeasurements.get("oilTemperature");
        if (oilTempT == null) {
            mOilTempTxt.setText("----");
        } else {
            mOilTempTxt.setText(String.format(Locale.US,
                    getContext().getText(R.string.temp_format).toString(), oilTempT));
        }

        Float gearboxTemp = (Float) mLastMeasurements.get("gearboxOilTemperature");
        if (gearboxTemp == null) {
            mGearboxTemp.setText("----");
        } else {
            mGearboxTemp.setText(String.format(Locale.US,
                    getContext().getText(R.string.temp_format).toString(), gearboxTemp));
        }

        Float batteryVoltage = (Float) mLastMeasurements.get("batteryVoltage");
        if (batteryVoltage == null) {
            mBatteryVoltage.setText("----");
        } else {
            mBatteryVoltage.setText(String.format(Locale.US,
                    getContext().getText(R.string.volt_format).toString(), batteryVoltage));
        }

        // Last speed

        Float lastSpeed = (Float) mLastMeasurements.get("vehicleSpeed");
        String speedUnit = (String) mLastMeasurements.get("vehicleSpeed.unit");
        if (lastSpeed != null && speedUnit != null) {
            switch (speedUnit) {
                case "mph":
                    lastSpeed *= 1.60934f;
                    break;
            }
        }
        mLastSpeedKmh = lastSpeed;

        // Steering wheel angle

        Float currentWheelAngle = (Float) mLastMeasurements.get("wheelAngle");
        mWheelState = mWheelStateMonitor == null ? WheelStateMonitor.WheelState.WHEEL_UNKNOWN
                : mWheelStateMonitor.getWheelState();
        mSteeringWheelAngle.setRotation(currentWheelAngle == null ? 0.0f :
                Math.min(Math.max(-WheelStateMonitor.WHEEL_CENTER_THRESHOLD_DEG, -currentWheelAngle),
                        WheelStateMonitor.WHEEL_CENTER_THRESHOLD_DEG));

        animateAlpha(mOilTemp, currentOilTemp == null ? DISABLED_ALPHA : 1.0f);
        animateAlpha(mOutputTorque, currentOutputTorque == null ? DISABLED_ALPHA : 1.0f);
        animateAlpha(mBrakeAccel, brakePressure == null || accelPos == null ? DISABLED_ALPHA : 1.0f);
        for (ImmutableMap.Entry<String, View> gear : mGearViews.entrySet()) {
            animateAlpha(gear.getValue(), currentGear == null ? DISABLED_ALPHA : 1.0f);
        }
        animateAlpha(mSteeringWheelAngle, mWheelState == WheelStateMonitor.WheelState.WHEEL_DRIVING
                || mWheelState == WheelStateMonitor.WheelState.WHEEL_UNKNOWN ? 0.0f : 1.0f);
        animateAlpha(mChargingPressure, mWheelState != WheelStateMonitor.WheelState.WHEEL_DRIVING
                && mWheelState != WheelStateMonitor.WheelState.WHEEL_UNKNOWN ? 0.0f :
                (currentChargingPressure == null ? DISABLED_ALPHA : 1.0f));
                */



    private void animateAlpha(View view, float alpha) {
        if (view.getAlpha() == alpha) {
            return;
        }
        if (isVisible()) {
            view.animate().alpha(alpha).setDuration(mAnimationDuration).setListener(null);
        } else {
            view.setAlpha(alpha);
        }
    }


}
