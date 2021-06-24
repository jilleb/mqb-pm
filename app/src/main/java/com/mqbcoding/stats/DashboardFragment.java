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
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.github.anastr.speedviewlib.RaySpeedometer;
import com.github.anastr.speedviewlib.Speedometer;
import com.github.anastr.speedviewlib.components.Section;
import com.github.anastr.speedviewlib.components.indicators.ImageIndicator;
import com.github.anastr.speedviewlib.components.indicators.Indicator;
import com.github.martoreto.aauto.vex.CarStatsClient;
import com.github.martoreto.aauto.vex.FieldSchema;
import com.google.android.apps.auto.sdk.StatusBarController;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.jetbrains.annotations.NotNull;
import org.prowl.torque.remote.ITorqueService;

import java.math.BigInteger;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

public class DashboardFragment extends CarFragment {
    private final String TAG = "DashboardFragment";
    private Timer updateTimer;
    private CarStatsClient mStatsClient;
    private Speedometer mClockLeft, mClockCenter, mClockRight;
    private Speedometer mClockMaxLeft, mClockMaxCenter, mClockMaxRight;
    private RaySpeedometer mRayLeft, mRayCenter, mRayRight;
    private String mElement1Query, mElement2Query, mElement3Query, mElement4Query;
    private String selectedTheme, selectedBackground;
    private String mClockLQuery, mClockCQuery, mClockRQuery, mCustomPID;
    private String pressureUnit, temperatureUnit;
    private float pressureFactor, speedFactor, powerFactor, fueltanksize;
    private int pressureMin, pressureMax;
    //icons/labels of the data elements. upper left, upper right, lower left, lower right.
    private TextView mIconElement1, mIconElement2, mIconElement3, mIconElement4;
    private TextView mtextTitleMain;
    //values of the data elements. upper left, upper right, lower left, lower right.
    private TextView mValueElement1, mValueElement2, mValueElement3, mValueElement4, mTitleElement,
            mTitleElementRight, mTitleElementLeft, mTitleElementNavDistance, mTitleElementNavTime, mTitleNAVDestinationAddress;
    private TextView mTitleIcon1, mTitleIcon2, mTitleIcon3, mTitleIcon4, mTitleClockLeft, mTitleClockCenter, mTitleClockRight;
    private ConstraintLayout mConstraintClockLeft, mConstraintClockRight, mConstraintClockCenter;
    private ConstraintLayout mConstraintGraphLeft, mConstraintGraphRight, mConstraintGraphCenter;
    private TextView  mTextMaxLeft,mTextMaxCenter,mTextMaxRight;
    //icons on the clocks
    private TextView mIconClockL, mIconClockC, mIconClockR;
    private Boolean pressureUnits, temperatureUnits, powerUnits;
    private Boolean stagingDone;
    private Boolean raysOn, maxOn, maxMarksOn, ticksOn, ambientOn, accurateOn, proximityOn;
    private Boolean Dashboard2_On,Dashboard3_On;
    private Map<String, Object> mLastMeasurements = new HashMap<>();
    private final Handler mHandler = new Handler();
    private ITorqueService torqueService;
    private boolean torqueBind = false;
    private GraphView mGraphLeft, mGraphCenter, mGraphRight;
    private LineGraphSeries<DataPoint> mSpeedSeriesLeft;
    private LineGraphSeries<DataPoint> mSpeedSeriesCenter;
    private LineGraphSeries<DataPoint> mSpeedSeriesRight;
    private double graphLeftLastXValue = 5d;
    private double graphCenterLastXValue = 5d;
    private double graphRightLastXValue = 5d;
    //value displayed on graphlayout
    private TextView mGraphValueLeft, mGraphValueCenter, mGraphValueRight;
    private View rootView;
    private String androidClockFormat = "hh:mm a";
    int dashboardNum=1;
    private String googleGeocodeLocationStr = null;
    private String googleMapsLocationStr = null;
    private GeocodeLocationService mGeocodingService;

    private Button mBtnNext, mBtnPrev;
    private String mLabelClockL, mLabelClockC, mLabelClockR;
    private HashMap<String, FieldSchema> mSchema;


    // notation formats
    private static final String FORMAT_DECIMALS = "%.1f";
    private static final String FORMAT_DECIMALS_WITH_UNIT = "%.1f %s";
    private static final String FORMAT_DEGREES = "%.1f°";
    private static final String FORMAT_GFORCE = "%.1fG";
    private static final String FORMAT_KM = "%.1f km";
    private static final String FORMAT_MILES = "%.1f miles";
    private static final String FORMAT_NO_DECIMALS = "%.0f";
    private static final String FORMAT_PERCENT = "%.1f";
    private static final String FORMAT_DEGREESPEC = "%.1f°/s";
    private static final String FORMAT_TEMPERATURE = "%.1f°";
    private static final String FORMAT_TEMPERATURE0 = "-,-°";
    private static final String FORMAT_TEMPERATUREC = "%.1f°C";
    private static final String FORMAT_TEMPERATUREF = "%.1f°F";
    private static final String FORMAT_VOLT = "%.1fV";
    private static final String FORMAT_VOLT0 = "-,-V";
    private boolean celsiusTempUnit;
    private boolean showStreetName, useGoogleGeocoding, forceGoogleGeocoding;
    private String sourceLocation;
    private String selectedFont1, selectedFont2;
    private boolean selectedPressureUnits;
    private int updateSpeed = 2000;

    private float[] MaxspeedLeft;
    private float[] MaxspeedCenter;
    private float[] MaxspeedRight;
    private TextView mValueLeftElement1,mValueLeftElement2,mValueLeftElement3,mValueLeftElement4,mValueLeftElement5,mValueLeftElement6;
    private TextView mValueCenterElement1,mValueCenterElement2,mValueCenterElement3,mValueCenterElement4,mValueCenterElement5,mValueCenterElement6;
    private TextView mValueRightElement1,mValueRightElement2,mValueRightElement3,mValueRightElement4,mValueRightElement5,mValueRightElement6;
    @Override
    protected void setupStatusBar(StatusBarController sc) {
        sc.hideTitle();
    }


    // todo: reset min/max when clock is touched

    private final View.OnClickListener resetMinMax = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            MaxspeedLeft[dashboardNum] = 0;
            MaxspeedCenter[dashboardNum] = 0;
            MaxspeedRight[dashboardNum] = 0;

            float speedLeft = MaxspeedLeft[dashboardNum];// mClockLeft.getSpeed();
            float speedCenter = MaxspeedCenter[dashboardNum]; //;mClockCenter.getSpeed();
            float speedRight = MaxspeedRight[dashboardNum]; //mClockRight.getSpeed();

            mClockMaxLeft.speedTo(speedLeft);
            mClockMaxCenter.speedTo(speedCenter);
            mClockMaxRight.speedTo(speedRight);

