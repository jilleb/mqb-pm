package com.mqbcoding.stats;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.cardview.widget.CardView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.anastr.speedviewlib.Gauge;
import com.github.anastr.speedviewlib.RaySpeedometer;
import com.github.anastr.speedviewlib.Speedometer;
import com.github.anastr.speedviewlib.components.Indicators.ImageIndicator;
import com.github.anastr.speedviewlib.components.Indicators.Indicator;
import com.github.martoreto.aauto.vex.CarStatsClient;
import com.google.android.apps.auto.sdk.StatusBarController;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.prowl.torque.remote.ITorqueService;

import java.math.BigInteger;
import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class DashboardFragment extends CarFragment {
    private final String TAG = "DashboardFragment";
    private CarStatsClient mStatsClient;
    private WheelStateMonitor mWheelStateMonitor;
    private Speedometer mClockLeft, mClockCenter, mClockRight;
    private Speedometer mClockMaxLeft, mClockMaxCenter, mClockMaxRight;
    private Speedometer mClockMinLeft, mClockMinCenter, mClockMinRight;
    private RaySpeedometer mRayLeft, mRayCenter, mRayRight;
    private ImageView mSteeringWheelAngle;
    private String mElement1Query, mElement2Query, mElement3Query, mElement4Query;
    private String selectedTheme, selectedBackground;
    private String mClockLQuery, mClockCQuery, mClockRQuery;
    private String pressureUnit, temperatureUnit, selectedFont;
    private float pressureFactor, speedFactor, powerFactor;
    private int pressureMin, pressureMax;
    //icons/labels of the data elements. upper left, upper right, lower left, lower right.
    private TextView mIconElement1, mIconElement2, mIconElement3, mIconElement4;
    //values of the data elements. upper left, upper right, lower left, lower right.
    private TextView mValueElement1, mValueElement2, mValueElement3, mValueElement4, mTitleElement, mTitleElementRight, mTitleElementLeft;
    private ConstraintLayout mConstraintClockLeft, mConstraintClockRight, mConstraintClockCenter;
    private ConstraintLayout mConstraintGraphLeft, mConstraintGraphRight, mConstraintGraphCenter;
    private TextView mTextMinLeft, mTextMaxLeft;
    private TextView mTextMinCenter, mTextMaxCenter;
    private TextView mTextMinRight, mTextMaxRight;
    //icons on the clocks
    private TextView mIconClockL, mIconClockC, mIconClockR;
    private WheelStateMonitor.WheelState mWheelState;
    private Boolean pressureUnits, temperatureUnits,powerUnits;
    private Boolean stagingDone = false;
    private Boolean raysOn, maxOn, maxMarksOn, ticksOn, ambientOn;
    private Map<String, Object> mLastMeasurements = new HashMap<>();
    private Handler mHandler = new Handler();
    private ITorqueService torqueService;
    private boolean torqueBind = false;
    private GraphView mGraphLeft, mGraphCenter, mGraphRight;
    private CardView mGraphLayoutLeft, mGraphLayoutCenter, mGraphLayoutRight;
    private LineGraphSeries<DataPoint> mSpeedSeriesLeft;
    private LineGraphSeries<DataPoint> mSpeedSeriesCenter;
    private LineGraphSeries<DataPoint> mSpeedSeriesRight;
    private View mBackGroundLayout;
    private double graphLeftLastXValue = 5d;
    private double graphCenterLastXValue = 5d;
    private double graphRightLastXValue = 5d;
    //value displayed on graphlayout
    private TextView mGraphValueLeft, mGraphValueCenter, mGraphValueRight;
    private View rootView;

    int dashboardNum=1;

    public DashboardFragment() {
        // Required empty public constructor

    }
    public DashboardFragment(int dashboardNum) {
        this.dashboardNum=dashboardNum;
        if (this.dashboardNum<=1) {
            this.dashboardNum=1;
        }
    }

    @Override
    protected void setupStatusBar(StatusBarController sc) {
        sc.hideTitle();
    }


    // todo: reset min/max when clock is touched

    private View.OnClickListener resetMinMax = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            float speedLeft = mClockLeft.getSpeed();
            float speedCenter = mClockCenter.getSpeed();
            float speedRight = mClockRight.getSpeed();

            mClockMaxLeft.speedTo(speedLeft);
            mClockMinLeft.speedTo(speedLeft);
            mClockMinCenter.speedTo(speedCenter);
            mClockMaxCenter.speedTo(speedCenter);
            mClockMaxRight.speedTo(speedRight);
            mClockMinRight.speedTo(speedRight);

            mTextMaxLeft.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), speedLeft));
            mTextMinLeft.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), speedLeft));
            mTextMaxCenter.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), speedCenter));
            mTextMinCenter.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), speedCenter));
            mTextMinRight.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), speedRight));
            mTextMaxRight.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), speedRight));
            postUpdate();

        }
    };

    private View.OnClickListener toggleView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v==mConstraintClockLeft){
                fadeOutfadeIn(v, mConstraintGraphLeft);
            } else if (v==mConstraintClockCenter) {
                fadeOutfadeIn(v, mConstraintGraphCenter);
            } else if (v==mConstraintClockRight) {
                fadeOutfadeIn(v, mConstraintGraphRight);
            } else if (v == mGraphLeft) {
                fadeOutfadeIn(mConstraintGraphLeft, mConstraintClockLeft);
            } else if (v == mGraphCenter) {
                fadeOutfadeIn(mConstraintGraphCenter, mConstraintClockCenter);
            } else if (v == mGraphRight) {
                fadeOutfadeIn(mConstraintGraphRight, mConstraintClockRight);
            }
        }
    };

    private final CarStatsClient.Listener mCarStatsListener = new CarStatsClient.Listener() {
        @Override
        public void onNewMeasurements(String provider, Date timestamp, Map<String, Object> values) {
            mLastMeasurements.putAll(values);

            postUpdate();

            //Log.i(TAG, "onCarStatsClient.Listener");
        }

        @Override
        public void onSchemaChanged() {
            // do nothing
        }


    };

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CarStatsService.CarStatsBinder carStatsBinder = (CarStatsService.CarStatsBinder) iBinder;
            Log.i(TAG, "ServiceConnected");
            mStatsClient = carStatsBinder.getStatsClient();
            mWheelStateMonitor = carStatsBinder.getWheelStateMonitor();
            mLastMeasurements = mStatsClient.getMergedMeasurements();
            mStatsClient.registerListener(mCarStatsListener);
            doUpdate();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mStatsClient.unregisterListener(mCarStatsListener);
            Log.i(TAG, "ServiceDisconnected");
        }
    };


    // random, for use in Test value
    public static float randFloat(float min, float max) {
        Random rand = new Random();
        return rand.nextFloat() * (max - min) + min;
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
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
        startTorque();
        updateDisplay();

    }

    private void updateDisplay() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                postUpdate();
            }

        }, 0, 500);//Update display 0.5 second
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);

        //Get preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());

        pressureUnits = sharedPreferences.getBoolean("selectPressureUnit", true);  //true = bar, false = psi
        temperatureUnits = sharedPreferences.getBoolean("selectTemperatureUnit", true);  //true = celcius, false = fahrenheit
        powerUnits = sharedPreferences.getBoolean("selectPowerUnit", true);  //true = kw, false = ps

        raysOn = sharedPreferences.getBoolean("highVisActive", false);  //true = show high vis rays, false = don't show them.
        maxOn = sharedPreferences.getBoolean("maxValuesActive", false); //true = show max values, false = hide them
        maxMarksOn = sharedPreferences.getBoolean("maxMarksActive", false); //true = show max values as a mark on the clock, false = hide them
        selectedFont = sharedPreferences.getString("selectedFont", "segments");
        ticksOn = sharedPreferences.getBoolean("ticksActive", false); // if true, it will display the value of each of the ticks
        ambientOn = sharedPreferences.getBoolean("ambientActive", false);  //true = use ambient colors, false = don't use.
        selectedTheme = sharedPreferences.getString("selectedTheme", "");
        //todo: fix this. currently not very efficient, because this is already requested in MainCarActivity
        selectedBackground = sharedPreferences.getString("selectedBackground", "background_incar_black");

        //Set wallpaper
        int resId = getResources().getIdentifier(selectedBackground, "drawable", getContext().getPackageName());

        if (resId != 0) {
            Drawable wallpaperImage = getResources().getDrawable(resId);
            rootView.setBackground(wallpaperImage);
        }

        //set textview to have a custom digital font:
        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "digital.ttf");
        switch (selectedFont) {
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
            case "larabie":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "Larabie.ttf");
                break;
            case "ford":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "UnitedSans.otf");
                break;
        }

        Log.d(TAG, "font: " + typeface);

        //-------------------------------------------------------------
        //find all elements needed

        mBackGroundLayout = rootView.findViewById(R.id.fragment_container);

        //layouts/constrains:
        mConstraintClockLeft = rootView.findViewById(R.id.constraintClockLeft);
        mConstraintClockCenter = rootView.findViewById(R.id.constraintClockCenter);
        mConstraintClockRight = rootView.findViewById(R.id.constraintClockRight);

        mConstraintGraphLeft = rootView.findViewById(R.id.constraintGraphLeft);
        mConstraintGraphCenter = rootView.findViewById(R.id.constraintGraphCenter);
        mConstraintGraphRight = rootView.findViewById(R.id.constraintGraphRight);

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

        //graph test
        mGraphLeft = (GraphView) rootView.findViewById(R.id.chart_Left);
        mGraphCenter = (GraphView) rootView.findViewById(R.id.chart_Center);
        mGraphRight = (GraphView) rootView.findViewById(R.id.chart_Right);

        mGraphValueLeft = rootView.findViewById(R.id.graphValueLeft);
        mGraphValueCenter = rootView.findViewById(R.id.graphValueCenter);
        mGraphValueRight = rootView.findViewById(R.id.graphValueRight);

        mGraphLayoutLeft = (CardView) rootView.findViewById(R.id.left_layout);
        mGraphLayoutCenter = (CardView) rootView.findViewById(R.id.center_layout);
        mGraphLayoutRight = (CardView) rootView.findViewById(R.id.right_layout);

        mSpeedSeriesLeft = new LineGraphSeries<>();
        mSpeedSeriesCenter = new LineGraphSeries<>();
        mSpeedSeriesRight = new LineGraphSeries<>();

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

        //title text element
        mTitleElement = rootView.findViewById(R.id.textTitleElement);
        mTitleElementRight = rootView.findViewById(R.id.textTitleElementRight);
        mTitleElementLeft = rootView.findViewById(R.id.textTitleElementLeft);

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

        //-------------------------------------------------------------
        //Give them all the right custom typeface
        //clocks
        mClockLeft.setSpeedTextTypeface(typeface);
        mClockCenter.setSpeedTextTypeface(typeface);
        mClockRight.setSpeedTextTypeface(typeface);
        mGraphValueLeft.setTypeface(typeface);
        mGraphValueCenter.setTypeface(typeface);
        mGraphValueRight.setTypeface(typeface);

        if (selectedTheme.equals("Beetle")) {
            Typeface beetletypeface = Typeface.createFromAsset(getContext().getAssets(), "Schluber.ttf");
            mClockLeft.setTextTypeface(beetletypeface);
            mClockCenter.setTextTypeface(beetletypeface);
            mClockRight.setTextTypeface(beetletypeface);
            mClockMaxLeft.setWithTremble(false);
            mClockMinLeft.setWithTremble(false);
            mClockMaxCenter.setWithTremble(false);
            mClockMinCenter.setWithTremble(false);
            mClockMaxRight.setWithTremble(false);
            mClockMinRight.setWithTremble(false);
        }

        //elements
        mValueElement1.setTypeface(typeface);
        mValueElement2.setTypeface(typeface);
        mValueElement3.setTypeface(typeface);
        mValueElement4.setTypeface(typeface);
        mTitleElement.setTypeface(typeface);
        mTitleElementRight.setTypeface(typeface);
        mTitleElementLeft.setTypeface(typeface);

        //max
        mTextMinLeft.setTypeface(typeface);
        mTextMaxLeft.setTypeface(typeface);
        mTextMinCenter.setTypeface(typeface);
        mTextMaxCenter.setTypeface(typeface);
        mTextMinRight.setTypeface(typeface);
        mTextMaxRight.setTypeface(typeface);

        mSteeringWheelAngle = rootView.findViewById(R.id.wheel_angle_image);

        //click the
        mTitleElement.setOnClickListener(resetMinMax);
        mGraphLeft.setOnClickListener(toggleView);
        mConstraintClockLeft.setOnClickListener(toggleView);
        mGraphCenter.setOnClickListener(toggleView);
        mConstraintClockCenter.setOnClickListener(toggleView);
        mGraphRight.setOnClickListener(toggleView);
        mConstraintClockRight.setOnClickListener(toggleView);

        String dashboardId = "d"+dashboardNum+"_";
        //determine what data the user wants to have on the 4 data views
        mElement1Query = sharedPreferences.getString(dashboardId+"selectedView1", "none");
        mElement2Query = sharedPreferences.getString(dashboardId+"selectedView2", "none");
        mElement3Query = sharedPreferences.getString(dashboardId+"selectedView3", "none");
        mElement4Query = sharedPreferences.getString(dashboardId+"selectedView4", "none");

        //determine what data the user wants to have on the 3 clocks, but set defaults first
        mClockLQuery = sharedPreferences.getString(dashboardId+"selectedClockLeft", "exlap-batteryVoltage");
        mClockCQuery = sharedPreferences.getString(dashboardId+"selectedClockCenter", "exlap-oilTemperature");
        mClockRQuery = sharedPreferences.getString(dashboardId+"selectedClockRight", "exlap-engineSpeed");

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
        pressureUnit = "bar";

        //set pressure dial to the wanted units
        //Most bar dials go from -2 to 3 bar.
        //Most PSI dials go from -30 to 30 psi.
        //pressurefactor is used to calculate the right value for psi later
        if (pressureUnits) {
            pressureFactor = 1;
            pressureUnit = "bar";
            pressureMin = -3;
            pressureMax = 3;

        } else {
            pressureFactor = (float) 14.5037738;
            pressureUnit = "psi";
            pressureMin = -30;
            pressureMax = 30;
        }

        if (powerUnits) {
            powerFactor= 1;
            //powerUnit = "KW";

        } else {
            powerFactor = 1.35962f;
            //pressureUnit = "HP";
        }

        if (temperatureUnits) {
            temperatureUnit = getString(R.string.unit_c);

        } else {
            temperatureUnit = getString(R.string.unit_f);
        }

        // build ImageIndicator using the resourceId
        // get the size of the Clock, to make sure the imageindicator has the right size.
        mClockLeft.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mClockLeft.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int clockSize = mClockLeft.getHeight();
                if (clockSize == 0) {
                    clockSize = 250;
                }
                //this is to enable an image as indicator.
                TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.themedNeedle});
                int resourceId = typedArray.getResourceId(0, 0);
                typedArray.recycle();

                ImageIndicator imageIndicator = new ImageIndicator(getContext(), resourceId, clockSize, clockSize);

                int color = mClockLeft.getIndicatorColor();
                // if rays on, turn off everything else.
                // it doesn't look too efficient at the moment, but that's to prevent the theme from adding an indicator to the rays.
                if (raysOn) {
                    // todo: move this to setupClock

                    mClockLeft.setIndicator(Indicator.Indicators.NoIndicator);
                    mClockCenter.setIndicator(Indicator.Indicators.NoIndicator);
                    mClockRight.setIndicator(Indicator.Indicators.NoIndicator);

                    mRayLeft.setIndicator(Indicator.Indicators.NoIndicator);
                    mRayRight.setIndicator(Indicator.Indicators.NoIndicator);
                    mRayCenter.setIndicator(Indicator.Indicators.NoIndicator);

                    //make indicatorlight color transparent if you don't need it:
                    mClockLeft.setIndicatorLightColor(Color.parseColor("#00FFFFFF"));
                    mClockCenter.setIndicatorLightColor(Color.parseColor("#00FFFFFF"));
                    mClockRight.setIndicatorLightColor(Color.parseColor("#00FFFFFF"));
//
                    mRayLeft.setIndicatorLightColor(Color.parseColor("#00FFFFFF"));
                    mRayRight.setIndicatorLightColor(Color.parseColor("#00FFFFFF"));
                    mRayCenter.setIndicatorLightColor(Color.parseColor("#00FFFFFF"));


                } else if (color == -14575885) {
                    //if theme has transparent indicator color, give clocks a custom image indicator
                    //todo: do this on other fragments as well
                    mClockLeft.setIndicator(imageIndicator);
                    mClockCenter.setIndicator(imageIndicator);
                    mClockRight.setIndicator(imageIndicator);
                }

                setupGraph(mClockLeft, mGraphLeft, mSpeedSeriesLeft, mConstraintGraphLeft);
                setupGraph(mClockCenter, mGraphCenter, mSpeedSeriesCenter, mConstraintGraphCenter);
                setupGraph(mClockRight, mGraphRight, mSpeedSeriesRight, mConstraintGraphRight);

                if (ticksOn) {

                    int tickNum = 9;
                    if (selectedTheme.equals("Beetle")) tickNum = 7; //special for Beetle theme

                    mClockLeft.setTickNumber(tickNum);
                    mClockLeft.setTextColor(Color.WHITE);
                    mClockCenter.setTickNumber(tickNum);
                    mClockCenter.setTextColor(Color.WHITE);
                    mClockRight.setTickNumber(tickNum);
                    mClockRight.setTextColor(Color.WHITE);
                }

                //initiating staging:
                if (!stagingDone) {

                    mClockLeft.speedPercentTo(100, 1000);
                    mClockCenter.speedPercentTo(100, 1000);
                    mClockRight.speedPercentTo(100, 1000);
                    mRayLeft.speedPercentTo(100, 1000);
                    mRayCenter.speedPercentTo(100, 1000);
                    mRayRight.speedPercentTo(100, 1000);

                    final Handler staging = new Handler();
                    staging.postDelayed(new Runnable() {
                        public void run() {
                            if (mClockLeft != null) {
                                mClockLeft.speedTo(0, 1000);
                                mClockCenter.speedTo(0, 1000);
                                mClockRight.speedTo(0, 1000);
                                mRayLeft.speedTo(0, 1000);
                                mRayCenter.speedTo(0, 1000);
                                mRayRight.speedTo(0, 1000);
                            }
                        }
                    }, 1000);

                    final Handler stagingReset = new Handler();
                    stagingReset.postDelayed(new Runnable() {
                        public void run() {
                            if (mClockLeft != null) {
                                mClockMaxLeft.speedTo(mClockLeft.getSpeed(), 1000);
                                mClockMinLeft.speedTo(mClockLeft.getSpeed(), 1000);
                                mClockMinCenter.speedTo(mClockCenter.getSpeed(), 1000);
                                mClockMaxCenter.speedTo(mClockCenter.getSpeed(), 1000);
                                mClockMaxRight.speedTo(mClockRight.getSpeed(), 1000);
                                mClockMinRight.speedTo(mClockRight.getSpeed(), 1000);

                                mTextMaxLeft.setText("-");
                                mTextMinLeft.setText("-");
                                mTextMaxCenter.setText("-");
                                mTextMinCenter.setText("-");
                                mTextMinRight.setText("-");
                                mTextMaxRight.setText("-");
                                stagingDone = true;

                            }
                        }
                    }, 2000);


                }
            }

        });

        //set up each of the elements with the query and icon that goes with it
        setupElement(mElement1Query, mValueElement1, mIconElement1);
        setupElement(mElement2Query, mValueElement2, mIconElement2);
        setupElement(mElement3Query, mValueElement3, mIconElement3);
        setupElement(mElement4Query, mValueElement4, mIconElement4);

        //setup clocks, including the max/min clocks and highvis rays and icons:
        //usage: setupClocks(query value, what clock, what icon, which ray, which min clock, which max clock)
        //could probably be done MUCH more efficient but that's for the future ;)
        setupClocks(mClockLQuery, mClockLeft, mIconClockL, mRayLeft, mClockMinLeft, mClockMaxLeft);
        setupClocks(mClockCQuery, mClockCenter, mIconClockC, mRayCenter, mClockMinCenter, mClockMaxCenter);
        setupClocks(mClockRQuery, mClockRight, mIconClockR, mRayRight, mClockMinRight, mClockMaxRight);

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

        } else { // don't show any of it
            mTextMaxLeft.setVisibility(View.INVISIBLE);
            mTextMaxCenter.setVisibility(View.INVISIBLE);
            mTextMaxRight.setVisibility(View.INVISIBLE);

            mTextMinLeft.setVisibility(View.INVISIBLE);
            mTextMinCenter.setVisibility(View.INVISIBLE);
            mTextMinRight.setVisibility(View.INVISIBLE);

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

        //update!
        doUpdate();

        return rootView;
    }

    private String currentLocationFromGoogleMapsNotification = null;

    private BroadcastReceiver onNoticeGoogleNavigationUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String title = intent.getStringExtra("title");
            String text = intent.getStringExtra("text");

            currentLocationFromGoogleMapsNotification = title;

            updateTitle();
        }
    };
    private BroadcastReceiver onNoticeGoogleNavigationClosed = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            currentLocationFromGoogleMapsNotification=null;

            updateTitle();
        }
    };

    @Override
    public void onStart() {
        super.onStart();
        Log.i(TAG, "onStart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onActivate");
        Intent serviceIntent = new Intent(getContext(), CarStatsService.class);
        getContext().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        try {
            // Bind to the torque service
            Intent intent = new Intent();
            intent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");
            boolean successfulBind = getContext().bindService(intent, torqueConnection, 0);

            if (!successfulBind) {
                throw new Exception("Couldn't connect to Torque");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error: " + e.getMessage());
        }

        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(onNoticeGoogleNavigationUpdate, new IntentFilter("GoogleNavigationUpdate"));
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(onNoticeGoogleNavigationClosed, new IntentFilter("GoogleNavigationClosed"));
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onDeactivate");
        mStatsClient.unregisterListener(mCarStatsListener);
        getContext().unbindService(mServiceConnection);
        //getContext().unbindService(torqueConnection);
        if (torqueBind)
            try {
                getContext().unbindService(torqueConnection);
            } catch (Exception E) {
                throw E;
            }

        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(onNoticeGoogleNavigationUpdate);
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(onNoticeGoogleNavigationClosed);

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
        mSteeringWheelAngle = null;
        mValueElement1 = null;
        mValueElement2 = null;
        mValueElement3 = null;
        mValueElement4 = null;
        mTitleElement = null;
        mTitleElementRight = null;
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
        selectedFont = null;
        pressureUnit = null;
        stagingDone = false;
        mGraphCenter = null;
        mGraphLeft = null;
        mGraphRight = null;
        mSpeedSeriesCenter = null;
        mSpeedSeriesLeft = null;
        mSpeedSeriesRight = null;
        mConstraintClockLeft = null;
        mConstraintClockRight = null;
        mConstraintClockCenter = null;
        mConstraintGraphLeft = null;
        mConstraintGraphRight = null;
        mConstraintGraphCenter = null;
        mGraphValueLeft = null;
        mGraphValueCenter = null;
        mGraphValueRight = null;

        if (torqueBind)
            try {
                //   getContext().unbindService(torqueConnection);
            } catch (Exception E) {
                throw E;
            }

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
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }


    private final static int UPDATE_AFTER = 200; //ms
    private long lastUpdate = -1;
    private void postUpdate() {

        /*
        mTimer1 = new Runnable() {
            public void run() {
                doUpdate();
            }
        };
        //experimental delay .. but why?? -> 5ms
        mHandler.postDelayed(mTimer1, 100);
        */


        if (lastUpdate<0 || (System.currentTimeMillis()-lastUpdate) > UPDATE_AFTER) {
            lastUpdate = System.currentTimeMillis();
            mHandler.post(new Runnable() {
                public void run() {
                    doUpdate();
                }
            });

        }

    }

    private void doUpdate() {

        if (mClockLeft == null) {
            return;
        }

        //wait until staging is done before displaying any data on the clocks.
        if (!stagingDone) {
            Log.d(TAG,"Staging not done yet");
            return;
        }

        //update each of the elements:
        updateElement(mElement1Query, mValueElement1, mIconElement1);
        updateElement(mElement2Query, mValueElement2, mIconElement2);
        updateElement(mElement3Query, mValueElement3, mIconElement3);
        updateElement(mElement4Query, mValueElement4, mIconElement4);

        //update each of the clocks and the min/max/ray elements that go with it
        // query, dial, visray, textmax, textmin, clockmax, clockmin)

        updateClock(mClockLQuery, mClockLeft, mRayLeft, mTextMaxLeft, mTextMinLeft, mClockMaxLeft, mClockMinLeft, mGraphLeft, mSpeedSeriesLeft, graphLeftLastXValue, mGraphValueLeft);
        updateClock(mClockCQuery, mClockCenter, mRayCenter, mTextMaxCenter, mTextMinCenter, mClockMaxCenter, mClockMinCenter, mGraphCenter, mSpeedSeriesCenter, graphCenterLastXValue, mGraphValueCenter);
        updateClock(mClockRQuery, mClockRight, mRayRight, mTextMaxRight, mTextMinRight, mClockMaxRight, mClockMinRight, mGraphRight, mSpeedSeriesRight, graphRightLastXValue, mGraphValueRight);

        updateTitle();

        // get ambient color, change color of some elements to match the ambient color.
        // this can't be done during setup, because then the ambientColor is probably not received yet.
        if (ambientOn) {
            String ambientColor =
                    mLastMeasurements.containsKey("Car_ambienceLightColour.ColourSRGB")?
                            (String) mLastMeasurements.get("Car_ambienceLightColour.ColourSRGB") : null;
            //ambientColor = "#FF0000"; // for testing purposes
            if (ambientColor != null && !ambientColor.equals("")) {
                if ((Color.parseColor(ambientColor) != mClockLeft.getIndicatorColor()) || ((Color.parseColor(ambientColor) != mRayLeft.getLowSpeedColor()))){
                    if (raysOn) {
                        mRayLeft.setLowSpeedColor(Color.parseColor(ambientColor));
                        mRayCenter.setLowSpeedColor(Color.parseColor(ambientColor));
                        mRayRight.setLowSpeedColor(Color.parseColor(ambientColor));
                        mRayLeft.setMediumSpeedColor(Color.parseColor(ambientColor));
                        mRayCenter.setMediumSpeedColor(Color.parseColor(ambientColor));
                        mRayRight.setMediumSpeedColor(Color.parseColor(ambientColor));
                    } else {
                        mClockLeft.setIndicatorColor(Color.parseColor(ambientColor));
                        mClockCenter.setIndicatorColor(Color.parseColor(ambientColor));
                        mClockRight.setIndicatorColor(Color.parseColor(ambientColor));
                        mClockLeft.setIndicatorLightColor(Color.parseColor(ambientColor));
                        mClockCenter.setIndicatorLightColor(Color.parseColor(ambientColor));
                        mClockRight.setIndicatorLightColor(Color.parseColor(ambientColor));
                    }

                    switch (selectedBackground) {
                        case "background_incar_dots":
                        case "background_incar_skoda2":
                            int resId = getResources().getIdentifier(selectedBackground, "drawable", getContext().getPackageName());
                            Drawable wallpaperImage = getResources().getDrawable(resId);

                            wallpaperImage.setColorFilter(new LightingColorFilter(Color.parseColor(ambientColor), Color.parseColor("#010101")));

                            rootView.setBackground(wallpaperImage);
                            break;
                    }


                }
            }
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
        value.setText("-");
        label.setText("");
        String icon = "";

        // set items to have a "-" as value.
        switch (queryElement) {
            //todo: clean this up. This can be done much nicer.
            case "test":
            case "torque_version":
            case "batteryVoltage":
            case "torque_voltage_0xff1238":
            case "Nav_Altitude":
            case "Nav_Heading":
            case "coolantTemperature":
            case "oilTemperature":
            case "vehicleSpeed":
            case "torque_speed_0x0d":
            case "torque_rpm_0x0c":
            case "engineSpeed":
            case "currentOutputPower":
            case "currentTorque":
            case "gearboxOilTemperature":
            case "outsideTemperature":
            case "currentGear":
            case "torque_accelerometer_total_0xff1223":
            case "lateralAcceleration":
            case "longitudinalAcceleration":
            case "yawRate":
            case "wheelAngle":
            case "acceleratorPosition":
            case "brakePressure":
            case "powermeter":
            case "EcoHMI_Score.AvgShort":
            case "EcoHMI_Score.AvgTrip":
            case "shortTermConsumptionPrimary":
            case "shortTermConsumptionSecondary":
            case "Nav_CurrentPosition.Longitude":
            case "Nav_CurrentPosition.Latitude":
            case "Nav_CurrentPosition.City":
            case "Nav_CurrentPosition.State":
            case "Nav_CurrentPosition.Country":
            case "Nav_CurrentPosition.Street":
            case "blinkingState":
            case "Sound_Volume":
            case "Radio_Tuner.Name":
            case "Radio_Text":
            case "totalDistance.distanceValue":
            case "vehicleIdenticationNumber.VIN":
            case "tyreStates.stateRearRight":
            case "tyreStates.stateRearLeft":
            case "tyreStates.stateFrontRight":
            case "tyreStates.stateFrontLeft":
            case "torque_fuelpressure_0x0a":
            case "torque_engineload_0x04":
            case "torque_timing_advance_0x0e":
            case "torque_intake_air_temperature_0x0f":
            case "torque_mass_air_flow_0x10":
            case "torque_throttle_position_0x11":
            case "torque_turboboost_0xff1202":
            case "torque_AFR_0xff1249":
            case "torque-AFRc_0xff124d":
            case "torque_fueltrimshortterm1_0x06":
            case "torque_fueltrimlongterm1_0x07":
            case "torque_fueltrimshortterm2_0x08":
            case "torque_fueltrimlongterm2_0x09":
                label.setText("");
                value.setText("-");
                break;
            case "none":
                label.setText("");
                value.setText("");
                icon = "empty";
                value.setVisibility(View.INVISIBLE);
                break;
        }

        // set the labels
        switch (queryElement) {
            //todo: clean this up. maybe get the needed icons/labels from arrays.xml, so it's not needed here.
            case "none":
                icon = "empty";
                break;
            case "test":
                label.setBackground(getContext().getDrawable(R.drawable.ic_measurement));
                break;
            case "torque_version":
                label.setBackground(getContext().getDrawable(R.drawable.ic_obd2));
                break;
            case "batteryVoltage":
            case "torque_voltage_0xff1238":
                value.setText("-");//value.setText(R.string.format_volt0);
                label.setBackground(getContext().getDrawable(R.drawable.ic_battery));
                break;
            case "Nav_Altitude":
                label.setBackground(getContext().getDrawable(R.drawable.ic_altitude));
                break;
            case "Nav_Heading":
                label.setBackground(getContext().getDrawable(R.drawable.ic_heading));
                break;
            case "coolantTemperature":
                label.setText("");
                value.setText("-");//value.setText(R.string.format_temperature0);
                label.setBackground(getContext().getDrawable(R.drawable.ic_water));
                break;
            case "oilTemperature":
                value.setText("-");//value.setText(R.string.format_temperature0);
                label.setBackground(getContext().getDrawable(R.drawable.ic_oil));
                break;
            case "vehicleSpeed":
            case "torque_speed_0x0d":
                label.setText(R.string.unit_kmh);
                icon = "empty";
                break;
            case "torque_rpm_0x0c":
            case "engineSpeed":
                label.setText(R.string.unit_rpm);
                icon = "empty";
                break;
            case "currentOutputPower":
                label.setText(powerUnits?getString(R.string.unit_kw):getString(R.string.unit_hp));
                icon = "empty";
                break;
            case "currentTorque":
                label.setText(R.string.unit_nm);
                icon = "empty";
                break;
            case "gearboxOilTemperature":
                value.setText("-");
                //value.setText(R.string.format_temperature0);
                label.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "outsideTemperature":
                value.setText("-");//value.setText(R.string.format_temperature0);
                label.setBackground(getContext().getDrawable(R.drawable.ic_outsidetemperature));
                break;
            case "currentGear":
                label.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "torque_accelerometer_total_0xff1223":
            case "lateralAcceleration":
                label.setBackground(getContext().getDrawable(R.drawable.ic_lateral));
                break;
            case "longitudinalAcceleration":
                label.setBackground(getContext().getDrawable(R.drawable.ic_longitudinal));
                break;
            case "yawRate":
                label.setBackground(getContext().getDrawable(R.drawable.ic_yaw));
                break;
            case "wheelAngle":
                label.setBackground(getContext().getDrawable(R.drawable.ic_wheelangle));
                break;
            case "acceleratorPosition":
                label.setBackground(getContext().getDrawable(R.drawable.ic_pedalposition));
                break;
            case "brakePressure":
                label.setBackground(getContext().getDrawable(R.drawable.ic_brakepedalposition));
                break;
            case "powermeter":
                label.setBackground(getContext().getDrawable(R.drawable.ic_powermeter));
                break;
            case "EcoHMI_Score.AvgShort":
                label.setBackground(getContext().getDrawable(R.drawable.ic_eco));
                break;
            case "EcoHMI_Score.AvgTrip":
                label.setBackground(getContext().getDrawable(R.drawable.ic_ecoavg));
                break;
            case "shortTermConsumptionPrimary":
                label.setBackground(getContext().getDrawable(R.drawable.ic_fuelprimary));
                break;
            case "shortTermConsumptionSecondary":
                label.setBackground(getContext().getDrawable(R.drawable.ic_fuelsecondary));
                break;
            case "Nav_CurrentPosition.Longitude":
            case "Nav_CurrentPosition.Latitude":
            case "Nav_CurrentPosition.City":
            case "Nav_CurrentPosition.State":
            case "Nav_CurrentPosition.Country":
            case "Nav_CurrentPosition.Street":
                label.setBackground(getContext().getDrawable(R.drawable.ic_world));
                break;
            case "blinkingState":
                break;
            case "Sound_Volume":
                label.setBackground(getContext().getDrawable(R.drawable.ic_volume));
                break;
            case "Radio_Tuner.Name":
            case "Radio_Text":
                label.setBackground(getContext().getDrawable(R.drawable.ic_radio));
                break;
            case "totalDistance.distanceValue":
                label.setBackground(getContext().getDrawable(R.drawable.ic_odometer));
                break;
            case "vehicleIdenticationNumber.VIN":
                label.setBackground(getContext().getDrawable(R.drawable.ic_vin));
                break;
            case "tyreStates.stateRearRight":
                label.setText(getString(R.string.label_tyreRR));
                label.setBackground(getContext().getDrawable(R.drawable.ic_tyre));
                break;
            case "tyreStates.stateRearLeft":
                label.setText(getString(R.string.label_tyreRL));
                label.setBackground(getContext().getDrawable(R.drawable.ic_tyre));
                break;
            case "tyreStates.stateFrontRight":
                label.setText(getString(R.string.label_tyreFR));
                label.setBackground(getContext().getDrawable(R.drawable.ic_tyre));
                break;
            case "tyreStates.stateFrontLeft":
                label.setText(getString(R.string.label_tyreFL));
                label.setBackground(getContext().getDrawable(R.drawable.ic_tyre));
                break;
            case "torque_fuelpressure_0x0a":
                label.setText(getString(R.string.label_fuel));
                icon = "empty";
                break;
            case "torque_engineload_0x04":
                label.setText(getString(R.string.label_load));
                icon = "empty";
                break;
            case "torque_timing_advance_0x0e":
                label.setText(getString(R.string.label_timing));
                icon = "empty";
                break;
            case "torque_intake_air_temperature_0x0f":
                label.setText(getString(R.string.label_iat));
                icon = "empty";
                break;
            case "torque_mass_air_flow_0x10":
                label.setText(getString(R.string.label_maf));
                icon = "empty";
                break;
            case "torque_throttle_position_0x11":
                label.setBackground(getContext().getDrawable(R.drawable.ic_throttle));
                break;
            case "torque_turboboost_0xff1202":
                label.setBackground(getContext().getDrawable(R.drawable.ic_turbo));
                break;
            case "torque_AFR_0xff1249":
                label.setText(getString(R.string.label_afr));
                icon = "empty";
                break;
            case "torque_AFRc_0xff124d":
                label.setText(getString(R.string.label_afrc));
                icon = "empty";
                break;
            case "torque_fueltrimshortterm1_0x06":
                label.setText(getString(R.string.label_ftst1));
                icon = "empty";
                break;
            case "torque_fueltrimlongterm1_0x07":
                label.setText(getString(R.string.label_ftlt1));
                icon = "empty";
                break;
            case "torque_fueltrimshortterm2_0x08":
                label.setText(getString(R.string.label_ftst2));
                icon = "empty";
                break;
            case "torque_fueltrimlongterm2_0x09":
                label.setText(getString(R.string.label_ftlt2));
                icon = "empty";
                break;
            default:
                label.setText("");
                value.setText("");
                icon = "empty";
                break;
        }


        if (icon.equals("empty")) {
            label.setBackgroundResource(0);
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) label.getLayoutParams();
            params.width = 600;
            label.setLayoutParams(params);

        }
    }

    private void setupGraph(Speedometer clock, GraphView graph, LineGraphSeries<DataPoint> serie, ConstraintLayout constraint) {

        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.themedBlankDialBackground});
        int blankBackgroundResource = typedArray.getResourceId(0, 0);
        typedArray.recycle();

        graph.addSeries(serie);

        graph.setTitle(clock.getUnit());

        constraint.setBackgroundResource(blankBackgroundResource); // put blank background
        serie.setAnimated(false);
        graph.setElevation(55);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getViewport().setYAxisBoundsManual(false);
        graph.getViewport().setMinX(0);
        // set default max and min, these will be set dynamically later
        graph.getViewport().setMaxX(120);
        //graph.getViewport().setMaxY(360);
        //graph.getViewport().setMinY(0);
        graph.getViewport().setScrollable(false);
        graph.getGridLabelRenderer().setVerticalLabelsVisible(true);
        graph.getGridLabelRenderer().setGridColor(Color.parseColor("#22FFFFFF"));

        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);
        graph.getGridLabelRenderer().setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);
        graph.getViewport().setBackgroundColor(Color.argb(0, 255, 0, 0));
        serie.setDrawDataPoints(false);
        serie.setThickness(2);
        serie.setColor(Color.argb(80, 255, 255, 255));
    }

    private void setupClocks(String queryClock, Speedometer clock, TextView icon, RaySpeedometer ray, Speedometer min, Speedometer max) {

        String queryTrim = "";
        String queryLong = queryClock;
        String torqueUnit = "";
        long queryPid = 0;

        TypedArray typedArray2 = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.themedStopWatchBackground});
        int swBackgroundResource = typedArray2.getResourceId(0, 0);
        typedArray2.recycle();

        if (queryClock.contains("-")) {
            queryTrim = queryClock.substring(0, queryClock.indexOf("-")); // check the prefix
        } else {
            queryTrim = "other";
        }
        // get min/max values and unit from torque
        if (queryTrim.equals("torque")) {
            queryClock = queryClock.substring(queryClock.lastIndexOf('_') + 1);
            queryClock = queryClock.substring(2);
            queryPid = new BigInteger(queryClock, 16).longValue();

            try {
                if (torqueService != null) {
                    torqueUnit = torqueService.getUnitForPid(queryPid);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
            }
        }


        // setup each of the clocks:
        switch (queryLong) {
            case "none": // currently impossible to choose, maybe in the future?
                setupClock(icon, "ic_none", "", clock, false, "", 0, 100, "float");
                break;
            case "test":
                setupClock(icon, "ic_measurement", "", clock, false, getString(R.string.testing), 0, 360, "float");
                break;
            case "exlap-vehicleSpeed":
            case "torque-speed_0x0d":
                setupClock(icon, "ic_none", "", clock, false, getString(R.string.unit_kmh), 0, 300, "integer");
                break;
            case "exlap-Nav_Altitude":
                setupClock(icon, "ic_altitude", "", clock, false, "m", -100, 3000, "integer");
                break;
            case "exlap-Nav_Heading": // this is a compass, so a little bit more is needed to setup the clock
                setupClock(icon, "ic_heading", "", clock, false, "°", 0, 360, "integer");
                clock.setMarkColor(Color.parseColor("#00FFFFFF"));

                //set the degrees so it functions as a circle
                clock.setStartDegree(270);
                clock.setEndDegree(630);
                ray.setStartDegree(270);
                ray.setEndDegree(630);
                min.setStartDegree(270);
                min.setEndDegree(630);
                max.setStartDegree(270);
                max.setEndDegree(630);
                // set background resource to the same as stopwatch
                clock.setBackgroundResource(swBackgroundResource);
                break;
            case "exlap-engineSpeed":
            case "torque-rpm_0x0c":
                setupClock(icon, "ic_none", getString(R.string.unit_rpm), clock, true, getString(R.string.unit_rpm1000), 0, 9, "float");
                clock.setTicks();
                clock.setTickTextFormat(0);
                break;
            case "torque-voltage_0xff1238":
            case "exlap-batteryVoltage":
            case "torque-voltagemodule_0x42":
                setupClock(icon, "ic_battery", "", clock, false, getString(R.string.unit_volt), 0, 17, "float");
                break;
            case "exlap-oilTemperature":
                setupClock(icon, "ic_oil", "", clock, true, "°", 0, 200, "float");
                break;
            case "exlap-coolantTemperature":
            case "torque-enginecoolanttemp_0x05":
                setupClock(icon, "ic_water", "", clock, true, "°", 0, 200, "float");
                break;
            case "exlap-outsideTemperature":
            case "torque-ambientairtemp_0x46":
                setupClock(icon, "ic_outsidetemperature", "", clock, false, "°", -25, 50, "float");
                break;
            case "torque-transmissiontemp_0x0105":
            case "exlap-gearboxOilTemperature":
                setupClock(icon, "ic_gearbox", "", clock, false, "°", 0, 200, "float");
                break;
            case "torque-turboboost_0xff1202":
            case "exlap-absChargingAirPressure":
            case "exlap-relChargingAirPressure":
                setupClock(icon, "ic_turbo", "", clock, true, pressureUnit, pressureMin, pressureMax, "float");
                break;
            case "exlap-lateralAcceleration":
                setupClock(icon, "ic_lateral", "", clock, false, getString(R.string.unit_g), -3, 3, "float");
                break;
            case "exlap-longitudinalAcceleration":
                setupClock(icon, "ic_longitudinal", "", clock, false, getString(R.string.unit_g), -3, 3, "float");
                break;
            case "exlap-yawRate":
                setupClock(icon, "ic_yaw", "", clock, false, "°/s", -1, 1, "float");
                break;
            case "wheelAngle":
                setupClock(icon, "ic_wheelangle", "", clock, false, "°", -45, 45, "float");
                break;
            case "exlap-EcoHMI_Score.AvgShort":
            case "exlap-EcoHMI_Score.AvgTrip":
                setupClock(icon, "ic_eco", "", clock, false, "", 0, 100, "integer");
                break;
            case "exlap-powermeter":
                setupClock(icon, "ic_powermeter", "", clock, false, "%", 0, 5000, "integer");
                break;
            case "exlap-acceleratorPosition":
                setupClock(icon, "ic_pedalposition", "", clock, false, "%", 0, 100, "integer");
                break;
            case "exlap-brakePressure":
                setupClock(icon, "ic_brakepedalposition", "", clock, false, "%", 0, 100, "integer");
                break;
            case "exlap-currentTorque":
                setupClock(icon, "ic_none", "", clock, false, getString(R.string.unit_nm), 0, 500, "integer");
                break;
            case "exlap-currentOutputPower":
                setupClock(icon, "ic_none", "", clock, false, powerUnits?getString(R.string.unit_kw):getString(R.string.unit_hp), 0, 500, "integer");
                break;
            case "exlap-currentConsumptionPrimary":
            case "exlap-cycleConsumptionPrimary":
                setupClock(icon, "ic_fuelprimary", "", clock, false, "l/h", 0, 100, "float");
                break;
            case "exlap-currentConsumptionSecondary":
            case "exlap-cycleConsumptionSecondary":
                setupClock(icon, "ic_fuelsecondary", "", clock, false, "l/h", 0, 100, "float");
                break;
            case "exlap-tankLevelPrimary":
            case "torque-fuellevel_0x2f":
                setupClock(icon, "ic_fuelprimary", "", clock, false, "%", 0, 100, "float");
                break;
            case "exlap-tankLevelSecondary":
                setupClock(icon, "ic_fuelsecondary", "", clock, false, "%", 0, 100, "float");
                break;
            case "torque-fuelpressure_0x0a":
                setupClock(icon, "ic_none", getString(R.string.label_fuel), clock, false, torqueUnit, 0, 800, "float");
                break;
            case "torque-engineload_0x04":
            case "torque-engineloadabsolute_0x43":
                setupClock(icon, "ic_none", getString(R.string.label_load), clock, false, torqueUnit, 0, 100, "float");
                break;
            case "torque-timing_advance_0x0e":
                setupClock(icon, "ic_none", getString(R.string.label_timing), clock, false, torqueUnit, -60, 60, "float");
                break;
            case "torque-intake_air_temperature_0x0f":
                setupClock(icon, "ic_none", getString(R.string.label_iat), clock, false, torqueUnit, 0, 100, "float");
                break;
            case "torque-mass_air_flow_0x10":
                setupClock(icon, "ic_none", getString(R.string.label_maf), clock, false, torqueUnit, 0, 700, "float");
                break;
            case "torque-AFR_0xff1249":
                setupClock(icon, "ic_none", getString(R.string.label_afr), clock, false, torqueUnit, 0, 35, "float");
                break;
            case "torque-AFRc_0xff124d":
                setupClock(icon, "ic_none", getString(R.string.label_afrc), clock, false, torqueUnit, 0, 35, "float");
                break;
            case "torque-fueltrimshortterm1_0x06":
                setupClock(icon, "ic_none", getString(R.string.label_ftst1), clock, false, torqueUnit, -20, 20, "float");
                break;
            case "torque-fueltrimlongterm1_0x07":
                setupClock(icon, "ic_none", getString(R.string.label_ftlt1), clock, false, torqueUnit, -20, 20, "float");
                break;
            case "torque-fueltrimshortterm2_0x08":
                setupClock(icon, "ic_none", getString(R.string.label_ftst2), clock, false, torqueUnit, -20, 20, "float");
                break;
            case "torque-fueltrimlongterm2_0x09":
                setupClock(icon, "ic_none", getString(R.string.label_ftlt2), clock, false, torqueUnit, -20, 20, "float");
                break;
            case "torque-accelerometer_total_0xff1223":
                setupClock(icon, "ic_none", "", clock, false, "G", -3, 3, "float");
                break;
            case "torque-phonebatterylevel_0xff129a":
                setupClock(icon, "ic_battery", "", clock, false, "%", 0, 100, "integer");
                break;
            case "torque-phonebarometer_0xff1270":
                setupClock(icon, "ic_none", "", clock, false, torqueUnit, 900, 1070, "float");
                break;
            case "torque-obdadaptervoltage_0xff1238":
                setupClock(icon, "ic_obd2", "", clock, false, torqueUnit, 0, 17, "float");
                break;
            case "torque-hybridbattlevel_0x5b":
                setupClock(icon, "ic_battery", "", clock, false, "%", 0, 100, "float");
                break;
            case "torque-commandedequivalenceratiolambda_0x44":
                setupClock(icon, "ic_none", "lambda", clock, false, torqueUnit, 0, 3, "float");
                break;
            case "torque-catalysttemperature_0x3c":
                setupClock(icon, "ic_catalyst", "", clock, false, torqueUnit, 0, 1000, "float");
                break;
            case "torque-relativethrottleposition_0x45":
            case "torque-absolutethrottlepostion_0x47":
            case "torque-throttle_position_0x11":
                setupClock(icon, "ic_throttle", "", clock, false, torqueUnit, 0, 100, "float");
                break;
            case "torque-intakemanifoldpressure_0x0b":
                setupClock(icon, "ic_manifold", "", clock, false, torqueUnit, 0, 200, "float");
                break;
            case "torque-chargeaircoolertemperature_0x77":
                setupClock(icon, "ic_cact", "", clock, false, torqueUnit, 0, 100, "float");
                break;
            case "torque-pressurecontrol_0x70":
                setupClock(icon, "ic_turbo", "", clock, false, pressureUnit, pressureMin * 30, pressureMax * 30, "float");
                break;
            case "torque-o2sensor1equivalenceratio_0x34":
                setupClock(icon, "ic_none", "O2 sensor", clock, false, torqueUnit, 0, 3, "float");
                break;
        }

        // make the icon appear in the color of unitTextColor
        Drawable iconBackground = (Drawable) icon.getBackground();
        if (iconBackground != null) {
            int iconTint = clock.getUnitTextColor();
            iconBackground.setColorFilter(iconTint, PorterDuff.Mode.SRC_ATOP);
            icon.setBackground(iconBackground);
            icon.setTextColor(iconTint);
        }

        // bring mins and max's in line with the clock
        float minimum = clock.getMinSpeed();
        float maximum = clock.getMaxSpeed();

        min.setMinMaxSpeed(minimum, maximum);
        ray.setMinMaxSpeed(minimum, maximum);
        max.setMinMaxSpeed(minimum, maximum);
    }

    //update clock with data
    private void updateClock(String query, Speedometer clock, RaySpeedometer visray, TextView
            textmax, TextView textmin, Speedometer clockmax, Speedometer clockmin, GraphView graph, LineGraphSeries<DataPoint> series, Double graphLastXValue, TextView graphValue) {
        if (query == null) {
            return;

        } else if (stagingDone) {
            Float clockValue = (Float) mLastMeasurements.get(query);
            Float oldValue = (Float) clock.getSpeed();

            // don't update when there's nothing to update
            if (Objects.equals(clockValue, oldValue)) {
                return;
            }

            float randomClockVal = randFloat(0, 360);
            speedFactor = 1f;
            pressureFactor = 1f;
            long queryPid = 0;

            clockValue = 0f;
            oldValue = 0f;

            String queryTrim = "";
            String queryLong = query;
            String unitText = "";

            String temperatureUnitExlap = (String) mLastMeasurements.get("unitTemperature.temperatureUnit");


            if (temperatureUnitExlap == null) {
                temperatureUnitExlap = "°";
            }

            if (query.contains("-")) {
                queryTrim = query.substring(0, query.indexOf("-")); // check the prefix
            } else {
                queryTrim = "other";
            }
            // Get the value that should be put on the clock, depending on the query
            // exlap queries use mLastMeasurements.get(query)
            // torque pid queries use torqueService.getValueForPid(queryPid), queryPid is trimmed from the query string
            switch (queryTrim) {
                case "torque":
                    query = query.substring(query.lastIndexOf('_') + 1);
                    query = query.substring(2);
                    queryPid = new BigInteger(query, 16).longValue();

                    try {
                        if (torqueService != null) {
                            clockValue = torqueService.getValueForPid(queryPid, true);
                            unitText = torqueService.getUnitForPid(queryPid);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                    }
                    break;
                case "exlap":
                    query = query.substring(query.lastIndexOf('-') + 1);
                    clockValue = (Float) mLastMeasurements.get(query);
                    break;
                default:  // the only other kind of query is the  "random" one.
                    clockValue = randomClockVal;
                    break;
            }

            if (clockValue != null) {
                switch (queryLong) {
                    case "test":
                    case "none":    // none cannot happen currently
                        //don't do anything
                        break;
                    // all data that can be put on the clock without further modification:
                    case "exlap-Nav_Heading":
                    case "exlap-batteryVoltage":
                    case "exlap-Nav_Altitude":
                    case "exlap-lateralAcceleration":
                    case "exlap-longitudinalAcceleration":
                    case "exlap-yawRate":
                    case "exlap-EcoHMI_Score.AvgShort":
                    case "exlap-EcoHMI_Score.AvgTrip":
                    case "exlap-brakePressure":
                    case "exlap-currentTorque":
                        // all data that can be put on the clock without further modification:
                        break;
                    case "exlap-currentOutputPower":
                        clockValue=clockValue*powerFactor;

                        break;
                    //rpm data, needs to be divided by 1000 before displayed on the clock
                    case "exlap-engineSpeed":
                    case "torque-rpm_0x0c":
                        clockValue = clockValue / 1000;
                        break;
                    // temperatures
                    case "exlap-oilTemperature":
                    case "exlap-coolantTemperature":
                    case "exlap-outsideTemperature":
                    case "exlap-gearboxOilTemperature":
                        clock.setUnit(temperatureUnitExlap);
                        break;
                    // pressures
                    case "exlap-absChargingAirPressure":
                    case "exlap-relChargingAirPressure":
                        clockValue = clockValue * pressureFactor;
                        break;
                    // specific case for wheel angle, since it needs to be turned around
                    case "exlap-wheelAngle":
                        clockValue = clockValue * -1; // make it negative, otherwise right = left and vice versa
                        break;
                    // hybrid power has 1020 as value 0.
                    case "exlap-powermeter":
                        clockValue = clockValue - 1020;
                        break;
                    // percentages
                    case "exlap-acceleratorPosition":
                    case "exlap-tankLevelPrimary":
                    case "exlap-tankLevelSecondary":
                        clockValue = clockValue * 100;
                        break;
                    // specific consumption data with specific consumption units
                    // todo: maybe it's better to remove setting the unit from updateclock, but do it on setupclock
                    case "exlap-currentConsumptionPrimary":
                        String consumptionUnit = (String) mLastMeasurements.get("currentConsumptionPrimary.unit");
                        if (consumptionUnit != null) {
                            clock.setUnit(consumptionUnit);
                        }
                        break;
                    case "exlap-currentConsumptionSecondary":
                        String consumption2Unit = (String) mLastMeasurements.get("currentConsumptionSecondary.unit");
                        if (consumption2Unit != null) {
                            clock.setUnit(consumption2Unit);
                        }
                        break;
                    case "exlap-cycleConsumptionPrimary":
                        String cycconsumptionUnit = (String) mLastMeasurements.get("cycleConsumptionPrimary.unit");
                        if (cycconsumptionUnit != null) {
                            clock.setUnit(cycconsumptionUnit);
                        }
                        break;
                    case "exlap-cycleConsumptionSecondary":
                        String cycconsumption2Unit = (String) mLastMeasurements.get("cycleConsumptionSecondary.unit");
                        if (cycconsumption2Unit != null) {
                            clock.setUnit(cycconsumption2Unit);
                        }
                        break;
                    // speed, has specific unit requirements and mph calculation
                    case "exlap-vehicleSpeed":
                        String speedUnit = (String) mLastMeasurements.get("vehicleSpeed.unit");
                        if (speedUnit != null) {
                            switch (speedUnit) {
                                case "mph":
                                    speedFactor = 1.60934f;
                                    clock.setUnit("mph");
                                    break;
                                case "kmh":
                                    speedFactor = 1f;
                                    clock.setUnit("kmh");
                                    break;
                            }
                            clockValue = clockValue * speedFactor;

                        }
                        break;
                    // torque data elements:
                    case "torque-speed_0x0d":
                    case "torque-fuelpressure_0x0a":
                    case "torque-engineload_0x04":
                    case "torque-timing_advance_0x0e":
                    case "torque-mass_air_flow_0x10":
                    case "torque-throttle_position_0x11":
                    case "torque-AFR_0xff1249":
                    case "torque-fueltrimshortterm1_0x06":
                    case "torque-fueltrimlongterm1_0x07":
                    case "torque-fueltrimshortterm2_0x08":
                    case "torque-fueltrimlongterm2_0x09":
                    case "torque-accelerometer_total_0xff1223":
                    case "torque-phonebatterylevel_0xff129a":
                    case "torque-phonebarometer_0xff1270":
                    case "torque-obdadaptervoltage_0xff1238":
                    case "torque-hybridbattlevel_0x5b":
                    case "torque-voltage_0xff1238":
                    case "torque-transmissiontemp2_0xfe1805":
                    case "torque-pressurecontrol_0x70":
                    case "torque-relativethrottleposition_0x45":
                    case "torque-absolutethrottlepostion_0x47":
                    case "torque-voltagemodule_0x42":
                    case "torque-ambientairtemp_0x46":
                    case "torque-intakemanifoldpressure_0x0b":
                    case "torque-commandedequivalenceratiolambda_0x44":
                    case "torque-o2sensor1equivalenceratio_0x34":
                    case "torque-engineloadabsolute_0x43":
                    case "torque-fuellevel_0x2f":
                        clock.setUnit(unitText); // use the units Torque is providing
                        break;
                    case "torque-turboboost_0xff1202":
                        if (unitText.equals("psi") && pressureUnit.equals("bar")) {
                            clockValue = clockValue / 14.5037738f;
                            unitText = "bar";
                        }

                        clock.setUnit(unitText);
                        break;
                    case "torque-intake_air_temperature_0x0f":
                    case "torque-transmissiontemp_0x0105":
                    case "torque-catalysttemperature_0x3c":
                    case "torque-chargeaircoolertemperature_0x77":
                    case "torque-enginecoolanttemp_0x05":
                        if (unitText.equals("°C") && temperatureUnit.equals("°C")) {
                            unitText = "°C";
                        } else {
                            unitText = "°F";
                            clockValue = clockValue * 1.8f;
                            clockValue = clockValue + 32;
                        }
                        clock.setUnit(unitText);
                        break;
                }

                // only shift x asis 0.5 positions when there's new data
                if (clock == mClockLeft) {
                    graphLeftLastXValue += 0.5d;
                } else if (clock == mClockCenter) {
                    graphCenterLastXValue += 0.5d;
                } else if (clock == mClockRight) {
                    graphRightLastXValue += 0.5d;
                }

                // update graph, based on the value of the clock
                series.appendData(new DataPoint(graphLastXValue, clockValue), true, 400);
                String tempString = (String.format(Locale.US, getText(R.string.format_decimals).toString(), clockValue));
                graphValue.setText(tempString);


                // don't update when there's nothing to update
                if (clockValue.equals(oldValue)) {
                    return;
                }
                // update clock with latest clockValue
                    clock.speedTo(clockValue);
                    visray.speedTo(clockValue);

                    graph.getViewport().setMaxY(clock.getMaxSpeed());
                    graph.getViewport().setMinY(clock.getMinSpeed());


            }

            // get the speed from the clock and have the high-visibility rays move to this speed as well
            float currentClockValue = clock.getSpeed();


            series.appendData(new DataPoint(graphLastXValue, currentClockValue), true, 400);
            String tempString = (String.format(Locale.US, getText(R.string.format_decimals).toString(), currentClockValue));
            graphValue.setText(tempString);

            // update the max clocks and text
            if (stagingDone) {
                float maxValue = clockmax.getSpeed();
                float minValue = clockmin.getSpeed();

                if (currentClockValue > maxValue) {
                    clockmax.setSpeedAt(currentClockValue);
                    textmax.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), currentClockValue));
                }

                // update the min clocks and text
                if (currentClockValue < minValue) {
                    clockmin.setSpeedAt(currentClockValue);
                    textmin.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), currentClockValue));
                }
            }

        }
    }

    private void updateTitle() {

        String currentTitleValue = mTitleElement.getText().toString();
        String currentRightTitleValue = mTitleElementRight.getText().toString();
        String currentLeftTitleValue = mTitleElementLeft.getText().toString();

        // Display location in center of title bar:

        Date time = new Date();
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);

        String currentTime = df.format(time);


        if (!Objects.equals(currentTitleValue, currentTime)) {
            mTitleElement.setText(currentTime);
        }

        // Display location in left side of Title  bar
        String leftTitle = "" ;

        //TODO: if no data -> get Data from Google Maps notification if available

        String location1 = (String) mLastMeasurements.get("Nav_CurrentPosition.Street");
        String location2 = (String) mLastMeasurements.get("Nav_CurrentPosition.City");

        if (location1==null && location2==null) {
            leftTitle = currentLocationFromGoogleMapsNotification; // ="";
        } else if (location1==null || location1.equals("")) {
            leftTitle = location2;
        } else if (location2==null || location2.equals("")) {
            leftTitle = location1;
        } else {
            leftTitle = location1 + ", " + location2;
        }

        if (!Objects.equals(currentLeftTitleValue, leftTitle)) {
            mTitleElementLeft.setText(leftTitle);
        }

        // Display temperature in right side of Title  bar
        Float currentTemperature = (Float) mLastMeasurements.get("outsideTemperature");
        if (currentTemperature != null) {
            String temperature = String.format(Locale.US,
                    (temperatureUnits?getText(R.string.format_temperatureC):getText(R.string.format_temperatureF)).toString(),
                    currentTemperature);
            if (!Objects.equals(temperature, currentRightTitleValue)){
                mTitleElementRight.setText(temperature);
            }
        } else {
            mTitleElementRight.setText("");
        }
    }

    //update the elements
    private void updateElement(String queryElement, TextView value, TextView label) {
        long queryPid = 0;
        if (queryElement == null) {
            return;
        } else switch (queryElement) {
            case "none":
                value.setText("");
                break;
            case "test":
                float randomValue = randFloat(0, 100);
                value.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), randomValue));
                break;
            case "torque_version":
                try {
                    if (torqueService != null) {
                        String torqueVersion = Integer.toString(torqueService.getVersion());
                        if (torqueVersion != null) {
                            value.setText(torqueVersion);
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error: " + e.getMessage());
                }
                break;
            // the following are torque PIDs.
            case "torque_fuelpressure_0x0a":
            case "torque_engineload_0x04":
            case "torque_timing_advance_0x0e":
            case "torque_intake_air_temperature_0x0f":
            case "torque_mass_air_flow_0x10":
            case "torque_throttle_position_0x11":
            case "torque_voltage_0xff1238":
            case "torque_AFR_0xff1249":
            case "torque_fueltrimshortterm1_0x06":
            case "torque_fueltrimlongterm1_0x07":
            case "torque_fueltrimshortterm2_0x08":
            case "torque_fueltrimlongterm2_0x09":
            case "torque_accelerometer_total_0xff1223":
                queryElement = queryElement.substring(queryElement.lastIndexOf('_') + 1);
                queryElement = queryElement.substring(2);
                queryPid = new BigInteger(queryElement, 16).longValue();
                float torqueData = 0f;

                try {
                    if (torqueService != null) {
                        torqueData = torqueService.getValueForPid(queryPid, true);
                        String unitText = torqueService.getUnitForPid(queryPid);
                        String valueText = (String.format(Locale.US, getText(R.string.format_decimals).toString(), torqueData));
                        value.setText(valueText + unitText);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error: " + e.getMessage());
                }
                break;
            // the following torque values should have the unit as label
            case "torque_turboboost_0xff1202":
                queryElement = queryElement.substring(queryElement.lastIndexOf('_') + 1);
                queryElement = queryElement.substring(2);
                queryPid = new BigInteger(queryElement, 16).longValue();
                float torqueData3 = 0f;

                try {
                    if (torqueService != null) {
                        torqueData3 = torqueService.getValueForPid(queryPid, true);


                        String unitText = torqueService.getUnitForPid(queryPid);
                        // workaround for Torque displaying the unit for turbo pressure
                        if (unitText.equals("psi") && pressureUnit.equals("bar")) {
                            torqueData3 = torqueData3 / 14.5037738f;
                            unitText = "bar";
                        }

                        String valueText = (String.format(Locale.US, getText(R.string.format_decimals).toString(), torqueData3));
                        value.setText(valueText + unitText);

                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error: " + e.getMessage());
                }
                break;
            case "torque_rpm_0x0c":
            case "torque_speed_0x0d":
                queryElement = queryElement.substring(queryElement.lastIndexOf('_') + 1);
                queryElement = queryElement.substring(2);
                queryPid = new BigInteger(queryElement, 16).longValue();
                try {
                    if (torqueService != null) {
                        float torqueData2 = torqueService.getValueForPid(queryPid, true);
                        String unitText = torqueService.getUnitForPid(queryPid);
                        value.setText(String.format(Locale.US, getText(R.string.format_noDecimals).toString(), torqueData2));
                        label.setText(unitText);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error: " + e.getMessage());
                }
                break;

            case "batteryVoltage":
                Float mBatteryVoltage = (Float) mLastMeasurements.get("batteryVoltage");
                if (mBatteryVoltage != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_volt).toString(), mBatteryVoltage));
                }
                break;

            // all temperatures can be handled in the same way, the only difference is the queryElement string
            case "coolantTemperature":
            case "oilTemperature":
            case "gearboxOilTemperature":
                Float mTemperature = (Float) mLastMeasurements.get(queryElement);
                if (mTemperature != null && mTemperature > 0) {
                    value.setText(String.format(Locale.US, getText(R.string.format_temperature).toString(), mTemperature));
                }
                break;
            case "outsideTemperature":
                Float mTemperatureOutside = (Float) mLastMeasurements.get(queryElement);
                if (mTemperatureOutside != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_temperature).toString(), mTemperatureOutside));
                }
                break;
            case "vehicleSpeed":
                Float mVehicleSpeed = (Float) mLastMeasurements.get("vehicleSpeed");
                String speedUnit = (String) mLastMeasurements.get("vehicleSpeed.unit");
                if (mVehicleSpeed != null && speedUnit != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), mVehicleSpeed));
                    label.setText(speedUnit);
                }
                // values that don't need any decimals
            case "engineSpeed":
            case "Nav_Heading":
            case "Nav_Altitude":
                Float mNoDecimalValue = (Float) mLastMeasurements.get(queryElement);
                if (mNoDecimalValue != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_noDecimals).toString(), mNoDecimalValue));
                }
                break;

            // Decimal values, without any specific modification:
            case "currentOutputPower":
                Float mCurrentPowerValue = (Float) mLastMeasurements.get(queryElement);
                if (mCurrentPowerValue != null) {
                    if (!powerUnits) {
                        // HP
                        mCurrentPowerValue*=powerFactor;
                    }
                    value.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), mCurrentPowerValue));
                    //label.setText(powerUnits?getString(R.string.unit_kw):getString(R.string.unit_hp));
                }
                break;
            case "currentTorque":
                Float mCurrentTorqueValue = (Float) mLastMeasurements.get(queryElement);
                if (mCurrentTorqueValue != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_decimals).toString(), mCurrentTorqueValue));
                }
                break;

            case "currentGear":
                Boolean reverseGear = (Boolean) mLastMeasurements.get("reverseGear.engaged");
                Boolean parkingBrake = (Boolean) mLastMeasurements.get("parkingBrake.engaged");
                String currentGear = (String) mLastMeasurements.get("currentGear");
                String recommendedGear = (String) mLastMeasurements.get("recommendedGear");
                String gearText = "-";

                if (parkingBrake != null && parkingBrake) {
                    value.setTextColor(Color.WHITE);
                    gearText = "P";
                } else if (reverseGear != null && reverseGear) {
                    value.setTextColor(Color.WHITE);
                    gearText = "R";
                } else if (currentGear == null || currentGear.equals("0")) {
                    value.setTextColor(Color.WHITE);
                    gearText = "-";
                } else if (currentGear != null && recommendedGear != null) {
                    if (recommendedGear.equals(currentGear) || recommendedGear.equals("NoRecommendation")) {
                        value.setTextColor(Color.WHITE);
                        gearText = convGear(currentGear);
                    } else if (!recommendedGear.equals(currentGear)) {
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
                    value.setText(String.format(Locale.US, getText(R.string.format_gforce).toString(), mAcceleration));
                }
                break;
            case "yawRate":
                Float mYawRate = (Float) mLastMeasurements.get(queryElement);
                if (mYawRate != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_degreespersec).toString(), mYawRate));
                }
                break;
            case "Sound_Volume":
                Float mSoundVol = (Float) mLastMeasurements.get(queryElement);
                if (mSoundVol != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_noDecimals).toString(), mSoundVol));
                }
                break;
            case "acceleratorPosition":
                Float mAcceleratorPosition = (Float) mLastMeasurements.get("acceleratorPosition");
                if (mAcceleratorPosition != null) {
                    Float mAccelPosPercent = mAcceleratorPosition * 100;
                    value.setText(String.format(Locale.US, getText(R.string.format_percent).toString(), mAccelPosPercent));
                }
                break;
            case "brakePressure":
                Float mBrakePressure = (Float) mLastMeasurements.get("brakePressure");
                if (mBrakePressure != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_percent).toString(), mBrakePressure));
                }
                break;
            case "wheelAngle":
                Float mWheelAngle = (Float) mLastMeasurements.get(queryElement);
                if (mWheelAngle != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_degrees).toString(), mWheelAngle));
                }
                break;
            case "powermeter":
                Float mPowermeter = (Float) mLastMeasurements.get(queryElement);
                if (mPowermeter != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_noDecimals).toString(), mPowermeter));
                }
                break;

            // eco values
            case "EcoHMI_Score.AvgShort":
            case "EcoHMI_Score.AvgTrip":
                Float mEcoScore = (Float) mLastMeasurements.get(queryElement);
                if (mEcoScore != null) {
                    value.setText(String.format(Locale.US, getText(R.string.format_noDecimals).toString(), mEcoScore));
                }
                break;
            case "shortTermConsumptionPrimary":
            case "shortTermConsumptionSecondary":
                Float mshortConsumption = (Float) mLastMeasurements.get(queryElement);
                if (mshortConsumption != null) {
                    value.setText(String.format(Locale.US, "%.1f", mshortConsumption));
                }
                break;
            case "Nav_CurrentPosition.Longitude":
            case "Nav_CurrentPosition.Latitude":
            case "Nav_CurrentPosition.City":
            case "Nav_CurrentPosition.State":
            case "Nav_CurrentPosition.Country":
            case "Nav_CurrentPosition.Street":
            case "Radio_Tuner.Name":
            case "Radio_Text":
            case "totalDistance.distanceValue":
            case "vehicleIdenticationNumber.VIN":
                String elementValue = (String) mLastMeasurements.get(queryElement);
                if (elementValue != null) value.setText(elementValue);
                break;
            case "blinkingState":
                break;
            case "tyreStates.stateRearRight":
            case "tyreStates.stateRearLeft":
            case "tyreStates.stateFrontRight":
            case "tyreStates.stateFrontLeft":
                String tyreState = (String) mLastMeasurements.get(queryElement);
                if (tyreState != null) {
                    value.setText(tyreState);
                    //if (tyreState != "OK") value.setTextColor(Color.RED);
                }
                break;

        }
    }

    // set clock label, units, etc.
    private void setupClock(TextView icon, String iconDrawableName, String iconText, Speedometer clock, Boolean backgroundWithWarningArea, String unit, Integer minspeed, Integer maxspeed, String speedFormat) {

        Log.d(TAG, "icon: " + icon + " iconDrawableName: " + iconDrawableName);

        int resId = getResources().getIdentifier(iconDrawableName, "drawable", getContext().getPackageName());
        Drawable iconDrawable = getContext().getResources().getDrawable(resId);
        int resIdEmpty = getResources().getIdentifier("ic_none", "drawable", getContext().getPackageName());
        Drawable iconEmptyDrawable = getContext().getResources().getDrawable(resIdEmpty);

        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.themedEmptyDialBackground});
        int emptyBackgroundResource = typedArray.getResourceId(0, 0);
        typedArray.recycle();

        // set icon. Clocks that don't need an icon have ic_none as icon
        icon.setBackground(iconDrawable);
        icon.setText(iconText);
        clock.setUnit(unit);
        clock.setMinMaxSpeed(minspeed, maxspeed);

        //dynamically scale the icon_space in case there's only an icon, and no text
        if (!iconText.equals("") && resId == resIdEmpty) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) icon.getLayoutParams();
            params.width = 600;
            icon.setLayoutParams(params);
        }


        // determine if an empty background, without red warning area is wanted
        if (!backgroundWithWarningArea) {
            clock.setBackgroundResource(emptyBackgroundResource);
        }

        //determine the clock format
        if (speedFormat.equals("float")) {
            clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);

        } else if (speedFormat.equals("integer")) {
            clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
        }

    }

    private ServiceConnection torqueConnection = new ServiceConnection() {
        /**
         * What to do when we get connected to Torque.
         *
         * @param arg0
         * @param service
         */
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            torqueService = ITorqueService.Stub.asInterface(service);
            postUpdate();
        }

        ;

        /**
         * What to do when we get disconnected from Torque.
         *
         * @param name
         */
        public void onServiceDisconnected(ComponentName name) {
            torqueService = null;
        }

        ;
    };

    private void startTorque() {
        Intent intent = new Intent();
        intent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");
        getContext().startService(intent);
        Log.d(TAG, "Torque start");

        boolean successfulBind = getContext().bindService(intent, torqueConnection, 0);
        if (successfulBind) {
            torqueBind = true;
            Log.d("HU", "Connected to torque service!");
        } else {
            torqueBind = false;
            Log.e("HU", "Unable to connect to Torque plugin service");
        }
    }

    // fade out 1 view, fade the other in during 500ms.
    private void fadeOutfadeIn(final View oldView, final View newView) {
        oldView.animate()
                .alpha(0f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        oldView.setVisibility(View.INVISIBLE);
                        newView.setVisibility(View.VISIBLE);
                        newView.setAlpha(1f);


                    }
                });
        newView.setAlpha(0f);
        newView.setVisibility(View.VISIBLE);
        newView.animate()
                .alpha(1f)
                .setDuration(500)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {

                    }
                });
    }

}
