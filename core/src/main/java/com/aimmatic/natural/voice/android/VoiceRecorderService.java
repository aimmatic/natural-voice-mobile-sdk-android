/*
Copyright 2018 The AimMatic Authors.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package com.aimmatic.natural.voice.android;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.aimmatic.natural.BuildConfig;
import com.aimmatic.natural.core.rest.AndroidAppContext;
import com.aimmatic.natural.voice.rest.VoiceSender;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

/**
 * This class represent voice recorder service, a service that record the speech and send
 * wave data to Placenext SDK to analise and process voice recognition.
 */

public class VoiceRecorderService extends Service {

    private static final String TAG = "VoiceRecorderService";

    /**
     * Helper class to return {@VoiceRecorderService} from an interface binder.
     *
     * @param binder an interface bind
     * @return {@VoiceRecorderService} instance
     */
    public static VoiceRecorderService from(IBinder binder) {
        return ((AudioRecordBinder) binder).getService();
    }

    // Binder class
    private class AudioRecordBinder extends Binder {

        VoiceRecorderService getService() {
            return VoiceRecorderService.this;
        }

    }

    // list of listener event
    private final ArrayList<VoiceRecorder.EventListener> listeners = new ArrayList<>();
    // binder
    private AudioRecordBinder binder = new AudioRecordBinder();

    private int recordSampleRate;
    private VoiceRecorder voiceRecorder;
    private VoiceRecorder.EventListener eventListener;

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    /**
     * Add voice recorder listener
     *
     * @param listener voice recorder listener
     */
    public void addListener(@NonNull VoiceRecorder.EventListener listener) {
        listeners.add(listener);
    }

    /**
     * Remove voice recorder listener if existed
     *
     * @param listener voice recorder listener
     */
    public void removeListener(@NonNull VoiceRecorder.EventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Start a voice recorder. Caller must check the record voice permission first before calling
     * this method
     */
    public void startRecordVoice() {
        if (voiceRecorder != null) {
            voiceRecorder.stop();
        }
        // internal voice recorder listener
        eventListener = new VoiceRecorder.EventListener() {

            private FileOutputStream outWav;

            /**
             * {@inheritDoc}
             */
            @Override
            public void onRecordStart() {
                recordSampleRate = voiceRecorder.getSampleRate();
                if (listeners != null) {
                    for (VoiceRecorder.EventListener listener : listeners) {
                        listener.onRecordStart();
                    }
                }
                try {
                    outWav = new FileOutputStream(new File(getCacheDir(), "aimmatic-audio.wav"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void onRecording(byte[] data, int size) {
                if (listeners != null) {
                    for (VoiceRecorder.EventListener listener : listeners) {
                        listener.onRecording(data, size);
                    }
                }
                try {
                    outWav.write(data, 0, size);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void onRecordEnd() {
                if (listeners != null) {
                    for (VoiceRecorder.EventListener listener : listeners) {
                        listener.onRecordEnd();
                    }
                }
                if (outWav != null) {
                    try {
                        outWav.close();
                    } catch (IOException e) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "unable to close output temporary wave file due to " + e.getLocalizedMessage());
                        }
                    }
                    new BackgroundTask(recordSampleRate).execute(getApplicationContext());
                }
            }
        };
        voiceRecorder = new VoiceRecorder(eventListener);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "start voice recorder thread");
        }
        voiceRecorder.start();
    }

    /**
     * Stop a voice recorder manually without reaching 29 second or 2 second not voice timeout
     */
    public void stopRecordVoice() {
        if (voiceRecorder != null) {
            Log.d("KAKA>>>", "Stop voice recorder");
            voiceRecorder.stop();
            voiceRecorder = null;
        }
    }

    // get default language of the device. This may use to define person origin language
    private static String getDefaultLanguageCode() {
        final Locale locale = Locale.getDefault();
        final StringBuilder language = new StringBuilder(locale.getLanguage());
        final String country = locale.getCountry();
        if (!TextUtils.isEmpty(country)) {
            language.append("-");
            language.append(country);
        }
        return language.toString();
    }

    private static class BackgroundTask extends AsyncTask<Context, Void, Void> {

        private int recordSampleRate;

        BackgroundTask(int sampleRate) {
            recordSampleRate = sampleRate;
        }

        @Override
        protected Void doInBackground(Context... ctxs) {
            Context ctx = ctxs[0];
            // set send the file
            File file = new File(ctx.getCacheDir(), "aimmatic-audio.wav");
            File sendFile = new File(ctx.getCacheDir(), System.currentTimeMillis() + ".wav");
            if (file.renameTo(sendFile))

            {
                VoiceSender voiceSender = new VoiceSender(new AndroidAppContext(ctx));
                int permission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION);
                double lat = 0, lng = 0;
                if (permission == PackageManager.PERMISSION_GRANTED) {
                    LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
                    String provider = lm.getBestProvider(criteria, true);
                    Location location = lm.getLastKnownLocation(provider);
                    lat = location.getLatitude();
                    lng = location.getLongitude();
                }
                try {
                    voiceSender.sentVoice(sendFile, getDefaultLanguageCode(), lat, lng, recordSampleRate);
                } catch (IOException e) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "unable to send voice data to backend due to " + e.getLocalizedMessage());
                    }
                } finally {
                    sendFile.delete();
                }
            }
            return null;
        }
    }

}
