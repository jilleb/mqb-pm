package com.mqbcoding.stats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.anastr.speedviewlib.Gauge;
import com.github.anastr.speedviewlib.Speedometer;
import com.github.anastr.speedviewlib.components.Indicators.ImageIndicator;
import com.github.martoreto.aauto.vex.CarStatsClient;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GraphFragment extends CarFragment {
    private final String TAG = "GraphFragment";

    private CarStatsClient mStatsClient;
    private GraphView mGraph;
    private BarGraphSeries<DataPoint> mSpeedSeries;
    private double graphLastXValue = 5d;
    private Speedometer mClockGraph;
    private TextView mClockIcon;
    private String mGraphQuery;
    private Boolean pressureUnits;
    private int pressureMin, pressureMax;
    private String pressureUnit;
    private float pressureFactor, speedFactor;
    private int graphDelay;
    private Runnable mTimer1;


    private Map<String, Object> mLastMeasurements = new HashMap<>();
    private Handler mHandler = new Handler();
    private final CarStatsClient.Listener mCarStatsListener = new CarStatsClient.Listener() {
        @Override
        public void onNewMeasurements(String provider, Date timestamp, Map<String, Object> values) {
            mLastMeasurements.putAll(values);
            postUpdate();
        }
    };

    /*
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
    */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CarStatsService.CarStatsBinder carStatsBinder = (CarStatsService.CarStatsBinder) iBinder;
            mStatsClient = carStatsBinder.getStatsClient();
            mLastMeasurements = mStatsClient.getMergedMeasurements();
            mStatsClient.registerListener(mCarStatsListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mStatsClient.unregisterListener(mCarStatsListener);
        }
    };

    public GraphFragment() {
        // Required empty public constructor
    }

    public static float randFloat(float min, float max) {

        Random rand = new Random();

        float result = rand.nextFloat() * (max - min) + min;

        return result;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");


        Intent serviceIntent = new Intent(getContext(), CarStatsService.class);
        getContext().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");

        setTitle(getContext().getString(R.string.activity_graph_title));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");

        View rootView = inflater.inflate(R.layout.fragment_graph, container, false);
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.themedNeedle});
        int resourceId = typedArray.getResourceId(0, 0);
        typedArray.recycle();

        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "digital.ttf");
        ImageIndicator imageIndicator = new ImageIndicator(getContext(), resourceId, 200, 200);

        mClockGraph = rootView.findViewById(R.id.dial_graph);
        mClockGraph.setIndicator(imageIndicator);
        mClockGraph.setSpeedTextTypeface(typeface);
        mClockIcon = rootView.findViewById(R.id.icon_GraphClock);

        //Get shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        pressureUnits = sharedPreferences.getBoolean("selectPressureUnit", true);  //true = bar, false = psi
        graphDelay = Integer.parseInt(sharedPreferences.getString("graphDelay", "1000"));

        //determine what data the user wants to have on the graph and clock.
        mGraphQuery = sharedPreferences.getString("selectedGraphItem", "vehicleSpeed");
        Log.d(TAG, "Graphing element selected:" + mGraphQuery);

        //Setup the graph and series
        mGraph = rootView.findViewById(R.id.graph);
        mSpeedSeries = new BarGraphSeries<>();
        mGraph.addSeries(mSpeedSeries);
        mGraph.getViewport().setXAxisBoundsManual(true);
        mGraph.getViewport().setYAxisBoundsManual(true);
        mGraph.getViewport().setMinX(0);
        // set default max and min, these will be set dynamically later
        mGraph.getViewport().setMaxX(120);
        mGraph.getViewport().setMaxY(200);
        mGraph.getViewport().setMinY(0);
        //Only show horizontal lines on the grid
        mGraph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        mSpeedSeries.setDataWidth(1);

        mGraph.getViewport().setBackgroundColor(Color.argb(150, 0,0,0));
        mSpeedSeries.setColor(Color.argb(80, 255, 255, 255));

        pressureMin = -2;
        pressureMax = 3;
        pressureFactor = 1;

        //set pressure dial to the wanted units
        //Most bar dials go from -2 to 3 bar.
        //Most PSI dials go from -30 to 30 psi.
        //pressurefactor is used to calculate the right value for psi later
        if (pressureUnits) {
            pressureFactor = 1;
            pressureUnit = "bar";
            pressureMin = -2;
            pressureMax = 3;

        } else {
            pressureFactor = (float) 14.5037738;
            pressureUnit = "psi";
            pressureMin = -30;
            pressureMax = 30;
        }

        setupClock(mGraphQuery);


        doUpdate();

        return rootView;
    }

    private void setupClock(String query) {

        if (query == null) {
            query = "test";
        }
        switch (query) {
            case "none":
                mClockIcon.setText("");
                mClockGraph.setUnit("");
                mClockIcon.setBackgroundResource(0);
                mGraph.setTitle("No data selected");
                break;
            case "test":
                mClockIcon.setText("");
                mClockGraph.setUnit("testing");
                mClockGraph.setMinMaxSpeed(-100, 200);
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_measurement));
                mGraph.setTitle(getContext().getString(R.string.test));
                break;
            case "vehicleSpeed":
                mClockIcon.setText("");
                mClockGraph.setUnit(getContext().getText(R.string.kmh).toString());
                mClockGraph.setMinMaxSpeed(0, 350);
                mClockIcon.setBackgroundResource(0);
                mClockGraph.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.vehicleSpeed));
                break;
            case "engineSpeed":
                mClockIcon.setText("");
                mClockGraph.setUnit(getContext().getText(R.string.rpm).toString());
                mClockGraph.setMinMaxSpeed(0, 8000);
                mClockGraph.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                mClockIcon.setBackgroundResource(0);
                mGraph.setTitle(getContext().getString(R.string.engineSpeed));
                break;
            case "batteryVoltage":
                mClockIcon.setText("");
                mClockGraph.setUnit(getContext().getText(R.string.volt).toString());
                mClockGraph.setMinMaxSpeed(0, 15);
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_battery));
                mGraph.setTitle(getContext().getString(R.string.batteryVoltage));
                break;
            case "oilTemperature":
                mClockIcon.setText("");
                mClockGraph.setUnit("°C");
                mClockGraph.setMinMaxSpeed(0, 150);
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_oil));
                mGraph.setTitle(getContext().getString(R.string.oilTemperature));
                break;
            case "coolantTemperature":
                mClockIcon.setText("");
                mClockGraph.setUnit("°C");
                mClockGraph.setMinMaxSpeed(0, 150);
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_water));
                mGraph.setTitle(getContext().getString(R.string.coolantTemperature));
                break;
            case "gearboxOilTemperature":
                mClockIcon.setText("");
                mClockGraph.setUnit("°C");
                mClockGraph.setMinMaxSpeed(0, 150);
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                mGraph.setTitle(getContext().getString(R.string.gearboxOilTemperature));
                break;
            case "absChargingAirPressure":
                mClockIcon.setText("");
                mClockGraph.setUnit(pressureUnit);
                mClockGraph.setMinMaxSpeed(pressureMin, pressureMax);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_turbo));
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.absChargingAirPressure));
                break;
            case "relChargingAirPressure":
                mClockIcon.setText("");
                mClockGraph.setUnit(pressureUnit);
                mClockGraph.setMinMaxSpeed(pressureMin, pressureMax);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_turbo));
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.relChargingAirPressure));
                break;
            case "lateralAcceleration":
                mClockIcon.setText("");
                mClockGraph.setUnit(getContext().getText(R.string.g).toString());
                mClockGraph.setMinMaxSpeed(-2, 2);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_lateral));
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.lateralAcceleration));

                break;
            case "longitudinalAcceleration":
                mClockIcon.setText("");
                mClockGraph.setUnit(getContext().getText(R.string.g).toString());
                mClockGraph.setMinMaxSpeed(-2, 2);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_longitudinal));
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.longitudinalAcceleration));

                break;
            case "yawRate":
                mClockIcon.setText("");
                mClockGraph.setUnit("%");
                mClockGraph.setMinMaxSpeed(-1, 1);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_yaw));
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.yawRate));
                break;
            case "wheelAngle":
                mClockIcon.setText("");
                mClockGraph.setUnit("°");
                mClockGraph.setMinMaxSpeed(-45, 45);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_wheelangle));
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.wheelAngle));
                break;
            case "EcoHMI_Score.AvgShort":
                mClockIcon.setText("");
                mClockGraph.setUnit("");
                mClockGraph.setMinMaxSpeed(0, 100);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                mClockGraph.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.EcoHMI_Score_AvgShort));
                break;
            case "EcoHMI_Score.AvgTrip":
                mClockIcon.setText("");
                mClockGraph.setUnit("");
                mClockGraph.setMinMaxSpeed(0, 100);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                mClockGraph.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.EcoHMI_Score_AvgTrip));
                break;
            case "powermeter":
                mClockIcon.setText("");
                mClockGraph.setUnit("");
                mClockGraph.setMinMaxSpeed(0, 2000);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_powermeter));
                mClockGraph.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.powermeter));
                break;
            case "acceleratorPosition":
                mClockIcon.setText("");
                mClockGraph.setUnit("%");
                mClockGraph.setMinMaxSpeed(0, 100);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_pedalposition));
                mClockGraph.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.acceleratorPosition));
                break;
            case "brakePressure":
                mClockIcon.setText("");
                mClockGraph.setUnit("%");
                mClockGraph.setMinMaxSpeed(0, 100);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_brakepedalposition));
                mClockGraph.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.brakePressure));
                break;
            case "currentTorque":
                mClockIcon.setText("");
                mClockGraph.setUnit(getContext().getText(R.string.nm).toString());
                mClockGraph.setMinMaxSpeed(0, 500);
                mClockIcon.setBackgroundResource(0);
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.currentTorque));
                break;
            case "currentOutputPower":
                mClockIcon.setText("");
                mClockGraph.setUnit(getContext().getText(R.string.kw).toString());
                mClockGraph.setMinMaxSpeed(0, 500);
                mClockIcon.setBackgroundResource(0);
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.currentOutputPower));
                break;
            case "currentConsumptionPrimary":
                mClockIcon.setText("");
                mClockGraph.setUnit("l/h");
                mClockGraph.setMinMaxSpeed(0, 100);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_fuelprimary));
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.currentConsumptionPrimary));
                break;
            case "currentConsumptionSecondary":
                mClockIcon.setText("");
                mClockGraph.setUnit("l/h");
                mClockGraph.setMinMaxSpeed(0, 100);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_fuelsecondary));
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.currentConsumptionSecondary));
                break;
            case "cycleConsumptionPrimary":
                mClockIcon.setText("");
                mClockGraph.setUnit("l/h");
                mClockGraph.setMinMaxSpeed(0, 100);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_fuelprimary));
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.cycleConsumptionPrimary));
                break;
            case "cycleConsumptionSecondary":
                mClockIcon.setText("");
                mClockGraph.setUnit("l/h");
                mClockGraph.setMinMaxSpeed(0, 100);
                mClockIcon.setBackground(getContext().getDrawable(R.drawable.ic_fuelsecondary));
                mClockGraph.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                mGraph.setTitle(getContext().getString(R.string.cycleConsumptionSecondary));
                break;
        }

        float tempMax = mClockGraph.getMaxSpeed();
        float tempMin = mClockGraph.getMinSpeed();
        mGraph.getViewport().setMaxY(tempMax);
        mGraph.getViewport().setMinY(tempMin);
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

        mTimer1 = new Runnable() {
            @Override
            public void run() {

                Float temp = mClockGraph.getCurrentSpeed();
                if (temp != null) {
                    graphLastXValue += 1d;
                    mSpeedSeries.appendData(new DataPoint(graphLastXValue, temp), true, 4000);
                    mHandler.postDelayed(this, graphDelay);
                }
            }
        };
        mHandler.postDelayed(mTimer1, 1000);
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onDeactivate");
        mHandler.removeCallbacks(mTimer1);

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

    private void postUpdate() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                doUpdate();

            }

        }, 1000);
    }

    private void doUpdate() {
        if (mGraphQuery != null) {
            updateClock(mGraphQuery);
        }
        postUpdate();
    }

    private void updateClock(String query) {

        Speedometer dial = mClockGraph;
        Float clockValue = (Float) mLastMeasurements.get(query);

        float randomClockVal = randFloat(-100, 200);
        speedFactor = 1f;
        pressureFactor = 1f;

        if (query == null) {
            query = "test";
        }

        switch (query) {
            case "test":
                dial.speedTo(randomClockVal);
                break;
            case "none":    // none cannot happen currently
                break;
            // all data that can be put on the clock without further modification
            case "engineSpeed":
            case "batteryVoltage":
            case "oilTemperature":
            case "coolantTemperature":
            case "gearboxOilTemperature":
            case "lateralAcceleration":
            case "longitudinalAcceleration":
            case "yawRate":
            case "EcoHMI_Score.AvgShort":
            case "EcoHMI_Score.AvgTrip":
            case "powermeter":
            case "brakePressure":
            case "currentTorque":
            case "currentOutputPower":
                if (clockValue != null) {
                    dial.speedTo(clockValue);
                }
                break;
            // pressures
            case "absChargingAirPressure":
            case "relChargingAirPressure":
                if (clockValue != null) {
                    clockValue = clockValue * pressureFactor;
                    dial.speedTo(clockValue);
                }
                break;
            // specific case for wheel angle, since it needs to be turned around
            case "wheelAngle":
                if (clockValue != null) {
                    clockValue = clockValue * -1; // make it negative, otherwise right = left and vice versa
                    dial.speedTo(clockValue);
                }
                break;
            // percentages
            case "acceleratorPosition":
                if (clockValue != null) {
                    float accelPercent = clockValue * 100;
                    dial.speedTo(accelPercent);
                }
                break;
            // specific consumption data with specific consumption units
            // todo: maybe it's better to remove setting the unit from updateclock, but do it on setupclock
            case "currentConsumptionPrimary":
                String consumptionUnit = (String) mLastMeasurements.get("currentConsumptionPrimary.unit");
                if (clockValue != null && consumptionUnit != null) {
                    dial.setUnit(consumptionUnit);
                    dial.speedTo(clockValue);
                }
                break;
            case "currentConsumptionSecondary":
                String consumption2Unit = (String) mLastMeasurements.get("currentConsumptionSecondary.unit");
                if (clockValue != null && consumption2Unit != null) {
                    dial.setUnit(consumption2Unit);
                    dial.speedTo(clockValue);
                }
                break;
            case "cycleConsumptionPrimary":
                String cycconsumptionUnit = (String) mLastMeasurements.get("cycleConsumptionPrimary.unit");
                if (clockValue != null && cycconsumptionUnit != null) {
                    dial.setUnit(cycconsumptionUnit);
                    dial.speedTo(clockValue);
                }
                break;
            case "cycleConsumptionSecondary":
                String cycconsumption2Unit = (String) mLastMeasurements.get("cycleConsumptionSecondary.unit");
                if (clockValue != null && cycconsumption2Unit != null) {
                    dial.setUnit(cycconsumption2Unit);
                    dial.speedTo(clockValue);
                }
                break;
            // speed, has specific unit requirements and mph calculation
            case "vehicleSpeed":
                String speedUnit = (String) mLastMeasurements.get("vehicleSpeed.unit");
                if (clockValue != null && speedUnit != null) {
                    switch (speedUnit) {
                        case "mph":
                            speedFactor = 1.60934f;
                            dial.setUnit("mph");
                            break;
                        case "kmh":
                            speedFactor = 1f;
                            dial.setUnit("kmh");
                            break;
                    }
                    clockValue = clockValue * speedFactor;
                    dial.speedTo(clockValue);
                }
                break;
        }
    }

}