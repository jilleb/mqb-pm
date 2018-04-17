package com.mqbcoding.stats;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MeasurementsFragment extends CarFragment {
    private String selectedFont;
    private final String TAG = "MeasurementsFragment";

    TextView textMeasTimer;
    TextView textSeconds;
    TextView textTimer;


    Button btnStart, btnPause, btnReset;
    long MillisecondTime, StartTime, TimeBuff, UpdateTime = 0L ;
    Handler handler;
    int Seconds, Minutes, MilliSeconds, Laps ;
    long Hours;
    ListView listView ;
    String[] ListElements = new String[] {  };
    private Speedometer mStopwatch;


    List<String> ListElementsArrayList ;

    ArrayAdapter<String> adapter ;

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
        }


        textMeasTimer= rootView.findViewById(R.id.textMeasTimer);
        textSeconds = rootView.findViewById(R.id.textMeasSeconds);
        textTimer = rootView.findViewById(R.id.textMeasTimerLong);
        btnStart=rootView.findViewById(R.id.imgbtnMeasStart);
        btnReset=rootView.findViewById(R.id.imgbtnMeasReset);
        btnPause=rootView.findViewById(R.id.imgbtnMeasPause);
        listView=rootView.findViewById(R.id.listRecords);
        mStopwatch = rootView.findViewById(R.id.dialMeasStopWatch);

        textMeasTimer.setTypeface(typeface);
        textSeconds.setTypeface(typeface);
        textTimer.setTypeface(typeface);




        TypedArray typedArray = getContext().getTheme().obtainStyledAttributes(new int[] { R.attr.themedNeedle });
        int resourceId = typedArray.getResourceId(0, 0);
        typedArray.recycle();



        // build ImageIndicator using the resourceId
        ImageIndicator imageIndicator = new ImageIndicator(getContext(), resourceId, 200,200);

        mStopwatch.setIndicator(imageIndicator);
        mStopwatch.speedPercentTo(100,5000);

        handler = new Handler();

        ListElementsArrayList = new ArrayList<String>(Arrays.asList(ListElements));

        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1, ListElementsArrayList);

        listView.setAdapter(adapter);

        // here should be something "if speed > 0, start timer"



    /*
        mCurrentSpeed.setOnSectionChangeListener(new OnSectionChangeListener() {
            @Override
            public void onSectionChangeListener(byte oldSection, byte newSection) {
                if (oldSection==Speedometer.LOW_SECTION && newSection == Speedometer.MEDIUM_SECTION && start.getText()=="Armed"){
                    ListElementsArrayList.add("start!/h @ "+ ((textTimer.getText().toString())));
                    adapter.notifyDataSetChanged();
                    StartTime = SystemClock.uptimeMillis();
                    PreviousMilliSecondTime = 0;
                    DistanceDelta = 0;
                    Distance = 0;
                    handler.postDelayed(runnable,0);
*/








                    btnStart.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {

                btnStart.setText("Armed");
                btnStart.setVisibility(View.INVISIBLE);
                btnPause.setVisibility(View.VISIBLE);
                btnReset.setVisibility(View.INVISIBLE);
                StartTime = SystemClock.uptimeMillis();




                // when speed > 0, start clock

                handler.postDelayed(runnable,0);

                //    reset.setEnabled(false);

            }
        });

        btnPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TimeBuff += MillisecondTime;

                handler.removeCallbacks(runnable);

                //reset.setEnabled(true);
                btnReset.setVisibility(View.VISIBLE);

                btnPause.setVisibility(View.INVISIBLE);
                btnStart.setVisibility(View.VISIBLE);
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

                textMeasTimer.setText("00:00");
                textSeconds.setText("00");
                mStopwatch.speedTo(0,1000);
                ListElementsArrayList.clear();
                textTimer.setText("00:00:00:000");

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

            MillisecondTime = SystemClock.uptimeMillis() - StartTime;

            UpdateTime = TimeBuff + MillisecondTime;

            Seconds = (int) (UpdateTime / 1000);

            Minutes = Seconds / 60;

            Hours = TimeUnit.MILLISECONDS.toHours(UpdateTime);

            Seconds = Seconds % 60;

            MilliSeconds = (int) (UpdateTime % 1000);

            textMeasTimer.setText((String.format("%02d", Hours)) +":" +(String.format("%02d", Minutes)));
            textSeconds.setText(String.format("%02d", Seconds));
            textTimer.setText(formatInterval(UpdateTime));
            mStopwatch.speedTo(Seconds);




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
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }


}