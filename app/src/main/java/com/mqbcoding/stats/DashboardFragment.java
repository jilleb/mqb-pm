package com.mqbcoding.stats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.anastr.speedviewlib.Gauge;
import com.github.anastr.speedviewlib.RaySpeedometer;
import com.github.anastr.speedviewlib.Speedometer;
import com.github.anastr.speedviewlib.components.Indicators.ImageIndicator;
import com.github.anastr.speedviewlib.components.Indicators.Indicator;
import com.github.martoreto.aauto.vex.CarStatsClient;
import com.google.android.apps.auto.sdk.DayNightStyle;
import com.google.android.apps.auto.sdk.StatusBarController;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class DashboardFragment extends CarFragment {
    private final String TAG = "DashboardFragment";

    private CarStatsClient mStatsClient;
    private WheelStateMonitor mWheelStateMonitor;

    private static final float DISABLED_ALPHA = 1.0f;
    private ProgressBar mBrakeAccel;
    private Speedometer mClockLeft, mClockCenter, mClockRight;
    private Speedometer mClockMaxLeft, mClockMaxCenter, mClockMaxRight;
    private Speedometer mClockMinLeft, mClockMinCenter, mClockMinRight;
    private ImageView mImageMaxLeft, mImageMaxCenter, mImageMaxRight;
    private RaySpeedometer mRayLeft, mRayCenter, mRayRight;
    private ImageView mSteeringWheelAngle;

    private String mElement1Query, mElement2Query, mElement3Query, mElement4Query;
    private String mClockLQuery, mClockCQuery, mClockRQuery;
    private String pressureUnit;
    private float pressureFactor, speedFactor;
    private float mLeftMax,mCenterMax,mRightMax;
    private float mLeftMin,mCenterMin,mRightMin;
    private int pressureMin, pressureMax;

    //icons/labels of the data elements. upper left, upper right, lower left, lower right.
    private TextView mIconElement1, mIconElement2,mIconElement3,mIconElement4;

    //valuesof the data elements. upper left, upper right, lower left, lower right.
    private TextView mValueElement1, mValueElement2,mValueElement3,mValueElement4;

    private TextView mTextMinLeft, mTextMaxLeft;
    private TextView mTextMinCenter, mTextMaxCenter;
    private TextView mTextMinRight, mTextMaxRight;

    //icons on the clocks
    private TextView mIconClockL, mIconClockC,mIconClockR;


    private View.OnClickListener celebrateOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            postUpdate();
        }
    };



    private int mAnimationDuration;
    private WheelStateMonitor.WheelState mWheelState;
    private Boolean pressureUnits;
    private Boolean raysOn, maxOn;
    public static final float FULL_BRAKE_PRESSURE = 100.0f;
    private Map<String, Object> mLastMeasurements = new HashMap<>();
    private Handler mHandler = new Handler();
    public DashboardFragment() {
        // Required empty public constructor
    }

    //@Override
    protected void setupStatusBar(StatusBarController sc) {
        sc.setDayNightStyle(DayNightStyle.FORCE_NIGHT);
        sc.showAppHeader();
        sc.hideBatteryLevel();
        sc.showClock();
        sc.hideConnectivityLevel();
        sc.showTitle();
        sc.setTitle(getContext().getString(R.string.dashboard_title));
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
            mWheelStateMonitor = carStatsBinder.getWheelStateMonitor();
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
        View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        //this is to enable an image as indicator.
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[] { R.attr.themedNeedle });
        int resourceId = typedArray.getResourceId(0, 0);
        typedArray.recycle();

        //set textview to have a custom digital font:
        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "digital.ttf");

        // build ImageIndicator using the resourceId
        ImageIndicator imageIndicator = new ImageIndicator(getContext(), resourceId, 200,200);

        //-------------------------------------------------------------
        //find all elements needed
        //clocks:
        mClockLeft = rootView.findViewById(R.id.dial_Left);
        mClockCenter = rootView.findViewById(R.id.dial_Center);
        mClockRight = rootView.findViewById(R.id.dial_Right);
        //give clocks a custom image indicator
        mClockLeft.setIndicator(imageIndicator);
        mClockCenter.setIndicator(imageIndicator);
        mClockRight.setIndicator(imageIndicator);

        //max & min dials
        mClockMaxLeft = rootView.findViewById(R.id.dial_MaxLeft);
        mClockMaxCenter = rootView.findViewById(R.id.dial_MaxCenter);
        mClockMaxRight = rootView.findViewById(R.id.dial_MaxRight);
        mClockMinLeft = rootView.findViewById(R.id.dial_MinLeft);
        mClockMinCenter = rootView.findViewById(R.id.dial_MinCenter);
        mClockMinRight = rootView.findViewById(R.id.dial_MinRight);

        //set max/min values to 0
        mLeftMax = 0;
        mCenterMax = 0;
        mRightMax=0;
        mLeftMin = 0;
        mCenterMin = 0;
        mRightMin=0;

        //icons on the clocks
        mIconClockL= rootView.findViewById(R.id.icon_ClockLeft);
        mIconClockC = rootView.findViewById(R.id.icon_ClockCenter);
        mIconClockR = rootView.findViewById(R.id.icon_ClockRight);

        //ray speedometers for high visibility
        mRayLeft = rootView.findViewById(R.id.rayLeft);
        mRayCenter = rootView.findViewById(R.id.rayCenter);
        mRayRight = rootView.findViewById(R.id.rayRight);

        //the 4 additional dashboard "text" elements:
        mValueElement1 = rootView.findViewById(R.id.value_Element1);
        mValueElement2 = rootView.findViewById(R.id.value_Element2);
        mValueElement3 = rootView.findViewById(R.id.value_Element3);
        mValueElement4 = rootView.findViewById(R.id.value_Element4);

        //labels at these text elements:
        mIconElement1 = rootView.findViewById(R.id.icon_Element1);
        mIconElement2 = rootView.findViewById(R.id.icon_Element2);
        mIconElement3 = rootView.findViewById(R.id.icon_Element3);
        mIconElement4 = rootView.findViewById(R.id.icon_Element4);

        //minmax texts:
        mTextMaxLeft = rootView.findViewById(R.id.textMaxLeft);
        mTextMaxCenter = rootView.findViewById(R.id.textMaxCenter);
        mTextMaxRight = rootView.findViewById(R.id.textMaxRight);
        mTextMinLeft = rootView.findViewById(R.id.textMinLeft);
        mTextMinCenter = rootView.findViewById(R.id.textMinCenter);
        mTextMinRight = rootView.findViewById(R.id.textMinRight);

        //minmax backgrounds:
        mImageMaxLeft = rootView.findViewById(R.id.image_MaxLeft);
        mImageMaxCenter = rootView.findViewById(R.id.image_MaxCenter);
        mImageMaxRight = rootView.findViewById(R.id.image_MaxRight);

        //-------------------------------------------------------------
        //Give them all the right custom typeface
        //clocks
        mClockLeft.setSpeedTextTypeface(typeface);
        mClockCenter.setSpeedTextTypeface(typeface);
        mClockRight.setSpeedTextTypeface(typeface);
        //elements
        mValueElement1.setTypeface(typeface);
        mValueElement2.setTypeface(typeface);
        mValueElement3.setTypeface(typeface);
        mValueElement4.setTypeface(typeface);

        //max
        mTextMinLeft.setTypeface(typeface);
        mTextMaxLeft.setTypeface(typeface);
        mTextMinCenter.setTypeface(typeface);
        mTextMaxCenter.setTypeface(typeface);
        mTextMinRight.setTypeface(typeface);
        mTextMaxRight.setTypeface(typeface);

        //additional dashboard elements:
        mBrakeAccel = rootView.findViewById(R.id.brake_accel_view);
        mSteeringWheelAngle = rootView.findViewById(R.id.wheel_angle_image);

        //
        mClockCenter.setOnClickListener(celebrateOnClickListener);

        //-------------------------------------------------------------
        //Get shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        pressureUnits   = sharedPreferences.getBoolean("selectPressureUnit", true);  //true = bar, false = psi
        raysOn          = sharedPreferences.getBoolean("highVisActive", false);  //true = show high vis rays, false = don't show them.
        maxOn           = sharedPreferences.getBoolean("maxValuesActive", false); //true = show max values, false = hide them

        //determine what data the user wants to have on the 4 data views
        mElement1Query = sharedPreferences.getString("selectedView1", "none");
        mElement2Query = sharedPreferences.getString("selectedView2", "none");
        mElement3Query = sharedPreferences.getString("selectedView3", "none");
        mElement4Query = sharedPreferences.getString("selectedView4", "none");

        //determine what data the user wants to have on the 3 clocks.
        mClockLQuery = sharedPreferences.getString("selectedClockLeft", "batteryVoltage");
        mClockCQuery = sharedPreferences.getString("selectedClockCenter", "oilTemperature");
        mClockRQuery = sharedPreferences.getString("selectedClockRight", "engineSpeed");

        //debug logging of each of the chosen elements
        Log.d(TAG, "element 1 selected:" + mElement1Query);
        Log.d(TAG, "element 2 selected:" + mElement2Query);
        Log.d(TAG, "element 3 selected:" + mElement3Query);
        Log.d(TAG, "element 4 selected:" + mElement4Query);

        Log.d(TAG, "clock l selected:" + mClockLQuery);
        Log.d(TAG, "clock c selected:" + mClockCQuery);
        Log.d(TAG, "clock r selected:" + mClockRQuery);

        //set default min/max pressures. Is this still used? Not sure.
        pressureMin = -2;
        pressureMax = 3;
        pressureFactor = 1;

        //set pressure dial to the wanted units
        //Most bar dials go from -2 to 3 bar.
        //Most PSI dials go from -30 to 30 psi.
        //pressurefactor is used to calculate the right value for psi later
        if (pressureUnits == true){
            pressureFactor = 1;
            pressureUnit = "bar";
            pressureMin = -2;
            pressureMax= 3;

        } else {
            pressureFactor = (float) 14.5037738;
            pressureUnit = "psi";
            pressureMin = -30;
            pressureMax= 30;
        }

        //set up each of the elements with the query and icon that goes with it
        setupElement(mElement1Query, mValueElement1 , mIconElement1);
        setupElement(mElement2Query, mValueElement2 , mIconElement2);
        setupElement(mElement3Query, mValueElement3 , mIconElement3);
        setupElement(mElement4Query, mValueElement4 , mIconElement4);

        //setup clocks, including the max/min clocks and highvis rays and icons:
        //usage: setupClock(query value, what clock, what icon, which ray, which min clock, which max clock)
        //could probably be done MUCH more efficient but that's for the future ;)
        setupClock(mClockLQuery, mClockLeft, mIconClockL, mRayLeft, mClockMinLeft, mClockMaxLeft);
        setupClock(mClockCQuery, mClockCenter, mIconClockC, mRayCenter, mClockMinCenter, mClockMaxCenter);
        setupClock(mClockRQuery, mClockRight, mIconClockR, mRayRight, mClockMinRight, mClockMaxRight);

        //show high visible rays on, according to the setting
        if (raysOn==true){
            mRayLeft.setVisibility(View.VISIBLE);
            mRayCenter.setVisibility(View.VISIBLE);
            mRayRight.setVisibility(View.VISIBLE);

            //also hide the needle on the clocks
            mClockLeft.setIndicator(Indicator.Indicators.NoIndicator);
            mClockCenter.setIndicator(Indicator.Indicators.NoIndicator);
            mClockRight.setIndicator(Indicator.Indicators.NoIndicator);

        } else{
            mRayLeft.setVisibility(View.INVISIBLE);
            mRayCenter.setVisibility(View.INVISIBLE);
            mRayRight .setVisibility(View.INVISIBLE);
        }

        //show clocks, texts and backgrounds for max/min, according to the setting
        if (maxOn==true){   // show all minmax stuff
            mClockMaxLeft.setVisibility(View.VISIBLE);
            mClockMaxCenter.setVisibility(View.VISIBLE);
            mClockMaxRight.setVisibility(View.VISIBLE);
            mClockMinLeft.setVisibility(View.VISIBLE);
            mClockMinCenter.setVisibility(View.VISIBLE);
            mClockMinRight.setVisibility(View.VISIBLE);

            mTextMaxLeft.setVisibility(View.VISIBLE);
            mTextMaxCenter.setVisibility(View.VISIBLE);
            mTextMaxRight.setVisibility(View.VISIBLE);
            mTextMinLeft.setVisibility(View.VISIBLE);
            mTextMinCenter.setVisibility(View.VISIBLE);
            mTextMinRight.setVisibility(View.VISIBLE);

            mImageMaxLeft.setVisibility(View.VISIBLE);
            mImageMaxCenter.setVisibility(View.VISIBLE);
            mImageMaxRight.setVisibility(View.VISIBLE);


        } else{ // don't show any of it
            mClockMaxLeft.setVisibility(View.INVISIBLE);
            mClockMaxCenter.setVisibility(View.INVISIBLE);
            mClockMaxRight.setVisibility(View.INVISIBLE);
            mClockMinLeft.setVisibility(View.INVISIBLE);
            mClockMinCenter.setVisibility(View.INVISIBLE);
            mClockMinRight.setVisibility(View.INVISIBLE);

            mTextMaxLeft.setVisibility(View.INVISIBLE);
            mTextMaxCenter.setVisibility(View.INVISIBLE);
            mTextMaxRight.setVisibility(View.INVISIBLE);
            mTextMinLeft.setVisibility(View.INVISIBLE);
            mTextMinCenter.setVisibility(View.INVISIBLE);
            mTextMinRight.setVisibility(View.INVISIBLE);

            mImageMaxLeft.setVisibility(View.INVISIBLE);
            mImageMaxCenter.setVisibility(View.INVISIBLE);
            mImageMaxRight.setVisibility(View.INVISIBLE);
        }

        //update!
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

        //put things back to null.
        //todo: check if this list is complete (probably some things are still missing)
        mClockLeft = null;
        mClockCenter = null;
        mClockRight = null;
        mBrakeAccel = null;
        mSteeringWheelAngle = null;
        mValueElement1 = null;
        mValueElement2 = null;
        mValueElement3 = null;
        mValueElement4 = null;
        mIconElement1 = null;
        mIconElement2 = null;
        mIconElement3 = null;
        mIconElement4 = null;
        mElement1Query = null;
        mElement2Query = null;
        mElement3Query = null;
        mElement4Query = null;
        mClockLQuery = null;
        mClockCQuery = null;
        mClockRQuery = null;
        mIconClockL = null;
        mIconClockC = null;
        mIconClockR = null;
        mClockMinLeft = null;
        mClockMinCenter = null;
        mClockMinRight = null;
        mClockMaxLeft = null;
        mClockMaxCenter = null;
        mClockMaxRight = null;

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
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                doUpdate();
            }
        });
    }

    private void doUpdate() {

        //temporary commented the following part, because I am afraid it might kill updates to the clocks
/*
        if (mClockLeft == null) {
            return;
        }
        */

        //update each of the elements:
        updateElement(mElement1Query, mValueElement1, mIconElement1);
        updateElement(mElement2Query, mValueElement2, mIconElement2);
        updateElement(mElement3Query, mValueElement3, mIconElement3);
        updateElement(mElement4Query, mValueElement4, mIconElement4);

        //update each of the clocks
        updateClock(mClockLQuery, mClockLeft, mRayLeft);
        updateClock(mClockCQuery, mClockCenter, mRayCenter);
        updateClock(mClockRQuery, mClockRight, mRayRight);

        //update the max clocks and texts
        updateMax(mLeftMax, mClockLeft, mTextMaxLeft, mClockMaxLeft);
        updateMax(mCenterMax, mClockCenter, mTextMaxCenter, mClockMaxCenter);
        updateMax(mRightMax, mClockRight, mTextMaxRight, mClockMaxRight);

        //update the min clocks and text
        updateMin(mLeftMin, mClockLeft, mTextMinLeft, mClockMinLeft);
        updateMin(mCenterMin, mClockCenter, mTextMinCenter, mClockMinCenter);
        updateMin(mRightMin, mClockRight, mTextMinRight, mClockMinRight);


        //get brakePressure and accelPos, used in other dash views
        //I might get rid of these, since I've got a selectable view for this now
        Float brakePressure = (Float) mLastMeasurements.get("brakePressure");
        Float accelPos = (Float) mLastMeasurements.get("acceleratorPosition");

        if (brakePressure != null && accelPos != null) {
            float normalizedBrakePressure = Math.min(Math.max(0.0f, brakePressure / FULL_BRAKE_PRESSURE), 1.0f);
            boolean isBraking = normalizedBrakePressure > 0;
            mBrakeAccel.setRotation(isBraking ? 180.0f : 0.0f);
            //noinspection deprecation
            mBrakeAccel.setProgressTintList(ColorStateList.valueOf(getContext().getResources()
                    .getColor(isBraking ? R.color.car_accent : R.color.car_primary)));
            mBrakeAccel.setProgress((int) ((isBraking ? normalizedBrakePressure : accelPos) * 10000));
        } else {
            mBrakeAccel.setProgress(0);
        }

        // Footer

        Float currentWheelAngle = (Float) mLastMeasurements.get("wheelAngle");
        mWheelState = mWheelStateMonitor == null ? WheelStateMonitor.WheelState.WHEEL_UNKNOWN
                : mWheelStateMonitor.getWheelState();
        mSteeringWheelAngle.setRotation(currentWheelAngle == null ? 0.0f :
                Math.min(Math.max(-WheelStateMonitor.WHEEL_CENTER_THRESHOLD_DEG, -currentWheelAngle),
                        WheelStateMonitor.WHEEL_CENTER_THRESHOLD_DEG));

        mSteeringWheelAngle.setVisibility(View.INVISIBLE);

    }

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

    // this sets all the labels/values in an initial state, depending on the chosen options
    private void setupElement(String queryElement, TextView value, TextView label){

        //set element label/value to default value first
        label.setBackgroundResource(0);
        value.setVisibility(View.VISIBLE);
        value.setText("");
        label.setText("");

        switch(queryElement) {
            case "none":
                label.setText("");
                value.setText("");
                label.setBackgroundResource(0);
                value.setVisibility(View.INVISIBLE);
                break;
            case "test":
                label.setText("");
                value.setText("0");
                label.setBackground(getContext().getDrawable(R.drawable.ic_measurement));
                break;
            case "batteryVoltage":
                label.setText("");
                value.setText("0,0V");
                label.setBackground(getContext().getDrawable(R.drawable.ic_battery));
                break;
            case "coolantTemperature":
                label.setText("");
                value.setText("0,0°C");
                label.setBackground(getContext().getDrawable(R.drawable.ic_water));
                break;
            case "oilTemperature":
                label.setText("");
                value.setText("0,0°C");
                label.setBackground(getContext().getDrawable(R.drawable.ic_oil));
                break;
            case "vehicleSpeed":
                label.setText("kmh");
                value.setText("0");
                label.setBackgroundResource(0);
                break;
            case "engineSpeed":
                label.setText("RPM");
                value.setText("0");
                label.setBackgroundResource(0);
                break;
            case "currentOutputPower":
                label.setText("kW");
                value.setText("0");
                label.setBackgroundResource(0);
                break;
            case "currentTorque":
                label.setText("Nm");
                value.setText("0");
                label.setBackgroundResource(0);
                break;
            case "gearboxOilTemperature":
                label.setText("");
                value.setText("0,0°C");
                label.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "outsideTemperature":
                label.setText("");
                value.setText("0,0°C");
                label.setBackground(getContext().getDrawable(R.drawable.ic_outsidetemperature));
                break;
            case "currentGear":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "recommendedGear":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "lateralAcceleration":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_lateral));
                break;
            case "longitudinalAcceleration":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_longitudinal));
                break;
            case "yawRate":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_yaw));
                break;
            case "wheelAngle":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_wheelangle));
                break;
            case "acceleratorPosition":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_pedalposition));
                break;
            case "brakePressure":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_brakepedalposition));
                break;
            case "powermeter":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_powermeter));
                break;
            case "EcoHMI_Score.AvgShort":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                break;
            case "EcoHMI_Score.AvgTrip":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_ecoavg));
                break;
        }
    }

    private void setupClock(String queryClock, Speedometer clock, TextView icon, RaySpeedometer ray, Speedometer min, Speedometer max){

        switch(queryClock) {
            case "none":
                icon.setText("");
                clock.setUnit("");
                icon.setBackgroundResource(0);
                break;
            case "test":
                icon.setText("");
                clock.setUnit("testing");
                clock.setMinMaxSpeed(-100,200);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_measurement));
                break;
            case "vehicleSpeed":
                icon.setText("");
                clock.setUnit("kmh");
                clock.setMinMaxSpeed(0,350);
                icon.setBackgroundResource(0);
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "engineSpeed":
                icon.setText("");
                clock.setUnit("RPM");
                clock.setMinMaxSpeed(0,8000);
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                icon.setBackgroundResource(0);
                break;
            case "batteryVoltage":
                icon.setText("");
                clock.setUnit("Volt");
                clock.setMinMaxSpeed(0,15);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_battery));
                break;
            case "oilTemperature":
                icon.setText("");
                clock.setUnit("°C");
                clock.setMinMaxSpeed(0,150);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_oil));
                break;
            case "coolantTemperature":
                icon.setText("");
                clock.setUnit("°C");
                clock.setMinMaxSpeed(0,150);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_water));
                break;
            case "gearboxOilTemperature":
                icon.setText("");
                clock.setUnit("°C");
                clock.setMinMaxSpeed(0,150);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "absChargingAirPressure":
                icon.setText("");
                clock.setUnit(pressureUnit);
                clock.setMinMaxSpeed(pressureMin,pressureMax);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_turbo));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "relChargingAirPressure":
                icon.setText("");
                clock.setUnit(pressureUnit);
                clock.setMinMaxSpeed(pressureMin,pressureMax);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_turbo));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "lateralAcceleration":
                icon.setText("");
                clock.setUnit("G");
                clock.setMinMaxSpeed(-2,2);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_lateral));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "longitudinalAcceleration":
                icon.setText("");
                clock.setUnit("G");
                clock.setMinMaxSpeed(-2,2);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_longitudinal));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "yawRate":
                icon.setText("");
                clock.setUnit("%");
                clock.setMinMaxSpeed(-1,1);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_yaw));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "wheelAngle":
                icon.setText("");
                clock.setUnit("°");
                clock.setMinMaxSpeed(-45,45);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_wheelangle));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "EcoHMI_Score.AvgShort":
                icon.setText("");
                clock.setUnit("");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "EcoHMI_Score.AvgTrip":
                icon.setText("");
                clock.setUnit("");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "powermeter":
                icon.setText("");
                clock.setUnit("");
                clock.setMinMaxSpeed(0,2000);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_powermeter));
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "acceleratorPosition":
                icon.setText("");
                clock.setUnit("%");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_pedalposition));
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "brakePressure":
                icon.setText("");
                clock.setUnit("%");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_brakepedalposition));
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "currentTorque":
                icon.setText("");
                clock.setUnit("Nm");
                clock.setMinMaxSpeed(0,500);
                icon.setBackgroundResource(0);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "currentOutputPower":
                icon.setText("");
                clock.setUnit("Kw");
                clock.setMinMaxSpeed(0,500);
                icon.setBackgroundResource(0);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "currentConsumptionPrimary":
                icon.setText("");
                clock.setUnit("l/h");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_fuelprimary));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "currentConsumptionSecondary":
                icon.setText("");
                clock.setUnit("l/h");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_fuelsecondary));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "cycleConsumptionPrimary":
                icon.setText("");
                clock.setUnit("l/h");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_fuelprimary));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "cycleConsumptionSecondary":
                icon.setText("");
                clock.setUnit("l/h");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_fuelsecondary));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
        }

        int minimum = clock.getMinSpeed();
        int maximum = clock.getMaxSpeed();

        min.setMinMaxSpeed(minimum, maximum);
        ray.setMinMaxSpeed(minimum, maximum);
        max.setMinMaxSpeed(minimum, maximum);


    }


    //update clock with data
    private void updateClock(String query, Speedometer dial, RaySpeedometer visray) {

        String generalTempUnit = (String) mLastMeasurements.get("unitTemperature.temperatureUnit");

        Float clockValue = (Float) mLastMeasurements.get(query);
        float randomClockVal = randFloat(-100,200);
        speedFactor = 1f;
        pressureFactor = 1f;

        switch (query){
            case "test":
                dial.speedTo(randomClockVal);
                break;
            case "none":
                break;

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
                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }
                break;

            case "engineSpeed":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0f : clockValue);

                }
                break;
            case "batteryVoltage":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }
                break;

            case "oilTemperature":
                String tempUnit = (String) mLastMeasurements.get("oilTemperature.unit");
                if (clockValue != null && tempUnit != null) {
                    switch (tempUnit) {
                        case "F":
                            dial.setUnit("°F");
                            break;
                        case "C":
                            dial.setUnit("°C");
                            break;
                    }

                } else if (clockValue != null && tempUnit == null && generalTempUnit != null) {
                    switch (generalTempUnit) {
                        case "fahrenheit":
                            dial.setUnit("°F");
                            break;
                        case "celcius":
                            dial.setUnit("°C");
                            break;
                    }

                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }
                break;
            case "coolantTemperature":
                String tempUnit2 = (String) mLastMeasurements.get("coolantTemperature.unit");
                if (clockValue != null && tempUnit2 != null) {
                    switch (tempUnit2) {
                        case "F":
                            dial.setUnit("°F");
                            break;
                        case "C":
                            dial.setUnit("°C");
                            break;

                    }
                } else if (clockValue != null && tempUnit2 == null && generalTempUnit != null) {
                    switch (generalTempUnit) {
                        case "fahrenheit":
                            dial.setUnit("°F");
                            break;
                        case "celcius":
                            dial.setUnit("°C");
                            break;
                    }

                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }
                break;
            case "gearboxTemperature":
                String tempUnit3 = (String) mLastMeasurements.get("gearboxTemperature.unit");
                if (clockValue != null && tempUnit3 != null) {
                    switch (tempUnit3) {
                        case "F":
                            dial.setUnit("°F");
                            break;
                        case "C":
                            dial.setUnit("°C");
                            break;

                    }
                } else if (clockValue != null && tempUnit3 == null && generalTempUnit != null) {
                    switch (generalTempUnit) {
                        case "fahrenheit":
                            dial.setUnit("°F");
                            break;
                        case "celcius":
                            dial.setUnit("°C");
                            break;
                    }

                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }
                break;

            case "absChargingAirPressure":
                if (clockValue != null) {
                    clockValue = clockValue * pressureFactor;
                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }
                break;
            case "relChargingAirPressure":
                if (clockValue != null) {
                    clockValue = clockValue * pressureFactor;
                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }
                break;
            case "lateralAcceleration":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }
                break;
            case "longitudinalAcceleration":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }
                break;
            case "yawRate":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }

                break;
            case "wheelAngle":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0.0f : clockValue);
                }
                break;
            case "EcoHMI_Score.AvgShort":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0f : clockValue);
                }
                break;
            case "EcoHMI_Score.AvgTrip":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0f : clockValue);
                }

                break;
            case "powermeter":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0f : clockValue);
                }
                break;

            case "acceleratorPosition":
                if (clockValue != null) {
                    float accelPercent = clockValue * 100;
                    dial.speedTo(clockValue == null ? 0f : accelPercent);
                }

                break;
            case "brakePressure":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0f : clockValue);
                }
                break;
            case "currentTorque":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0f : clockValue);
                }
                break;
            case "currentOutputPower":
                if (clockValue != null) {
                    dial.speedTo(clockValue == null ? 0f : clockValue);
                }
                break;
            case "currentConsumptionPrimary":
                String consumptionUnit = (String) mLastMeasurements.get("currentConsumptionPrimary.unit");
                if (clockValue != null && consumptionUnit != null) {
                    dial.setUnit(consumptionUnit);
                    dial.speedTo(clockValue == null ? 0f : clockValue);
                }
                break;
            case "currentConsumptionSecondary":
                String consumption2Unit = (String) mLastMeasurements.get("currentConsumptionSecondary.unit");
                if (clockValue != null && consumption2Unit != null) {
                    dial.setUnit(consumption2Unit);
                    dial.speedTo(clockValue == null ? 0f : clockValue);
                }
                break;
            case "cycleConsumptionPrimary":
                String cycconsumptionUnit = (String) mLastMeasurements.get("cycleConsumptionPrimary.unit");
                if (clockValue != null && cycconsumptionUnit != null) {
                    dial.setUnit(cycconsumptionUnit);
                    dial.speedTo(clockValue == null ? 0f : clockValue);
                }
                break;
            case "cycleConsumptionSecondary":
                String cycconsumption2Unit = (String) mLastMeasurements.get("cycleConsumptionSecondary.unit");
                if (clockValue != null && cycconsumption2Unit != null) {
                    dial.setUnit(cycconsumption2Unit);
                    dial.speedTo(clockValue == null ? 0f : clockValue);
                }
                break;


        }
        float temp = dial.getSpeed();
        visray.speedTo(temp);

    }

    //update the elements
    private void updateElement(String queryElement, TextView value, TextView label) {

     //   String elementValue = (String) mLastMeasurements.get(queryElement); // this causes troubles, since it can't be determined if the output is String or Float, based on the query
        String generalTempUnit = (String) mLastMeasurements.get("unitTemperature.temperatureUnit");

        switch (queryElement) {
            case "none":
                value.setText("");
                break;
            case "test":
                float randomValue = randFloat(0,100);
                value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), randomValue));
                break;

            case "batteryVoltage":
                Float mBatteryVoltage = (Float) mLastMeasurements.get(queryElement);
                if (mBatteryVoltage != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mBatteryVoltage + "V"));
                }
                break;
            case "coolantTemperature":
                Float mCoolantTemp = (Float) mLastMeasurements.get(queryElement);
                String tempUnit = (String) mLastMeasurements.get("coolantTemperature.unit");
                if (mCoolantTemp != null && tempUnit != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mCoolantTemp + tempUnit));
                } else if (mCoolantTemp != null && tempUnit == null && generalTempUnit != null) {
                    switch (generalTempUnit) {
                        case "fahrenheit":
                            value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mCoolantTemp + "°F"));
                            break;
                        case "celcius":
                            value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mCoolantTemp + "°C"));
                            break;
                    }
                }
                break;
            case "oilTemperature":
                Float mOilTemp = (Float) mLastMeasurements.get(queryElement);
                String tempUnit2 = (String) mLastMeasurements.get("coolantTemperature.unit");
                if (mOilTemp != null && tempUnit2 != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mOilTemp + tempUnit2));
                } else if (mOilTemp != null && tempUnit2 == null && generalTempUnit != null) {
                    switch (generalTempUnit) {
                        case "fahrenheit":
                            value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mOilTemp + "°F"));
                            break;
                        case "celcius":
                            value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mOilTemp + "°C"));
                            break;
                    }
                }
                break;
            case "vehicleSpeed":
                Float mVehicleSpeed = (Float) mLastMeasurements.get("vehicleSpeed");
                String speedUnit = (String) mLastMeasurements.get("vehicleSpeed.unit");
                if (mVehicleSpeed != null && speedUnit != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mVehicleSpeed));
                    label.setText(speedUnit);
                }
            case "engineSpeed":
                Float mEngineSpeed = (Float) mLastMeasurements.get(queryElement);
                if (mEngineSpeed != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.no_decimals).toString(), mEngineSpeed));
                }
                break;
            case "currentOutputPower":
                Float mCurrentOutputPower = (Float) mLastMeasurements.get(queryElement);
                if (mCurrentOutputPower != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mCurrentOutputPower));
                }
                break;
            case "currentTorque":
                Float mCurrentTorque = (Float) mLastMeasurements.get(queryElement);
                if (mCurrentTorque != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mCurrentTorque));
                }
                break;
            case "gearboxOilTemperature":
                String tempUnit3 = (String) mLastMeasurements.get("gearboxOilTemperature.unit");
                Float mGearboxOilTemp = (Float) mLastMeasurements.get(queryElement);
                if (mGearboxOilTemp != null && tempUnit3 != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mGearboxOilTemp + tempUnit3));
                } else if (mGearboxOilTemp != null && tempUnit3 == null && generalTempUnit != null) {
                    switch (generalTempUnit) {
                        case "fahrenheit":
                            value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mGearboxOilTemp + "°F"));
                            break;
                        case "celcius":
                            value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mGearboxOilTemp + "°C"));
                            break;
                    }
                }
                break;
            case "outsideTemperature":
                String tempUnit4 = (String) mLastMeasurements.get("outsideTemperature.unit");

                Float mOutsideTemperature = (Float) mLastMeasurements.get(queryElement);
                if (mOutsideTemperature != null && tempUnit4 != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mOutsideTemperature + tempUnit4));
                } else if (mOutsideTemperature != null && tempUnit4 == null && generalTempUnit != null) {
                    switch (generalTempUnit) {
                        case "fahrenheit":
                            value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mOutsideTemperature + "°F"));
                            break;
                        case "celcius":
                            value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mOutsideTemperature + "°C"));
                            break;
                    }
                }
                break;
            case "currentGear":
                Boolean reverseGear = (Boolean) mLastMeasurements.get("reverseGear.engaged");
                Boolean parkingBrake = (Boolean) mLastMeasurements.get("parkingBrake.engaged");
                String currentGear = (String) mLastMeasurements.get("currentGear");

                if (parkingBrake != null && parkingBrake) {
                    currentGear = "P";
                } else if (reverseGear != null && reverseGear) {
                    currentGear = "R";
                } else if (currentGear == null) {
                    value.setText("-");
                } else if (currentGear == "Gear1") {
                    value.setText("1");
                } else if (currentGear == "Gear2") {
                    value.setText("2");
                } else if (currentGear == "Gear3") {
                    value.setText("3");
                } else if (currentGear == "Gear4") {
                    value.setText("4");
                } else if (currentGear == "Gear5") {
                    value.setText("5");
                } else if (currentGear == "Gear6") {
                    value.setText("6");
                } else if (currentGear == "Gear7") {
                    value.setText("7");
                }
                break;
            case "recommendedGear":
                String mRecommendedGear = (String) mLastMeasurements.get("currentGear");
                String mCurrentGear2 = (String) mLastMeasurements.get("currentGear");

                if (mRecommendedGear == null) {
                    value.setText("-");
                } else if (mRecommendedGear == "Gear1") {
                    value.setText("1");
                } else if (mRecommendedGear == "Gear2") {
                    value.setText("2");
                } else if (mRecommendedGear == "Gear3") {
                    value.setText("3");
                } else if (mRecommendedGear == "Gear4") {
                    value.setText("4");
                } else if (mRecommendedGear == "Gear5") {
                    value.setText("5");
                } else if (mRecommendedGear == "Gear6") {
                    value.setText("6");
                } else if (mRecommendedGear == "Gear7") {
                    value.setText("7");
                } else if (mRecommendedGear == "NoRecommendation") {
                    value.setText("-");
                }

                //if the currentgear is not equal to recommended gear, highlight the gear in red.
                if (mRecommendedGear != mCurrentGear2) {
                    value.setTextColor(Color.RED);

                } else {
                    value.setTextColor(Color.WHITE);

                }
                break;
            case "lateralAcceleration":
                Float mLateralAcceleration = (Float) mLastMeasurements.get(queryElement);
                if (mLateralAcceleration != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mLateralAcceleration + "G"));
                }
                break;
            case "longitudinalAcceleration":
                Float mlongitudinalAcceleration = (Float) mLastMeasurements.get(queryElement);
                if (mlongitudinalAcceleration != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mlongitudinalAcceleration + "G"));
                }
                break;
            case "yawRate":
                Float mYawRate = (Float) mLastMeasurements.get(queryElement);
                if (mYawRate != null) {
                    Float mYawRatePct = mYawRate * 100;
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mYawRatePct + "%"));
                }
                break;
            case "acceleratorPosition":
                Float mAcceleratorPosition = (Float) mLastMeasurements.get(queryElement);
                if (mAcceleratorPosition != null) {
                    Float mYawRatePct = mAcceleratorPosition * 100;
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mYawRatePct + "%"));
                }
                break;
            case "brakePressure":
                Float mBrakePressure = (Float) mLastMeasurements.get(queryElement);
                if (mBrakePressure != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mBrakePressure + "%"));
                }
                break;
            case "wheelAngle":
                Float mWheelAngle = (Float) mLastMeasurements.get(queryElement);
                if (mWheelAngle != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mWheelAngle + "°"));
                }
                break;
            case "powermeter":
                Float mPowermeter = (Float) mLastMeasurements.get(queryElement);
                if (mPowermeter != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.no_decimals).toString(), mPowermeter));
                }
                break;
            case "EcoHMI_Score.AvgShort":
                Float mEcoScoreShort = (Float) mLastMeasurements.get(queryElement);
                if (mEcoScoreShort != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.no_decimals).toString(), mEcoScoreShort));
                }
                break;
            case "EcoHMI_Score.AvgTrip":
                Float mEcoScoreTrip = (Float) mLastMeasurements.get(queryElement);
                if (mEcoScoreTrip != null) {
                    value.setText(String.format(Locale.US, getContext().getText(R.string.no_decimals).toString(), mEcoScoreTrip));
                }
                break;



        }


    }

    //update the max speed indicator:
    //todo: remove currentmax from this, since it's no longer used, using the current state of the max-clock for that
    private void updateMax(Float currentmax, Speedometer dial, TextView textmax, Speedometer maxclock){
        float currentvalue = dial.getSpeed();
        float maxvalue = maxclock.getSpeed();
        if (currentvalue > maxvalue){
            maxclock.speedTo(currentvalue);
        }


        textmax.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), maxvalue));
    }
    //todo: remove currentmin from this, since it's no longer used, using the current state of the min-clock for that
    private void updateMin(Float currentmin, Speedometer dial, TextView textmin, Speedometer minclock){
        float currentvalue = dial.getSpeed();
        float minvalue = minclock.getSpeed();
        if (currentvalue < minvalue){
            minclock.speedTo(currentvalue);
        }

        textmin.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), minvalue));
    }

    public static float randFloat(float min, float max) {

        Random rand = new Random();

        float result = rand.nextFloat() * (max - min) + min;

        return result;

    }



}