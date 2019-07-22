package com.mqbcoding.stats

import android.app.Notification
import android.content.*
import android.service.notification.StatusBarNotification
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.graphics.Bitmap
import android.os.Build
import androidx.preference.Preference
import android.service.notification.NotificationListenerService
import androidx.preference.PreferenceManager


class NotificationService : NotificationListenerService() {

    override fun onCreate() {

        super.onCreate()

    }

    override fun onDestroy() {
        super.onDestroy()
    }


    override fun onNotificationPosted(sbn: StatusBarNotification) {
        try {
            if (sbn.packageName == "com.google.android.apps.maps" && sbn.notification?.group == "navigation_status_notification_group") {

                val pack = sbn.packageName
                val ticker = sbn.notification.tickerText?.toString()
                val extras = sbn.notification.extras
                val title = extras.getString("android.title")
                val text = extras.getCharSequence("android.text")!!.toString()

                val msgrcv = Intent("GoogleNavigationUpdate")
                msgrcv.putExtra("package", pack)
                msgrcv.putExtra("ticker", ticker)
                msgrcv.putExtra("title", title)
                msgrcv.putExtra("text", text)
                //msgrcv.putExtra("icon", icon)

                LocalBroadcastManager.getInstance(this).sendBroadcast(msgrcv)

            }
        }catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {

        try {
            if (sbn.packageName == "com.google.android.apps.maps" && sbn.notification?.group == "navigation_status_notification_group") {

                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent("GoogleNavigationClosed"))

            }
        } catch (e:Exception) {
            e.printStackTrace()
        }
    }
}