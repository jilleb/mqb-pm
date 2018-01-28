package com.mqbcoding.stats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
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
import com.github.anastr.speedviewlib.SpeedView;
import com.github.anastr.speedviewlib.Speedometer;
import com.github.anastr.speedviewlib.components.Indicators.ImageIndicator;
import com.github.martoreto.aauto.vex.CarStatsClient;
import com.google.android.apps.auto.sdk.DayNightStyle;
import com.google.android.apps.auto.sdk.StatusBarController;
import com.google.common.collect.ImmutableMap;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

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
            doCelebrate();
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
        sc.showMicButton();
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
        // find all elements needed
        //clocks:
        mClockLeft = rootView.findViewById(R.id.dial_Left);
        mClockCenter = rootView.findViewById(R.id.dial_Center);
        mClockRight = rootView.findViewById(R.id.dial_Right);
        //give clocks a custom image indicator
        mClockLeft.setIndicator(imageIndicator);
        mClockCenter.setIndicator(imageIndicator);
        mClockRight.setIndicator(imageIndicator);

        //max dials
        mClockMaxLeft = rootView.findViewById(R.id.dial_MaxLeft);
        mClockMaxCenter = rootView.findViewById(R.id.dial_MaxCenter);
        mClockMaxRight = rootView.findViewById(R.id.dial_MaxRight);
        mLeftMax = 0;
        mCenterMax = 0;
        mRightMax=0;
        mClockMinLeft = rootView.findViewById(R.id.dial_MinLeft);
        mClockMinCenter = rootView.findViewById(R.id.dial_MinCenter);
        mClockMinRight = rootView.findViewById(R.id.dial_MinRight);
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
        //text elements:
        mValueElement1 = rootView.findViewById(R.id.value_Element1);
        mValueElement2 = rootView.findViewById(R.id.value_Element2);
        mValueElement3 = rootView.findViewById(R.id.value_Element3);
        mValueElement4 = rootView.findViewById(R.id.value_Element4);
        //labels:
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
        //maxes
        mClockMaxLeft.setSpeedTextTypeface(typeface);
        mClockMaxCenter.setSpeedTextTypeface(typeface);
        mClockMaxRight.setSpeedTextTypeface(typeface);

        mClockMinLeft.setSpeedTextTypeface(typeface);
        mClockMinCenter.setSpeedTextTypeface(typeface);
        mClockMinRight.setSpeedTextTypeface(typeface);

        mTextMinLeft.setTypeface(typeface);
        mTextMaxLeft.setTypeface(typeface);
        mTextMinCenter.setTypeface(typeface);
        mTextMaxCenter.setTypeface(typeface);
        mTextMinRight.setTypeface(typeface);
        mTextMaxRight.setTypeface(typeface);

           // and set the image indicator for the clocks:

        mBrakeAccel = rootView.findViewById(R.id.brake_accel_view);
        mSteeringWheelAngle = rootView.findViewById(R.id.wheel_angle_image);

        //center clock click = celebrate indicators
        mClockCenter.setOnClickListener(celebrateOnClickListener);

        //Get shared preferences
        // get setting, to determine what value needs to be shown

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        pressureUnits   = sharedPreferences.getBoolean("selectPressureUnit", true);  //true = bar, false = psi
        raysOn          = sharedPreferences.getBoolean("highVisActive", false);  //true = show high vis rays, false = don't show them.
        maxOn           = sharedPreferences.getBoolean("maxValuesActive", false); //true = show max values, false = hide them

        mElement1Query = sharedPreferences.getString("selectedView1", "none");
        mElement2Query = sharedPreferences.getString("selectedView2", "none");
        mElement3Query = sharedPreferences.getString("selectedView3", "none");
        mElement4Query = sharedPreferences.getString("selectedView4", "none");

        mClockLQuery = sharedPreferences.getString("selectedClockLeft", "none");
        mClockCQuery = sharedPreferences.getString("selectedClockCenter", "none");
        mClockRQuery = sharedPreferences.getString("selectedClockRight", "none");

        Log.d(TAG, "element 1 selected:" + mElement1Query);
        Log.d(TAG, "element 2 selected:" + mElement2Query);
        Log.d(TAG, "element 3 selected:" + mElement3Query);
        Log.d(TAG, "element 4 selected:" + mElement4Query);

        Log.d(TAG, "clock l selected:" + mClockLQuery);
        Log.d(TAG, "clock c selected:" + mClockCQuery);
        Log.d(TAG, "clock r selected:" + mClockRQuery);

        //set up the elements
        setupElement(mElement1Query, mValueElement1 , mIconElement1);
        setupElement(mElement2Query, mValueElement2 , mIconElement2);
        setupElement(mElement3Query, mValueElement3 , mIconElement3);
        setupElement(mElement4Query, mValueElement4 , mIconElement4);

        pressureMin = -2;
        pressureMax = 3;
        pressureFactor = 1;
        //set pressure dial to the right units
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




        //setup clocks:
        setupClock(mClockLQuery, mClockLeft, mIconClockL, mRayLeft, mClockMinLeft, mClockMaxLeft);
        setupClock(mClockCQuery, mClockCenter, mIconClockC, mRayCenter, mClockMinCenter, mClockMaxCenter);
        setupClock(mClockRQuery, mClockRight, mIconClockR, mRayLeft, mClockMinLeft, mClockMaxLeft);



