package com.mqbcoding.stats

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.preference.PreferenceManager
import androidx.core.app.NotificationCompat
import android.util.Log

import com.github.martoreto.aauto.vex.CarStatsClient
import com.google.android.apps.auto.sdk.notification.CarNotificationExtender

import java.util.Date

class EngineSpeedMonitor(private val mContext: Context, private val mHandler: Handler) : CarStatsClient.Listener {

    //private val mNotificationManager: NotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private var mIsEnabled = false
    private var mIsSoundEnabled= false
    private var mSoundUpToGear = 4
    private var mESInform: Float = 5500f
    private var mESHint: Float = 5900f
    private var mESWarn: Float = 6300f

    private val packageName = mContext.packageName

    private var mState = State.ENGINE_SPEED_OK

    private val mPreferencesListener = SharedPreferences.OnSharedPreferenceChangeListener { sharedPreferences, _ -> readPreferences(sharedPreferences) }

    private val mDismissNotification = Runnable {
        //Log.d(TAG, "Dismissing oil temperature notification")
        //mNotificationManager.cancel(TAG, NOTIFICATION_ID)
    }

    internal enum class State {
        ENGINE_SPEED_OK,
        ENGINE_SPEED_INFORM,
        ENGINE_SPEED_HINT,
        ENGINE_SPEED_WARN,
    }

    init {
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        sharedPreferences.registerOnSharedPreferenceChangeListener(mPreferencesListener)
        readPreferences(sharedPreferences)
    }

    private fun readPreferences(preferences: SharedPreferences) {
        mIsEnabled = preferences.getBoolean(PREF_ENABLED, false)
        mIsSoundEnabled = preferences.getBoolean(PREF_SOUND_ENABLED, true)
        mSoundUpToGear = preferences.getString(PREF_SOUND_UP_TO_GEAR, "")?.toIntOrNull() ?: 4

        mESInform = preferences.getString(PREF_ES_INFORM, "")?.toFloatOrNull() ?: 5500f
        mESHint = preferences.getString(PREF_ES_HINT, "")?.toFloatOrNull() ?: 5900f
        mESWarn = preferences.getString(PREF_ES_WARN, "")?.toFloatOrNull() ?: 6300f

        //mLowThreshold = mHighThreshold - HYSTERESIS
        if (!mIsEnabled) {
            mHandler.post(mDismissNotification)
            mState = State.ENGINE_SPEED_OK
        }

    }

    private fun notifyES(state:State,goesUp:Boolean, playSound:Boolean=false) {
        /*val title = mContext.getString(R.string.notification_oil_title)
        val text = mContext.getString(R.string.notification_oil_text)

        val notification = NotificationCompat.Builder(mContext, CarStatsService.NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_oil)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .extend(CarNotificationExtender.Builder()
                        .setTitle(title)
                        .setSubtitle(text)
                        .setActionIconResId(R.drawable.ic_check_white_24dp)
                        .setThumbnail(CarUtils.getCarBitmap(mContext, R.drawable.ic_oil,
                                R.color.car_primary, 128))
                        .setShouldShowAsHeadsUp(true)
                        .build())
                .build()
        mNotificationManager.notify(TAG, NOTIFICATION_ID, notification)
        mHandler.postDelayed(mDismissNotification, NOTIFICATION_TIMEOUT_MS.toLong())
        */

        // Broadcast: state, goesUp

        val stateInt = when(state) {
            State.ENGINE_SPEED_OK -> 0
            State.ENGINE_SPEED_INFORM -> 1
            State.ENGINE_SPEED_HINT -> 2
            State.ENGINE_SPEED_WARN -> 3
        }

        val intent = Intent().apply {
            //addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES) //TODO: maybe needed?
            action = packageName
            putExtra("engineSpeedState", stateInt)
        }
        mContext.sendBroadcast(intent)

        if (playSound) {
            val soundPlayer = CarNotificationSoundPlayer(mContext, R.raw.light)
            soundPlayer.play()
        }
    }

    private var currentGear = Int.MAX_VALUE
    override fun onNewMeasurements(provider: String, timestamp: Date, values: Map<String, Any>) {
        if (!mIsEnabled) {
            return
        }
        if (values.containsKey("currentGear")) {
            currentGear = (values["currentGear"] as? String)?.toIntOrNull() ?: Int.MAX_VALUE
            Log.i(TAG,"Current gear: %s".format(currentGear))
        }

        if (values.containsKey("engineSpeed")) {
            val measurement = values["engineSpeed"] as? Float ?: -1f


            val newState = when {
                measurement >= mESWarn -> State.ENGINE_SPEED_WARN
                measurement >= mESHint -> State.ENGINE_SPEED_HINT
                measurement >= mESInform -> State.ENGINE_SPEED_INFORM
                else -> State.ENGINE_SPEED_OK
            }

            if (newState > mState && newState != State.ENGINE_SPEED_OK)  {
                val gearToPlaySound = if (currentGear==Int.MAX_VALUE) true else (currentGear in 1..mSoundUpToGear)

                notifyES(newState, true,mIsSoundEnabled && gearToPlaySound)
            } else if (newState < mState) {
                notifyES(newState, false)
            }

            mState=newState
        }
    }

    override fun onSchemaChanged() {
        // do nothing
    }

    @Synchronized
    fun close() {
        mHandler.post(mDismissNotification)
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext)
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(mPreferencesListener)
    }

    companion object {
        private const val TAG = "EngineSpeedMonitor"

        const val PREF_ENABLED = "engineSpeedMonitoringActive"

        const val PREF_SOUND_ENABLED = "engineSpeedSoundActive"
        const val PREF_SOUND_UP_TO_GEAR = "engineSpeedSoundUpToGear"

        const val PREF_ES_INFORM = "engineSpeedESInform"
        const val PREF_ES_HINT = "engineSpeedESHint"
        const val PREF_ES_WARN = "engineSpeedESWarm"

        private const val NOTIFICATION_ID = 22

        private const val NOTIFICATION_TIMEOUT_MS = 60000

        private const val HYSTERESIS = 15f
    }
}
