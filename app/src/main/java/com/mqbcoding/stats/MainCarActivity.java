package com.mqbcoding.stats;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.DayNightStyle;
import com.google.android.apps.auto.sdk.MenuController;
import com.google.android.apps.auto.sdk.MenuItem;
import com.google.android.apps.auto.sdk.StatusBarController;

import java.util.HashSet;

import eu.chainfire.libsuperuser.Shell;

public class MainCarActivity extends CarActivity {
    static final String MENU_HOME = "home";

    //menu stuff//
    static final String MENU_CARDATA = "cardata";
    static final String MENU_CREDITS = "credits";
    static final String MENU_STOPWATCH = "stopwatch";
    static final String MENU_MEASUREMENTS = "measurements";
    static final String MENU_GRAPH = "graph";
    static final String MENU_UNLOCK = "unlock";
    private static final String TAG = "MainCarActivity";


    // static final String MENU_DEBUG_LOG = "log";
    // static final String MENU_DEBUG_TEST_NOTIFICATION = "test_notification";
    private static final String FRAGMENT_CARDATA = "cardata";
    private static final String FRAGMENT_CAR = "dashboard";
    private static final String FRAGMENT_CREDITS = "credits";
    private static final String FRAGMENT_STOPWATCH = "stopwatch";
    private static final String FRAGMENT_MEASUREMENTS = "measurements";
    private static final String FRAGMENT_GRAPH = "graph";
    private static final String CURRENT_FRAGMENT_KEY = "app_current_fragment";

