package com.mqbcoding.stats;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
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
import com.github.anastr.speedviewlib.util.OnSectionChangeListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MeasurementsFragment extends CarFragment {
    private final String TAG = "MeasurementsFragment";

    TextView textSwTimer;
    TextView textSeconds;
    TextView textTimer;
    TextView textCurrentspeed;
    TextView textDistance;
    TextView textDelta;
    Speedometer mCurrentSpeed;


    Button start, pause, reset, simulate;
    long MillisecondTime, StartTime, TimeBuff,PreviousMilliSecondTime,TimeDifference, UpdateTime = 0L ;
    Handler handler;
    int Seconds, Minutes, MilliSeconds, Laps ;
    long CurrentSpeed, Distance, DistanceDelta;
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



        setTitle("Measurements");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_measurements, container, false);


        textSwTimer= rootView.findViewById(R.id.textSwTimer2);
        textDelta= rootView.findViewById(R.id.textDelta);
        textSeconds = rootView.findViewById(R.id.textSeconds2);
        textTimer = rootView.findViewById(R.id.textTimer2);
        start=rootView.findViewById(R.id.imgbtnStart2);
        reset=rootView.findViewById(R.id.imgbtnReset2);
        pause=rootView.findViewById(R.id.imgbtnPause2);
        pause=rootView.findViewById(R.id.imgbtnPause2);
        simulate=rootView.findViewById(R.id.btnDriver);

        textCurrentspeed=rootView.findViewById(R.id.textCurrentspeed);
        listView=rootView.findViewById(R.id.listRecords);
        textDistance=rootView.findViewById(R.id.textDistance);
        Laps = 1;

        mStopwatch = rootView.findViewById(R.id.dialStopWatch2);
        mCurrentSpeed = rootView.findViewById(R.id.dialSpeed);


        ImageIndicator imageIndicator = new ImageIndicator(getContext(),R.drawable.needle,200,200);
        mStopwatch.setIndicator(imageIndicator);


        handler = new Handler();

        ListElementsArrayList = new ArrayList<String>(Arrays.asList(ListElements));

        adapter = new ArrayAdapter<String>(getContext(),android.R.layout.simple_list_item_1, ListElementsArrayList);

        listView.setAdapter(adapter);



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


                    //Laps = Laps + 1;
                }
            }
        });



        start.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view)
            {
                start.setText("Armed");
                start.setVisibility(View.INVISIBLE);
                pause.setVisibility(View.VISIBLE);
                reset.setVisibility(View.INVISIBLE);


                //    reset.setEnabled(false);
              //  lap.setVisibility(View.VISIBLE);
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                TimeBuff += MillisecondTime;

                handler.removeCallbacks(runnable);

                //reset.setEnabled(true);
                reset.setVisibility(View.VISIBLE);

                pause.setVisibility(View.INVISIBLE);
                start.setVisibility(View.VISIBLE);
            }
        });

        simulate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mCurrentSpeed.speedTo(400,60000);
              //  mCurrentSpeed.slowDown();
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
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
                mCurrentSpeed.speedTo(0,1000);
                textSwTimer.setText("00:00");
                textSeconds.setText("00");
                mStopwatch.speedTo(0,1000);
                ListElementsArrayList.clear();
                textTimer.setText("00:00:00:000");
                adapter.notifyDataSetChanged();
                start.setText("Ready?");
            }
        });

        /*
        //toevoegen aan listview
        lap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ListElementsArrayList.add(Laps+": "+ ((textTimer.getText().toString())));
                Laps = Laps + 1;
                adapter.notifyDataSetChanged();

            }
        });
*/

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

                textSwTimer.setText((String.format("%02d", Hours)) + ":" + (String.format("%02d", Minutes)));
                textSeconds.setText(String.format("%02d", Seconds));
                textTimer.setText(formatInterval(UpdateTime));
                mStopwatch.speedTo(Seconds);

                CurrentSpeed = mCurrentSpeed.getCurrentIntSpeed();

               /* this is crappy stuff*/
                DistanceDelta = (long) ((CurrentSpeed/3.6));
                Distance = Distance + DistanceDelta;
                textDistance.setText((String.format("%05d",Distance)));
                textDelta.setText((String.format("%05d",DistanceDelta)));
                DistanceDelta = 0;



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