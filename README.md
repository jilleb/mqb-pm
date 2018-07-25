# MQB-stats

This is a performance monitor, based on Martoreto's aa-stats.
It's made specifically for use with the Volkswagen/Skoda/Seat/Audi MIB2 infotainment units, but it might also work on others, as long as they have Android Auto support.

# Screenshots:
Some screenshots of the app:

Golf GTI theme with high visibility-dials.
![VW theme](https://i.imgur.com/dTG3oBq.png)

Seat theme with high min/max turned on.
![Seat theme](https://i.imgur.com/g8J8whV.png)

Skoda ONEapp theme with high min/max turned on.
![Skoda ONE theme](https://i.imgur.com/OfO3jpb.png)

Stopwatch/laptimer mode in VW GTI theme:
![VW stopwatch](https://i.imgur.com/0jm310L.png)


# Installation instructions:

Download the latest release here: https://github.com/jilleb/mqb-pm/releases

- Make sure Android Auto is in developer mode: Open Android Auto (while not connected to the car), go to About. Tap the "About Android Auto" header 10 times untill you see a toast message saying you're a developer. From the right top corner select developer options, scroll down and make sure you check "Unknown sources". This will allow programs from non-Playstore apps to be run on Android Auto. See a picture guide how to enable Developer mode here: https://www.howtogeek.com/271132/how-to-enable-developer-settings-on-android-auto/
- Install aauto-vex-vag.apk (open source service by Martoreto: https://github.com/martoreto/aauto-vex-vag/) , the service that supplies exlap-communication between Android and the head-unit. Note: this app will not show up in your launcher.
- Working only on Rooted phone at this time
- Install the Performance Monitor apk
- Open the Performance Monitor settings, grant all the rights it's requesting. If you don't do this, the app will NOT work.
- Start Android Auto on your phone (the phone is not attached to anything at this time)
- Click on the menu on the top left (three bars)
- Click "About" The following steps may already be done. I am including them for reference for newbies.
- If developer mode is not enabled, click on the bar "About Android Auto" 10 times.
- click on the three dot menu (on the top right)
- click on "Developer settings"
- click on Application Mode and set to "Developer". Also go to the bottom of the developers settings and make sure "Unknown Sources" is checked.
- click back, and then go to the three dot menu. Click "Start head unit server".
- connect the phone to the computer, enable "USB debugging" in your phone's developer settings. This is different from Android auto developer settings.
- open a command shell 
- type "adb forward tcp:5277 tcp:5277"
- navigate to /extras/google/auto. This is usually found at "c:\users\USERNAME\AppData\Local\Android\Sdk\extras\google\auto"
- start the desktop head unit with the command "desktop-head-unit"
- accept all the permissions, etc.
- At this time you should see the typical head unit display that you would see in your car.
- click on the lower right speedometer symbol (to return to Sync, or other OS).
- you should see 'Performance Monitor ' listed. if not restart all instructions
- Deploy the left top menu
- clic on 'unlock' bottom of the list
- accept root permission on the phone
- Hook your phone to your car's USB, start Android Auto on your unit.
- You can find the Performance Monitor in the menu on the lower right in Android Auto, the one with the dashboard clock on it.


# Known issues
- Google is actively blocking custom apps for Android Auto. So the app is not working with Android Auto v3.0 (unless you apply the workaround). Its also not working on Google Play Services v12.5.21. There are workarounds, but I will not describe or support them. 
- App slowing down over time, and restarting. I haven't pinned this down completely yet. 

- Black screen of death: On some cars it doesn't work for unknown reasons. This will not kill your car or headunit. Send me a logcat. 
On Huawei and Honor devices this is a common problem, caused by a bug in their ROMs. A workaround to get OEM apps working again is to clear the Android Auto apk cache when this happens. (thanks to nerone-github for the information.)

- No data
If you don't see any data in the app, it can have various reasons:
Some cars don't provide data to the headunit. 
Some MIB2 firmware versions don't provide data to the exlap channel.
Not all data elements are available on all cars. If one element doesn't provide data, try an other one.
Most cars do not report oil and coolant temperatures below 50.

- Android Auto crashing after some time
It looks like the app is crashing Android Auto when it's not getting any data. This doesn't happen on all cars.

- Appkillers/memory managers kill the service
Put the Vag extensions for Android Auto app in the whitelist of your memory manager.

- Phone locking causes data to stop
Some phones kill the datafeed as soon as the screen is locked. This doesn't happen with all phones.

# What to do when it's not working?
- Try choosing a different type of data in the settings app. Not all cars have all data available. Reconnect to the car after you've done this.
- Check if you have applied all rights. Did you open the settings app after installing? Did you install both APK's?
- Unplug/replug your phone and try again

# Currently not working firmware versions:
**MIB2:**
- VW 0245
- VW 0343
- VW 0613
- VW 0617
- VW 0755
- VW 1156

**MIB2Std**
- Seat 0351
- Seat 0359

**RCD330:**
- VW 5406

# Currently confirmed working firmware versions:
**MIB2Std**
- Seat 0462
- Skoda 0468

**MIB2:**
- VW 0814
- VW 1187
- Seat 1146
- Seat 1219
- Seat 1308
- Seat 1338
- Skoda 0468 

**MIB2.5**
- Seat 1162

# Mixed results:
**MIB2:**
The following firmwares are working, but for some users they don't.
- VW 0751
- VW 0753