package com.mqbcoding.stats;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.SparseArray;

import com.mqbcoding.stats.BuildConfig;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.bigquery.Bigquery;
import com.google.api.services.bigquery.model.Job;
import com.google.api.services.bigquery.model.JobConfiguration;
import com.google.api.services.bigquery.model.JobConfigurationLoad;
import com.google.api.services.bigquery.model.TableFieldSchema;
import com.google.api.services.bigquery.model.TableReference;
import com.google.api.services.bigquery.model.TableSchema;
import com.google.common.collect.ImmutableList;
import com.google.common.net.MediaType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;

public class LogUploadService extends JobService {
    private static final String TAG = "LogUploadService";

    public static final String EXTRA_FILENAME = "filename";

    public static final String PREF_BIGQUERY_ENABLED = "uploadToBigquery";
    public static final String PREF_BIGQUERY_PROJECT_ID = "bigqueryProjectId";
    public static final String PREF_BIGQUERY_DATASET = "bigqueryDataset";
    public static final String PREF_BIGQUERY_TABLE = "bigqueryTable";

    private static final DateFormat TABLE_SUFFIX_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.US);

    private final HttpTransport mHttpTransport = new NetHttpTransport();
    private final JsonFactory mJsonFactory = GsonFactory.getDefaultInstance();

    private SparseArray<LogUploadTask> mTasks = new SparseArray<>();

    public static void schedule(Context context, File logFile) {
        JobScheduler scheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        assert scheduler != null;
        PersistableBundle extras = new PersistableBundle();
        extras.putString(EXTRA_FILENAME, logFile.getAbsolutePath());
        ComponentName jobService =
                new ComponentName(BuildConfig.APPLICATION_ID, LogUploadService.class.getName());
        int jobId = (int) (System.currentTimeMillis() / 1000);
        JobInfo jobInfo = new JobInfo.Builder(jobId, jobService)
                .setExtras(extras)
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .build();
        if (scheduler.schedule(jobInfo) != JobScheduler.RESULT_SUCCESS) {
            Log.e(TAG, logFile + ": failed to schedule upload");
        }
    }

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        LogUploadTask task = new LogUploadTask(jobParameters);
        mTasks.put(jobParameters.getJobId(), task);
        task.execute();
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        LogUploadTask task = mTasks.get(jobParameters.getJobId());
        if (task != null) {
            task.cancel(true);
            mTasks.remove(jobParameters.getJobId());
        }
        return true;
    }

    private class LogUploadTask extends AsyncTask<Void, Void, Boolean> {
        private final JobParameters mJobParameters;
        private final File mLogFile;
        private final Bundle mAnalyticsBundle;

        @SuppressWarnings("ConstantConditions")
        public LogUploadTask(JobParameters jobParameters) {
            this.mJobParameters = jobParameters;
            this.mLogFile = new File(jobParameters.getExtras().getString(EXTRA_FILENAME));
            this.mAnalyticsBundle = new Bundle();
            this.mAnalyticsBundle.putString("filename", mLogFile.getName());
        }

        @Override
        protected Boolean doInBackground(Void... args) {
            if (!mLogFile.exists()) {
                Log.d(TAG, mLogFile + ": does not exist anymore");
                return true;
            }
            if (mLogFile.length() == 0) {
                Log.d(TAG, mLogFile + ": is empty");
                return true;
            }

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(LogUploadService.this);
            if (!preferences.getBoolean(PREF_BIGQUERY_ENABLED, false)) {
                Log.d(TAG, mLogFile + ": upload disabled");
                return true;
            }
            if (!preferences.contains(PREF_BIGQUERY_PROJECT_ID) || !preferences.contains(PREF_BIGQUERY_DATASET)) {
                Log.d(TAG, mLogFile + ": missing preferences");
                return true;
            }

            try {
                Log.d(TAG, mLogFile + ": starting upload");

                // Detect schema.
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new GZIPInputStream(new FileInputStream(mLogFile)), StandardCharsets.UTF_8));
                String line;
                JsonParser jsonParser = new JsonParser();
                Set<String> fieldNames = new HashSet<>();
                List<TableFieldSchema> fields = new ArrayList<>();
                while ((line = reader.readLine()) != null) {
                    JsonObject record = jsonParser.parse(line).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> entry: record.entrySet()) {
                        String key = entry.getKey();
                        JsonPrimitive value = entry.getValue().getAsJsonPrimitive();
                        if (value == null) {
                            continue;
                        }
                        if (fieldNames.contains(key)) {
                            continue;
                        }
                        TableFieldSchema fieldSchema = new TableFieldSchema();
                        fieldSchema.setName(key);
                        if (key.toLowerCase().contains("timestamp")) {
                            fieldSchema.setType("TIMESTAMP");
                        } else if (value.isBoolean()) {
                            fieldSchema.setType("BOOLEAN");
                        } else if (value.isNumber()) {
                            fieldSchema.setType("FLOAT");
                        } else {
                            fieldSchema.setType("STRING");
                        }
                        fieldNames.add(key);
                        fields.add(fieldSchema);
                    }
                }
                TableSchema schema = new TableSchema();
                schema.setFields(fields);

                App app = (App) getApplication();
                Bigquery bigquery =
                        new Bigquery.Builder(mHttpTransport, mJsonFactory, app.getGoogleCredential())
                                .setApplicationName("MQB Stats").build();
                String projectId = preferences.getString(PREF_BIGQUERY_PROJECT_ID, "");
                String datasetId = preferences.getString(PREF_BIGQUERY_DATASET, "");
                String tablePrefix = preferences.getString(PREF_BIGQUERY_TABLE, "mqbstats");
                String dateSuffix = TABLE_SUFFIX_DATE_FORMAT.format(new Date(mJobParameters.getJobId() * 1000L));
                String tableId = tablePrefix + "$" + dateSuffix;
                FileContent content = new FileContent(MediaType.JSON_UTF_8.toString(), mLogFile);
                Job job = new Job().setConfiguration(
                        new JobConfiguration().setLoad(
                                new JobConfigurationLoad()
                                        .setSourceFormat("NEWLINE_DELIMITED_JSON")
                                        .setSchema(schema)
                                        .setWriteDisposition("WRITE_APPEND")
                                        .setSchemaUpdateOptions(ImmutableList.of(
                                                "ALLOW_FIELD_ADDITION", "ALLOW_FIELD_RELAXATION"))
                                        .setDestinationTable(
                                                new TableReference()
                                                    .setProjectId(projectId)
                                                    .setDatasetId(datasetId)
                                                    .setTableId(tableId))));
                Job result = bigquery.jobs().insert(projectId, job, content).execute();
                Log.d(TAG, mLogFile + ": job id is " + result.getId());

                return true;
            } catch (UserRecoverableAuthIOException e) {
                Log.w(TAG, mLogFile + ": authorization needed", e);
                mAnalyticsBundle.putString("exception", e.getClass().getSimpleName());
                Intent intent = new Intent(LogUploadService.this, SettingsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra(SettingsActivity.EXTRA_AUTHORIZATION_INTENT, e.getIntent());
                startActivity(intent);
                return false;
            } catch (Exception e) {
                Log.w(TAG, mLogFile + ": upload failed", e);
                mAnalyticsBundle.putString("exception", e.getClass().getSimpleName());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            mTasks.remove(mJobParameters.getJobId());
            jobFinished(mJobParameters, !success);
        }
    }
}
