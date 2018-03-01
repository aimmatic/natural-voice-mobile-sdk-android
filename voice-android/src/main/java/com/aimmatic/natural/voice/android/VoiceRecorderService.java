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
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.aimmatic.natural.core.rest.AndroidAppContext;
import com.aimmatic.natural.voice.rest.Language;
import com.aimmatic.natural.voice.rest.Resources;
import com.aimmatic.natural.voice.rest.VoiceSender;
import com.aimmatic.natural.voice.rest.response.Status;
import com.aimmatic.natural.voice.rest.response.VoiceResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.Response;

/**
 * This class represent voice recorder service, a service that record the speech and send
 * wave data to Placenext SDK to analise and process voice recognition.
 */

public class VoiceRecorderService extends Service {

    private static final String TAG = "VoiceRecorderService";

    /**
     * Helper class to return VoiceRecorderService from an interface binder.
     *
     * @param binder an interface bind
     * @return VoiceRecorderService instance
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

    // list of listeners event
    private final ArrayList<VoiceRecorderCallback> listeners = new ArrayList<>();
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
     * Add voice recorder listeners
     *
     * @param listener voice recorder listeners
     */
    public void addListener(@NonNull VoiceRecorderCallback listener) {
        listeners.add(listener);
    }

    /**
     * Remove voice recorder listeners if existed
     *
     * @param listener voice recorder listeners
     */
    public void removeListener(@NonNull VoiceRecorder.EventListener listener) {
        listeners.remove(listener);
    }

    /**
     * Start a voice recorder. Caller must check the record voice permission first before calling
     * this method. Flac will be use as default voice encoding.
     *
     * @param speechLength a maximum length of speech can be recorded. if length value is 0 or negative, default length of 24 second is used.
     * @param language     a language of BCP-47 code. Get the language from {@link com.aimmatic.natural.voice.rest.Language#bcp47Code}
     */
    public void startRecordVoice(int speechLength, String language) {
        this.startRecordVoice(speechLength, language, VoiceRecorder.VOICE_ENCODE_AS_FLAC);
    }