            mTextMaxLeft.setText(String.format(Locale.US, FORMAT_DECIMALS, speedLeft));
            mTextMaxCenter.setText(String.format(Locale.US, FORMAT_DECIMALS, speedCenter));
            mTextMaxRight.setText(String.format(Locale.US, FORMAT_DECIMALS, speedRight));

        }
    };

    private final View.OnClickListener toggleView = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v == mConstraintClockLeft) {
                fadeOutfadeIn(v, mConstraintGraphLeft);
                mTextMaxLeft.setVisibility(View.INVISIBLE);
            } else if (v == mConstraintClockCenter) {
                fadeOutfadeIn(v, mConstraintGraphCenter);
                mTextMaxCenter.setVisibility(View.INVISIBLE);
            } else if (v == mConstraintClockRight) {
                fadeOutfadeIn(v, mConstraintGraphRight);
                mTextMaxRight.setVisibility(View.INVISIBLE);
            } else if (v == mGraphLeft) {
                fadeOutfadeIn(mConstraintGraphLeft, mConstraintClockLeft);
                if (maxOn) mTextMaxLeft.setVisibility(View.VISIBLE);
            } else if (v == mGraphCenter) {
                fadeOutfadeIn(mConstraintGraphCenter, mConstraintClockCenter);
                if (maxOn) mTextMaxCenter.setVisibility(View.VISIBLE);
            } else if (v == mGraphRight) {
                fadeOutfadeIn(mConstraintGraphRight, mConstraintClockRight);
                if (maxOn) mTextMaxRight.setVisibility(View.VISIBLE);
            } else if (v == mBtnPrev) {
                dashboardNum++;
                if (dashboardNum==2 && !Dashboard2_On) dashboardNum++;
                if (dashboardNum==3 && !Dashboard3_On) dashboardNum++;
                Log.v(TAG,"Button Prev: "+dashboardNum);
                if (dashboardNum > 3) dashboardNum = 1;
                onPreferencesChangeHandler();
            } else if (v == mBtnNext) {
                dashboardNum--;
                Log.v(TAG,"Button Next: "+dashboardNum);
                if (dashboardNum < 1) dashboardNum = 3;
                if (dashboardNum==3 && !Dashboard3_On) dashboardNum--;
                if (dashboardNum==2 && !Dashboard2_On) dashboardNum--;
                onPreferencesChangeHandler();
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

    private final ServiceConnection mVexServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            CarStatsService.CarStatsBinder carStatsBinder = (CarStatsService.CarStatsBinder) iBinder;
            Log.i(TAG, "ServiceConnected");
            mStatsClient = carStatsBinder.getStatsClient();
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
    private static float randFloat(float min, float max) {
        Random rand = new Random();
        return rand.nextFloat() * (max - min) + min;
    }

    public static String convGear(String gear) {

        String convertedGear = "0";
        switch (gear) {
            case "Gear1":
                convertedGear = "1";
                break;
            case "Gear2":
                convertedGear = "2";
                break;
            case "Gear3":
                convertedGear = "3";
                break;
            case "Gear4":
                convertedGear = "4";
                break;
            case "Gear5":
                convertedGear = "5";
                break;
            case "Gear6":
                convertedGear = "6";
                break;
            case "Gear7":
                convertedGear = "7";
                break;
            case "Gear8":
                convertedGear = "8";
                break;
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

    private void setupViews(View rootView) {
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

        mtextTitleMain = rootView.findViewById(R.id.textTitle);
        //reset value max
        MaxspeedLeft = null;
        MaxspeedCenter = null;
        MaxspeedRight = null;
        MaxspeedLeft = new float[5];
        MaxspeedCenter = new float[5];
        MaxspeedRight = new float[5];

        mBtnNext = rootView.findViewById(R.id.imageButton2);
        mBtnPrev = rootView.findViewById(R.id.imageButton3);

        //graph test
        mGraphLeft = rootView.findViewById(R.id.chart_Left);
        mGraphCenter = rootView.findViewById(R.id.chart_Center);
        mGraphRight = rootView.findViewById(R.id.chart_Right);

        mGraphValueLeft = rootView.findViewById(R.id.graphValueLeft);
        mGraphValueCenter = rootView.findViewById(R.id.graphValueCenter);
        mGraphValueRight = rootView.findViewById(R.id.graphValueRight);

        mSpeedSeriesLeft = new LineGraphSeries<>();
        mSpeedSeriesCenter = new LineGraphSeries<>();
        mSpeedSeriesRight = new LineGraphSeries<>();


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
        mTitleNAVDestinationAddress = rootView.findViewById(R.id.textTitleNAVDestinationAddress);
        mTitleClockLeft = rootView.findViewById(R.id.textTitleLabel1);
        mTitleClockCenter = rootView.findViewById(R.id.textTitleLabel2);
        mTitleClockRight = rootView.findViewById(R.id.textTitleLabel3);
        mTitleElementNavDistance = rootView.findViewById(R.id.textTitleNavDistance);
        mTitleElementNavTime = rootView.findViewById(R.id.textTitleNavTime);

        mTitleIcon1 = rootView.findViewById(R.id.titleIcon1);
        mTitleIcon2 = rootView.findViewById(R.id.titleIcon2);
        mTitleIcon3 = rootView.findViewById(R.id.titleIcon3);
        mTitleIcon4 = rootView.findViewById(R.id.titleIcon4);
        //labels at these text elements:
        mIconElement1 = rootView.findViewById(R.id.icon_Element1);
        mIconElement2 = rootView.findViewById(R.id.icon_Element2);
        mIconElement3 = rootView.findViewById(R.id.icon_Element3);
        mIconElement4 = rootView.findViewById(R.id.icon_Element4);

        //max texts:
        mTextMaxLeft = rootView.findViewById(R.id.textMaxLeft);
        mTextMaxCenter = rootView.findViewById(R.id.textMaxCenter);
        mTextMaxRight = rootView.findViewById(R.id.textMaxRight);

        setupListeners();
    }

    private void setupListeners() {
        mGraphLeft.setOnClickListener(toggleView);
        mConstraintClockLeft.setOnClickListener(toggleView);
        mGraphCenter.setOnClickListener(toggleView);
        mConstraintClockCenter.setOnClickListener(toggleView);
        mGraphRight.setOnClickListener(toggleView);
        mConstraintClockRight.setOnClickListener(toggleView);
        mBtnPrev.setOnClickListener(toggleView);
        mBtnNext.setOnClickListener(toggleView);
    }


    private void setupTypeface(String selectedFont1, String selectedFont2) {
        AssetManager assetsMgr = getContext().getAssets();
        //-------------------------------------------------------------
        //Give them all the right custom typeface
        //clocks
        mClockLeft.setSpeedTextTypeface(determineTypeFace(selectedFont1));
        mClockCenter.setSpeedTextTypeface(determineTypeFace(selectedFont1));
        mClockRight.setSpeedTextTypeface(determineTypeFace(selectedFont1));
        mGraphValueLeft.setTypeface(determineTypeFace(selectedFont1));
        mGraphValueCenter.setTypeface(determineTypeFace(selectedFont1));
        mGraphValueRight.setTypeface(determineTypeFace(selectedFont1));
        //elements
        mValueElement1.setTypeface(determineTypeFace(selectedFont1));
        mValueElement2.setTypeface(determineTypeFace(selectedFont1));
        mValueElement3.setTypeface(determineTypeFace(selectedFont1));
        mValueElement4.setTypeface(determineTypeFace(selectedFont1));
        mIconElement1.setTypeface(determineTypeFace(selectedFont1));
        mIconElement2.setTypeface(determineTypeFace(selectedFont1));
        mIconElement3.setTypeface(determineTypeFace(selectedFont1));
        mIconElement4.setTypeface(determineTypeFace(selectedFont1));

        //title
        mTitleElement.setTypeface(determineTypeFace(selectedFont2));
        mTitleElementRight.setTypeface(determineTypeFace(selectedFont2));
        mTitleElementLeft.setTypeface(determineTypeFace(selectedFont2));
        mTitleNAVDestinationAddress.setTypeface(determineTypeFace(selectedFont2));

        mTitleElementNavDistance.setTypeface(determineTypeFace(selectedFont2));
        mTitleElementNavTime.setTypeface(determineTypeFace(selectedFont2));
        mtextTitleMain.setTypeface(determineTypeFace(selectedFont2));

        //max
        mTextMaxLeft.setTypeface(determineTypeFace(selectedFont1));
        mTextMaxCenter.setTypeface(determineTypeFace(selectedFont1));
        mTextMaxRight.setTypeface(determineTypeFace(selectedFont1));

        Log.d(TAG, "font: " + determineTypeFace(selectedFont1) + " font title: " + determineTypeFace(selectedFont2));
        this.selectedFont1 = selectedFont1;
        this.selectedFont2 = selectedFont2;

    }

    private void onPreferencesChangeHandler() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        ambientOn = sharedPreferences.getBoolean("ambientActive", false);  //true = use ambient colors, false = don't use.
        accurateOn = sharedPreferences.getBoolean("accurateActive", false);  //true = be accurate. false = have 2000ms of animation time
        proximityOn = sharedPreferences.getBoolean("proximityActive", false);  //true = be accurate. false = have 2000ms of animation time
        if (accurateOn) {
            updateSpeed = 1;
        } else {
            updateSpeed = 2000;
        }

        if (!proximityOn) {
            mBtnNext.setVisibility(View.VISIBLE);
            mBtnPrev.setVisibility(View.VISIBLE);
            mtextTitleMain.setVisibility(View.VISIBLE);
        }

        // Load this only on first run, then leave it alone
        if (stagingDone == null) {
            stagingDone = !sharedPreferences.getBoolean("stagingActive", true);
        }
        showStreetName = sharedPreferences.getBoolean("showStreetNameInTitle", true);
        useGoogleGeocoding = sharedPreferences.getBoolean("useGoogleGeocoding", false);
        forceGoogleGeocoding = sharedPreferences.getBoolean("forceGoogleGeocoding", false);
        sourceLocation = sharedPreferences.getString("locationSourceData","Geocoding");
        fueltanksize = Float.parseFloat(sharedPreferences.getString("fueltanksize", "50"));

        float speedLeft = MaxspeedLeft[dashboardNum];
        float speedCenter = MaxspeedCenter[dashboardNum];
        float speedRight = MaxspeedRight[dashboardNum];

        mClockMaxLeft.speedTo(speedLeft);
        mClockMaxCenter.speedTo(speedCenter);
        mClockMaxRight.speedTo(speedRight);

        mTextMaxLeft.setText(String.format(Locale.US, FORMAT_DECIMALS, speedLeft));
        mTextMaxCenter.setText(String.format(Locale.US, FORMAT_DECIMALS, speedCenter));
        mTextMaxRight.setText(String.format(Locale.US, FORMAT_DECIMALS, speedRight));

        String dashboardId = String.valueOf(dashboardNum);
        String mtextTitlePerformance;
        if (dashboardNum<4) {
            mtextTitlePerformance = sharedPreferences.getString("performanceTitle" + dashboardId, "Performance monitor" + dashboardId);
        } else {
            mtextTitlePerformance = getResources().getString(R.string.pref_title_performance_4);
        }

        mtextTitleMain.setText(mtextTitlePerformance);

        String readedBackground = sharedPreferences.getString("selectedBackground", "background_incar_black");
        if (!readedBackground.equals(selectedBackground)) {
            setupBackground(readedBackground);
        }

        String readedFont1 = sharedPreferences.getString("selectedFont1", "segments");
        String readedFont2 = sharedPreferences.getString("selectedFont2", "segments");
        if (!readedFont1.equals(selectedFont1) || (!readedFont2.equals(selectedFont2))) {
            setupTypeface(readedFont1, readedFont2);
        }


        // todo: make the themes update nicely again when changing the theme.
        //show high visible rays on, according to the setting
        boolean readedRaysOn = sharedPreferences.getBoolean("highVisActive", false);  //true = show high vis rays, false = don't show them.
        if (raysOn == null || readedRaysOn != raysOn) {
            raysOn = readedRaysOn;
            turnRaysEnabled(raysOn);

        }

        String readedTheme = sharedPreferences.getString("selectedTheme", "");
        if (!readedTheme.equals(selectedTheme)) {
            selectedTheme = readedTheme;
            turnRaysEnabled(raysOn);
        }


        boolean readedTicksOn = sharedPreferences.getBoolean("ticksActive", false); // if true, it will display the value of each of the ticks
        if(ticksOn == null || readedTicksOn != ticksOn) {
            ticksOn = readedTicksOn;
            turnTickEnabled(ticksOn);
        }

        //determine what data the user wants to have on the 4 data views
        String readedElement1Query = sharedPreferences.getString("selectedView1_" + dashboardId, "none");
        if (!readedElement1Query.equals(mElement1Query)) {
            mElement1Query = readedElement1Query;
            setupElement(mElement1Query, mValueElement1, mIconElement1);
        }
        String readedElement2Query = sharedPreferences.getString("selectedView2_"+dashboardId, "none");
        if (!readedElement2Query.equals(mElement2Query)) {
            mElement2Query = readedElement2Query;
            setupElement(mElement2Query, mValueElement2, mIconElement2);
        }
        String readedElement3Query = sharedPreferences.getString("selectedView3_"+dashboardId, "none");
        if (!readedElement3Query.equals(mElement3Query)) {
            mElement3Query = readedElement3Query;
            setupElement(mElement3Query, mValueElement3, mIconElement3);
        }
        String readedElement4Query = sharedPreferences.getString("selectedView4_"+dashboardId, "none");
        if (!readedElement4Query.equals(mElement4Query)) {
            mElement4Query = readedElement4Query;
            setupElement(mElement4Query, mValueElement4, mIconElement4);
        }
        //determine what data the user wants to have on the 3 clocks, but set defaults first
        //setup clocks, including the max/min clocks and highvis rays and icons:
        //usage: setupClocks(query value, what clock, what icon, which ray, which min clock, which max clock)
        //could probably be done MUCH more efficient but that's for the future ;)
        String readedClockLQuery = sharedPreferences.getString("selectedClockLeft"+dashboardId, "exlap-batteryVoltage");
        if (!readedClockLQuery.equals(mClockLQuery)) {
            mClockLQuery = readedClockLQuery;
            setupClocks(mClockLQuery, mClockLeft, mIconClockL, mRayLeft, mClockMaxLeft);
        }
        String readedClockCQuery = sharedPreferences.getString("selectedClockCenter"+dashboardId, "exlap-oilTemperature");
        if (!readedClockCQuery.equals(mClockCQuery)) {
            mClockCQuery = readedClockCQuery;
            setupClocks(mClockCQuery, mClockCenter, mIconClockC, mRayCenter, mClockMaxCenter);
        }
        String readedClockRQuery = sharedPreferences.getString("selectedClockRight"+dashboardId, "exlap-engineSpeed");
        if (!readedClockRQuery.equals(mClockRQuery)) {
            mClockRQuery = readedClockRQuery;
            setupClocks(mClockRQuery, mClockRight, mIconClockR, mRayRight,mClockMaxRight);
        }

        String readedCustomPID = sharedPreferences.getString("readedCustomPID","none");
        if (!readedCustomPID.equals(mCustomPID)) {
            mCustomPID = readedCustomPID;
            setupClocks(mClockRQuery, mClockRight, mIconClockR, mRayRight,mClockMaxRight);
        }



        //debug logging of each of the chosen elements
        Log.d(TAG, "element 1 selected:" + mElement1Query);
        Log.d(TAG, "element 2 selected:" + mElement2Query);
        Log.d(TAG, "element 3 selected:" + mElement3Query);
        Log.d(TAG, "element 4 selected:" + mElement4Query);

        Log.d(TAG, "clock l selected:" + mClockLQuery);
        Log.d(TAG, "clock c selected:" + mClockCQuery);
        Log.d(TAG, "clock r selected:" + mClockRQuery);

        //determine what data the user wants to have on the 4 data views
        mLabelClockL = getLabelClock(mClockLQuery);
        mLabelClockC = getLabelClock(mClockCQuery);
        mLabelClockR = getLabelClock(mClockRQuery);

         boolean readedPressureUnits = sharedPreferences.getBoolean("selectPressureUnit", true);  //true = bar, false = psi
        if (readedPressureUnits != selectedPressureUnits) {
            selectedPressureUnits = readedPressureUnits;
            pressureFactor = selectedPressureUnits ? 1 : (float) 14.5037738;
            pressureUnit = selectedPressureUnits ? "bar" : "psi";
            pressureMin = selectedPressureUnits ? -3 : -30;
            pressureMax = selectedPressureUnits ? 3 : 30;
        }

        boolean readedTempUnit = sharedPreferences.getBoolean("selectTemperatureUnit", true);  //true = celcius, false = fahrenheit
        if (readedTempUnit != celsiusTempUnit) {
            celsiusTempUnit = readedTempUnit;
            temperatureUnit = getString(celsiusTempUnit ? R.string.unit_c : R.string.unit_f);
        }

        boolean readedPowerUnits = sharedPreferences.getBoolean("selectPowerUnit", true);  //true = kw, false = ps
        if (powerUnits == null || readedPowerUnits != powerUnits) {
            powerUnits = readedPowerUnits;
            powerFactor = powerUnits ? 1 : 1.35962f;
        }
//

        //show texts and backgrounds for max/min, according to the setting
        boolean readedMaxOn = sharedPreferences.getBoolean("maxValuesActive", false); //true = show max values, false = hide them
        if (maxOn == null || readedMaxOn != maxOn) {
            maxOn = readedMaxOn;
            turnMinMaxTextViewsEnabled(maxOn);
        }

        boolean readedMaxMarksOn = sharedPreferences.getBoolean("maxMarksActive", false); //true = show max values as a mark on the clock, false = hide them
        if (maxMarksOn == null || readedMaxMarksOn != maxMarksOn) {
            maxMarksOn = readedMaxMarksOn;
            turnMinMaxMarksEnabled(maxMarksOn);
        }
    }

    private void setupBackground(String newBackground) {
        int resId = getResources().getIdentifier(newBackground, "drawable", getContext().getPackageName());
        if (resId != 0) {
            Drawable wallpaperImage = ContextCompat.getDrawable(getContext(), resId);
            rootView.setBackground(wallpaperImage);
        }
        selectedBackground = newBackground;
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            onPreferencesChangeHandler();
        }
    };

    private void turnMinMaxMarksEnabled(boolean enabled) {
        //show clock marks for max/min, according to the setting
        mClockMaxLeft.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        mClockMaxCenter.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        mClockMaxRight.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
    }

    private void turnMinMaxTextViewsEnabled(boolean enabled) {
        mTextMaxLeft.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        mTextMaxCenter.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        mTextMaxRight.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
    }

    private void turnRaysEnabled(boolean enabled) {
        mRayLeft.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        mRayCenter.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        mRayRight.setVisibility(enabled ? View.VISIBLE : View.INVISIBLE);
        if (enabled) {
            //also hide the needle on the clocks
            mClockLeft.setIndicator(Indicator.Indicators.NoIndicator);
            mClockCenter.setIndicator(Indicator.Indicators.NoIndicator);
            mClockRight.setIndicator(Indicator.Indicators.NoIndicator);
        } else {
            setupIndicators();
        }
    }

    private void turnTickEnabled(boolean enabled) {
        int tickNum = 9;
        int tickColor = Color.TRANSPARENT;
        if (enabled) {
            if (selectedTheme.contains("Sport"))
                tickColor = Color.BLACK;
            else
                tickColor = Color.WHITE;
        }
        Log.i(TAG, "tickColor: " + tickColor + " selectedTheme: " + selectedTheme);

        mClockLeft.setTickNumber(enabled ? tickNum : 0);
        mClockLeft.setTextColor(tickColor);
        mClockCenter.setTickNumber(enabled ? tickNum : 0);
        mClockCenter.setTextColor(tickColor);
        mClockRight.setTickNumber(enabled ? tickNum : 0);
        mClockRight.setTextColor(tickColor);
    }

    private void setupIndicators() {
        int clockSize = mClockLeft.getHeight();
        if (clockSize == 0) {
            clockSize = 250;
        }
        //this is to enable an image as indicator.
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.themedNeedle});
        int resourceId = typedArray.getResourceId(0, 0);
        typedArray.recycle();

        Drawable indicatorImageUnScaled = ContextCompat.getDrawable(getContext(), resourceId);
        Bitmap bitmap = ((BitmapDrawable) indicatorImageUnScaled).getBitmap();

        //int indicatorColor = 1996533487; //(test value,makes indicator aqua)
        int indicatorColor = 0;
        //Drawable indicatorImage = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(colorize(bitmap, indicatorColor), 250, 250, true));
        Drawable indicatorImage = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, 250, 250, true));
        // todo: Make Image indicator Ambient colored!

        ImageIndicator imageIndicator = new ImageIndicator(getContext(), indicatorImage);

        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.sv_indicatorColor, typedValue, true);
        @ColorInt int color = typedValue.data;

        Log.i(TAG, "IndicatorColor: " + color);

        mClockLeft.setIndicator(imageIndicator);
        mClockCenter.setIndicator(imageIndicator);
        mClockRight.setIndicator(imageIndicator);

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

            mRayLeft.setIndicatorLightColor(Color.parseColor("#00FFFFFF"));
            mRayRight.setIndicatorLightColor(Color.parseColor("#00FFFFFF"));
            mRayCenter.setIndicatorLightColor(Color.parseColor("#00FFFFFF"));

        }
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        rootView = inflater.inflate(R.layout.fragment_dashboard, container, false);
        setupViews(rootView);
        onPreferencesChangeHandler();

        //Get preferences
        //set pressure dial to the wanted units
        //Most bar dials go from -2 to 3 bar.
        //Most PSI dials go from -30 to 30 psi.
        //pressurefactor is used to calculate the right value for psi later
        // build ImageIndicator using the resourceId
        // get the size of the Clock, to make sure the imageindicator has the right size.

        mClockLeft.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mClockLeft.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setupIndicators();

                setupGraph(mClockLeft, mGraphLeft, mSpeedSeriesLeft, mConstraintGraphLeft);
                setupGraph(mClockCenter, mGraphCenter, mSpeedSeriesCenter, mConstraintGraphCenter);
                setupGraph(mClockRight, mGraphRight, mSpeedSeriesRight, mConstraintGraphRight);
                turnTickEnabled(ticksOn);
                runStagingAnimation();
            }

        });

        androidClockFormat = android.text.format.DateFormat.is24HourFormat(getContext())
                ? "HH:mm" : "hh:mm a";

        //update!
        doUpdate();

        return rootView;
    }

    private String getLabelClock (String queryClock ) {
        String mtext = "";
        if ((queryClock != null && !queryClock.equals(""))){
            String[] valueArray = getResources().getStringArray(R.array.ClockDataElementsValues);
            String[] stringArray = getResources().getStringArray(R.array.ClockDataElementsEntries);
            int lindex = Arrays.asList(valueArray).indexOf(queryClock);
            if (lindex >= 0) {
                mtext = stringArray[lindex];
            }
        }
        return mtext;
    }



    private void runStagingAnimation() {
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
            }, 1700);

            final Handler stagingReset = new Handler();
            stagingReset.postDelayed(new Runnable() {
                public void run() {
                    if (mClockLeft != null) {
                        mClockMaxLeft.speedTo(mClockLeft.getSpeed(), 1000);
                        mClockMaxCenter.speedTo(mClockCenter.getSpeed(), 1000);
                        mClockMaxRight.speedTo(mClockRight.getSpeed(), 1000);

                        mTextMaxLeft.setText("-");
                        mTextMaxCenter.setText("-");
                        mTextMaxRight.setText("-");
                        stagingDone = true;

                    }
                }
            }, 2700);
        }
    }

    private final BroadcastReceiver onNoticeGoogleNavigationUpdate = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //String text = intent.getStringExtra("text"); // Not used right now
            googleMapsLocationStr = intent.getStringExtra("title");
        }
    };
    private final BroadcastReceiver onNoticeGoogleNavigationClosed = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            googleMapsLocationStr = null;
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
        getContext().bindService(serviceIntent, mVexServiceConnection, Context.BIND_AUTO_CREATE);
        startTorque();
        createAndStartUpdateTimer();
        if (useGoogleGeocoding) {
            if(!getContext().bindService(new Intent(getContext(), GeocodeLocationService.class),
                    mGeocodingServiceConnection,
                    Context.BIND_AUTO_CREATE)) {
                Log.e("Geocode", "Cannot bind?!");
            }
        }

        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(onNoticeGoogleNavigationUpdate, new IntentFilter("GoogleNavigationUpdate"));
        LocalBroadcastManager.getInstance(getContext())
                .registerReceiver(onNoticeGoogleNavigationClosed, new IntentFilter("GoogleNavigationClosed"));
        PreferenceManager.getDefaultSharedPreferences(getContext()).registerOnSharedPreferenceChangeListener(preferenceChangeListener);
        onPreferencesChangeHandler();
        // Force reload of components
        turnRaysEnabled(raysOn);
        turnMinMaxTextViewsEnabled(maxOn);
        turnMinMaxMarksEnabled(maxMarksOn);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        Dashboard2_On = sharedPreferences.getBoolean("d2_active", false);  //Enabled dashboard2.
        Dashboard3_On = sharedPreferences.getBoolean("d3_active", false);  //Enabled dashboard3
    }

    private final GeocodeLocationService.IGeocodeResult geocodeResultListener = new GeocodeLocationService.IGeocodeResult() {
        @Override
        public void onNewGeocodeResult(Address result) {
            StringBuilder sb = new StringBuilder();
            String tmp = result.getThoroughfare();
            if (tmp != null)
                sb.append(tmp);
            if (sb.length() != 0)
                sb.append(", ");
            tmp = result.getSubAdminArea(); //Town
            if (tmp != null)
                sb.append(tmp);
            sb.append(' ');
        //    tmp = result.getPostalCode();  //PostalCode
        //    if (tmp != null)
        //        sb.append("("+tmp+")");
            tmp = result.getUrl();  //URL -> Altitude
            if (tmp != null)
            sb.append("(").append(tmp).append(")");

            googleGeocodeLocationStr = sb.toString();
        }
    };

    private final ServiceConnection mGeocodingServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mGeocodingService = ((GeocodeLocationService.LocalBinder)service).getService();
            mGeocodingService.setOnNewGeocodeListener(geocodeResultListener);
            Log.d("Geocode", "Service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.e("Geocode", "Service disconnected");
            mGeocodingService = null;
        }
    };

    private void startTorque() {
        Intent intent = new Intent();
        intent.setClassName("org.prowl.torque", "org.prowl.torque.remote.TorqueService");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getContext().startForegroundService(intent);
        } else {
            getContext().startService(intent);
        }
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


    private void stopTorque() {
        Intent sendIntent = new Intent();
        sendIntent.setAction("org.prowl.torque.REQUEST_TORQUE_QUIT");
        getContext().sendBroadcast(sendIntent);
        Log.d(TAG, "Torque stop");
    }

    private void createAndStartUpdateTimer() {
        updateTimer = new Timer();
        updateTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                Runnable updateTimerRunnable = new Runnable() {
                    public void run() {
                        doUpdate();
                    }
                };
                //experimental delay
                if (mHandler != null)
                    mHandler.postDelayed(updateTimerRunnable, 1);
            }

        }, 0, 250);//Update display 0,25 second
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onDeactivate");
        updateTimer.cancel();

        mStatsClient.unregisterListener(mCarStatsListener);
        getContext().unbindService(mVexServiceConnection);
        if (useGoogleGeocoding) {
            getContext().unbindService(mGeocodingServiceConnection);
        }

        if (torqueBind)
            try {
                getContext().unbindService(torqueConnection);
                stopTorque();
            } catch (Exception E) {
                throw E;
            }


        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(onNoticeGoogleNavigationUpdate);
        LocalBroadcastManager.getInstance(getContext())
                .unregisterReceiver(onNoticeGoogleNavigationClosed);

        PreferenceManager.getDefaultSharedPreferences(getContext()).unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);

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
        mValueElement1 = null;
        mValueElement2 = null;
        mValueElement3 = null;
        mValueElement4 = null;
        mTitleElement = null;
        mTitleElementRight = null;
        mTitleElementLeft = null;
        mTitleNAVDestinationAddress = null;
        mTitleClockLeft = null;
        mTitleClockCenter = null;
        mTitleClockRight = null;
        mTitleElementNavDistance = null;
        mTitleElementNavTime = null;
        mTitleIcon1 = null;
        mTitleIcon2 = null;
        mTitleIcon3 = null;
        mTitleIcon4 = null;
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
        mClockMaxLeft = null;
        mClockMaxCenter = null;
        mClockMaxRight = null;
        mRayLeft = null;
        mRayCenter = null;
        mRayRight = null;
        selectedFont1 = null;
        selectedFont2 = null;
        pressureUnit = null;
        //stagingDone = false;
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
        if (lastUpdate<0 || (System.currentTimeMillis()-lastUpdate) > UPDATE_AFTER) {
            lastUpdate = System.currentTimeMillis();
            mHandler.post(new Runnable() {
                public void run() {
                    doUpdate();
                }
            });
        }
    }

    private void SetLayoutElements(TextView mValueElement, String mMeasurements, String mUnit, String mDefUnit,  String mFormat) {
        Float mGetMeasurement;
        String mGetUnit;
        if (mMeasurements == null || mMeasurements.isEmpty()) {
            mValueElement.setText("");
        } else {
            mGetMeasurement = (Float) mLastMeasurements.get(mMeasurements);
            if (mGetMeasurement == null) mGetMeasurement = (float) 0;
            if (mMeasurements.equals("tankLevelPrimary")) mGetMeasurement = mGetMeasurement * fueltanksize;
            if (mMeasurements.equals("driving distance")) {
                mGetMeasurement = (Float) mLastMeasurements.get("tankLevelPrimary");
                if (mGetMeasurement==null) mGetMeasurement= (float) 0;
                mGetMeasurement = mGetMeasurement * fueltanksize;
                Float mShortCons = (Float) mLastMeasurements.get("shortTermConsumptionPrimary");
                Float mLongCons = (Float) mLastMeasurements.get("LongTermConsumptionPrimary");
                if (mShortCons==null || mShortCons==0){
                    if (mLongCons==null || mLongCons==0){
                        mGetMeasurement = (float) 0;
                    } else {
                        mGetMeasurement = (mGetMeasurement/mLongCons) * 100 ;
                    }
                } else {
                    mGetMeasurement = (mGetMeasurement/mShortCons)*100;
                }
            }

            if (mUnit == null || mUnit.isEmpty()) {
                mGetUnit=mDefUnit;
            } else {
                mGetUnit = (String) mLastMeasurements.get(mUnit);
                if (mGetUnit == null || mGetUnit.isEmpty()) {
                    mGetUnit = mDefUnit;
                }
            }

            if (mFormat.equals("FORMAT_SHORTTIME")) {
                mValueElement.setText(ConvertMinutesTime(mGetMeasurement.intValue()) + " " + mGetUnit);
            } else {
                mValueElement.setText(String.format(mFormat, mGetMeasurement) + " " + mGetUnit);
            }
        }
    }

    private static String ConvertMinutesTime(int minutesTime) {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        df.setTimeZone(timeZone);
        String time = df.format(new Date(minutesTime * 60 * 1000L));
        return time;
    }

    private void UpdateLayoutElements() {
        //Left elements
        SetLayoutElements(mValueLeftElement1,"consumptionShortTermGeneral.distanceValue","consumptionShortTermGeneral.distanceUnit","km",FORMAT_DECIMALS);
        SetLayoutElements(mValueLeftElement2,"consumptionShortTermGeneral.speedValue","consumptionShortTermGeneral.speedUnit","km/h",FORMAT_DECIMALS);
        SetLayoutElements(mValueLeftElement3,"consumptionShortTermGeneral.time","","","FORMAT_SHORTTIME");
        SetLayoutElements(mValueLeftElement5,"","","",FORMAT_DECIMALS );
        SetLayoutElements(mValueLeftElement5,"","","",FORMAT_DECIMALS );
        SetLayoutElements(mValueLeftElement6,"shortTermConsumptionPrimary","shortTermConsumptionPrimary.unit","l/100km",FORMAT_DECIMALS );

        //Center elements
        SetLayoutElements(mValueCenterElement1,"consumptionLongTermGeneral.distanceValue","consumptionLongTermGeneral.distanceUnit","km",FORMAT_DECIMALS);
        SetLayoutElements(mValueCenterElement2,"consumptionLongTermGeneral.speedValue","consumptionLongTermGeneral.speedUnit","km/h",FORMAT_DECIMALS);
        SetLayoutElements(mValueCenterElement3,"consumptionLongTermGeneral.time","","","FORMAT_SHORTTIME");
        SetLayoutElements(mValueCenterElement5,"","","",FORMAT_DECIMALS );
        SetLayoutElements(mValueCenterElement5,"","","",FORMAT_DECIMALS );
        SetLayoutElements(mValueCenterElement6,"longTermConsumptionPrimary","longTermConsumptionPrimary.unit","l/100km",FORMAT_DECIMALS );

        //Right elements
        SetLayoutElements(mValueRightElement1,"tankLevelPrimary","","l",FORMAT_DECIMALS);
        SetLayoutElements(mValueRightElement2,"driving distance","consumptionShortTermGeneral.distanceUnit","km",FORMAT_NO_DECIMALS);
        SetLayoutElements(mValueRightElement3,"","", "","");
        SetLayoutElements(mValueRightElement5,"","","",FORMAT_DECIMALS );
        SetLayoutElements(mValueRightElement5,"","","",FORMAT_DECIMALS );
        SetLayoutElements(mValueRightElement6,"currentConsumptionPrimary","currentConsumptionPrimary.unit","l/100km",FORMAT_DECIMALS );

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
        // Update Title - always!!!
        updateTitle();

        if (dashboardNum < 3) {
            // settings
            mConstraintClockLeft.setVisibility(View.VISIBLE);
            mConstraintClockCenter.setVisibility(View.VISIBLE);
            mConstraintClockRight.setVisibility(View.VISIBLE);

        //update each of the elements:
        updateElement(mElement1Query, mValueElement1, mIconElement1);
        updateElement(mElement2Query, mValueElement2, mIconElement2);
        updateElement(mElement3Query, mValueElement3, mIconElement3);
        updateElement(mElement4Query, mValueElement4, mIconElement4);

        //update each of the clocks and the min/max/ray elements that go with it
        // query, dial, visray, textmax, textmin, clockmax, clockmin)

        updateClock(mClockLQuery, mClockLeft, mRayLeft, mTextMaxLeft, mClockMaxLeft, mGraphLeft, mSpeedSeriesLeft, graphLeftLastXValue, mGraphValueLeft, MaxspeedLeft);
        updateClock(mClockCQuery, mClockCenter, mRayCenter, mTextMaxCenter, mClockMaxCenter, mGraphCenter, mSpeedSeriesCenter, graphCenterLastXValue, mGraphValueCenter, MaxspeedCenter);
        updateClock(mClockRQuery, mClockRight, mRayRight, mTextMaxRight,  mClockMaxRight,  mGraphRight, mSpeedSeriesRight, graphRightLastXValue, mGraphValueRight, MaxspeedRight);


        // get ambient color, change color of some elements to match the ambient color.
        // this can't be done during setup, because then the ambientColor is probably not received yet.
        if (ambientOn) {
            String ambientColor =
                    mLastMeasurements.containsKey("Car_ambienceLightColour.ColourSRGB")?
                            (String) mLastMeasurements.get("Car_ambienceLightColour.ColourSRGB") : null;
            //ambientColor = "#FF0000"; // for testing purposes
            if (ambientColor != null && !ambientColor.equals("")) {
                int parsedColor = Color.parseColor(ambientColor);

                if ((/*parsedColor != mClockLeft.getIndicatorColor()) || */((parsedColor != mRayLeft.getRayColor())))) {
                    if (raysOn) {
                        mRayLeft.setRayColor(parsedColor);
                        mRayCenter.setRayColor(parsedColor);
                        mRayRight.setRayColor(parsedColor);
                    } else {
                        //mClockLeft.setIndicatorColor(parsedColor);
                        //mClockCenter.setIndicatorColor(parsedColor);
                        //mClockRight.setIndicatorColor(parsedColor);
                        mClockLeft.setIndicatorLightColor(parsedColor);
                        mClockCenter.setIndicatorLightColor(parsedColor);
                        mClockRight.setIndicatorLightColor(parsedColor);
                    }

                    switch (selectedBackground) {
                        case "background_incar_dots":
                        case "background_incar_skoda2":
                            int resId = getResources().getIdentifier(selectedBackground, "drawable", getContext().getPackageName());
                            Drawable wallpaperImage = ContextCompat.getDrawable(getContext(),resId);

                            wallpaperImage.setColorFilter(new LightingColorFilter(parsedColor, Color.parseColor("#010101")));

                            rootView.setBackground(wallpaperImage);
                            break;
                    }
                }
            }
        }
        } else {
            mConstraintClockLeft.setVisibility(View.INVISIBLE);
            mConstraintClockCenter.setVisibility(View.INVISIBLE);
            mConstraintClockRight.setVisibility(View.INVISIBLE);

            UpdateLayoutElements();
        }

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
        //todo: clean this up. This can be done much nicer.
        if (queryElement.equals("none")) {
            label.setText("");
            value.setText("");
            icon = "empty";
            value.setVisibility(View.INVISIBLE);
        } else {
            label.setText("");
            value.setText("-");
        }


        // set the labels
        switch (queryElement) {
            case "none":
                icon = "empty";
                break;
            case "test":
                label.setBackground(getContext().getDrawable(R.drawable.ic_measurement));
                break;
            case "batteryVoltage":
            case "torque-voltage_0xff1238":
                value.setText(FORMAT_VOLT0);
                label.setBackground(getContext().getDrawable(R.drawable.ic_battery));
                break;
            case "Nav_Altitude":
                label.setBackground(getContext().getDrawable(R.drawable.ic_altitude));
                break;
            case "Nav_Heading":
                label.setBackground(getContext().getDrawable(R.drawable.ic_heading));
                break;
            case "coolantTemperature":
            case "torque-enginecoolanttemp_0x05":
                label.setText("");
                value.setText(FORMAT_TEMPERATURE0);
                label.setBackground(getContext().getDrawable(R.drawable.ic_water));
                break;
            case "oilTemperature":
            case "torque-oiltemperature_0x5c":
                value.setText(FORMAT_TEMPERATURE0);
                label.setBackground(getContext().getDrawable(R.drawable.ic_oil));
                break;
            case "vehicleSpeed":
            case "torque-speed_0x0d":
                label.setText(R.string.unit_kmh);
                icon = "empty";
                break;
            case "torque-rpm_0x0c":
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
            case "torque-transmissiontemp_0x0105":
            case "torque-transmissiontemp_0xfe1805":
                value.setText(FORMAT_TEMPERATURE0);
                label.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "outsideTemperature":
            case "torque-ambientairtemp_0x46":
                value.setText("-");//value.setText(R.string.format_temperature0);
                label.setBackground(getContext().getDrawable(R.drawable.ic_outsidetemperature));
                break;
            case "currentGear":
                label.setBackground(getContext().getDrawable(R.drawable.ic_gearbox));
                break;
            case "torque-accelerometer_total_0xff1223":
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
                label.setBackground(getContext().getDrawable(R.drawable.ic_steering));
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
            case "tyrePressures.pressureRearRight":
            case "tyreTemperatures.temperatureRearRight":
                label.setText(getString(R.string.label_tyreRR));
                label.setBackground(getContext().getDrawable(R.drawable.ic_tyre));
                break;
            case "tyreStates.stateRearLeft":
            case "tyrePressures.pressureRearLeft":
            case "tyreTemperatures.temperatureRearLeft":
                label.setText(getString(R.string.label_tyreRL));
                label.setBackground(getContext().getDrawable(R.drawable.ic_tyre));
                break;
            case "tyreStates.stateFrontRight":
            case "tyrePressures.pressureFrontRight":
            case "tyreTemperatures.temperatureFrontRight":
                label.setText(getString(R.string.label_tyreFR));
                label.setBackground(getContext().getDrawable(R.drawable.ic_tyre));
                break;
            case "tyreStates.stateFrontLeft":
            case "tyrePressures.pressureFrontLeft":
            case "tyreTemperatures.temperatureFrontLeft":
                label.setText(getString(R.string.label_tyreFL));
                label.setBackground(getContext().getDrawable(R.drawable.ic_tyre));
                break;
            case "tankLevelPrimary":
            case "torque-fuellevel_0x2f":
                //label.setText("1");
                label.setBackground(getContext().getDrawable(R.drawable.ic_fuel));
                break;
            case "tankLevelSecondary":
                //label.setText("2");
                label.setBackground(getContext().getDrawable(R.drawable.ic_fuel));
                break;
            case "torque-engineload_0x04":
                label.setText(getString(R.string.label_load));
                icon = "empty";
                break;
            case "torque-timing_advance_0x0e":
                label.setBackground(getContext().getDrawable(R.drawable.ic_timing));
                break;
            case "torque-intake_air_temperature_0x0f":
                label.setText(getString(R.string.label_iat));
                icon = "empty";
                break;
            case "torque-mass_air_flow_0x10":
                label.setText(getString(R.string.label_maf));
                icon = "empty";
                break;
            case "torque-throttle_position_0x11":
                label.setBackground(getContext().getDrawable(R.drawable.ic_throttle));
                break;
            case "torque-turboboost_0xff1202":
                label.setBackground(getContext().getDrawable(R.drawable.ic_turbo));
                break;
            case "torque-AFR_0xff1249":
                label.setText(getString(R.string.label_afr));
                icon = "empty";
                break;
            case "torque-AFRc_0xff124d":
                label.setText(getString(R.string.label_afrc));
                icon = "empty";
                break;
            case "torque-fueltrimshortterm1_0x06":
                label.setText(getString(R.string.label_ftst1));
                icon = "empty";
                break;
            case "torque-fueltrimlongterm1_0x07":
                label.setText(getString(R.string.label_ftlt1));
                icon = "empty";
                break;
            case "torque-fueltrimshortterm2_0x08":
                label.setText(getString(R.string.label_ftst2));
                icon = "empty";
                break;
            case "torque-fueltrimlongterm2_0x09":
                label.setText(getString(R.string.label_ftlt2));
                icon = "empty";
                break;
            case "torque-exhaustgastempbank1sensor1_0x78":
                label.setText("1");
                label.setBackground(getContext().getDrawable(R.drawable.ic_fuelpressure));
                break;
            case "torque-exhaustgastempbank1sensor2_0xff1282":
                label.setText("2");
                label.setBackground(getContext().getDrawable(R.drawable.ic_fuelpressure));
                break;
            case "torque-exhaustgastempbank1sensor3_0xff1283":
                label.setText("3");
                label.setBackground(getContext().getDrawable(R.drawable.ic_fuelpressure));
                break;
            case "torque-exhaustgastempbank1sensor4_0xff1284":
                label.setText("4");
                label.setBackground(getContext().getDrawable(R.drawable.ic_exhaust));
                break;
            case "torque-fuelrailpressure_0x23":
            case "torque-fuelpressure_0x0a":
                label.setBackground(getContext().getDrawable(R.drawable.ic_fuelpressure));
                break;
            case "torque-absolutethrottlepostion_0x47":
                label.setBackground(getContext().getDrawable(R.drawable.ic_throttle));
                break;
            case "torque-catalysttemperature_0x3c":
                label.setBackground(getContext().getDrawable(R.drawable.ic_catalyst));
                break;
            case "torque-chargeaircoolertemperature_0x77":
                label.setBackground(getContext().getDrawable(R.drawable.ic_cact));
                break;
            case "torque-commandedequivalenceratiolambda_0x44":
                label.setText("λ");
                break;
            case "torque-o2sensor1equivalenceratio_0x34":
                label.setText("O²");
                break;
            case "torque-phonebarometer_0xff1270":
                label.setBackground(getContext().getDrawable(R.drawable.ic_barometer));
                break;
            case "torque-engineloadabsolute_0x43":
                label.setText("Load");
                break;
            case "torque-phonebatterylevel_0xff129a":
                label.setBackground(getContext().getDrawable(R.drawable.ic_phone));
                break;
            case "torque-obdadaptervoltage_0xff1238":
                label.setBackground(getContext().getDrawable(R.drawable.ic_obd2));
                break;
            case "torque-intakemanifoldpressure_0x0b":
                label.setBackground(getContext().getDrawable(R.drawable.ic_manifold));
                break;
            case "torque-pressurecontrol_0x70":
                label.setBackground(getContext().getDrawable(R.drawable.ic_turbo));
                break;
            case "torque-relativethrottleposition_0x45":
                label.setBackground(getContext().getDrawable(R.drawable.ic_throttle));
                break;
            case "torque-voltagemodule_0x42":
                label.setBackground(getContext().getDrawable(R.drawable.ic_voltage));
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
            params.width = 40;
            label.setLayoutParams(params);

        }
    }

    private void setupGraph(Speedometer clock, GraphView graph, LineGraphSeries<DataPoint> serie, ConstraintLayout constraint) {

        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.themedBlankDialBackground});
        int blankBackgroundResource = typedArray.getResourceId(0, 0);
        int graphColor1 = 0;
        int graphColor2 = 0;
        typedArray.recycle();

            if (selectedTheme.contains("Sport")){
                graphColor1 = Color.BLACK;
                graphColor2 = Color.GRAY;}

            else {
                graphColor1 = Color.parseColor("#AAFFFFFF");
                graphColor2 = Color.parseColor("#22FFFFFF");
            }

        graph.addSeries(serie);

        graph.setTitle(clock.getUnit());
        graph.setTitleColor(graphColor1);

        constraint.setBackgroundResource(blankBackgroundResource); // put blank background
        serie.setAnimated(true);
        graph.setElevation(55);
        Viewport graphViewport = graph.getViewport();
        GridLabelRenderer gridLabelRenderer = graph.getGridLabelRenderer();

        graphViewport.setXAxisBoundsManual(true);
        graphViewport.setYAxisBoundsManual(true);
        graphViewport.setMinX(0);
        // set default max and min, these will be set dynamically later
        graphViewport.setMaxX(120);

        graphViewport.setScrollable(false);
        gridLabelRenderer.setVerticalLabelsVisible(true);
        gridLabelRenderer.setHighlightZeroLines(false);
        gridLabelRenderer.setGridColor(graphColor2);
        gridLabelRenderer.setVerticalLabelsColor(graphColor2);



        gridLabelRenderer.setHorizontalLabelsVisible(false);
        gridLabelRenderer.setGridStyle(GridLabelRenderer.GridStyle.HORIZONTAL);

        graphViewport.setBackgroundColor(Color.argb(0, 255, 0, 0));
        serie.setDrawDataPoints(false);
        serie.setThickness(2);

        serie.setColor(graphColor1);
    }

    private void setupClocks(String queryClock, Speedometer clock, TextView icon, RaySpeedometer ray,  Speedometer max) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());


        //todo: get all the min/max unit stuff for exlap items from schema.json


        String queryTrim;
        String queryLong = queryClock;
        String torqueUnit = "";
        int torqueMin = 0;
        int torqueMax = 100;
        clock.clearSections();
        max.clearSections();
        ray.clearSections();

        //add sections:
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = getContext().getTheme();
        theme.resolveAttribute(R.attr.themedNeedleColor, typedValue, true);
        @ColorInt int color = typedValue.data;

        ray.addSections(
                new Section(0f, .75f, color)
                , new Section(.76f, .99f, Color.RED)

        );

        //ray.clearSections();

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
            long queryPid = new BigInteger(queryClock, 16).longValue();

            if (queryPid == 2236463){ //if query == 22202f, this means the custom PID is used.
                String mQueryTemp = sharedPreferences.getString("customPID", "22202f");
                assert mQueryTemp != null;
                queryPid = Long.decode("0x" + mQueryTemp);
                Log.e(TAG, "queryPid: " + queryPid);
            }

            try {
                if (torqueService != null) {
                    torqueUnit = torqueService.getUnitForPid(queryPid);

                    //todo: use torque min and max values to determine min/max values for torque elements
                    torqueMin = Math.round(torqueService.getMinValueForPid(queryPid));
                    torqueMax = Math.round(torqueService.getMaxValueForPid(queryPid));
                    if (torqueMin == torqueMax) {
                        torqueMin = torqueMax - 1;  // prevent min and max are equal. Speedview cannot handle this.
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error: " + e.getMessage());
            }
        } else {
            torqueMax = 100;
        }


        Log.d(TAG, "minmax speed: " + torqueMin + " " + torqueMax);

        pressureUnit = "bar";
        pressureMax = 5;
        pressureMin = -1;

        //setupClock(icon, "ic_none", "", clock, false, "", 0, 100, "float");

        // setup each of the clocks:
        switch (queryLong) {
            case "none": // currently impossible to choose, maybe in the future?
                setupClock(icon, "ic_none", "", clock, false, "", 0, 100, "float", "float");
                break;
            case "test":
                setupClock(icon, "ic_measurement", "", clock, false, getString(R.string.testing), 0, 360, "float", "integer");
                break;
            case "exlap-vehicleSpeed":
            case "torque-speed_0x0d":
                setupClock(icon, "ic_none", "", clock, false, getString(R.string.unit_kmh), 0, 300, "integer", "integer");
                break;
            case "exlap-Nav_Altitude":
                setupClock(icon, "ic_altitude", "", clock, false, "m", -100, 3000, "integer", "integer");
                break;
            case "exlap-Nav_Heading": // this is a compass, so a little bit more is needed to setup the clock
                setupClock(icon, "ic_heading", "", clock, false, "°", 0, 360, "integer", "integer");
                clock.setMarkColor(Color.parseColor("#00FFFFFF"));

                //set the degrees so it functions as a circle
                clock.setStartDegree(270);
                clock.setEndDegree(630);
                ray.setStartDegree(270);
                ray.setEndDegree(630);
                //min.setStartDegree(270);
                //min.setEndDegree(630);
                max.setStartDegree(270);
                max.setEndDegree(630);
                // set background resource to the same as stopwatch
                clock.setBackgroundResource(swBackgroundResource);
                break;
            case "exlap-engineSpeed":
            case "torque-rpm_0x0c":
                setupClock(icon, "ic_none", getString(R.string.unit_rpm), clock, true, getString(R.string.unit_rpm1000), 0, 9, "float", "integer");
                //clock.setTicks();
                //clock.setTickTextFormat(0);
                break;
            case "torque-voltage_0xff1238":
            case "exlap-batteryVoltage":
            case "torque-voltagemodule_0x42":
                setupClock(icon, "ic_battery", "", clock, false, getString(R.string.unit_volt), 0, 17, "float", "integer");
                break;
            case "exlap-oilTemperature":
            case "torque-oiltemperature_0x5c":
                setupClock(icon, "ic_oil", "", clock, true, "°", 0, 200, "float", "integer");
                break;
            case "exlap-coolantTemperature":
            case "torque-enginecoolanttemp_0x05":
                setupClock(icon, "ic_water", "", clock, true, "°", 0, 200, "float", "integer");
                break;
            case "exlap-outsideTemperature":
            case "torque-ambientairtemp_0x46":
                setupClock(icon, "ic_outsidetemperature", "", clock, false, "°", -25, 50, "float", "integer");
                break;
            case "torque-transmissiontemp_0x0105":
            case "torque-transmissiontemp_0xfe1805":
            case "exlap-gearboxOilTemperature":
                setupClock(icon, "ic_gearbox", "", clock, false, "°", 0, 200, "float", "integer");
                break;
            case "torque-turboboost_0xff1202":
                setupClock(icon, "ic_turbo", "", clock, true, torqueUnit, torqueMin, torqueMax, "float", "float");
                break;
            case "exlap-absChargingAirPressure":
            case "exlap-relChargingAirPressure":
                setupClock(icon, "ic_turbo", "", clock, true, pressureUnit, pressureMin, pressureMax, "float", "integer");
                break;
            case "exlap-lateralAcceleration":
                setupClock(icon, "ic_lateral", "", clock, false, getString(R.string.unit_g), -3, 3, "float", "float");
                break;
            case "exlap-longitudinalAcceleration":
                setupClock(icon, "ic_longitudinal", "", clock, false, getString(R.string.unit_g), -3, 3, "float", "float");
                break;
            case "exlap-yawRate":
                setupClock(icon, "ic_yaw", "", clock, false, "°/s", -1, 1, "float", "integer");
                break;
            case "wheelAngle":
                setupClock(icon, "ic_wheelangle", "", clock, false, "°", -45, 45, "float", "integer");
                break;
            case "exlap-EcoHMI_Score.AvgShort":
            case "exlap-EcoHMI_Score.AvgTrip":
                setupClock(icon, "ic_eco", "", clock, false, "", 0, 100, "integer", "integer");
                break;
            case "exlap-powermeter":
                setupClock(icon, "ic_powermeter", "", clock, false, "%", -1000, 5000, "integer", "integer");
                break;
            case "exlap-acceleratorPosition":
                setupClock(icon, "ic_pedalposition", "", clock, false, "%", 0, 100, "integer", "integer");
                break;
            case "exlap-brakePressure":
                setupClock(icon, "ic_brakepedalposition", "", clock, false, "%", 0, 100, "integer", "integer");
                break;
            case "exlap-currentTorque":
                setupClock(icon, "ic_none", "", clock, false, getString(R.string.unit_nm), 0, 500, "integer", "integer");
                break;
            case "exlap-currentOutputPower":
                setupClock(icon, "ic_none", "", clock, false, getString(R.string.unit_kw) , 0, 500, "integer", "integer");
                break;
            case "exlap-currentConsumptionPrimary":
            case "exlap-cycleConsumptionPrimary":
                setupClock(icon, "ic_fuelprimary", "", clock, false, "l/100km", 0, 100, "float", "integer");
                break;
            case "exlap-currentConsumptionSecondary":
            case "exlap-cycleConsumptionSecondary":
                setupClock(icon, "ic_fuelsecondary", "", clock, false, "l/100km", 0, 100, "float", "integer");
                break;
            case "exlap-tankLevelPrimary":
            case "torque-fuellevel_0x2f":
                setupClock(icon, "ic_fuelprimary", "", clock, false, "l", 0, 100, "float", "integer");
                break;
            case "exlap-tankLevelSecondary":
                setupClock(icon, "ic_fuelsecondary", "", clock, false, "%", 0, 100, "float", "integer");
                break;
            case "torque-fuelpressure_0x0a":
                setupClock(icon, "ic_fuelpressure", getString(R.string.label_fuel), clock, false, torqueUnit, torqueMin, torqueMax, "float", "integer");
                break;
            case "torque-engineload_0x04":
            case "torque-engineloadabsolute_0x43":
                setupClock(icon, "ic_none", getString(R.string.label_load), clock, false, torqueUnit, 0, 100, "float", "integer");
                break;
            case "torque-timing_advance_0x0e":
                setupClock(icon, "ic_timing", "", clock, false, torqueUnit, torqueMin, torqueMax, "float", "integer");
                break;
            case "torque-intake_air_temperature_0x0f":
                setupClock(icon, "ic_none", getString(R.string.label_iat), clock, false, torqueUnit, 0, 100, "float", "integer");
                break;
            case "torque-mass_air_flow_0x10":
                setupClock(icon, "ic_none", getString(R.string.label_maf), clock, false, torqueUnit, torqueMin, torqueMax, "float", "integer");
                break;
            case "torque-AFR_0xff1249":
                setupClock(icon, "ic_none", getString(R.string.label_afr), clock, false, torqueUnit, 0, 35, "float", "integer");
                break;
            case "torque-AFRc_0xff124d":
                setupClock(icon, "ic_none", getString(R.string.label_afrc), clock, false, torqueUnit, 0, 35, "float", "integer");
                break;
            case "torque-fueltrimshortterm1_0x06":
                setupClock(icon, "ic_none", getString(R.string.label_ftst1), clock, false, torqueUnit, -20, 20, "float", "integer");
                break;
            case "torque-fueltrimlongterm1_0x07":
                setupClock(icon, "ic_none", getString(R.string.label_ftlt1), clock, false, torqueUnit, -20, 20, "float", "integer");
                break;
            case "torque-fueltrimshortterm2_0x08":
                setupClock(icon, "ic_none", getString(R.string.label_ftst2), clock, false, torqueUnit, -20, 20, "float", "integer");
                break;
            case "torque-fueltrimlongterm2_0x09":
                setupClock(icon, "ic_none", getString(R.string.label_ftlt2), clock, false, torqueUnit, -20, 20, "float", "integer");
                break;
            case "torque-accelerometer_total_0xff1223":
                setupClock(icon, "ic_none", "", clock, false, "G", -3, 3, "float", "float");
                break;
            case "torque-phonebatterylevel_0xff129a":
                setupClock(icon, "ic_phone", "", clock, false, "%", 0, 100, "integer", "integer");
                break;
            case "torque-phonebarometer_0xff1270":
                setupClock(icon, "ic_barometer", "", clock, false, torqueUnit, torqueMin, torqueMax, "float", "integer");
                break;
            case "torque-obdadaptervoltage_0xff1238":
                setupClock(icon, "ic_obd2", "", clock, false, torqueUnit, 0, 17, "float", "integer");
                break;
            case "torque-hybridbattlevel_0x5b":
                setupClock(icon, "ic_battery", "", clock, false, "%", 0, 100, "float", "integer");
                break;
            case "torque-commandedequivalenceratiolambda_0x44":
                setupClock(icon, "ic_none", "lambda", clock, false, torqueUnit, 0, 3, "float", "float");
                break;
            case "torque-catalysttemperature_0x3c":
                setupClock(icon, "ic_catalyst", "", clock, false, torqueUnit, 0, 1000, "float", "integer");
                break;
            case "torque-relativethrottleposition_0x45":
            case "torque-absolutethrottlepostion_0x47":
            case "torque-throttle_position_0x11":
                setupClock(icon, "ic_throttle", "", clock, false, torqueUnit, 0, 100, "float", "integer");
                break;
            case "torque-intakemanifoldpressure_0x0b":
                setupClock(icon, "ic_manifold", "", clock, false, torqueUnit, 0, 200, "float", "integer");
                break;
            case "torque-chargeaircoolertemperature_0x77":
                setupClock(icon, "ic_cact", "", clock, false, torqueUnit, 0, 100, "float", "integer");
                break;
            case "torque-pressurecontrol_0x70":
                setupClock(icon, "ic_turbo", "", clock, false, pressureUnit, pressureMin * 30, pressureMax * 30, "float", "integer");
                break;
            case "torque-o2sensor1equivalenceratio_0x34":
                setupClock(icon, "ic_none", "O2 sensor", clock, false, torqueUnit, 0, 3, "float", "float");
                break;
            case "exlap-tyrePressures.pressureRearRight":
                setupClock(icon, "ic_tyre", getString(R.string.label_tyreRR), clock, false, pressureUnit, 0, 4, "float", "float");
                break;
            case "exlap-tyrePressures.pressureRearLeft":
                setupClock(icon, "ic_tyre", getString(R.string.label_tyreRL), clock, false, pressureUnit, 0, 4, "float", "float");
                break;
            case "exlap-tyrePressures.pressureFrontRight":
                setupClock(icon, "ic_tyre", getString(R.string.label_tyreFR), clock, false, pressureUnit, 0, 4, "float", "float");
                break;
            case "exlap-tyrePressures.pressureFrontLeft":
                setupClock(icon, "ic_tyre", getString(R.string.label_tyreFL), clock, false, pressureUnit, 0, 4, "float", "float");
                break;
            case "exlap-tyreTemperatures.temperatureRearRight":
                setupClock(icon, "ic_tyre", getString(R.string.label_tyreRR), clock, false, temperatureUnit, 0, 100, "float", "integer");
                break;
            case "exlap-tyreTemperatures.temperatureRearLeft":
                setupClock(icon, "ic_tyre", getString(R.string.label_tyreRL), clock, false, temperatureUnit, 0, 100, "float", "integer");
                break;
            case "exlap-tyreTemperatures.temperatureFrontRight":
                setupClock(icon, "ic_tyre", getString(R.string.label_tyreFR), clock, false, temperatureUnit, 0, 100, "float", "integer");
                break;
            case "exlap-tyreTemperatures.temperatureFrontLeft":
                setupClock(icon, "ic_tyre", getString(R.string.label_tyreFL), clock, false, temperatureUnit, 0, 100, "float", "integer");
                break;
            case "torque-exhaustgastempbank1sensor1_0x78":
                setupClock(icon, "ic_exhaust", "1", clock, false, torqueUnit, 0, 1000, "float", "integer");
                break;
            case "torque-exhaustgastempbank1sensor2_0xff1282":
                setupClock(icon, "ic_exhaust", "2", clock, false, torqueUnit, 0, 1000, "float", "integer");
                break;
            case "torque-exhaustgastempbank1sensor3_0xff1283":
                setupClock(icon, "ic_exhaust", "3", clock, false, torqueUnit, 0, 1000, "float", "integer");
                break;
            case "torque-exhaustgastempbank1sensor4_0xff1284":
                setupClock(icon, "ic_exhaust", "4", clock, false, torqueUnit, 0, 1000, "float", "integer");
                break;
            case "torque-fuelrailpressure_0x23":
                setupClock(icon, "ic_fuelpressure", "", clock, false, torqueUnit, 0, 100, "float", "integer");
                break;
            case "torque-customPID_0x22202f":
                setupClock(icon, "ic_obd2", "", clock, false, torqueUnit, 0, 100, "float", "integer");
                break;
        }

        // make the icon appear in the color of unitTextColor
        Drawable iconBackground = icon.getBackground();
        if (iconBackground != null) {
            int iconTint = clock.getUnitTextColor();
            iconBackground.setColorFilter(iconTint, PorterDuff.Mode.SRC_ATOP);
            icon.setBackground(iconBackground);
            icon.setTextColor(iconTint);
        }

        // bring mins and max's in line with the clock
        float minimum = clock.getMinSpeed();
        float maximum = clock.getMaxSpeed();

        //min.setMinMaxSpeed(minimum, maximum);
        ray.setMinMaxSpeed(minimum, maximum);
        max.setMinMaxSpeed(minimum, maximum);
    }


    //update clock with data
    private void updateClock(String query, Speedometer clock, RaySpeedometer visray, TextView
            textmax, Speedometer clockmax, GraphView graph, LineGraphSeries<DataPoint> series, Double graphLastXValue,
             TextView graphValue, float[] MaxSpeed) {
        if (query != null && stagingDone) {

            float speedFactor = 1f;
            pressureFactor = 1f;

            Float clockValue = 0f;
            Float oldValue = clock.getSpeed();

            String queryLong = query;
            String unitText = "";

            String temperatureUnitExlap = (String) mLastMeasurements.get("unitTemperature.temperatureUnit");


            if (temperatureUnitExlap == null) {
                temperatureUnitExlap = "°";
            }

            // Get the value that should be put on the clock, depending on the query
            // exlap queries use mLastMeasurements.get(query)
            // torque pid queries use torqueService.getValueForPid(queryPid), queryPid is trimmed from the query string
            String queryTrim = query.contains("-") ? query.substring(0, query.indexOf("-")) : "other";
            switch (queryTrim) {
                case "torque":
                    query = query.substring(query.lastIndexOf('_') + 1);
                    query = query.substring(2);
                    long queryPid = new BigInteger(query, 16).longValue();

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
                    clockValue = randFloat(0, 360);
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
                    case "exlap-yawRate":
                    case "exlap-EcoHMI_Score.AvgShort":
                    case "exlap-EcoHMI_Score.AvgTrip":
                    case "exlap-brakePressure":
                    case "exlap-currentTorque":
                    case "exlap-lateralAcceleration":

                        // all data that can be put on the clock without further modification:
                        break;
                    // car reports longitudinal acceleration as m/s². This is a conversion to G's
                    case "exlap-longitudinalAcceleration":
                        clockValue = clockValue / (float) 9.80665;
                        break;
                    case "exlap-currentOutputPower":
                        clockValue = clockValue * powerFactor;

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
                    case "exlap-tyreTemperatures.temperatureRearRight":
                    case "exlap-tyreTemperatures.temperatureRearLeft":
                    case "exlap-tyreTemperatures.temperatureFrontRight":
                    case "exlap-tyreTemperatures.temperatureFrontLeft":
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
                        clockValue = clockValue * fueltanksize;
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
                    case "exlap-tyrePressures.pressureRearRight":
                    case "exlap-tyrePressures.pressureRearLeft":
                    case "exlap-tyrePressures.pressureFrontRight":
                    case "exlap-tyrePressures.pressureFrontLeft":
                        clock.setUnit(pressureUnit);
                        clockValue = (clockValue / 10) * pressureFactor;
                        //clock.setTickTextFormat(Gauge.FLOAT_FORMAT);
                        break;
                    // torque data elements:
                    case "torque-speed_0x0d":
                    case "torque-fuelpressure_0x0a":
                    case "torque-engineload_0x04":
                    case "torque-timing_advance_0x0e":
                    case "torque-mass_air_flow_0x10":
                    case "torque-throttle_position_0x11":
                    case "torque-AFR_0xff1249":
                    case "torque-AFRc_0xff124d":
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
                    case "torque-fuelrailpressure_0x23":
                    case "torque-customPID_0x22202f":
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
                    case "torque-transmissiontemp_0xfe1805":
                    case "torque-oiltemperature_0x5c":
                    case "torque-catalysttemperature_0x3c":
                    case "torque-chargeaircoolertemperature_0x77":
                    case "torque-enginecoolanttemp_0x05":
                    case "torque-exhaustgastempbank1sensor1_0x78":
                    case "torque-exhaustgastempbank1sensor2_0xff1282":
                    case "torque-exhaustgastempbank1sensor3_0xff1283":
                    case "torque-exhaustgastempbank1sensor4_0xff1284":
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

                graph.getViewport().setMaxY(clock.getMaxSpeed());
                graph.getViewport().setMinY(clock.getMinSpeed());

            }

            // get the speed from the clock and have the high-visibility rays move to this speed as well

            boolean noNewData = clockValue==null;
            if (noNewData)
                clockValue=oldValue;


            //TODO: Updates with a non fixed period could lead to strange graphs
            series.appendData(new DataPoint(graphLastXValue, clockValue), true, 2400);
            String tempString = (String.format(Locale.US, FORMAT_DECIMALS, clockValue));
            graphValue.setText(tempString);


            // don't update when there's nothing to update
            // check if old value and new value (rounded to 1 decimal placed) are equal
            if (noNewData || Math.round(clockValue*10) == Math.round(oldValue*10)) {
                return;
            }

            // update clock with latest clockValue
            clock.speedTo(clockValue);

            if (visray.isShown()) {
                visray.speedTo(clockValue);
            }

            // update the max clocks and text
            float maxValue = clockmax.getSpeed();

            if (clockValue > maxValue) {
                if (clockmax.isShown()) {
                    clockmax.setSpeedAt(clockValue);
                }
            }
            // Max Value update
            if (maxOn && clockValue > MaxSpeed[dashboardNum]) {
                textmax.setText(String.format(Locale.US, FORMAT_DECIMALS, clockValue));
            // Save max Value
            MaxSpeed[dashboardNum] = clockValue;
            }
        }

     /*           // update the min clocks and text
                if (clockValueToGraph < minValue) {
                    clockmin.setSpeedAt(clockValueToGraph);
                    textmin.setText(String.format(Locale.US, getContext().getText(R.string.format_decimals).toString(), clockValueToGraph));
                }
      */
    }


    private String getTime() {
        String clockFormat = "hh:mm a";

        // If available, force car clock format
        String carClockFormat = (String)mLastMeasurements.get("unitTimeFormat.clockFormat");
        if (carClockFormat != null) {
            switch (carClockFormat) {
                case "format_24h":
                    clockFormat = "HH:mm";
                    break;
                case "format_12h":
                    clockFormat = "hh:mm a";
                    break;
            }
        } else { // if not, set time format based on phone settings
            clockFormat = androidClockFormat;
        }
        return new SimpleDateFormat(clockFormat, Locale.US).format(new Date());
    }


    private Boolean checkTextNav(String mText){
        Boolean mistWrong = false;
        String str2=" �";
        String str3 = "  ";
        mText = mText.trim();

        if (mText.contains(str2)){
            mistWrong = true;
        }
        if (mText.contains(str3)){
            mistWrong = true;
        }
        return mistWrong;
    }

    private void updateTitle() {

        String currentTitleValue = mTitleElement.getText().toString();
        String currentRightTitleValue = mTitleElementRight.getText().toString();
        String currentLeftTitleValue = mTitleElementLeft.getText().toString();
        String currentNavDistanceTitleValue = mTitleElementNavDistance.getText().toString();
        String currentNavTimeTitleValue = mTitleElementNavTime.getText().toString();

        // Display location in center of title bar:

        Boolean mProximity = (Boolean) mLastMeasurements.get("System_ProximityRecognition.InRange");


        //mProximity = true;
        if (mProximity != null && mProximity && proximityOn) {
            mTitleClockLeft.setText(mLabelClockL);
            mTitleClockCenter.setText(mLabelClockC);
            mTitleClockRight.setText(mLabelClockR);
            mBtnNext.setVisibility(View.VISIBLE);
            mBtnPrev.setVisibility(View.VISIBLE);
            mtextTitleMain.setVisibility(View.VISIBLE);
            // mtextTitleMain.setTextColor(Color.WHITE);



        } else if (!proximityOn) {
            mTitleClockLeft.setText("");
            mTitleClockCenter.setText("");
            mTitleClockRight.setText("");
            mBtnNext.setVisibility(View.VISIBLE);
            mBtnPrev.setVisibility(View.VISIBLE);
            mtextTitleMain.setVisibility(View.VISIBLE);
            //mtextTitleMain.setTextColor(Color.WHITE);


        } else {
            mTitleClockLeft.setText("");
            mTitleClockCenter.setText("");
            mTitleClockRight.setText("");
            mBtnNext.setVisibility(View.INVISIBLE);
            mBtnPrev.setVisibility(View.INVISIBLE);
            mtextTitleMain.setVisibility(View.INVISIBLE);
        }

        String currentTime = getTime();

        if (!Objects.equals(currentTitleValue, currentTime)) {
            mTitleElement.setText(currentTime);
        }

        // Display location in left side of Title  bar
        if (showStreetName) {
            String leftTitle="";
            if (sourceLocation.equals("Geocoding")) {
                leftTitle = googleGeocodeLocationStr;
            } else {
                if (googleMapsLocationStr != null && !googleMapsLocationStr.isEmpty()) {
                    leftTitle = googleMapsLocationStr;
                }
            }
            if (!forceGoogleGeocoding) {
                String location1 = (String) mLastMeasurements.get("Nav_CurrentPosition.Street");
                String location2 = (String) mLastMeasurements.get("Nav_CurrentPosition.City");

                if (location1 != null && checkTextNav(location1)) location1 = null;
                if (location2 != null && checkTextNav(location2)) location2 = null;

                if (location1 == null && location2 != null) leftTitle = location2;
                if (location1 != null && location2 == null) leftTitle = location1;
                if (location1 != null && location2 != null) leftTitle = location1 + ", " + location2;
            }

            if (!currentLeftTitleValue.equals(leftTitle)) {
                mTitleElementLeft.setText(leftTitle);
            }

            if (leftTitle.equals("")) {
                    mTitleIcon2.setVisibility(View.INVISIBLE);
                } else {
                    mTitleIcon2.setVisibility(View.VISIBLE);
                }
        }

        // Display temperature in right side of Title  bar
        Float currentTemperature = (Float) mLastMeasurements.get("outsideTemperature");
        if (currentTemperature != null) {
            if (!celsiusTempUnit) {
                currentTemperature = CarUtils.celsiusToFahrenheit(currentTemperature);
            }
            mTitleIcon1.setVisibility(View.VISIBLE);
            String temperature =
                    String.format(Locale.US, FORMAT_DECIMALS, currentTemperature) + " " + temperatureUnit;
            if (!temperature.equals(currentRightTitleValue)){
                mTitleElementRight.setText(temperature);
            }
        } else {
            mTitleIcon1.setVisibility(View.INVISIBLE);
            mTitleElementRight.setText("");
        }

        // Display NAV Distance, only value <> 0
        Float currentNavDistance = (Float) mLastMeasurements.get("Nav_GuidanceRemaining.DTD");
        //     int testDistance = 35623;
        //    currentNavDistance = new Float(testDistance);

        if (currentNavDistance != null) {
            if (currentNavDistance != 0) {
                currentNavDistance = currentNavDistance / 1000;
                String NavDistance = String.format(Locale.US, "%.1f km", currentNavDistance);
                mTitleIcon3.setVisibility(View.VISIBLE);
                mTitleElementNavDistance.setVisibility(View.VISIBLE);
                if (!NavDistance.equals(currentNavDistanceTitleValue)) {
                    mTitleElementNavDistance.setText(NavDistance);
                }
            } else if (currentNavDistance == 0) {
                mTitleIcon3.setVisibility(View.INVISIBLE);
                mTitleElementNavDistance.setVisibility(View.INVISIBLE);
                mTitleElementNavDistance.setText("");
            }
        } else if (currentNavDistance == null) {
            mTitleIcon3.setVisibility(View.INVISIBLE);
            mTitleElementNavDistance.setVisibility(View.INVISIBLE);
            mTitleElementNavDistance.setText("");
        }

        // Display NAV Time, only value <> 0
        Float currentNavTime = (Float) mLastMeasurements.get("Nav_GuidanceRemaining.RTT");


        //       int testValueTime = 2100;
        //       currentNavTime = new Float(testValueTime);
        Date time = new Date();
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);

        if (currentNavTime != null) {
            if (currentNavTime != 0) {
                //current time
                int NAVseconds = Math.round(currentNavTime);
                Calendar gcal = new GregorianCalendar();
                gcal.setTime(time);
                gcal.add(Calendar.SECOND, NAVseconds);
                Date NAVTimeNew = gcal.getTime();
                String NAVTime = df.format(NAVTimeNew);

                mTitleIcon4.setVisibility(View.VISIBLE);
                mTitleElementNavTime.setVisibility(View.VISIBLE);
                if (!NAVTime.equals(currentNavTimeTitleValue)) {
                    mTitleElementNavTime.setText(NAVTime);
                }
            } else if (currentNavTime == 0) {
                mTitleIcon4.setVisibility(View.INVISIBLE);
                mTitleElementNavTime.setVisibility(View.INVISIBLE);
                mTitleElementNavTime.setText("");
            }
        } else if (currentNavTime == null) {
            mTitleIcon4.setVisibility(View.INVISIBLE);
            mTitleElementNavTime.setVisibility(View.INVISIBLE);
            mTitleElementNavTime.setText("");
        }

        String mNAVStreet = (String) mLastMeasurements.get("Nav_GuidanceDestination.Street");
        String mNAVHousenumber = (String) mLastMeasurements.get("Nav_GuidanceDestination.Housenumber");
        String mNAVCity = (String) mLastMeasurements.get("Nav_GuidanceDestination.City");
        String mNAVadress = "";
        if (mNAVStreet != null && !mNAVStreet.equals("")) {
            mNAVadress=mNAVadress+mNAVStreet.trim()+" ";
        }
        if (mNAVHousenumber != null && !mNAVHousenumber.equals("")) {
            mNAVadress=mNAVadress+mNAVHousenumber.trim();
        }
        if (mNAVCity != null && !mNAVCity.equals("")) {
            if (!mNAVadress.equals("")) mNAVadress = mNAVadress + ", ";
            mNAVadress=mNAVadress+mNAVCity.trim();
        }
        mTitleNAVDestinationAddress.setText(mNAVadress);

        if (mProximity != null && !mNAVadress.equals("") && mProximity) {
            mTitleNAVDestinationAddress.setVisibility(View.VISIBLE);
            mTitleIcon4.setVisibility(View.INVISIBLE);
            mTitleElementNavTime.setVisibility(View.INVISIBLE);
            mTitleIcon3.setVisibility(View.INVISIBLE);
            mTitleElementNavDistance.setVisibility(View.INVISIBLE);
        }
        else {
            mTitleNAVDestinationAddress.setVisibility(View.INVISIBLE);
        }

    }

    private Typeface determineTypeFace(String selectedFont) {
        AssetManager assetsMgr = getContext().getAssets();
        Typeface typeface = Typeface.createFromAsset(assetsMgr, "digital.ttf");
        switch (selectedFont) {
            case "segments":
                typeface = Typeface.createFromAsset(assetsMgr, "digital.ttf");
                break;
            case "seat":
                typeface = Typeface.createFromAsset(assetsMgr, "SEAT_MetaStyle_MonoDigit_Regular.ttf");
                break;
            case "audi":
                typeface = Typeface.createFromAsset(assetsMgr, "AudiTypeDisplayHigh.ttf");
                break;
            case "vw":
                typeface = Typeface.createFromAsset(assetsMgr, "VWTextCarUI-Regular.ttf");
                break;
            case "vw2":
                typeface = Typeface.createFromAsset(assetsMgr, "VWThesis_MIB_Regular.ttf");
                break;
            case "frutiger":
                typeface = Typeface.createFromAsset(assetsMgr, "Frutiger.otf");
                break;
            case "vw3":
                typeface = Typeface.createFromAsset(assetsMgr, "VW_Digit_Reg.otf");
                break;
            case "skoda":
                typeface = Typeface.createFromAsset(assetsMgr, "Skoda.ttf");
                break;
            case "larabie":
                typeface = Typeface.createFromAsset(assetsMgr, "Larabie.ttf");
                break;
            case "ford":
                typeface = Typeface.createFromAsset(assetsMgr, "UnitedSans.otf");
                break;
        }
        return typeface;
    }

    //update the elements
    private void updateElement(String queryElement, TextView value, TextView label) {
        long queryPid;
        if (queryElement != null) {
            switch (queryElement) {
                case "none":
                    value.setText("");
                    break;
                case "test":
                    value.setText(String.format(Locale.US, FORMAT_DECIMALS, randFloat(0, 100)));
                    break;
                // the following are torque PIDs.
                case "torque-fuelpressure_0x0a":
                case "torque-engineload_0x04":
                case "torque-timing_advance_0x0e":
                case "torque-intake_air_temperature_0x0f":
                case "torque-mass_air_flow_0x10":
                case "torque-throttle_position_0x11":
                case "torque-voltage_0xff1238":
                case "torque-AFR_0xff1249":
                case "torque-AFRc_0xff124d":
                case "torque-fueltrimshortterm1_0x06":
                case "torque-fueltrimlongterm1_0x07":
                case "torque-fueltrimshortterm2_0x08":
                case "torque-fueltrimlongterm2_0x09":
                case "torque-accelerometer_total_0xff1223":
                case "torque-fuelrailpressure_0x23":
                case "torque-exhaustgastempbank1sensor1_0x78":
                case "torque-exhaustgastempbank1sensor2_0xff1282":
                case "torque-exhaustgastempbank1sensor3_0xff1283":
                case "torque-exhaustgastempbank1sensor4_0xff1284":
                case "torque-absolutethrottlepostion_0x47":
                case "torque-ambientairtemp_0x46":
                case "torque-catalysttemperature_0x3c":
                case "torque-chargeaircoolertemperature_0x77":
                case "torque-commandedequivalenceratiolambda_0x44":
                case "torque-enginecoolanttemp_0x05":
                case "torque-engineloadabsolute_0x43":
                case "torque-fuellevel_0x2f":
                case "torque-intakemanifoldpressure_0x0b":
                case "torque-o2sensor1equivalenceratio_0x34":
                case "torque-obdadaptervoltage_0xff1238":
                case "torque-oiltemperature_0x5c":
                case "torque-phonebarometer_0xff1270":
                case "torque-phonebatterylevel_0xff129a":
                case "torque-pressurecontrol_0x70":
                case "torque-relativethrottleposition_0x45":
                case "torque-transmissiontemp_0x0105":
                case "torque-transmissiontemp_0xfe1805":
                case "torque-voltagemodule_0x42":

// TODO: this seems useless, becuase we check the torqueQuery earlier than this
                    queryElement = queryElement.substring(queryElement.lastIndexOf('_') + 1);
                    queryElement = queryElement.substring(2);
                    queryPid = new BigInteger(queryElement, 16).longValue();
                    float torqueData;

                    try {
                        if (torqueService != null) {
                            torqueData = torqueService.getValueForPid(queryPid, true);
                            String unitText = torqueService.getUnitForPid(queryPid);
                            value.setText(String.format(Locale.US, FORMAT_DECIMALS_WITH_UNIT, torqueData, unitText));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                    }
                    break;
                // the following torque values should have the unit as label
                case "torque-turboboost_0xff1202":
                    queryElement = queryElement.substring(queryElement.lastIndexOf('_') + 1);
                    queryElement = queryElement.substring(2);
                    queryPid = new BigInteger(queryElement, 16).longValue();
                    float torqueData3;

                    try {
                        if (torqueService != null) {
                            torqueData3 = torqueService.getValueForPid(queryPid, true);


                            String unitText = torqueService.getUnitForPid(queryPid);
                            // workaround for Torque displaying the unit for turbo pressure
                            if (unitText.equals("psi") && pressureUnit.equals("bar")) {
                                torqueData3 = torqueData3 / 14.5037738f;
                                unitText = "bar";
                            }
                            value.setText(String.format(Locale.US, FORMAT_DECIMALS_WITH_UNIT, torqueData3,unitText));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                    }
                    break;
                case "torque-rpm_0x0c":
                case "torque-speed_0x0d":
                    queryElement = queryElement.substring(queryElement.lastIndexOf('_') + 1);
                    queryElement = queryElement.substring(2);
                    queryPid = new BigInteger(queryElement, 16).longValue();
                    try {
                        if (torqueService != null) {
                            float torqueData2 = torqueService.getValueForPid(queryPid, true);
                            String unitText = torqueService.getUnitForPid(queryPid);
                            value.setText(String.format(Locale.US, FORMAT_NO_DECIMALS, torqueData2));
                            label.setText(unitText);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error: " + e.getMessage());
                    }
                    break;

                case "batteryVoltage":
                    Float mBatteryVoltage = (Float) mLastMeasurements.get("batteryVoltage");
                    if (mBatteryVoltage != null) {
                        value.setText(String.format(Locale.US, FORMAT_VOLT, mBatteryVoltage));
                    }
                    break;

                // all temperatures can be handled in the same way, the only difference is the queryElement string
                case "coolantTemperature":
                case "oilTemperature":
                case "gearboxOilTemperature":
                    Float mTemperature = (Float) mLastMeasurements.get(queryElement);
                    if (mTemperature != null && mTemperature > 0) {
                        value.setText(String.format(Locale.US, FORMAT_DEGREES, mTemperature));
                        if (mTemperature < 70) {
                            value.setTextColor(Color.RED);
                        } else {
                            value.setTextColor(Color.WHITE);
                        }
                    }
                    break;
                case "outsideTemperature":
                    Float mTemperatureOutside = (Float) mLastMeasurements.get(queryElement);
                    if (mTemperatureOutside != null) {
                        value.setText(String.format(Locale.US, FORMAT_DEGREES, mTemperatureOutside));
                    }
                    break;
                case "vehicleSpeed":
                    Float mVehicleSpeed = (Float) mLastMeasurements.get("vehicleSpeed");
                    String speedUnit = (String) mLastMeasurements.get("vehicleSpeed.unit");
                    if (mVehicleSpeed != null && speedUnit != null) {
                        value.setText(String.format(Locale.US, FORMAT_DECIMALS, mVehicleSpeed));
                        label.setText(speedUnit);
                    }
                    // values that don't need any decimals
                case "engineSpeed":
                case "Nav_Heading":
                case "Nav_Altitude":
                    Float mNoDecimalValue = (Float) mLastMeasurements.get(queryElement);
                    if (mNoDecimalValue != null) {
                        value.setText(String.format(Locale.US, FORMAT_NO_DECIMALS, mNoDecimalValue));
                    }
                    break;

                // Decimal values, without any specific modification:
                case "currentOutputPower":
                    Float mCurrentPowerValue = (Float) mLastMeasurements.get(queryElement);
                    if (mCurrentPowerValue != null) {
                        // if (!powerUnits) {
                        //     // HP
                        //     mCurrentPowerValue *= powerFactor;
                        // }
                        value.setText(String.format(Locale.US, FORMAT_DECIMALS, mCurrentPowerValue));
                        //label.setText(powerUnits?getString(R.string.unit_kw):getString(R.string.unit_hp));
                    }
                    break;
                case "currentTorque":
                    Float mCurrentTorqueValue = (Float) mLastMeasurements.get(queryElement);
                    if (mCurrentTorqueValue != null) {
                        value.setText(String.format(Locale.US, FORMAT_DECIMALS, mCurrentTorqueValue));
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
                    Float mAcceleration = (Float) mLastMeasurements.get(queryElement);
                    if (mAcceleration != null) {
                        value.setText(String.format(Locale.US, FORMAT_GFORCE, mAcceleration));
                    }
                    break;
                case "longitudinalAcceleration":
                    Float mAcceleration2 = (Float) mLastMeasurements.get(queryElement);
                    if (mAcceleration2 != null) {
                        mAcceleration2 = mAcceleration2 / (float) 9.80665;  //conversion from m/s² to G force
                        value.setText(String.format(Locale.US, FORMAT_GFORCE, mAcceleration2));
                    }
                    break;
                case "yawRate":
                    Float mYawRate = (Float) mLastMeasurements.get(queryElement);
                    if (mYawRate != null) {
                        value.setText(String.format(Locale.US, FORMAT_DEGREESPEC, mYawRate));
                    }
                    break;
                case "Sound_Volume":
                    Float mSoundVol = (Float) mLastMeasurements.get(queryElement);
                    if (mSoundVol != null) {
                        value.setText(String.format(Locale.US, FORMAT_NO_DECIMALS, mSoundVol));
                    }
                    break;
                case "acceleratorPosition":
                    Float mAcceleratorPosition = (Float) mLastMeasurements.get("acceleratorPosition");
                    if (mAcceleratorPosition != null) {
                        Float mAccelPosPercent = mAcceleratorPosition * 100;
                        value.setText(String.format(Locale.US, FORMAT_DECIMALS, mAccelPosPercent));
                    }
                    break;
                case "brakePressure":
                    Float mBrakePressure = (Float) mLastMeasurements.get("brakePressure");
                    if (mBrakePressure != null) {
                        value.setText(String.format(Locale.US, FORMAT_DECIMALS, mBrakePressure));
                    }
                    break;
                case "wheelAngle":
                    Float mWheelAngle = (Float) mLastMeasurements.get(queryElement);
                    if (mWheelAngle != null) {
                        value.setText(String.format(Locale.US, FORMAT_DEGREES, mWheelAngle));
                    }
                    break;
                case "powermeter":
                    Float mPowermeter = (Float) mLastMeasurements.get(queryElement);
                    if (mPowermeter != null) {
                        value.setText(String.format(Locale.US, FORMAT_NO_DECIMALS, mPowermeter));
                    }
                    break;

                // eco values
                case "EcoHMI_Score.AvgShort":
                case "EcoHMI_Score.AvgTrip":
                    Float mEcoScore = (Float) mLastMeasurements.get(queryElement);
                    if (mEcoScore != null) {
                        value.setText(String.format(Locale.US, FORMAT_NO_DECIMALS, mEcoScore));
                    }
                    break;
                case "shortTermConsumptionPrimary":
                case "shortTermConsumptionSecondary":
                    Float mshortConsumption = (Float) mLastMeasurements.get(queryElement);
                    if (mshortConsumption != null) {
                        value.setText(String.format(Locale.US,FORMAT_DECIMALS, mshortConsumption));
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
                case "tyrePressures.pressureRearRight":
                case "tyrePressures.pressureRearLeft":
                case "tyrePressures.pressureFrontRight":
                case "tyrePressures.pressureFrontLeft":
                    Float tyrePressure = (Float) mLastMeasurements.get(queryElement);
                    if (tyrePressure != null) {
                        tyrePressure = tyrePressure / 10; // value in bar
                        tyrePressure = tyrePressure * pressureFactor; // convert to psi if needed.
                        value.setText(String.format(Locale.US, FORMAT_DECIMALS_WITH_UNIT, tyrePressure, pressureUnit));
                    }
                    break;
                case "tyreTemperatures.temperatureRearRight":
                case "tyreTemperatures.temperatureRearLeft":
                case "tyreTemperatures.temperatureFrontRight":
                case "tyreTemperatures.temperatureFrontLeft":
                    Float tyreTemp = (Float) mLastMeasurements.get(queryElement);
                    if (tyreTemp != null) {
                        value.setText(String.format(Locale.US, FORMAT_TEMPERATURE, tyreTemp));
                    }
                    break;
                case "tankLevelPrimary":
                case "tankLevelSecondary":
                    Float tankLevel = (Float) mLastMeasurements.get(queryElement);
                    if (tankLevel != null) {
                        value.setText(String.format(Locale.US, FORMAT_PERCENT, tankLevel));
                    }
                    break;
            }
        }
    }

    // set clock label, units, etc.
    private void setupClock(TextView icon, String iconDrawableName, String iconText, Speedometer clock, Boolean backgroundWithWarningArea, String unit, Integer minspeed, Integer maxspeed, String speedFormat, String tickFormat) {

        Log.d(TAG, "icon: " + icon + " iconDrawableName: " + iconDrawableName);
        int resId = getResources().getIdentifier(iconDrawableName, "drawable", getContext().getPackageName());
        Drawable iconDrawable = ContextCompat.getDrawable(getContext(),resId);
        int resIdEmpty = getResources().getIdentifier("ic_none", "drawable", getContext().getPackageName());

        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[]{R.attr.themedEmptyDialBackground});
        int emptyBackgroundResource = typedArray.getResourceId(0, 0);
        typedArray.recycle();

        // set icon. Clocks that don't need an icon have ic_none as icon
        icon.setBackground(iconDrawable);
        icon.setText(iconText);
        clock.setUnit(unit);
        clock.setMinMaxSpeed(minspeed, maxspeed);

        //  if (tickFormat.equals("float")) {
        //      clock.setTickTextFormat(Gauge.FLOAT_FORMAT);
//
        //  } else {
        //      clock.setTickTextFormat(Gauge.INTEGER_FORMAT);
        //  }


        //dynamically scale the icon_space in case there's only an icon, and no text
        if (!iconText.equals("") && resId == resIdEmpty) {
            ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) icon.getLayoutParams();
            params.width = 40;
            icon.setLayoutParams(params);
        }


        // determine if an empty background, without red warning area is wanted
        if (!backgroundWithWarningArea) {
            clock.setBackgroundResource(emptyBackgroundResource);
        }

        //  //determine the clock format
        //  if (speedFormat.equals("float")) {
        //      clock.setSpeedTextFormat(Gauge.FLOAT_FORMAT);
//
        //  } else if (speedFormat.equals("integer")) {
        //      clock.setSpeedTextFormat(Gauge.INTEGER_FORMAT);
        //  }

    }

    private final ServiceConnection torqueConnection = new ServiceConnection() {
        /**
         * What to do when we get connected to Torque.
         *
         * @param arg0
         * @param service
         */
        public void onServiceConnected(ComponentName arg0, IBinder service) {
            torqueService = ITorqueService.Stub.asInterface(service);
        }

        /**
         * What to do when we get disconnected from Torque.
         *
         * @param name
         */
        public void onServiceDisconnected(ComponentName name) {
            torqueService = null;
        }
    };

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

    public Bitmap colorize(Bitmap srcBmp, int dstColor) {

        int width = srcBmp.getWidth();
        int height = srcBmp.getHeight();

        float[] srcHSV = new float[3];
        float[] dstHSV = new float[3];

        Bitmap dstBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int pixel = srcBmp.getPixel(col, row);
                int alpha = Color.alpha(pixel);
                Color.colorToHSV(pixel, srcHSV);
                Color.colorToHSV(dstColor, dstHSV);

                // If it area to be painted set only value of original image
                dstHSV[2] = srcHSV[2];  // value
                dstBitmap.setPixel(col, row, Color.HSVToColor(alpha, dstHSV));
            }
        }

        return dstBitmap;
    }


    // get min/max/units from exlap schema
    private void getExlapDataElementDetails(String query) {
        if (mSchema.containsKey(query)) {
            FieldSchema field = mSchema.get(query);
            float minValue = field.getMin();
            float maxValue = field.getMax();
            String unit = field.getUnit();
        }
    }
}
