package com.mqbcoding.stats;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.View;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.DayNightStyle;
import com.google.android.apps.auto.sdk.MenuController;
import com.google.android.apps.auto.sdk.MenuItem;
import com.google.android.apps.auto.sdk.StatusBarController;

public class MainCarActivity extends CarActivity {
    static final String MENU_HOME = "home";

    //menu stuff//
    static final String MENU_CREDITS = "credits";
    static final String MENU_STOPWATCH = "stopwatch";
    private static final String TAG = "MainCarActivity";



    // static final String MENU_DEBUG_LOG = "log";
    // static final String MENU_DEBUG_TEST_NOTIFICATION = "test_notification";
    private static final String FRAGMENT_CAR = "dashboard";
    private static final String FRAGMENT_CREDITS = "credits";
    private static final String FRAGMENT_STOPWATCH = "stopwatch";
    private static final String CURRENT_FRAGMENT_KEY = "app_current_fragment";

    private static final int TEST_NOTIFICATION_ID = 1;
    private String mCurrentFragmentTag;
    private Boolean connectivityOn, batteryOn, clockOn, micOn;

    private final ListMenuAdapter.MenuCallbacks mMenuCallbacks = new ListMenuAdapter.MenuCallbacks() {
        @Override
        public void onMenuItemClicked(String name) {
            switch (name) {
                case MENU_HOME:
                    switchToFragment(FRAGMENT_CAR);
                    break;
                case MENU_STOPWATCH:
                    switchToFragment(FRAGMENT_STOPWATCH);
                    break;
                case MENU_CREDITS:
                    switchToFragment(FRAGMENT_CREDITS);
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
            case "Beetle":
                setTheme(R.style.AppTheme_Beetle);
                break;
            case "Seat Cupra":
                setTheme(R.style.AppTheme_SeatCupra);
                break;
            case "Cupra Division":
                setTheme(R.style.AppTheme_Cupra);
                break;
            case "Audi TT":
                setTheme(R.style.AppTheme_AudiTT);
                break;
            case "Seat":
                setTheme(R.style.AppTheme_Seat);
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
            case "Skoda Virtual Cockpit":
                setTheme(R.style.AppTheme_SkodaVC);
                break;
            case "Audi":
                setTheme(R.style.AppTheme_Audi);
                break;
            case "Audi Virtual Cockpit":
                setTheme(R.style.AppTheme_AudiVC);
                break;
            case "Clubsport":
                setTheme(R.style.AppTheme_Clubsport);
                break;
            case "Minimalistic":
                setTheme(R.style.AppTheme_Minimalistic);
                break;
            case "Test":
                setTheme(R.style.AppTheme_Testing);
                break;

        }
        Log.d(TAG, "Set theme: " + selectedTheme);

        connectivityOn  = sharedPreferences.getBoolean("connectivityActive",true);
        batteryOn       = sharedPreferences.getBoolean("batterylevelActive", true);
        clockOn         = sharedPreferences.getBoolean("clockActive", true);
        micOn           = sharedPreferences.getBoolean("micActive", true);

        setContentView(R.layout.activity_car_main);
       // getWindow().getDecorView().setBackgroundColor(Color.WHITE);


        // todo: make background user selectable:
       // View someView = findViewById(R.id.fragment_container);

        // Find the root view
        //View root = someView.getRootView();

        // Set the background
        //root.setBackground(someView.getContext().getDrawable(R.drawable.background_incar_outrun));



        CarUiController carUiController = getCarUiController();
        carUiController.getStatusBarController().showTitle();
        //force night mode
        carUiController.getStatusBarController().setDayNightStyle(DayNightStyle.FORCE_NIGHT);

        // Show or hide Android Auto icons in the header
        //signal:
        carUiController.getStatusBarController().hideConnectivityLevel();
        carUiController.getStatusBarController().hideBatteryLevel();
        carUiController.getStatusBarController().hideClock();
        carUiController.getStatusBarController().hideMicButton();
        carUiController.getStatusBarController().hideAppHeader();


        if (connectivityOn) {
            carUiController.getStatusBarController().showConnectivityLevel();
        }
        // battery
        if (batteryOn) {
            carUiController.getStatusBarController().showBatteryLevel();
        }
        // clock
        if (clockOn) {
            carUiController.getStatusBarController().showClock();
        }
        //microphone
        if (micOn) {
            carUiController.getStatusBarController().showMicButton();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();

        //set fragments:
        CarFragment carfragment = new DashboardFragment();
        StopwatchFragment stopwatchfragment = new StopwatchFragment();
        CreditsFragment creditsfragment = new CreditsFragment();
        fragmentManager.beginTransaction()
                .add(R.id.fragment_container, carfragment, FRAGMENT_CAR)
                .detach(carfragment)
                .add(R.id.fragment_container, stopwatchfragment, FRAGMENT_STOPWATCH)
                .detach(stopwatchfragment)
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

        mainMenu.addMenuItem(MENU_STOPWATCH, new MenuItem.Builder()
                .setTitle(getString(R.string.activity_stopwatch_title))
                .setType(MenuItem.Type.ITEM)
                .build());

        mainMenu.addMenuItem(MENU_CREDITS, new MenuItem.Builder()
                .setTitle(getString(R.string.activity_credits_title))
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


}