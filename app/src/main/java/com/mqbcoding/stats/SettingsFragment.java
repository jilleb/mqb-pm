package com.mqbcoding.stats;

import android.app.AlertDialog;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import androidx.annotation.Nullable;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.github.martoreto.aauto.vex.CarStatsClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingsFragment extends PreferenceFragment {
    private static final String TAG = "PreferenceFragment";

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            Log.d(TAG, "Preference change: " + preference.getKey());

            String stringValue = value == null ? "" : value.toString();

            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);
            } if (preference instanceof TemperaturePreference) {
                return true;
            } else {
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getAll().get(preference.getKey()));
    }

    private SharedPreferences.OnSharedPreferenceChangeListener sharedPrefChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals(SettingsActivity.PREF_LOCATION)) {
                SwitchPreference useGeocoding = (SwitchPreference) findPreference(SettingsActivity.PREF_LOCATION);
                useGeocoding.setChecked(sharedPreferences.getBoolean(SettingsActivity.PREF_LOCATION, false));
            }
        }
    };

    private List<File> findLogs() throws IOException {
        File logDir = CarStatsLogger.getLogsDir();
        List<File> files = new ArrayList<>();
        for (File f: logDir.listFiles()) {
            if (f.getName().endsWith(".log.gz")) {
                files.add(f);
            }
        }
        Collections.sort(files);
        return files;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        addPreferencesFromResource(R.xml.settings);

        findPreference("accountName").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsActivity activity = (SettingsActivity) getActivity();
                activity.chooseAccount();
                return true;
            }
        });

        findPreference("bigqueryReuploadAll").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    List<File> files = findLogs();
                    if (files.isEmpty()) {
                        Toast.makeText(getActivity(), R.string.reupload_last_nothing_to_do,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        for (File f: files) {
                            LogUploadService.schedule(getActivity(), f);
                        }
                        Toast.makeText(getActivity(), R.string.bigquery_reupload_ok,
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Re-upload error", e);
                    Toast.makeText(getActivity(), R.string.bigquery_reupload_all_failed,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });
        findPreference("bigqueryReuploadLast").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                try {
                    List<File> files = findLogs();
                    if (files.isEmpty()) {
                        Toast.makeText(getActivity(), R.string.reupload_last_nothing_to_do,
                                Toast.LENGTH_SHORT).show();
                    } else {
                        LogUploadService.schedule(getActivity(), files.get(files.size() - 1));
                        Toast.makeText(getActivity(), R.string.bigquery_reupload_ok,
                                Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Re-upload error", e);
                    Toast.makeText(getActivity(), R.string.pref_bigquery_reupload_last_failed,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        findPreference("kickUploads").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                JobScheduler scheduler = (JobScheduler) getActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE);
                assert scheduler != null;
                List<JobInfo> jobs = scheduler.getAllPendingJobs();
                if (jobs.isEmpty()) {
                    Toast.makeText(getActivity(), R.string.kick_uploads_nothing_to_do,
                            Toast.LENGTH_SHORT).show();
                } else {
                    for (JobInfo job : jobs) {
                        JobInfo newJob = new JobInfo.Builder(job.getId(), job.getService())
                                .setPersisted(job.isPersisted())
                                .setExtras(job.getExtras())
                                .setRequiredNetworkType(job.getNetworkType())
                                .setOverrideDeadline(1)
                                .build();
                        scheduler.schedule(newJob);
                    }
                    Toast.makeText(getActivity(), R.string.kick_uploads_done,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        });

        findPreference("cancelUploads").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                JobScheduler scheduler = (JobScheduler) getActivity().getSystemService(Context.JOB_SCHEDULER_SERVICE);
                assert scheduler != null;
                scheduler.cancelAll();
                Toast.makeText(getActivity(), R.string.cancel_uploads_done,
                        Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        findPreference(SettingsActivity.PREF_LOCATION).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SettingsActivity activity = (SettingsActivity) getActivity();
                activity.checkLocationPermissions();
                return true;
            }
        });

        findPreference("listProviders").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle(R.string.pref_providers);
                ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(getActivity(),
                        android.R.layout.simple_list_item_1);
                PackageManager pm = getActivity().getPackageManager();
                for (ResolveInfo i: CarStatsClient.getProviderInfos(getActivity())) {
                    adapter.add(i.serviceInfo.loadLabel(pm));
                }
                builder.setAdapter(adapter, null);
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
                return true;
            }
        });
        PreferenceManager.getDefaultSharedPreferences(getActivity()).registerOnSharedPreferenceChangeListener(sharedPrefChangeListener);
    }

    @Override
    public void onResume() {
        super.onResume();

        bindPreferenceSummaryToValue(findPreference("accountName"));
        bindPreferenceSummaryToValue(findPreference("bigqueryProjectId"));
        bindPreferenceSummaryToValue(findPreference("bigqueryDataset"));
        bindPreferenceSummaryToValue(findPreference("bigqueryTable"));
        bindPreferenceSummaryToValue(findPreference("oilTempThreshold"));
        bindPreferenceSummaryToValue(findPreference("fueltanksize"));
        bindPreferenceSummaryToValue(findPreference("performanceTitle1"));
        bindPreferenceSummaryToValue(findPreference("performanceTitle2"));
        bindPreferenceSummaryToValue(findPreference("performanceTitle3"));
        /*
        bindPreferenceSummaryToValue(findPreference("engineSpeedSoundUpToGear"));
        bindPreferenceSummaryToValue(findPreference("engineSpeedESInform"));
        bindPreferenceSummaryToValue(findPreference("engineSpeedESHint"));
        bindPreferenceSummaryToValue(findPreference("engineSpeedESWarn"));
        */
        Preference statsLoggerPref = findPreference(CarStatsLogger.PREF_ENABLED);
        try {
            statsLoggerPref.setSummary(
                    getString(R.string.pref_stats_logging_summary, CarStatsLogger.getLogsDir()));
            statsLoggerPref.setEnabled(true);
        } catch (IOException e) {
            statsLoggerPref.setSummary(
                    getString(R.string.pref_stats_logging_not_available, e.toString()));
            statsLoggerPref.setEnabled(false);
        }
    }
}