    private static final int TEST_NOTIFICATION_ID = 1;
    private String mCurrentFragmentTag;
    private final ListMenuAdapter.MenuCallbacks mMenuCallbacks = new ListMenuAdapter.MenuCallbacks() {
        @Override
        public void onMenuItemClicked(String name) {
            switch (name) {
                case MENU_HOME:
                    switchToFragment(FRAGMENT_CAR);
                    break;
                case MENU_CARDATA:
                    switchToFragment(FRAGMENT_CARDATA);
                    break;
                case MENU_STOPWATCH:
                    switchToFragment(FRAGMENT_STOPWATCH);
                    break;
                case MENU_MEASUREMENTS:
                    switchToFragment(FRAGMENT_MEASUREMENTS);
                    break;
                case MENU_GRAPH:
                    switchToFragment(FRAGMENT_GRAPH);
                    break;
                case MENU_CREDITS:
                    switchToFragment(FRAGMENT_CREDITS);
                    break;
                case MENU_UNLOCK:
                    unlock();
                    break;
            }
        }

        @Override
        public void onEnter() {
        }

        @Override
        public void onExit() {
            updateStatusBarTitle();
        }
    };
    //end menu stuff//
    private final FragmentManager.FragmentLifecycleCallbacks mFragmentLifecycleCallbacks
            = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentStarted(FragmentManager fm, Fragment f) {
            updateStatusBarTitle();
        }
    };
    private Handler mHandler = new Handler();

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String selectedTheme = sharedPreferences.getString("selectedTheme", "VW GTI");
        Log.d(TAG, "Selected theme: " + selectedTheme);
        setTheme(R.style.AppTheme_VolkswagenGTI);

        switch (selectedTheme) {
            case "VW GTI":
                setTheme(R.style.AppTheme_VolkswagenGTI);
                break;
            case "VW GTE":
                setTheme(R.style.AppTheme_VolkswagenGTE);
                break;
            case "VW":
                setTheme(R.style.AppTheme_Volkswagen);
                break;
            case "VW R":
                setTheme(R.style.AppTheme_VolkswagenR);
                break;
            case "VW MIB2":
                setTheme(R.style.AppTheme_VolkswagenMIB2);
                break;
            case "Seat Cupra":
                setTheme(R.style.AppTheme_SeatCupra);
                break;
            case "Cupra Division":
                setTheme(R.style.AppTheme_Cupra);
                break;
            case "Tesla":
                setTheme(R.style.AppTheme_Tesla);
                break;
            case "Back to the 80s":
                setTheme(R.style.AppTheme_80s);
                break;
            case "Seat":
                setTheme(R.style.AppTheme_Seat);
                break;
            case "MQB Coding":
                setTheme(R.style.AppTheme_SeatMQB);
                break;
            case "Skoda":
                setTheme(R.style.AppTheme_Skoda);
                break;
            case "Skoda ONE":
                setTheme(R.style.AppTheme_SkodaOneApp);
                break;
            case "Skoda vRS":
                setTheme(R.style.AppTheme_SkodavRS);
                break;
            case "Audi":
                setTheme(R.style.AppTheme_Audi);
                break;
            case "Audi RS":
                setTheme(R.style.AppTheme_AudiRS);
                break;
            case "Clubsport":
                setTheme(R.style.AppTheme_Clubsport);
                break;
            case "Outrun":
                setTheme(R.style.AppTheme_Outrun);
                break;

        }
        Log.d(TAG, "Set theme: " + selectedTheme);

        setContentView(R.layout.activity_car_main);


        CarUiController carUiController = getCarUiController();
        carUiController.getStatusBarController().showTitle();
        //force night mode
        carUiController.getStatusBarController().setDayNightStyle(DayNightStyle.FORCE_NIGHT);

        //hide all stuff you don't want to see on your screen
        //      carUiController.getStatusBarController().hideBatteryLevel();
        //     carUiController.getStatusBarController().hideMicButton();
        //    carUiController.getStatusBarController().hideConnectivityLevel();

        FragmentManager fragmentManager = getSupportFragmentManager();

        //set fragments:
        CarFragment carfragment = new DashboardFragment();
        StopwatchFragment stopwatchfragment = new StopwatchFragment();
        MeasurementsFragment measurementsfragment = new MeasurementsFragment();
        CardataFragment cardatafragment = new CardataFragment();
        GraphFragment graphfragment = new GraphFragment();
        CreditsFragment creditsfragment = new CreditsFragment();
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, carfragment, FRAGMENT_CAR)
                .detach(carfragment)
                .add(R.id.fragment_container, stopwatchfragment, FRAGMENT_STOPWATCH)
                .detach(stopwatchfragment)
               .add(R.id.fragment_container, measurementsfragment, FRAGMENT_MEASUREMENTS)
               .detach(measurementsfragment)
                .add(R.id.fragment_container, cardatafragment, FRAGMENT_CARDATA)
                .detach(cardatafragment)
                .add(R.id.fragment_container, graphfragment, FRAGMENT_GRAPH)
                .detach(graphfragment)
                .add(R.id.fragment_container, creditsfragment, FRAGMENT_CREDITS)
                .detach(creditsfragment)
                .commitNow();


        String initialFragmentTag = FRAGMENT_CAR;
        if (bundle != null && bundle.containsKey(CURRENT_FRAGMENT_KEY)) {
            initialFragmentTag = bundle.getString(CURRENT_FRAGMENT_KEY);
        }
        switchToFragment(initialFragmentTag);

        ListMenuAdapter mainMenu = new ListMenuAdapter();
        mainMenu.setCallbacks(mMenuCallbacks);

        //set menu
        mainMenu.addMenuItem(MENU_HOME, new MenuItem.Builder()
                .setTitle(getString(R.string.activity_main_title))
                .setType(MenuItem.Type.ITEM)
                .build());

        mainMenu.addMenuItem(MENU_CARDATA, new MenuItem.Builder()
                .setTitle(getString(R.string.activity_carstatus_title))
                .setType(MenuItem.Type.ITEM)
                .build());

        mainMenu.addMenuItem(MENU_STOPWATCH, new MenuItem.Builder()
                .setTitle(getString(R.string.activity_stopwatch_title))
                .setType(MenuItem.Type.ITEM)
                .build());

        mainMenu.addMenuItem(MENU_GRAPH, new MenuItem.Builder()
                .setTitle(getString(R.string.activity_graph_title))
                .setType(MenuItem.Type.ITEM)
                .build());

        mainMenu.addMenuItem(MENU_MEASUREMENTS, new MenuItem.Builder()
                    .setTitle(getString(R.string.activity_measurements_title))
                    .setType(MenuItem.Type.ITEM)
                    .build());


        mainMenu.addMenuItem(MENU_CREDITS, new MenuItem.Builder()
                .setTitle(getString(R.string.activity_credits_title))
                .setType(MenuItem.Type.ITEM)
                .build());

        mainMenu.addMenuItem(MENU_UNLOCK, new MenuItem.Builder()
                .setTitle("unlock")
                .setType(MenuItem.Type.ITEM)
                .build());


