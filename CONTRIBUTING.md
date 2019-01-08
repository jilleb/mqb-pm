# Contributing

There are a few ways to contribute the development of this app. On this page you can read all about it.

## Themes
Each app theme consists of:
- a needle / indicator
- a dial background
- a dial background without red warning marks ("empty")
- a stopwatch background
- an app background
- a needle color
- a theme name
- a mark-color

Google/Android Auto and VAG have strict guidelines when it comes to showing things on a screen that's visible while driving. **Not looking at the road when you should is dangerous!**
Here are my basic design principes:
- Nothing should be distracting
- Nothing should be hard to see/read
- Nothing should be too bright(especially at night)

### Needle
The needle image should always be in the 12'o'clock position. 
![Needle image](https://github.com/jilleb/mqb-pm/raw/master/app/src/main/res/drawable/needle_skoda.png "Needle")

### Dial background (with red warning zone)
- Dial backgound with marks on it:
If you use marks on the graphic, the start and end positions should be in the same location as the example. sv_markColor should be transparent in this case.

- Dial without marks on it:
The app will generate marks in the color which is specified in sv_markColor.

Always make sure there is enough room for data in the center circle. 

![Dial image](https://github.com/jilleb/mqb-pm/blob/master/app/src/main/res/drawable/dial_background_skoda.png "Dial background")

### Dial background empty
The "empty" dial background is identical to the other dial background, but is has no red warning zone.
Always make sure there is enough room for data in the center circle. 

![Dial image](https://github.com/jilleb/mqb-pm/blob/master/app/src/main/res/drawable/dial_background_empty_skoda.png "Dial background empty")

### Stopwatch background
Basically the same as the dial background, but this one should have marks on it.

![Stopwatch image](https://github.com/jilleb/mqb-pm/blob/master/app/src/main/res/drawable/sw_background_skoda.png "Stopwatch background")

### App background
The wallpaper in the app. This should be 800x400px. This is the image that will be visible throughout the entire application, so make sure it's nice.
![Background image](https://github.com/jilleb/mqb-pm/blob/master/app/src/main/res/drawable/background_incar_skoda.png "App background")

### Colors
- Needle color (themedNeedleColor)
Needle colors are used in high-visibility mode. This can be any color you like, I use semi-transparent ones.

- Mark color (sv_markColor)
Mark colors should be transparent when the dial background graphic has marks on it. Otherwise, keep them white or whatever suits your theme.

### Theme name
Pick whatever you like!
