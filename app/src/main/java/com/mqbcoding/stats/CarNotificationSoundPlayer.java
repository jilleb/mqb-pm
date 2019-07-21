package com.mqbcoding.stats;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import androidx.annotation.RawRes;
import android.support.car.Car;
import android.support.car.CarConnectionCallback;
import android.support.car.media.CarAudioManager;
import android.util.Log;

public class CarNotificationSoundPlayer {
    private final static String TAG = "CarNotifSoundPlayer";

    private final static int PLAYBACK_START_DELAY_MS = 300;

    private final @RawRes int mSoundResource;
    private final Context mContext;
    private final Handler mHandler;

    public CarNotificationSoundPlayer(Context context, @RawRes int soundResource) {
        this.mSoundResource = soundResource;
        this.mContext = context;
        this.mHandler = new Handler(context.getMainLooper());
    }

    public void play() {
        Log.d(TAG, "Starting");
        Car car = Car.createCar(mContext, mCarConnectionCallback);
        car.connect();
    }

    private final CarConnectionCallback mCarConnectionCallback = new CarConnectionCallback() {
        @Override
        public void onConnected(final Car car) {
            try {
                Log.d(TAG, "Connected to car, starting playback");
                final CarAudioManager carAudioManager = car.getCarManager(CarAudioManager.class);
                final AudioAttributes audioAttributes = carAudioManager.getAudioAttributesForCarUsage(
                        CarAudioManager.CAR_AUDIO_USAGE_ALARM);
                Log.d(TAG, "Audio attributes: " + audioAttributes);

                final AssetFileDescriptor fd = mContext.getResources().openRawResourceFd(mSoundResource);
                final MediaPlayer mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioAttributes(audioAttributes);
                mediaPlayer.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                fd.close();
                mediaPlayer.prepare();

                int ret = carAudioManager.requestAudioFocus(null, audioAttributes,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
                if (ret == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mediaPlayer) {
                            Log.d(TAG, "Playback completed.");
                            try {
                                mediaPlayer.release();
                                carAudioManager.abandonAudioFocus(null, audioAttributes);
                                car.disconnect();
                            } catch (Exception e) {
                                Log.w(TAG, "Error finalizing playback", e);
                            }
                        }
                    });
                } else {
                    Log.w(TAG, "Failed to obtain audio focus, playing anyway.");
                }

                // Allow some time for the ducking to take effect.
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mediaPlayer.start();
                    }
                }, PLAYBACK_START_DELAY_MS);
            } catch (Exception e) {
                Log.w(TAG, "Error initiating playback", e);
                car.disconnect();
            }
        }

        @Override
        public void onDisconnected(Car car) {
        }
    };
}