// 1 submenu item
     /*   ListMenuAdapter otherMenu = new ListMenuAdapter();
        otherMenu.setCallbacks(mMenuCallbacks);
        otherMenu.addMenuItem(MENU_DEMO, new MenuItem.Builder()
                .setTitle("Demo")
                .setType(MenuItem.Type.ITEM)
                .build());*/

        //   mainMenu.addSubmenu(MENU_OTHER, otherMenu);

        MenuController menuController = getCarUiController().getMenuController();
        menuController.setRootMenuAdapter(mainMenu);
        menuController.showMenuButton();
        StatusBarController statusBarController = getCarUiController().getStatusBarController();

        getSupportFragmentManager().registerFragmentLifecycleCallbacks(mFragmentLifecycleCallbacks,
                false);

    }

    @Override
    public void onSaveInstanceState(Bundle bundle) {
        bundle.putString(CURRENT_FRAGMENT_KEY, mCurrentFragmentTag);
        super.onSaveInstanceState(bundle);
    }

    @Override
    public void onStart() {
        super.onStart();
        switchToFragment(mCurrentFragmentTag);


    }

    private void switchToFragment(String tag) {
        if (tag.equals(mCurrentFragmentTag)) {
            return;
        }
        FragmentManager manager = getSupportFragmentManager();
        Fragment currentFragment = mCurrentFragmentTag == null ? null : manager.findFragmentByTag(mCurrentFragmentTag);
        Fragment newFragment = manager.findFragmentByTag(tag);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (currentFragment != null) {
            transaction.detach(currentFragment);
        }
        transaction.attach(newFragment);
        transaction.commit();
        mCurrentFragmentTag = tag;
    }

    private void updateStatusBarTitle() {
        CarFragment fragment = (CarFragment) getSupportFragmentManager().findFragmentByTag(mCurrentFragmentTag);
        getCarUiController().getStatusBarController().setTitle(fragment.getTitle());
    }



    private void unlock() {
        if (Shell.SU.available()) {
            Shell.SU.run("pm disable --user 0 com.google.android.gms/.phenotype.service.sync.PhenotypeConfigurator");
            Shell.SU.run("pm disable --user 0 com.google.android.gms/.phenotype.service.PhenotypeService");
            Shell.SU.run("chmod 777 /data/data/com.google.android.gms/databases/phenotype.db*");
            try {
                SQLiteDatabase sql = SQLiteDatabase.openDatabase("/data/data/com.google.android.gms/databases/phenotype.db", null, 0);
                if (sql != null) {
                    Cursor cursor = sql.rawQuery("SELECT stringVal FROM Flags WHERE packageName=? AND name=?;", new String[]{"com.google.android.gms.car", "app_white_list"});
                    HashSet<String> packageNames = new HashSet<>();
                    if (cursor.getCount() > 0) {
                        while (cursor.moveToNext()) {
                            String stringVal = cursor.getString(cursor.getColumnIndex("stringVal"));
                            if (stringVal != null) {
                                packageNames.add(stringVal);
                            }
                        }
                    }
                    cursor.close();
                    packageNames.add(getApplicationContext().getPackageName()); //add myself
                    sql.execSQL("DELETE FROM Flags WHERE packageName=\"com.google.android.gms.car\" AND name=\"app_white_list\";");
                    String joinedPackageNames = TextUtils.join(",", packageNames);
                    sql.execSQL("INSERT INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 209, 0, 0, \"\", \"app_white_list\", \"" + joinedPackageNames + "\", 1);");
                    sql.execSQL("INSERT INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 224, 0, 0, \"\", \"app_white_list\", \"" + joinedPackageNames + "\", 1);");
                    sql.close();
                    Toast.makeText(this, "Successfully unlocked. Reboot phone and connect to Android Auto", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("sql exception : ");
                stringBuilder.append(e.toString());
                Toast.makeText(this, "Error in executing commands : " + stringBuilder.toString(), Toast.LENGTH_LONG).show();
            }
            Shell.SU.run("chmod 660 /data/data/com.google.android.gms/databases/phenotype.db*");
            return;
        }
        Toast.makeText(this, "Root not available, install SuperSU and perform root first.", Toast.LENGTH_LONG).show();
    }

}