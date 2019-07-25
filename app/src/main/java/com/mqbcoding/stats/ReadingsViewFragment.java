package com.mqbcoding.stats;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.github.martoreto.aauto.vex.CarStatsClient;
import com.github.martoreto.aauto.vex.FieldSchema;
import com.google.android.apps.auto.sdk.StatusBarController;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class ReadingsViewFragment extends CarFragment {
    private final String TAG = "ReadingsViewFragment";
    public static final String PREF_OMIT_EMPTY_ENTRIES = "omitEmptyEntries";
    private CarStatsClient mStatsClient;
    private Map<String, Object> mLastMeasurements = new HashMap<>();
    private HashMap<String, FieldSchema> mSchema = new HashMap<>();
    private Handler mHandler;
    private Runnable updateTimerRunnable;
    private HashMapAdapter adapter = new HashMapAdapter();
    private Timer updateTimer;
    private HashMap<String, String> translationsMap;
    private boolean mOmitEmptyEntries;

    class HashMapAdapter extends BaseAdapter {
        private LinkedHashMap<String, Object> mData = new LinkedHashMap<>();
        private Map<String, FieldSchema> mSchema = null;
        private String[] mKeys;


        HashMapAdapter() {

        }

        void putAll(final Map<String, Object> values) {
            mData.putAll(values);
            mKeys = mData.keySet().toArray(new String[0]);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(mKeys[position]);
        }

        public String getKey(int position) {
            return mKeys[position];
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;
        }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            String key = mKeys[pos];
            Object obj = getItem(pos);
            String value = "";
            if (obj != null) {
                value = getItem(pos).toString();
            }

            String translated = key;
            if (translationsMap != null && translationsMap.containsKey(key))
                translated = translationsMap.get(key);

            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_readings_item, parent, false);
            }
            TextView tvName = convertView.findViewById(R.id.tvName);
            TextView tvValue = convertView.findViewById(R.id.tvValue);
            TextView tvUnit = convertView.findViewById(R.id.tvUnit);
            tvName.setText(translated);
            tvValue.setText(value);
            String unit = "";
            if (mSchema != null) {
                FieldSchema field = mSchema.get(key);
                if (field !=  null) {
                    String receivedUnit = field.getUnit();
                    if (receivedUnit != null && !receivedUnit.isEmpty()) {
                        unit = receivedUnit;
                    }
                }
            }
            tvUnit.setText(unit);
            return convertView;
        }

        public void putSchema(Map<String, FieldSchema> schema) {
            mSchema = schema;
            notifyDataSetChanged();
        }
    }

    public ReadingsViewFragment() {
        // Required empty public constructor
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onActivate");

        Intent serviceIntent = new Intent(getContext(), CarStatsService.class);
        getContext().bindService(serviceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);

        createAndStartUpdateTimer();
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onDeactivate");
        mStatsClient.unregisterListener(mCarStatsListener);
        getContext().unbindService(mServiceConnection);
        updateTimer.cancel();
        super.onPause();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView");
        View rootView = inflater.inflate(R.layout.fragment_readings, container, false);
        mHandler = new Handler(Looper.getMainLooper());
        adapter = new HashMapAdapter();
        final ListView listView = rootView.findViewById(R.id.lvItems);
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                listView.setAdapter(adapter);
                listView.setOnItemClickListener(listClickListener);
            }
        });

        HashMap<String, FieldSchema> schemaItem1 = new HashMap<>();
        HashMap<String, Object> tempMap = new HashMap<>();
        schemaItem1.put("batteryVoltage", new FieldSchema(0,"TestDescr","",0,0,0));
        tempMap.put("batteryVoltage", "DAfadslkfnadofnaskjdsa");
        schemaItem1.put("batteryVoltage2", new FieldSchema(0,"Test","V",0,0,0));
        tempMap.put("batteryVoltage2", "0.00");
        schemaItem1.put("batteryVoltage3", new FieldSchema(0,"Test","V",0,0,0));
        tempMap.put("batteryVoltage3", "0");
        mSchema.putAll(schemaItem1);

        mCarStatsListener.onNewMeasurements("DUPA", new Date(), tempMap);

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.i(TAG, "onAttach");

        this.translationsMap = generateTranslationsMap(context);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.registerOnSharedPreferenceChangeListener(mPreferencesListener);
        readPreferences(sharedPreferences);
    }

    @Override
    protected void setupStatusBar(StatusBarController sc) {

    }

    private void createAndStartUpdateTimer() {
        updateTimer = new Timer();
        updateTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                updateTimerRunnable = new Runnable() {
                    public void run() {
                        adapter.putAll(mLastMeasurements);
                        adapter.putSchema(mSchema);
                    }
                };
                //experimental delay
                if (mHandler != null)
                    mHandler.postDelayed(updateTimerRunnable, 1);
            }

        }, 0, 250);//Update display 0,25 second
    }

    private HashMap<String, String> generateTranslationsMap(Context context) {
        HashMap<String, String> translationsMap = new HashMap<>();

        String[] keys = context.getResources().getStringArray(R.array.TextDataElementsValues);
        String[] translations = context.getResources().getStringArray(R.array.TextDataElementsEntries);

        for(int i=0; i < keys.length; i++) {
            translationsMap.put(keys[i], translations[i]);
        }
        return translationsMap;
    }

    private final SharedPreferences.OnSharedPreferenceChangeListener mPreferencesListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
            readPreferences(sharedPreferences);
        }
    };

    private void readPreferences(SharedPreferences preferences) {
        mOmitEmptyEntries = preferences.getBoolean(PREF_OMIT_EMPTY_ENTRIES, true);
    }

    private final CarStatsClient.Listener mCarStatsListener = new CarStatsClient.Listener() {
        @Override
        public void onNewMeasurements(String provider, Date timestamp, final Map<String, Object> values) {
            if (mOmitEmptyEntries) {
                for (Map.Entry<String, Object> item : values.entrySet()) {
                    if (item.getValue() != null && !item.getValue().toString().isEmpty()) {
                        mLastMeasurements.put(item.getKey(), item.getValue());
                    }
                }
            } else {
                mLastMeasurements.putAll(values);
            }
        }

        @Override
        public void onSchemaChanged() {
            mSchema.putAll(mStatsClient.getSchema());
        }
    };


    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.i(TAG, "ServiceConnected");
            CarStatsService.CarStatsBinder carStatsBinder = (CarStatsService.CarStatsBinder) iBinder;
            mStatsClient = carStatsBinder.getStatsClient();
            mLastMeasurements.putAll(mStatsClient.getMergedMeasurements());
            mStatsClient.registerListener(mCarStatsListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.i(TAG, "ServiceDisconnected");
            mStatsClient.unregisterListener(mCarStatsListener);
        }
    };

    private AdapterView.OnItemClickListener listClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (!mSchema.isEmpty()) {
                Bundle bundle = new Bundle();
                bundle.putSerializable(ExlapItemDetailsFragment.ARG_SCHEMA, mSchema);
                String itemKey = adapter.getKey(position);
                bundle.putString(ExlapItemDetailsFragment.ARG_SELECTED_KEY, itemKey);

                ExlapItemDetailsFragment detailsFragment = new ExlapItemDetailsFragment();
                detailsFragment.setArguments(bundle);

                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, detailsFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        }
    };
}