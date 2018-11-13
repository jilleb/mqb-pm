package com.mqbcoding.stats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.github.anastr.speedviewlib.Gauge;
import com.github.anastr.speedviewlib.RaySpeedometer;
import com.github.anastr.speedviewlib.SpeedView;
import com.github.anastr.speedviewlib.Speedometer;
import com.github.anastr.speedviewlib.components.Indicators.ImageIndicator;
import com.github.anastr.speedviewlib.components.Indicators.Indicator;
import com.github.martoreto.aauto.vex.CarStatsClient;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static android.support.v7.widget.TintTypedArray.obtainStyledAttributes;

public class DashboardFragment extends CarFragment {
    public static final float FULL_BRAKE_PRESSURE = 100.0f;
    private final String TAG = "DashboardFragment";
    private Runnable mTimer1;
    private CarStatsClient mStatsClient;
    private WheelStateMonitor mWheelStateMonitor;
    private ProgressBar mBrakeAccel;
    private Speedometer mClockLeft, mClockCenter, mClockRight;
    private Speedometer mClockMaxLeft, mClockMaxCenter, mClockMaxRight;
    private Speedometer mClockMinLeft, mClockMinCenter, mClockMinRight;
    private ImageView mImageMaxLeft, mImageMaxCenter, mImageMaxRight;
    private RaySpeedometer mRayLeft, mRayCenter, mRayRight;
    private ImageView mSteeringWheelAngle;
    private String mElement1Query, mElement2Query, mElement3Query, mElement4Query;
    private String mClockLQuery, mClockCQuery, mClockRQuery;
    private String pressureUnit, selectedFont;
    private float pressureFactor, speedFactor;
    private int pressureMin, pressureMax;
    //icons/labels of the data elements. upper left, upper right, lower left, lower right.
    private TextView mIconElement1, mIconElement2, mIconElement3, mIconElement4;
    //values of the data elements. upper left, upper right, lower left, lower right.
    private TextView mValueElement1, mValueElement2, mValueElement3, mValueElement4;
    private TextView mTextMinLeft, mTextMaxLeft;
    private TextView mTextMinCenter, mTextMaxCenter;
    private TextView mTextMinRight, mTextMaxRight;
    //icons on the clocks
    private TextView mIconClockL, mIconClockC, mIconClockR;
    private WheelStateMonitor.WheelState mWheelState;
    private Boolean pressureUnits;
    private Boolean stagingDone = false;
    private Boolean raysOn, maxOn, accelOn, maxMarksOn, ticksOn, ambientOn;
    private Map<String, Object> mLastMeasurements = new HashMap<>();
    private Handler mHandler = new Handler();
    private View.OnClickListener celebrateOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            postUpdate();
        }
    };
    private final CarStatsClient.Listener mCarStatsListener;

    {
        mCarStatsListener = new CarStatsClient.Listener() {
            @Override
            public void onNewMeasurements(String provider, Date timestamp, Map<String, Object> values) {
                mLastMeasurements.putAll(values);
                postUpdate();
            }
        };
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CarStatsService.CarStatsBinder carStatsBinder = (CarStatsService.CarStatsBinder) iBinder;
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

    public DashboardFragment() {
        // Required empty public constructor
    }

    // random, for use in Test value
    public static float randFloat(float min, float max) {
        Random rand = new Random();
        float result = rand.nextFloat() * (max - min) + min;
        return result;
    }

    public static String convGear(String gear) {
        
        String convertedGear = "0";
        if (gear == null) {
        convertedGear = "-";
        } else if (gear.equals("Gear1")) {
            convertedGear = "1";
        } else if (gear.equals("Gear2")) {
            convertedGear = "2";
        } else if (gear.equals("Gear3")) {
            convertedGear = "3";
        } else if (gear.equals("Gear4")) {
            convertedGear = "4";
        } else if (gear.equals("Gear5")) {
            convertedGear = "5";
        } else if (gear.equals("Gear6")) {
            convertedGear = "6";
        } else if (gear.equals("Gear7")) {
            convertedGear = "7";
        } else if (gear.equals("Gear8")) {
            convertedGear = "8";
        }

        return convertedGear;
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);


        //Get preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        pressureUnits = sharedPreferences.getBoolean("selectPressureUnit", true);  //true = bar, false = psi
        raysOn = sharedPreferences.getBoolean("highVisActive", false);  //true = show high vis rays, false = don't show them.
        maxOn = sharedPreferences.getBoolean("maxValuesActive", false); //true = show max values, false = hide them
        maxMarksOn = sharedPreferences.getBoolean("maxMarksActive", false); //true = show max values as a mark on the clock, false = hide them
        accelOn = sharedPreferences.getBoolean("showAccelView", false); //true = show indicator, false = hide it
        selectedFont = sharedPreferences.getString("selectedFont", "segments");
        ticksOn = sharedPreferences.getBoolean("ticksActive",false); // if true, it will display the value of each of the ticks
        ambientOn = sharedPreferences.getBoolean("ambientActive", false);  //true = use ambient colors, false = don't use.

        //set textview to have a custom digital font:
        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "digital.ttf");
        switch(selectedFont){
            case "segments":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "digital.ttf");
                break;
            case "seat":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "SEAT_MetaStyle_MonoDigit_Regular.ttf");
                break;
            case "audi":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "AudiTypeDisplayHigh.ttf");
                break;
            case "vw":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "VWTextCarUI-Regular.ttf");
                break;
            case "vw2":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "VWThesis_MIB_Regular.ttf");
                break;
            case "frutiger":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "Frutiger.otf");
                break;
            case "vw3":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "VW_Digit_Reg.otf");
                break;
            case "skoda":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "Skoda.ttf");
                break;
     }
     //-------------------------------------------------------------
        //find all elements needed
        //clocks:
        mClockLeft = rootView.findViewById(R.id.dial_Left);
        mClockCenter = rootView.findViewById(R.id.dial_Center);
        mClockRight = rootView.findViewById(R.id.dial_Right);

        //max & min dials
        mClockMaxLeft = rootView.findViewById(R.id.dial_MaxLeft);
        mClockMaxCenter = rootView.findViewById(R.id.dial_MaxCenter);
        mClockMaxRight = rootView.findViewById(R.id.dial_MaxRight);
        mClockMinLeft = rootView.findViewById(R.id.dial_MinLeft);
        mClockMinCenter = rootView.findViewById(R.id.dial_MinCenter);
        mClockMinRight = rootView.findViewById(R.id.dial_MinRight);

        //set max/min values to 0

        //icons on the clocks
        mIconClockL = rootView.findViewById(R.id.icon_ClockLeft);
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

        // build ImageIndicator using the resourceId
        // get the size of the Clock, to make sure the imageindicator has the right size.
        mClockLeft.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mClockLeft.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int clockSize = mClockLeft.getHeight();
                if (clockSize==0){
                    clockSize=250;
                    }
                //this is to enable an image as indicator.
                TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.themedNeedle});
                int resourceId = typedArray.getResourceId(0, 0);
                typedArray.recycle();

                ImageIndicator imageIndicator = new ImageIndicator(getContext(), resourceId, clockSize, clockSize);

                int color = mClockLeft.getIndicatorColor();

                if (raysOn) {
                    mClockLeft.setIndicator(Indicator.Indicators.NoIndicator);
                    mClockCenter.setIndicator(Indicator.Indicators.NoIndicator);
                    mClockRight.setIndicator(Indicator.Indicators.NoIndicator);
                } else  if (color==-14575885) {
                    //if theme has transparent indicator color, give clocks a custom image indicator
                    //todo: do this on other fragments as well
                    mClockLeft.setIndicator(imageIndicator);
                    mClockCenter.setIndicator(imageIndicator);
                    mClockRight.setIndicator(imageIndicator);
                } else if (ambientOn) {
                    String ambientColor = (String) mLastMeasurements.get("Car.ambienceLightColour.ColourSRGB");

                    if (ambientColor != null){
                        mClockLeft.setIndicatorColor(Color.parseColor(ambientColor));
                        mClockCenter.setIndicatorColor(Color.parseColor(ambientColor));
                        mClockRight.setIndicatorColor(Color.parseColor(ambientColor));
                    }
                 }

                // show value of the ticks
                if (ticksOn){
                    int tickNum = 10;
                    int tickPad = 20;

                    mClockLeft.setTickNumber(tickNum);
                    mClockLeft.setTextColor(Color.WHITE);
                    mClockLeft.setTickPadding(tickPad);
                    mClockCenter.setTickNumber(tickNum);
                    mClockCenter.setTextColor(Color.WHITE);
                    mClockCenter.setTickPadding(tickPad);
                    mClockRight.setTickNumber(tickNum);
                    mClockRight.setTextColor(Color.WHITE);
                    mClockRight.setTickPadding(tickPad);
                }

                //initiating staging:
                if (!stagingDone){

                    mClockLeft.speedPercentTo(100,1000);
                    mClockCenter.speedPercentTo(100,1000);
                    mClockRight.speedPercentTo(100,1000);
                    mRayLeft.speedPercentTo(100,1000);
                    mRayCenter.speedPercentTo(100,1000);
                    mRayRight.speedPercentTo(100,1000);

                    final Handler staging = new Handler();
                    staging.postDelayed(new Runnable() {
                        public void run() {
                            mClockLeft.speedPercentTo(0,1000);
                            mClockCenter.speedPercentTo(0,1000);
                            mClockRight.speedPercentTo(0,1000);
                            mRayLeft.speedPercentTo(0,1000);
                            mRayCenter.speedPercentTo(0,1000);
                            mRayRight.speedPercentTo(0,1000);

                        }
                    }, 1000);   //5 seconds
                    stagingDone=true;

                }
            }

        });

        //set up each of the elements with the query and icon that goes with it
        setupElement(mElement1Query, mValueElement1, mIconElement1);
        setupElement(mElement2Query, mValueElement2, mIconElement2);
        setupElement(mElement3Query, mValueElement3, mIconElement3);
        setupElement(mElement4Query, mValueElement4, mIconElement4);

        //setup clocks, including the max/min clocks and highvis rays and icons:
        //usage: setupClock(query value, what clock, what icon, which ray, which min clock, which max clock)
        //could probably be done MUCH more efficient but that's for the future ;)
        setupClock(mClockLQuery, mClockLeft, mIconClockL, mRayLeft, mClockMinLeft, mClockMaxLeft);
        setupClock(mClockCQuery, mClockCenter, mIconClockC, mRayCenter, mClockMinCenter, mClockMaxCenter);
        setupClock(mClockRQuery, mClockRight, mIconClockR, mRayRight, mClockMinRight, mClockMaxRight);

        //show high visible rays on, according to the setting


        if (raysOn) {
            mRayLeft.setVisibility(View.VISIBLE);
            mRayCenter.setVisibility(View.VISIBLE);
            mRayRight.setVisibility(View.VISIBLE);

            //also hide the needle on the clocks
            mClockLeft.setIndicator(Indicator.Indicators.NoIndicator);
            mClockCenter.setIndicator(Indicator.Indicators.NoIndicator);
            mClockRight.setIndicator(Indicator.Indicators.NoIndicator);

        } else {
            mRayLeft.setVisibility(View.INVISIBLE);
            mRayCenter.setVisibility(View.INVISIBLE);
            mRayRight.setVisibility(View.INVISIBLE);
        }



        //show texts and backgrounds for max/min, according to the setting
        if (maxOn) {   // show the minmax values
            mTextMaxLeft.setVisibility(View.VISIBLE);
            mTextMaxCenter.setVisibility(View.VISIBLE);
            mTextMaxRight.setVisibility(View.VISIBLE);

            mTextMinLeft.setVisibility(View.VISIBLE);
            mTextMinCenter.setVisibility(View.VISIBLE);
            mTextMinRight.setVisibility(View.VISIBLE);

            mImageMaxLeft.setVisibility(View.VISIBLE);
            mImageMaxCenter.setVisibility(View.VISIBLE);
            mImageMaxRight.setVisibility(View.VISIBLE);
        } else { // don't show any of it
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

        //show clock marks for max/min, according to the setting
        if (maxMarksOn) { // show the minmax marks
            mClockMaxLeft.setVisibility(View.VISIBLE);
            mClockMaxCenter.setVisibility(View.VISIBLE);
            mClockMaxRight.setVisibility(View.VISIBLE);
            mClockMinLeft.setVisibility(View.VISIBLE);
            mClockMinCenter.setVisibility(View.VISIBLE);
            mClockMinRight.setVisibility(View.VISIBLE);
        } else { // don't show any of it
            mClockMaxLeft.setVisibility(View.INVISIBLE);
            mClockMaxCenter.setVisibility(View.INVISIBLE);
            mClockMaxRight.setVisibility(View.INVISIBLE);
            mClockMinLeft.setVisibility(View.INVISIBLE);
            mClockMinCenter.setVisibility(View.INVISIBLE);
            mClockMinRight.setVisibility(View.INVISIBLE);
        }

        if (accelOn) {
            mBrakeAccel.setVisibility(View.VISIBLE);
        } else {
            mBrakeAccel.setVisibility(View.INVISIBLE);
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
        mRayLeft = null;
        mRayCenter = null;
        mRayRight = null;

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
        mTimer1 = new Runnable() {
            public void run() {
                doUpdate();
            }

        };
        //experimental delay
        mHandler.postDelayed(mTimer1, 100);

    }

    private void doUpdate() {

        if (mClockLeft == null) {
            return;
        }

        //wait until staging is done before displaying any data on the clocks.
       if (!stagingDone){
           return;
       }
        //update each of the elements:
        updateElement(mElement1Query, mValueElement1, mIconElement1);
        updateElement(mElement2Query, mValueElement2, mIconElement2);
        updateElement(mElement3Query, mValueElement3, mIconElement3);
        updateElement(mElement4Query, mValueElement4, mIconElement4);

        //update each of the clocks and the min/max/ray elements that go with it
        // query, dial, visray, textmax, textmin, clockmax, clockmin) {
        updateClock(mClockLQuery, mClockLeft, mRayLeft, mTextMaxLeft, mTextMinLeft, mClockMaxLeft, mClockMinLeft);
        updateClock(mClockCQuery, mClockCenter, mRayCenter, mTextMaxCenter, mTextMinCenter, mClockMaxCenter, mClockMinCenter);
        updateClock(mClockRQuery, mClockRight, mRayRight, mTextMaxRight, mTextMinRight, mClockMaxRight, mClockMinRight);

        //get brakePressure and accelPos, used in other dash views
        Float brakePressure = (Float) mLastMeasurements.get("brakePressure");
        Float accelPos = (Float) mLastMeasurements.get("acceleratorPosition");

        if (brakePressure != null && accelPos != null && mBrakeAccel != null) {
            float normalizedBrakePressure = Math.min(Math.max(0.0f, brakePressure / FULL_BRAKE_PRESSURE), 1.0f);
            boolean isBraking = normalizedBrakePressure > 0;

                mBrakeAccel.setRotation(isBraking ? 180.0f : 0.0f);

                //noinspection deprecation
                mBrakeAccel.setProgressTintList(ColorStateList.valueOf(getContext().getResources()
                        .getColor(isBraking ? R.color.car_accent : R.color.car_primary)));
                mBrakeAccel.setProgress((int) ((isBraking ? normalizedBrakePressure : accelPos) * 10000));

        } else if (mBrakeAccel != null)   {
            mBrakeAccel.setProgress(0);
        }

 // wheel angle monitor
        Float currentWheelAngle = (Float) mLastMeasurements.get("wheelAngle");
        mWheelState = mWheelStateMonitor == null ? WheelStateMonitor.WheelState.WHEEL_UNKNOWN
                : mWheelStateMonitor.getWheelState();
        mSteeringWheelAngle.setRotation(currentWheelAngle == null ? 0.0f :
                Math.min(Math.max(-WheelStateMonitor.WHEEL_CENTER_THRESHOLD_DEG, -currentWheelAngle),
                        WheelStateMonitor.WHEEL_CENTER_THRESHOLD_DEG));
        mSteeringWheelAngle.setVisibility(View.INVISIBLE);

    }

    // this sets all the labels/values in an initial state, depending on the chosen options
    private void setupElement(String queryElement, TextView value, TextView label) {

        //set element label/value to default value first
        label.setBackgroundResource(0);
        value.setVisibility(View.VISIBLE);
        value.setText("");
        label.setText("");

        switch (queryElement) {
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
                value.setText(R.string.zeroVolt);
                label.setBackground(getContext().getDrawable(R.drawable.ic_battery));
                break;
            case "coolantTemperature":
                label.setText("");
                value.setText(R.string.zeroCelcius);
                label.setBackground(getContext().getDrawable(R.drawable.ic_water));
                break;
            case "oilTemperature":
                label.setText("");
                value.setText(R.string.zeroCelcius);
                label.setBackground(getContext().getDrawable(R.drawable.ic_oil));
                break;
            case "vehicleSpeed":
                label.setText(R.string.kmh);
                value.setText("0");
                label.setBackgroundResource(0);
                break;
            case "engineSpeed":
                label.setText(R.string.rpm);
                value.setText("0");
                label.setBackgroundResource(0);
                break;
            case "currentOutputPower":
                label.setText(R.string.kw);
                value.setText("0");
                label.setBackgroundResource(0);
                break;
            case "currentTorque":
                label.setText(R.string.nm);
                value.setText("0");
                label.setBackgroundResource(0);
                break;
            case "gearboxOilTemperature":
                label.setText("");
                value.setText(R.string.zeroCelcius);
                label.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "outsideTemperature":
                label.setText("");
                value.setText(R.string.zeroCelcius);
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

    private void setupClock(String queryClock, Speedometer clock, TextView icon, RaySpeedometer ray, Speedometer min, Speedometer max) {

        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.themedEmptyDialBackground});
        int emptyBackgroundResource = typedArray.getResourceId(0, 0);
        typedArray.recycle();

        switch (queryClock) {
            case "none":
                icon.setText("");
                clock.setUnit("");
                icon.setBackgroundResource(0);
                break;
            case "test":
                icon.setText("");
                clock.setUnit(getString(R.string.testing));
                clock.setMinMaxSpeed(-100, 200);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_measurement));
                break;
            case "vehicleSpeed":
                icon.setText("");
                clock.setUnit(getString(R.string.kmh));
                clock.setMinMaxSpeed(0, 350);
                icon.setBackgroundResource(0);
                clock.setBackgroundResource(emptyBackgroundResource);
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "engineSpeed":
                icon.setText("");
                clock.setUnit(getString(R.string.rpm));
                clock.setMinMaxSpeed(0, 8000);
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                icon.setBackgroundResource(0);
                break;
            case "batteryVoltage":
                icon.setText("");
                clock.setUnit(getString(R.string.volt));
                clock.setMinMaxSpeed(0, 16);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                clock.setBackgroundResource(emptyBackgroundResource);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_battery));
                break;
            case "oilTemperature":
                icon.setText("");
                clock.setUnit("°");
                clock.setMinMaxSpeed(0, 150);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_oil));
                break;
            case "coolantTemperature":
                icon.setText("");
                clock.setUnit("°");
                clock.setMinMaxSpeed(0, 150);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_water));
                break;
            case "outsideTemperature":
                icon.setText("");
                clock.setUnit("°");
                clock.setMinMaxSpeed(-25, 50);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                clock.setBackgroundResource(emptyBackgroundResource);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_outsidetemperature));
                break;
            case "gearboxOilTemperature":
                icon.setText("");
                clock.setUnit("°");
                clock.setMinMaxSpeed(0, 150);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "absChargingAirPressure":
                icon.setText("");
                clock.setUnit(pressureUnit);
                clock.setMinMaxSpeed(pressureMin, pressureMax);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_turbo));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "relChargingAirPressure":
                icon.setText("");
                clock.setUnit(pressureUnit);
                clock.setMinMaxSpeed(pressureMin, pressureMax);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_turbo));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "lateralAcceleration":
                icon.setText("");
                clock.setUnit(getString(R.string.g));
                clock.setMinMaxSpeed(-3, 3);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_lateral));
                clock.setBackgroundResource(emptyBackgroundResource);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "longitudinalAcceleration":
                icon.setText("");
                clock.setUnit(getString(R.string.g));
                clock.setMinMaxSpeed(-3, 3);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_longitudinal));
                clock.setBackgroundResource(emptyBackgroundResource);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "yawRate":
                icon.setText("");
                clock.setUnit("%");
                clock.setMinMaxSpeed(-1, 1);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_yaw));
                clock.setBackgroundResource(emptyBackgroundResource);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "wheelAngle":
                icon.setText("");
                clock.setUnit("°");
                clock.setMinMaxSpeed(-45, 45);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_wheelangle));
                clock.setBackgroundResource(emptyBackgroundResource);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "EcoHMI_Score.AvgShort":
                icon.setText("");
                clock.setUnit("");
                clock.setMinMaxSpeed(0, 100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                clock.setBackgroundResource(emptyBackgroundResource);
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "EcoHMI_Score.AvgTrip":
                icon.setText("");
                clock.setUnit("");
                clock.setMinMaxSpeed(0, 100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                clock.setBackgroundResource(emptyBackgroundResource);
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "powermeter":
                icon.setText("");
                clock.setUnit("");
                clock.setMinMaxSpeed(0, 5000);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_powermeter));
                clock.setBackgroundResource(emptyBackgroundResource);
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "acceleratorPosition":
                icon.setText("");
                clock.setUnit("%");
                clock.setMinMaxSpeed(0, 100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_pedalposition));
                clock.setBackgroundResource(emptyBackgroundResource);
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "brakePressure":
                icon.setText("");
                clock.setUnit("%");
                clock.setMinMaxSpeed(0, 100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_brakepedalposition));
                clock.setBackgroundResource(emptyBackgroundResource);
                clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
                break;
            case "currentTorque":
                icon.setText("");
                clock.setUnit(getString(R.string.nm));
                clock.setMinMaxSpeed(0, 500);
                icon.setBackgroundResource(0);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                clock.setBackgroundResource(emptyBackgroundResource);
                break;
            case "currentOutputPower":
                icon.setText("");
                clock.setUnit(getString(R.string.kw));
                clock.setMinMaxSpeed(0, 500);
                icon.setBackgroundResource(0);
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                clock.setBackgroundResource(emptyBackgroundResource);
                break;
            case "currentConsumptionPrimary":
                icon.setText("");
                clock.setUnit("l/h");
                clock.setMinMaxSpeed(0, 100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_fuelprimary));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "currentConsumptionSecondary":
                icon.setText("");
                clock.setUnit("l/h");
                clock.setMinMaxSpeed(0, 100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_fuelsecondary));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "cycleConsumptionPrimary":
                icon.setText("");
                clock.setUnit("l/h");
                clock.setMinMaxSpeed(0, 100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_fuelprimary));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
            case "cycleConsumptionSecondary":
                icon.setText("");
                clock.setUnit("l/h");
                clock.setMinMaxSpeed(0, 100);
                icon.setBackground(getContext().getDrawable(R.drawable.ic_fuelsecondary));
                clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
                break;
        }

        float minimum = clock.getMinSpeed();
        float maximum = clock.getMaxSpeed();

        min.setMinMaxSpeed(minimum, maximum);
        ray.setMinMaxSpeed(minimum, maximum);
        max.setMinMaxSpeed(minimum, maximum);
    }

    //update clock with data
    private void updateClock(String query, Speedometer dial, RaySpeedometer visray, TextView textmax, TextView textmin, Speedometer clockmax, Speedometer clockmin) {

        if (query == null) {
            return;
        } else {

            Float clockValue = (Float) mLastMeasurements.get(query);
            float randomClockVal = randFloat(-100, 200);
            speedFactor = 1f;
            pressureFactor = 1f;

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
                case "outsideTemperature":
                case "gearboxOilTemperature":
                case "lateralAcceleration":
                case "longitudinalAcceleration":
                case "yawRate":
                case "EcoHMI_Score.AvgShort":
                case "EcoHMI_Score.AvgTrip":
                case "brakePressure":
                case "currentTorque":
                case "currentOutputPower":
                    if (clockValue != null) {
                        dial.speedTo(clockValue,500);
                    }
                    break;
                // pressures
                case "absChargingAirPressure":
                case "relChargingAirPressure":
                    if (clockValue != null) {
                        clockValue = clockValue * pressureFactor;
                        dial.speedTo(clockValue,500);
                    }
                    break;
                // specific case for wheel angle, since it needs to be turned around
                case "wheelAngle":
                    if (clockValue != null) {
                        clockValue = clockValue * -1; // make it negative, otherwise right = left and vice versa
                        dial.speedTo(clockValue,500);
                    }
                    break;
                    // hybrid power has 1020 as value 0.
                case "powermeter":
                    if (clockValue != null) {
                        clockValue = clockValue - 1020;
                        dial.speedTo(clockValue,500);
                    }

                    // percentages
                case "acceleratorPosition":
                    if (clockValue != null) {
                        float accelPercent = clockValue * 100;
                        dial.speedTo(accelPercent,500);
                    }
                    break;
                // specific consumption data with specific consumption units
                // todo: maybe it's better to remove setting the unit from updateclock, but do it on setupclock
                case "currentConsumptionPrimary":
                    String consumptionUnit = (String) mLastMeasurements.get("currentConsumptionPrimary.unit");
                    if (clockValue != null && consumptionUnit != null) {
                        dial.setUnit(consumptionUnit);
                        dial.speedTo(clockValue,500);
                    }
                    break;
                case "currentConsumptionSecondary":
                    String consumption2Unit = (String) mLastMeasurements.get("currentConsumptionSecondary.unit");
                    if (clockValue != null && consumption2Unit != null) {
                        dial.setUnit(consumption2Unit);
                        dial.speedTo(clockValue,500);
                    }
                    break;
                case "cycleConsumptionPrimary":
                    String cycconsumptionUnit = (String) mLastMeasurements.get("cycleConsumptionPrimary.unit");
                    if (clockValue != null && cycconsumptionUnit != null) {
                        dial.setUnit(cycconsumptionUnit);
                        dial.speedTo(clockValue,500);
                    }
                    break;
                case "cycleConsumptionSecondary":
                    String cycconsumption2Unit = (String) mLastMeasurements.get("cycleConsumptionSecondary.unit");
                    if (clockValue != null && cycconsumption2Unit != null) {
                        dial.setUnit(cycconsumption2Unit);
                        dial.speedTo(clockValue,500);
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
                        dial.speedTo(clockValue,500);
                    }
                    break;
            }
            // get the speed from the clock and have the high-visibility rays move to this speed as well
            float tempValue = dial.getSpeed();
            visray.speedTo(tempValue,500);

            // update the max clocks and text
            Float maxValue = clockmax.getSpeed();
            if (tempValue > maxValue) {
                clockmax.setSpeedAt(tempValue);
                textmax.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), tempValue));
            }

            // update the min clocks and text
            Float minValue = clockmin.getSpeed();
            if (tempValue < minValue) {
                clockmin.setSpeedAt(tempValue);
                textmin.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), tempValue));
            }
        }
    }

    //update the elements
    private void updateElement(String queryElement, TextView value, TextView label) {

        if (queryElement == null) {
            return;
        } else {
            switch (queryElement) {
                case "none":
                    value.setText("");
                    break;
                case "test":
                    float randomValue = randFloat(0, 100);
                    value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), randomValue));
                    break;
                case "batteryVoltage":
                    Float mBatteryVoltage = (Float) mLastMeasurements.get("batteryVoltage");
                    if (mBatteryVoltage != null) {
                        value.setText(String.format(Locale.US, getContext().getText(R.string.volt_format).toString(), mBatteryVoltage));
                    }
                    break;

                    // all temperatures can be handled in the same way, the only difference is the queryElement string
                case "coolantTemperature":
                case "oilTemperature":
                case "gearboxOilTemperature":
                case "outsideTemperature":
                    Float mTemperature = (Float) mLastMeasurements.get(queryElement);
                    if (mTemperature!= null) {
                        value.setText(String.format(Locale.US, getContext().getText(R.string.temp_format).toString(), mTemperature));
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

                    // Decimal values, without any specific modification:
                case "currentOutputPower":
                case "currentTorque":
                    Float mCurrentDecimalValue = (Float) mLastMeasurements.get(queryElement);
                    if (mCurrentDecimalValue != null) {
                        value.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), mCurrentDecimalValue));
                    }
                    break;

                case "currentGear":
                    Boolean reverseGear = (Boolean) mLastMeasurements.get("reverseGear.engaged");
                    Boolean parkingBrake = (Boolean) mLastMeasurements.get("parkingBrake.engaged");
                    String currentGear = (String) mLastMeasurements.get("currentGear");
                    String recommendedGear = (String) mLastMeasurements.get("recommendedGear");
                    String gearText="-";

                    if (parkingBrake != null && parkingBrake) {
                        value.setTextColor(Color.WHITE);
                        gearText = "P";
                    } else if (reverseGear != null && reverseGear) {
                        value.setTextColor(Color.WHITE);
                        gearText = "R";
                    } else if (currentGear == null) {
                        value.setTextColor(Color.WHITE);
                        gearText = "-";
                    } else if (currentGear != null && recommendedGear != null) {
                        if (recommendedGear.equals(currentGear) || recommendedGear.equals("NoRecommendation")) {
                            value.setTextColor(Color.WHITE);
                            gearText = convGear(currentGear);
                        } else if (!recommendedGear.equals(currentGear)){
                            value.setTextColor(Color.RED);
                            gearText = (convGear(currentGear) + "▶" + convGear(recommendedGear));
                        }
                    }
                    value.setText(gearText);
                    break;
                case "lateralAcceleration":
                case "longitudinalAcceleration":
                    Float mAcceleration = (Float) mLastMeasurements.get(queryElement);
                    if (mAcceleration != null) {
                        value.setText(String.format(Locale.US, getContext().getText(R.string.gforce_format).toString(), mAcceleration));
                    }
                    break;
                case "yawRate":
                    Float mYawRate = (Float) mLastMeasurements.get(queryElement);
                    if (mYawRate != null) {
                        value.setText(String.format(Locale.US, getContext().getText(R.string.percent_format).toString(), mYawRate));
                    }
                    break;
                case "acceleratorPosition":
                    Float mAcceleratorPosition = (Float) mLastMeasurements.get("acceleratorPosition");
                    if (mAcceleratorPosition != null) {
                        Float mAccelPosPercent = mAcceleratorPosition * 100;
                        value.setText(String.format(Locale.US, getContext().getText(R.string.percent_format).toString(), mAccelPosPercent));
                    }
                    break;
                case "brakePressure":
                    Float mBrakePressure = (Float) mLastMeasurements.get("brakePressure");
                    if (mBrakePressure != null) {
                        value.setText(String.format(Locale.US, getContext().getText(R.string.percent_format).toString(), mBrakePressure));
                    }
                    break;
                case "wheelAngle":
                    Float mWheelAngle = (Float) mLastMeasurements.get(queryElement);
                    if (mWheelAngle != null) {
                        value.setText(String.format(Locale.US, getContext().getText(R.string.degrees_format).toString(), mWheelAngle));
                    }
                    break;
                case "powermeter":
                    Float mPowermeter = (Float) mLastMeasurements.get(queryElement);
                    if (mPowermeter != null) {
                        value.setText(String.format(Locale.US, getContext().getText(R.string.no_decimals).toString(), mPowermeter));
                    }
                    break;

                // eco values
                case "EcoHMI_Score.AvgShort":
                case "EcoHMI_Score.AvgTrip":
                    Float mEcoScore = (Float) mLastMeasurements.get(queryElement);
                    if (mEcoScore != null) {
                        value.setText(String.format(Locale.US, getContext().getText(R.string.no_decimals).toString(), mEcoScore));
                    }
                    break;
            }
        }
    }

    
    
    
    
    

}