    /**
     * Start a voice recorder. Caller must check the record voice permission first before calling
     * this method.
     *
     * @param speechLength  a maximum length of speech can be recorded. if length value is 0 or negative, default length of 24 second is used.
     * @param language      a language of BCP-47 code. Get the language from {@link Language#getBcp47Code()}
     * @param voiceEncoding a voice encode type from {@link com.aimmatic.natural.voice.android.VoiceRecorder#VOICE_ENCODE_AS_WAVE}
     *                      or {@link com.aimmatic.natural.voice.android.VoiceRecorder#VOICE_ENCODE_AS_FLAC}
     */
    public void startRecordVoice(int speechLength, final String language, final int voiceEncoding) {
        if (voiceRecorder != null) {
            voiceRecorder.stop();
        }
        // internal voice recorder listeners
        eventListener = new VoiceRecorder.EventListener() {

            private FileOutputStream outfile;

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
                    String filename = "aimmatic-audio." + ((voiceEncoding == VoiceRecorder.VOICE_ENCODE_AS_FLAC) ? "flac" : "wav");
                    outfile = new FileOutputStream(new File(getCacheDir(), filename));
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
                    outfile.write(data, 0, size);
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
                if (outfile != null) {
                    try {
                        outfile.close();
                    } catch (IOException e) {
                        Log.d(TAG, "unable to close output temporary (wave,flac) file due to " + e.getLocalizedMessage());
                    }
                    BackgroundTask bt = new BackgroundTask(recordSampleRate, language, voiceEncoding, getApplicationContext(), listeners);
                    bt.start();
                    bt.sendVoice();
                }
                voiceRecorder = null;
            }
        };
        voiceRecorder = new VoiceRecorder(speechLength, voiceEncoding, eventListener);
        Log.d(TAG, "start voice recorder thread");
        voiceRecorder.start();
    }

    /**
     * Stop a voice recorder manually without reaching 29 second or 2 second not voice timeout
     */
    public void stopRecordVoice() {
        if (voiceRecorder != null) {
            voiceRecorder.stop();
            voiceRecorder = null;
            Log.d(TAG, "stop voice recorder thread");
        }
    }

    /**
     * get default language of the device. This may use to define person origin language
     *
     * @return language code of the device
     */
    public static String getDefaultLanguageCode() {
        final Locale locale = Locale.getDefault();
        final StringBuilder language = new StringBuilder(locale.getLanguage());
        final String country = locale.getCountry();
        if (!TextUtils.isEmpty(country)) {
            language.append("-");
            language.append(country);
        }
        return language.toString();
    }

    /**
     *
     */
    public static abstract class VoiceRecorderCallback extends VoiceRecorder.EventListener {

        /**
         *
         */
        public void onVoiceSent(VoiceResponse response) {
        }

    }

    private static class BackgroundTask extends HandlerThread {

        private int recordSampleRate;
        private int voiceEncoding;
        private String language;
        private Context ctx;
        private ArrayList<VoiceRecorderCallback> listeners;

        BackgroundTask(int sampleRate, String language, int encodingType, Context ctx, ArrayList<VoiceRecorderCallback> listener) {
            super("voice-sender");
            recordSampleRate = sampleRate;
            this.language = language;
            this.voiceEncoding = encodingType;
            this.ctx = ctx;
            this.listeners = listener;
        }

        public void sendVoice() {
            new Handler(getLooper()).post(new Runnable() {
                @Override
                public void run() {
                    doInBackground();
                    quit();
                }
            });
        }

        private Void doInBackground() {
            // set send the file
            String filename = "aimmatic-audio." + ((voiceEncoding == VoiceRecorder.VOICE_ENCODE_AS_FLAC) ? "flac" : "wav");
            File file = new File(ctx.getCacheDir(), filename);
            final File sendFile = new File(ctx.getCacheDir(), System.currentTimeMillis() + "");
            if (file.renameTo(sendFile)) {
                int permission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION);
                double lat = 0;
                double lng = 0;
                if (permission == PackageManager.PERMISSION_GRANTED) {
                    LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
                    Criteria criteria = new Criteria();
                    criteria.setAccuracy(Criteria.ACCURACY_FINE);
                    criteria.setPowerRequirement(Criteria.POWER_MEDIUM);
                    final String provider = lm.getBestProvider(criteria, true);
                    Location location = lm.getLastKnownLocation(provider);
                    if (location != null) {
                        lat = location.getLatitude();
                        lng = location.getLongitude();
                    } else {
                        lm.requestSingleUpdate(provider, new LocationListener() {
                            @Override
                            public void onLocationChanged(Location location) {
                                if (location != null) {
                                    send(sendFile, location.getLongitude(), location.getLongitude());
                                }
                            }

                            @Override
                            public void onStatusChanged(String provider, int status, Bundle extras) {
                            }

                            @Override
                            public void onProviderEnabled(String provider) {
                            }

                            @Override
                            public void onProviderDisabled(String provider) {
                            }
                        }, getLooper());
                    }
                }
                send(sendFile, lat, lng);
            }
            return null;
        }

        private void send(File sendFile, double lat, double lng) {
            VoiceResponse voiceResponse = new VoiceResponse(null, new Status(-1, "unable to send audio to server", null));
            try {
                VoiceSender voiceSender = new VoiceSender(new AndroidAppContext(ctx));
                MediaType mediaType = Resources.MEDIA_TYPE_FLAC;
                if (voiceEncoding == VoiceRecorder.VOICE_ENCODE_AS_WAVE) {
                    Log.d(TAG, "sending wave voice data");
                    mediaType = Resources.MEDIA_TYPE_WAVE;
                } else {
                    Log.d(TAG, "sending flac voice data");
                }
                Response response = voiceSender.sentVoice(sendFile, mediaType, language, lat, lng, recordSampleRate);
                Gson gson = new GsonBuilder().create();
                String body = response.body().string();
                voiceResponse = gson.fromJson(body, VoiceResponse.class);
            } catch (IOException e) {
                Log.d(TAG, "unable to send voice data to backend due to " + e.getLocalizedMessage());
                voiceResponse = new VoiceResponse(null, new Status(-1, e.getMessage(), null));
            } finally {
                if (listeners != null) {
                    for (VoiceRecorderCallback vrc : this.listeners) {
                        vrc.onVoiceSent(voiceResponse);
                    }
                }
                sendFile.delete();
            }
        }

    }

}