//show high visible rays on, according to the setting
        if (raysOn==true){
            mRayLeft.setVisibility(View.VISIBLE);
            mRayCenter.setVisibility(View.VISIBLE);
            mRayRight.setVisibility(View.VISIBLE);
        } else{
            mRayLeft.setVisibility(View.INVISIBLE);
            mRayCenter.setVisibility(View.INVISIBLE);
            mRayRight .setVisibility(View.INVISIBLE);
        }

//show elements for max/min, according to the setting
        if (maxOn==true){
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


        } else{
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

        Log.d(TAG, "Units: " + pressureUnits);

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

   //     if (mClockLeft == null) {
   //         return;
   //    }

        updateClock(mClockLQuery, mClockLeft, mRayLeft);
        updateClock(mClockCQuery, mClockCenter, mRayCenter);
        updateClock(mClockRQuery, mClockRight, mRayRight);

        updateMax(mLeftMax, mClockLeft, mTextMaxLeft);
        updateMax(mCenterMax, mClockCenter, mTextMaxCenter);
        updateMax(mRightMax, mClockRight, mTextMaxRight);

        updateMin(mLeftMin, mClockLeft, mTextMinLeft);
        updateMin(mCenterMin, mClockCenter, mTextMinCenter);
        updateMin(mRightMin, mClockRight, mTextMinRight);

        updateElement(mElement1Query, mValueElement1, mIconElement1);
        updateElement(mElement2Query, mValueElement2, mIconElement2);
        updateElement(mElement3Query, mValueElement3, mIconElement3);
        updateElement(mElement4Query, mValueElement4, mIconElement4);

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

        switch(queryElement) {
            case "none":
                label.setText("");
                value.setText("");
                label.setBackgroundResource(0);
                value.setVisibility(View.INVISIBLE);
                break;
            case "batteryVoltage":
                label.setText("");
                value.setText("0.0V");

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
            case "EcoHMI_Score_AvgShort":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                break;
            case "EcoHMI_Score_AvgTrip":
                label.setText("");
                value.setText("-");
                label.setBackground(getContext().getDrawable(R.drawable.ic_eco));
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
                clock.speedPercentTo(100,10000);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_measurement));
                break;
            case "vehicleSpeed":
                icon.setText("");
                clock.setUnit("kmh");
                clock.setMinMaxSpeed(0,350);
                icon.setBackgroundResource(0);
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
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_battery));
                break;
            case "oilTemperature":
                icon.setText("");
                clock.setUnit("°C");
                clock.setMinMaxSpeed(0,150);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_oil));
                break;
            case "coolantTemperature":
                icon.setText("");
                clock.setUnit("°C");
                clock.setMinMaxSpeed(0,150);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_water));
                break;
            case "gearboxOilTemperature":
                icon.setText("");
                clock.setUnit("°C");
                clock.setMinMaxSpeed(0,150);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "absChargingAirPressure":
                icon.setText("");
                clock.setUnit(pressureUnit);
                clock.setMinMaxSpeed(pressureMin,pressureMax);
                icon.setBackground(getContext().getDrawable(R.drawable.turbo));
                break;
            case "relChargingAirPressure":
                icon.setText("");
                clock.setUnit(pressureUnit);
                clock.setMinMaxSpeed(pressureMin,pressureMax);
                icon.setBackground(getContext().getDrawable(R.drawable.turbo));
                break;
            case "lateralAcceleration":
                icon.setText("");
                clock.setUnit("G");
                clock.setMinMaxSpeed(-2,2);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_lateral));
                break;
            case "longitudinalAcceleration":
                icon.setText("");
                clock.setUnit("G");
                clock.setMinMaxSpeed(-2,2);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_longitudinal));
                break;
            case "yawRate":
                icon.setText("");
                clock.setUnit("%");
                clock.setMinMaxSpeed(-1,1);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_yaw));
                break;
            case "wheelAngle":
                icon.setText("");
                clock.setUnit("°");
                clock.setMinMaxSpeed(-45,45);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_wheelangle));
                break;
            case "EcoHMI_Score_AvgShort":
                icon.setText("");
                clock.setUnit("");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                break;
            case "EcoHMI_Score_AvgTrip":
                icon.setText("");
                clock.setUnit("");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                break;
            case "powermeter":
                icon.setText("");
                clock.setUnit("");
                clock.setMinMaxSpeed(0,2000);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_powermeter));
                break;
            case "acceleratorPosition":
                icon.setText("");
                clock.setUnit("%");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_pedalposition));
                break;
            case "brakePressure":
                icon.setText("");
                clock.setUnit("%");
                clock.setMinMaxSpeed(0,100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_brakepedalposition));
                break;
            case "currentTorque":
                icon.setText("");
                clock.setUnit("Nm");
                clock.setMinMaxSpeed(0,500);
                icon.setBackgroundResource(0);
                break;
            case "currentOutputPower":
                icon.setText("");
                clock.setUnit("Kw");
                clock.setMinMaxSpeed(0,500);
                icon.setBackgroundResource(0);
                break;




                        /*
        todo:

        <item>tankLevelPrimary</item>
        <item>tankLevelSecondary</item>

              */




        }

        int minimum = clock.getMinSpeed();
        int maximum = clock.getMaxSpeed();

        min.setMinMaxSpeed(minimum, maximum);
        ray.setMinMaxSpeed(minimum, maximum);
        max.setMinMaxSpeed(minimum, maximum);


    }


    //update clock with data
    private void updateClock(String query, Speedometer dial, RaySpeedometer visray) {


        Float clockValue = (Float) mLastMeasurements.get(query);
        speedFactor = 1f;
        pressureFactor = 1f;

        if (clockValue == null) {
            dial.speedTo(0);
        } else {

            switch (query) {
                case "none":
                    break;
                case "test":
                    dial.speedTo(15);

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
                case "EcoHMI_Score_AvgShort":
                    if (clockValue != null) {
                        dial.speedTo(clockValue == null ? 0f : clockValue);
                    }
                    break;
                case "EcoHMI_Score_AvgTrip":
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
                        dial.speedTo(clockValue == null ? 0f : clockValue);
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
            }
            float temp = dial.getSpeed();
            visray.speedTo(temp);
        }
    }

    //update the elements
    private void updateElement(String queryElement, TextView value, TextView label) {

        if (queryElement == "none") {
            value.setText("");
        } else {

            Float elementValue = (Float) mLastMeasurements.get(queryElement);

            if (elementValue == null) {
                value.setText("---");
            } else {
                switch (queryElement) {
                    case "none":
                        value.setText("");
                        break;
                    case "batteryVoltage":
                        if (elementValue != null) {
                            value.setText(elementValue.toString() + "V");
                        }
                        break;
                    case "coolantTemperature":
                        String tempUnit = (String) mLastMeasurements.get("coolantTemperature.unit");
                        if (elementValue != null && tempUnit != null) {
                            value.setText(elementValue.toString() + tempUnit);
                        }
                        break;
                    case "oilTemperature":
                        String tempUnit2 = (String) mLastMeasurements.get("oilTemperature.unit");
                        if (elementValue != null && tempUnit2 != null) {
                            value.setText(elementValue.toString() + tempUnit2);
                        }
                        break;
                    case "vehicleSpeed":
                        String speedUnit = (String) mLastMeasurements.get("vehicleSpeed.unit");
                        if (elementValue != null && speedUnit != null) {
                            value.setText(elementValue.toString());
                            label.setText(speedUnit);
                        }
                    case "engineSpeed":
                        if (elementValue != null) {
                            value.setText(elementValue.toString());
                        }
                        break;
                    case "currentOutputPower":
                        if (elementValue != null) {
                            value.setText(elementValue.toString());
                        }
                        break;
                    case "currentTorque":
                        if (elementValue != null) {
                            value.setText(elementValue.toString());
                        }
                        break;
                    case "gearboxOilTemperature":
                        String tempUnit3 = (String) mLastMeasurements.get("gearboxTemperature.unit");
                        if (elementValue != null && tempUnit3 != null) {
                            value.setText(elementValue.toString() + tempUnit3);
                        }
                        break;
                    case "outsideTemperature":
                        String tempUnit4 = (String) mLastMeasurements.get("outsideTemperature.unit");
                        if (elementValue != null && tempUnit4 != null) {
                            value.setText(elementValue.toString() + tempUnit4);
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
                }
            }
        }
    }

//update the max speed indicator:
    private void updateMax(Float currentmax, Speedometer dial, TextView textmax){
        float possiblemax = dial.getSpeed();
        if (possiblemax > currentmax){
            currentmax = possiblemax;
        }
        dial.setSpeedAt(currentmax);
        textmax.setText(currentmax.toString());


    }

    private void updateMin(Float currentmin, Speedometer dial, TextView textmin){
        float possiblemin = dial.getSpeed();
        if (possiblemin > currentmin){
            currentmin = possiblemin;
        }
        dial.setSpeedAt(currentmin);
        textmin.setText(currentmin.toString());
    }


    private void doCelebrate()
    {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                mClockRight.speedPercentTo(100,3000);
        mClockLeft.speedPercentTo(100,3000);
        mClockCenter.speedPercentTo(100,3000);
        mRayLeft.speedPercentTo(100,3000);
        mRayCenter.speedPercentTo(100,3000);
        mRayRight.speedPercentTo(100,3000);
            }
        }, 1);

        Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            public void run() {
                mClockRight.speedPercentTo(0, 3000);
                mClockLeft.speedPercentTo(0,3000);
                mClockCenter.speedPercentTo(0,3000);
                mRayLeft.speedPercentTo(0,3000);
                mRayCenter.speedPercentTo(0,3000);
                mRayRight.speedPercentTo(0,3000);

            }
        }, 2500);

    }

}
