package com.mqbcoding.stats;

import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.View;

import com.google.android.apps.auto.sdk.CarActivity;
import com.google.android.apps.auto.sdk.CarUiController;
import com.google.android.apps.auto.sdk.DayNightStyle;
import com.google.android.apps.auto.sdk.MenuController;
import com.google.android.apps.auto.sdk.MenuItem;
import com.google.android.apps.auto.sdk.StatusBarController;

public class MainCarActivity extends CarActivity {
    private static final String TAG = "MainCarActivity";

    //menu stuff//

    static final String MENU_DASHBOARD1 = "dashboard1";  // home
    static final String MENU_DASHBOARD2 = "dashboard2";

    static final String MENU_READINGS = "readings";
    static final String MENU_CREDITS = "credits";
    static final String MENU_STOPWATCH = "stopwatch";
    static final String MENU_RESTART = "restart";


    // static final String MENU_DEBUG_LOG = "log";
    // static final String MENU_DEBUG_TEST_NOTIFICATION = "test_notification";

    private static final String FRAGMENT_CAR_1 = "dashboard1";
    private static final String FRAGMENT_CAR_2 = "dashboard2";
    private static final String FRAGMENT_READINGS = "readings";
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
                case MENU_DASHBOARD1:
                    switchToFragment(FRAGMENT_CAR_1);
                    break;
                case MENU_DASHBOARD2:
                    switchToFragment(FRAGMENT_CAR_2);
                    break;
                case MENU_READINGS:
                    switchToFragment(FRAGMENT_READINGS);
                    break;
                case MENU_STOPWATCH:
                    switchToFragment(FRAGMENT_STOPWATCH);
                    break;
                case MENU_RESTART:
                    //apply settings
                    Runtime.getRuntime().exit(0);
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
        String selectedBackground = sharedPreferences.getString("selectedBackground", "Black");

        boolean d2Active = sharedPreferences.getBoolean("d2_active", false);

        setLocalTheme(selectedTheme);

        // get user setting for mic on/of
        micOn = sharedPreferences.getBoolean("micActive", true);

        setContentView(R.layout.activity_car_main);

        // todo: make background user selectable:
        View container = findViewById(R.id.fragment_container);
        container.setBackgroundResource(R.drawable.background_incar_black);

        int resId = getResources().getIdentifier(selectedBackground, "drawable", this.getPackageName());
        if (resId != 0) {
            Drawable wallpaperImage = getResources().getDrawable(resId);
            container.setBackground(wallpaperImage);
        }

        CarUiController carUiController = getCarUiController();
        carUiController.getStatusBarController().showTitle();
        //force night mode
        carUiController.getStatusBarController().setDayNightStyle(DayNightStyle.FORCE_NIGHT);

        // Show or hide Android Auto icons in the header
        carUiController.getStatusBarController().hideConnectivityLevel();
        carUiController.getStatusBarController().hideBatteryLevel();
        carUiController.getStatusBarController().hideClock();
        carUiController.getStatusBarController().hideMicButton();
        carUiController.getStatusBarController().hideAppHeader();

        //microphone
        if (micOn) {
            carUiController.getStatusBarController().showMicButton();
        }

        FragmentManager fragmentManager = getSupportFragmentManager();

        //set fragments:
        CarFragment carfragment1 = new DashboardFragment(1);


        CarFragment carfragment2 = null;
        if (d2Active)
            carfragment2 = new DashboardFragment(2);

        ReadingsViewFragment readingsViewFragment = new ReadingsViewFragment();

        StopwatchFragment stopwatchfragment = new StopwatchFragment();
        CreditsFragment creditsfragment = new CreditsFragment();
        FragmentTransaction transaction = fragmentManager.beginTransaction()
                .add(R.id.fragment_container, carfragment1, FRAGMENT_CAR_1)
                .detach(carfragment1);

        if (carfragment2!=null) {
            transaction
                    .add(R.id.fragment_container, carfragment2, FRAGMENT_CAR_2)
                    .detach(carfragment2);
        }

        transaction
                .add(R.id.fragment_container, readingsViewFragment, FRAGMENT_READINGS)
                .detach(readingsViewFragment)
                .add(R.id.fragment_container, stopwatchfragment, FRAGMENT_STOPWATCH)
                .detach(stopwatchfragment)
                .add(R.id.fragment_container, creditsfragment, FRAGMENT_CREDITS)
                .detach(creditsfragment)
                .commitNow();


        String initialFragmentTag = FRAGMENT_CAR_1;
        if (bundle != null && bundle.containsKey(CURRENT_FRAGMENT_KEY)) {
            initialFragmentTag = bundle.getString(CURRENT_FRAGMENT_KEY);
        }
        switchToFragment(initialFragmentTag);

        ListMenuAdapter mainMenu = new ListMenuAdapter();
        mainMenu.setCallbacks(mMenuCallbacks);

        //set menu
        mainMenu.addMenuItem(MENU_DASHBOARD1, new MenuItem.Builder()
                .setTitle(getString(R.string.activity_main_title))
                .setType(MenuItem.Type.ITEM)
                .build());

        if (d2Active) {
            mainMenu.addMenuItem(MENU_DASHBOARD2, new MenuItem.Builder()
                    .setTitle(getString(R.string.activity_main_title) + " 2")
                    .setType(MenuItem.Type.ITEM)
                    .build());
        }
        
        mainMenu.addMenuItem(MENU_READINGS, new MenuItem.Builder()
                .setTitle(getString(R.string.activity_readings_title))
                .setType(MenuItem.Type.ITEM)
                .build());

        mainMenu.addMenuItem(MENU_STOPWATCH, new MenuItem.Builder()
                .setTitle(getString(R.string.activity_stopwatch_title))
                .setType(MenuItem.Type.ITEM)
                .build());

        mainMenu.addMenuItem(MENU_RESTART, new MenuItem.Builder()
                .setTitle("Restart")
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

        carfragment1.setupStatusBar(statusBarController);
        if (carfragment2!=null)
            carfragment2.setupStatusBar(statusBarController);

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

    private void setLocalTheme(String theme) {

        switch (theme) {
            case "VW GTI":
                setTheme(R.style.AppTheme_VolkswagenGTI);
                break;
            case "VW R/GTE":
                setTheme(R.style.AppTheme_VolkswagenGTE);
                break;
            case "VW":
                setTheme(R.style.AppTheme_Volkswagen);
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
            case "Dark":
                setTheme(R.style.AppTheme_Dark);
                break;
            case "Mustang GT":
                setTheme(R.style.AppTheme_Ford);
                break;

            default:
                // set default theme:
                setTheme(R.style.AppTheme_VolkswagenMIB2);
                break;
        }
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
        if (newFragment!=null) {
            transaction.attach(newFragment);
            transaction.commit();
            mCurrentFragmentTag = tag;
        }
    }

    private void updateStatusBarTitle() {
        CarFragment fragment = (CarFragment) getSupportFragmentManager().findFragmentByTag(mCurrentFragmentTag);
        if(fragment!=null)
            getCarUiController().getStatusBarController().setTitle(fragment.getTitle());
    }
}