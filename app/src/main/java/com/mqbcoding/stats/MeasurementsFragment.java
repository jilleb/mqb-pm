package com.mqbcoding.stats;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.github.anastr.speedviewlib.Speedometer;
import com.github.anastr.speedviewlib.components.Indicators.ImageIndicator;
import com.github.martoreto.aauto.vex.CarStatsClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MeasurementsFragment extends CarFragment {
    private String selectedFont;
    private final String TAG = "MeasurementsFragment";
    private Float carSpeed, carGforce;
    private TextView textMeasTimer, textSeconds, textTimer;
    private TextView textSpeed;
    private boolean boolHundred, boolThirty;



    Button btnStart, btnReset;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    Handler handler;
    int Seconds, Minutes, MilliSeconds, Laps ;
    long Hours;
    ListView listView ;
    String[] ListElements = new String[] {  };
    private CarStatsClient mStatsClient;
    private Speedometer mStopwatch;
    private final CarStatsClient.Listener mCarStatsListener;
    private Map<String, Object> mLastMeasurements = new HashMap<>();

    List<String> ListElementsArrayList ;

    ArrayAdapter<String> adapter ;


    {
        mCarStatsListener = new CarStatsClient.Listener() {
            @Override
            public void onNewMeasurements(String provider, Date timestamp, Map<String, Object> values) {
                mLastMeasurements.putAll(values);
                //postUpdate();
            }
        };
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



    public MeasurementsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");

        setTitle(getContext().getString(R.string.activity_measurements_title));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_measurements, container, false);

//Get shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        selectedFont = sharedPreferences.getString("selectedFont", "segments");


        //set textview to have a custom digital font:
        Typeface typeface = Typeface.createFromAsset(getContext().getAssets(), "digital.ttf");
        switch(selectedFont){
            case "segments":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "digital.ttf");
                break;
            case "seat":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "SEAT_MetaStyle_MonoDigit_Regular.ttf");
                break;
            case "seat2":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "seatKombi_normal.ttf");
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
            case "porsche":
                typeface = Typeface.createFromAsset(getContext().getAssets(), "911porschav3cond.ttf");
                break;
        }


        textMeasTimer= rootView.findViewById(R.id.textMeasTimer);
        textSeconds = rootView.findViewById(R.id.textMeasSeconds);
        textTimer = rootView.findViewById(R.id.textMeasTimerLong);
        textSpeed = rootView.findViewById(R.id.textMeasSpeed);
        btnStart=rootView.findViewById(R.id.imgbtnMeasStart);
        btnReset=rootView.findViewById(R.id.imgbtnMeasReset);
        listView=rootView.findViewById(R.id.listRecords);
        mStopwatch = rootView.findViewById(R.id.dialMeasStopWatch);

        carSpeed = (Float) mLastMeasurements.get("vehicleSpeed");

        if (textSpeed != null)
        {
                if (carSpeed != null)
                {
                textSpeed.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), carSpeed));
                }


        textMeasTimer.setTypeface(typeface);
        textSeconds.setTypeface(typeface);
        textTimer.setTypeface(typeface);


        // build ImageIndicator using the resourceId
        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[] { R.attr.themedNeedle });
        int resourceId = typedArray.getResourceId(0, 0);
        typedArray.recycle();

        ImageIndicator imageIndicator = new ImageIndicator(getContext(), resourceId, 200,200);
        mStopwatch.setIndicator(imageIndicator);

        handler = new Handler();

        // make the list
        ListElementsArrayList = new ArrayList<String>(Arrays.asList(ListElements));
        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1, ListElementsArrayList);
        listView.setAdapter(adapter);
        }



    btnStart.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {

                btnStart.setText("Armed");
                //btnStart.setVisibility(View.INVISIBLE);
                boolHundred = false;
                boolThirty = false;

                //btnReset.setVisibility(View.INVISIBLE);
                StartTime = SystemClock.uptimeMillis();

                // when speed > 0, start clock
                // maybe it's best to use the onspeedchanged listener from Speedview for this.
                // for the time being it will just start
                // when it starts to run, change "armed" to "running"

                handler.postDelayed(runnable,0);

                //    reset.setEnabled(false);

            }
        });


        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                MillisecondTime = 0L ;
                StartTime = 0L ;
                TimeBuff = 0L ;
                UpdateTime = 0L ;
                Seconds = 0 ;
                Minutes = 0 ;
                Hours = 0 ;
                MilliSeconds = 0 ;
                Laps = 1;
                boolThirty = false;
                boolHundred = false;

                textMeasTimer.setText("00:00");
                textSeconds.setText("00");
                mStopwatch.speedTo(0,1000);
                ListElementsArrayList.clear();
                textTimer.setText("00:00:00:000");
                handler.removeCallbacks(runnable);


                adapter.notifyDataSetChanged();
            }
        });





        return rootView;
    }

    private static String formatInterval(final long millis) {
        final long hr = TimeUnit.MILLISECONDS.toHours(millis);
        final long min = TimeUnit.MILLISECONDS.toMinutes(millis - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(millis - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(millis - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
    }

    public Runnable runnable = new Runnable() {

        public void run() {

            carSpeed = (Float) mLastMeasurements.get("vehicleSpeed");
            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;
            Seconds = (int) (UpdateTime / 1000);
            Minutes = Seconds / 60;
            Hours = TimeUnit.MILLISECONDS.toHours(UpdateTime);
            Seconds = Seconds % 60;
            MilliSeconds = (int) (UpdateTime % 1000);
            mStopwatch.speedTo(Seconds);

            textMeasTimer.setText((String.format("%02d", Hours)) +":" +(String.format("%02d", Minutes)));
            textSeconds.setText(String.format("%02d", Seconds));
            textTimer.setText(formatInterval(UpdateTime));
            if (carSpeed != null){
                textSpeed.setText(String.format(Locale.US, getContext().getText(R.string.decimals).toString(), carSpeed));
                if ((carSpeed > 10 ) && (boolThirty = false) ){
                    ListElementsArrayList.add("0 - 10km/h @ " + ((textTimer.getText().toString())));
                    adapter.notifyDataSetChanged();
                    boolThirty = true;
                } else if ((carSpeed > 100 ) && (boolHundred = false) ){
                    ListElementsArrayList.add("0 - 100km/h @ " + ((textTimer.getText().toString())));
                    adapter.notifyDataSetChanged();
                    boolHundred = true;
                }

            }

            /* OLD CODE
            // this listens for a change to medium section, which is 100km/h
            mCurrentSpeed.setOnSectionChangeListener(new OnSectionChangeListener() {
                                                         @Override
                                                         public void onSectionChangeListener(byte oldSection, byte newSection) {
                                                             if (oldSection == Speedometer.MEDIUM_SECTION && newSection == Speedometer.HIGH_SECTION) {
                                                                 ListElementsArrayList.add("0 - 100km/h @ " + ((textTimer.getText().toString())));
                                                                 adapter.notifyDataSetChanged();
                                                                 //Laps = Laps + 1;
                                                             }
                                                         }
                                                     }
            );
                PreviousMilliSecondTime = MillisecondTime;
                handler.postDelayed(this, 1);

                }

             */

       handler.postDelayed(this, 0);
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


}