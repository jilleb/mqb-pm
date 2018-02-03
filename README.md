# MQB-stats

This is a performance monitor, based on Martoreto's aa-stats.
It's made specifically for use with the Volkswagen/Skoda/Seat/Audi MIB2 infotainment units, but it might also work on others, as long as they have Android Auto support.


# Installation instructions:

Download the latest release here: https://github.com/jilleb/mqb-pm/releases

- Make sure Android Auto is in developer mode: Open Android Auto (while not connected to the car), go to About. Tap the "About Android Auto" header 10 times till you see a toast message saying you're a developer. From the right top corner select developer options, scroll down and make sure you check "Unknown sources". This will allow programs from non-Playstore apps to be run on Android Auto. See a picture guide how to enable Developer mode here: https://www.howtogeek.com/271132/how-to-enable-developer-settings-on-android-auto/
- Install aauto-vex-vag.apk (open source service by Martoreto: https://github.com/martoreto/aauto-vex-vag/) , the service that supplies exlap-communication between Android and the head-unit. Note: this app will not show up in your launcher.
- Install the Performance Monitor apk
- Open the Performance Monitor settings, grant all the rights it's requesting. If you don't do this, the app will NOT work.
- Hook your phone to your car's USB, start Android Auto on your unit.
- You can find the Performance Monitor in the menu on the lower right in Android Auto, the one with the dashboard clock on it.


# Known issues
- Black screen of death: On some cars it doesn't work for unknown reasons. This will not kill your car or headunit. Send me a logcat. 

- Not starting
If the app doesn't start, there's probably a bug in the code. This will not kill your car either. Send me a logcat.

- No data
If you don't see any data in the app, it can have various reasons:
Some cars don't provide data to the headunit. 
Some MIB2 firmware versions don't provide data to the exlap channel.
Not all data elements are available on all cars. If one element doesn't provide data, try an other one.
Most cars do not report oil and coolant temperatures below 50.

# Currently not working firmware versions:
**MIB2:**
- VW 0245
- VW 0343
- VW 0613
- VW 0751
- VW 0753
- VW 1156

**RCD330:**
- VW 5406

# Currently confirmed working firmware versions:
**MIB2:**
- VW 0814
- VW 1187
- Seat 1146


**MIB2.5**
- Seat 1162
