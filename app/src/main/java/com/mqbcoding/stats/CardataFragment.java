package com.mqbcoding.stats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.martoreto.aauto.vex.CarStatsClient;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nullable;

public class CardataFragment extends CarFragment {
    private final String TAG = "CardataFragment";

    private CarStatsClient mStatsClient;

    //the various data elements included and their textviews
    private TextView mWheelStateFL;
    private TextView mWheelStateRL;
    private TextView mWheelStateFR;
    private TextView mWheelStateRR;

    private TextView mVIN;
    private TextView mOdometer;


    private int mAnimationDuration;
    private static final float DISABLED_ALPHA = 0.3f;

    private Map<String, Object> mLastMeasurements = new HashMap<>();
    private Handler mHandler = new Handler();


    public CardataFragment() {
        // Required empty public constructor
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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_cardata, container, false);

        //find the various layout elements and give them value

        //todo: sCompountState and styreSystem
        //       sCompoundState
        //        sTyreSystem

        mWheelStateFL = rootView.findViewById(R.id.txtWheelState_FL);
        mWheelStateRL = rootView.findViewById(R.id.txtWheelState_RL);
        mWheelStateFR = rootView.findViewById(R.id.txtWheelState_FR);
        mWheelStateRR = rootView.findViewById(R.id.txtWheelState_RR);

        //  mWarning2ID     = rootView.findViewById(R.id.txtWarning_2_WarnID);
        //  mWarning2Value  = rootView.findViewById(R.id.txtWarning_2_dynamicValue);
        mVIN = rootView.findViewById(R.id.txtVIN);
        mOdometer = rootView.findViewById(R.id.txtOdometer);

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
        Log.i(TAG, "onResume");
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        Log.i(TAG, "onDestroyView");
        // set everything back to null
        mWheelStateFL = null;
        mWheelStateRL = null;
        mWheelStateFR = null;
        mWheelStateRR = null;

        mVIN = null;
        mOdometer = null;

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

    private void postUpdate() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                doUpdate();
            }
        });
    }

    private void doUpdate() {

        // check if the element is there, otherwise return
        if (mVIN == null) {
            return;
        }

        //get strings from the Measurements:
        String sVIN = (String) mLastMeasurements.get("vehicleIdenticationNumber.VIN");

        String sCompoundState = (String) mLastMeasurements.get("tyreStates.compoundState");
        String sTyreSystem = (String) mLastMeasurements.get("tyreStates.system");

        Float sOdometer = (Float) mLastMeasurements.get("totalDistance.distanceValue"); //odometer value
        String sOdometerUnits = (String) mLastMeasurements.get("totalDistance.unit"); //odometer unit (km/miles)

        //check if VIN is known, and if so, display it.
        if (sVIN == null) {
            mVIN.setText("VIN: unknown");

        } else {
            mVIN.setText("VIN: " + sVIN);   //Should be some WVWZZZZbladiebla string
        }

        if (sOdometer == null) {
            mOdometer.setText(R.string.odometerunknown);
        } else if (sOdometerUnits.equals("km")) {
            mOdometer.setVisibility(View.VISIBLE);
            mOdometer.setText(String.format(Locale.US, getContext().getText(R.string.km_format).toString(), sOdometer));
        } else if (sOdometerUnits.equals("m")) {
            mOdometer.setText(String.format(Locale.US, getContext().getText(R.string.m_format).toString(), sOdometer));
        }

        readDataUpdateTextView("tyreStates.stateFrontLeft",mWheelStateFL,"State front left: ", "-");
        readDataUpdateTextView("tyreStates.stateFrontRight",mWheelStateFR,"State front right: ", "-");
        readDataUpdateTextView("tyreStates.stateRearLeft",mWheelStateRL,"State rear left: ", "-");
        readDataUpdateTextView("tyreStates.stateRearRight",mWheelStateRR,"State rear right: ", "-");

    }

    private void readDataUpdateTextView(String queryElement, TextView displayElement, String dataName, String customErrorText) {
        String sData = (String) mLastMeasurements.get("queryElement");

        if (sData == null) {
            String errorText=dataName+customErrorText;
            displayElement.setText(errorText);
            } else {
            String displayText=dataName+sData;
            displayElement.setText(displayText);
                  }
    }
}
    /* todo:

    driveMode_activeProfile

    serviceInspection_distance
serviceInspection_time
serviceInspection_timeState
serviceInspection_distanceState
serviceInspection_distanceUnit
serviceOil_distance
serviceOil_time
serviceOil_timeState
serviceOil_distanceState
serviceOil_distanceUnit

engineTypes_primaryEngine
engineTypes_secondaryEngine

combustionEngineDisplacement (1.4)
engineTypes_secondaryEngine (electric)
engineTypes_primaryEngine (petrol_gasoline)
combustionEngineInjection_type (turbo)
System_HMISkin (HIGH_VW_Skin_SPORT)


     */