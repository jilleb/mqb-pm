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
    private GraphView mGraph;
    private LineGraphSeries<DataPoint> mSpeedSeries;
    private double graphLastXValue =5d;

    private String mGraphQuery;

    private static final float DISABLED_ALPHA = 0.8f;

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

//

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        //determine what data the user wants to have on the 3 clocks.
        mGraphQuery = sharedPreferences.getString("selectedGraphItem", "vehicleSpeed");
        // todo: add code to set min/max for the chosen data element
        // todo: make sure the title is nice
        // todo: styling
        mGraph = rootView.findViewById(R.id.graph);
        mSpeedSeries = new LineGraphSeries<>();
        mGraph.addSeries(mSpeedSeries);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinX(0);
        mGraph.getViewport().setMaxX(120);
        mGraph.getViewport().setMaxY(200);
        mGraph.getViewport().setMinY(0);
        mGraph.setTitle(mGraphQuery);
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
               doUpdate();

            }

        }, 500 );

    }

    private void doUpdate() {

        float randomClockVal = randFloat(0,200);
        Float lastSpeed = (Float) mLastMeasurements.get("vehicleSpeed");
        //if (lastSpeed != null){
            graphLastXValue += 1d;
            mSpeedSeries.appendData(new DataPoint(graphLastXValue, randomClockVal), true, 120);

      //  }

            postUpdate();
        }

    public static float randFloat(float min, float max) {

        Random rand = new Random();

        float result = rand.nextFloat() * (max - min) + min;

        return result;

    }

}
