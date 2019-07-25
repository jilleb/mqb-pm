package com.mqbcoding.stats

import android.app.Notification
import android.content.*
import android.service.notification.StatusBarNotification
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.graphics.Bitmap
import android.os.Build
import androidx.preference.Preference
import android.service.notification.NotificationListenerService
import android.text.SpannableString
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
            //TODO: If CarApp visible

            if (sbn.packageName == "com.google.android.apps.maps" && sbn.notification?.group == "navigation_status_notification_group") {

                val pack = sbn.packageName
                val ticker = sbn.notification.tickerText?.toString()
                val extras = sbn.notification.extras
                var title =""
                val tmpTitle = extras.get("android.title")
                try {
                    if (tmpTitle is SpannableString) {
                        //title = tmpTitle.toString()
                    } else if (tmpTitle is CharSequence || tmpTitle is String) {
                        title = tmpTitle.toString()
                    }
                } catch (e:Exception){}
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