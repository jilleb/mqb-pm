package com.mqbcoding.stats;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;

import android.widget.Toast;

import com.github.martoreto.aauto.vex.CarStatsClient;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    private static final String TAG = "SettingsActivity";

    private static final int REQUEST_PERMISSIONS = 0;
    private static final int REQUEST_ACCOUNTS_PERMISSION = 1;
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 2;
    private static final int REQUEST_AUTHORIZATION = 3;
    private static final int REQUEST_ACCOUNT_PICKER = 4;
    private static final int REQUEST_LOCATION_PERMISSION = 5;

    static final String EXTRA_AUTHORIZATION_INTENT = "authorizationRequest";

    private static final String PERMISSION_CAR_VENDOR_EXTENSION = "com.google.android.gms.permission.CAR_VENDOR_EXTENSION";
    public static final String PREF_LOCATION = "useGoogleGeocoding";

    private GoogleAccountCredential mCredential;
    private Intent mCurrentAuthorizationIntent;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        App app = (App) getApplication();
        mCredential = app.getGoogleCredential();

        handleIntent();

        showNotificationSerrviceConfirmDialogIfNeeded();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent();
    }

    private void openNotificationAccess() {
          startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }
    private boolean isNotificationServiceEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (String name : names) {
                final ComponentName cn = ComponentName.unflattenFromString(name);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    private void showNotificationSerrviceConfirmDialog() {
        new AlertDialog.Builder(this)
                .setMessage("Please enable notification access in settings")
                .setTitle("Notification Access")
                .setIconAttribute(android.R.attr.alertDialogIcon)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                openNotificationAccess();
                            }
                        })
                .setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // do nothing
                            }
                        })
                .create().show();
    }
    private void showNotificationSerrviceConfirmDialogIfNeeded() {
        if (!isNotificationServiceEnabled()) {
            showNotificationSerrviceConfirmDialog();
        }
    }

    private void handleIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_AUTHORIZATION_INTENT) && mCurrentAuthorizationIntent == null) {
            mCurrentAuthorizationIntent = intent.getParcelableExtra(EXTRA_AUTHORIZATION_INTENT);
            startActivityForResult(mCurrentAuthorizationIntent, REQUEST_AUTHORIZATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        List<String> permissionsToRequest = new ArrayList<>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if (ContextCompat.checkSelfPermission(this, PERMISSION_CAR_VENDOR_EXTENSION)
                != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(PERMISSION_CAR_VENDOR_EXTENSION);
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS);
            return;
        }

        CarStatsClient.requestPermissions(this);

        if (checkGooglePlayServicesAvailable()) {
            haveGooglePlayServices();
        }
        checkLocationPermissions();
    }

    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode) {
        runOnUiThread(new Runnable() {
            public void run() {
                Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(
                        SettingsActivity.this, connectionStatusCode, REQUEST_GOOGLE_PLAY_SERVICES);
                dialog.show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == Activity.RESULT_OK) {
                    haveGooglePlayServices();
                } else {
                    checkGooglePlayServicesAvailable();
                }
                break;

            case REQUEST_AUTHORIZATION:
                mCurrentAuthorizationIntent = null;
                if (resultCode != Activity.RESULT_OK) {
                    chooseAccountIfNeeded();
                }
                break;

            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getExtras().getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(App.PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                    }
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case REQUEST_ACCOUNTS_PERMISSION:
                if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    chooseAccountIfNeeded();
                } else {
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(LogUploadService.PREF_BIGQUERY_ENABLED, false);
                    editor.apply();
                }
                break;
            case REQUEST_LOCATION_PERMISSION:
                if (grantResults.length == 1 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(getApplicationContext(),
                            R.string.location_permission_denied_toast, Toast.LENGTH_LONG).show();
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putBoolean(PREF_LOCATION, false);
                    editor.apply();
                    editor.commit();
                }
                break;
        }
    }

    /** Check that Google Play services APK is installed and up to date. */
    private boolean checkGooglePlayServicesAvailable() {
        GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = availability.isGooglePlayServicesAvailable(this);
        if (availability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        }
        return true;
    }

    private void haveGooglePlayServices() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                switch (s) {
                    case LogUploadService.PREF_BIGQUERY_ENABLED:
                        chooseAccountIfNeeded();
                        break;
                }
            }
        });

        chooseAccountIfNeeded();
    }

    void checkLocationPermissions() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (preferences.getBoolean(PREF_LOCATION, false)) {
            List<String> permissionsToRequest = new ArrayList<>();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(this,
                        permissionsToRequest.toArray(new String[0]),
                        REQUEST_LOCATION_PERMISSION);
            }
        }
    }

    void chooseAccount() {
        if (!checkAccountsPermission()) {
            return;
        }
        startActivityForResult(mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    void chooseAccountIfNeeded() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(LogUploadService.PREF_BIGQUERY_ENABLED, false)) {
            return;
        }
        if (!checkAccountsPermission()) {
            return;
        }
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        }
    }

    private boolean checkAccountsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.GET_ACCOUNTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(SettingsActivity.this,
                    new String[] {Manifest.permission.GET_ACCOUNTS},
                    REQUEST_ACCOUNTS_PERMISSION);
            return false;
        }
        return true;
    }

}
