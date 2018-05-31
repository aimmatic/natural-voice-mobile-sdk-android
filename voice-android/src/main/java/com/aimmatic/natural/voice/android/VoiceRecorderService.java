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
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;

import com.aimmatic.natural.core.rest.AndroidAppContext;
import com.aimmatic.natural.voice.encoder.AudioMeta;
import com.aimmatic.natural.voice.encoder.FlacEncoder;
import com.aimmatic.natural.voice.encoder.WavEncoder;
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

    private byte stopPolicy;
    private int recordSampleRate;
    private VoiceRecorder voiceRecorder;
    private RecordStrategy currentStrategy;

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
    public void removeListener(@NonNull VoiceRecorderCallback listener) {
        listeners.remove(listener);
    }

    /**
     * Start a voice recorder. Caller must check the record voice permission first before calling
     * this method. Flac will be use as default voice encoding.
     *
     * @param speechLength a maximum length of speech can be recorded. if length value is 0 or negative, default length of 24 second is used.
     * @param language     a language of BCP-47 code. Get the language from {@link com.aimmatic.natural.voice.rest.Language#bcp47Code}
     * @deprecated As of release 1.1.0, replaced by {@link VoiceRecorderService#startRecordVoice(RecordStrategy)}
     */
    @Deprecated
    public void startRecordVoice(int speechLength, String language) {
        this.startRecordVoice(new RecordStrategy()
                .setMaxRecordDuration(speechLength * 1000)
                .setLanguage(Language.getLanguage(language))
                .setEncoder(new WavEncoder()));
    }

    /**
     * Start a voice recorder. Caller must check the record voice permission first before calling
     * this method.
     *
     * @param speechLength  a maximum length of speech can be recorded. if length value is 0 or negative, default length of 24 second is used.
     * @param language      a language of BCP-47 code. Get the language from {@link Language#getBcp47Code()}
     * @param voiceEncoding a voice encode type from {@link com.aimmatic.natural.voice.android.VoiceRecorder#VOICE_ENCODE_AS_WAVE}
     *                      or {@link com.aimmatic.natural.voice.android.VoiceRecorder#VOICE_ENCODE_AS_FLAC}
     * @deprecated As of release 1.1.0, replaced by {@link VoiceRecorderService#startRecordVoice(RecordStrategy)}
     */
    @Deprecated
    public void startRecordVoice(int speechLength, final String language, final int voiceEncoding) {
        this.startRecordVoice(new RecordStrategy()
                .setMaxRecordDuration(speechLength * 1000)
                .setLanguage(Language.getLanguage(language))
                .setEncoder(voiceEncoding == VoiceRecorder.VOICE_ENCODE_AS_WAVE ? new WavEncoder() : new FlacEncoder()));
    }

    /**
     * Start record voice test
     *
     * @param recordStrategy a strategy to record audio
     */
    @VisibleForTesting
    void startTestRecordVoice(final RecordStrategy recordStrategy, VoiceRecorder voiceRecorder) {
        this.startRecordVoice(recordStrategy, voiceRecorder);
    }

    /**
     * Start a voice recorder. Caller must check the record voice permission first before calling
     * this method.
     *
     * @param recordStrategy a strategy to record audio
     */
    public void startRecordVoice(final RecordStrategy recordStrategy) {
        this.startRecordVoice(recordStrategy, new VoiceRecorder(recordStrategy));
    }

    // internal start record voice
    private void startRecordVoice(final RecordStrategy recordStrategy, VoiceRecorder newVoiceRecorder) {
        this.currentStrategy = recordStrategy;
        if (this.voiceRecorder != null) {
            this.voiceRecorder.stop();
        }
        this.voiceRecorder = newVoiceRecorder;
        // internal voice recorder listeners
        VoiceRecorder.EventListener eventListener = new VoiceRecorder.EventListener() {

            private FileOutputStream outfile;

            /**
             * {@inheritDoc}
             */
            @Override
            public void onRecordStart(AudioMeta audioMeta) {
                recordSampleRate = VoiceRecorderService.this.voiceRecorder.getSampleRate();
                for (VoiceRecorder.EventListener listener : listeners) {
                    listener.onRecordStart(audioMeta);
                }
                try {
                    String filename = "aimmatic-audio." + currentStrategy.getEncoder().extension();
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
                for (VoiceRecorder.EventListener listener : listeners) {
                    listener.onRecording(data, size);
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
            public void onRecordEnd(byte state) {
                for (VoiceRecorder.EventListener listener : listeners) {
                    listener.onRecordEnd(state);
                }
                if (outfile != null) {
                    try {
                        outfile.close();
                    } catch (IOException e) {
                        Log.d(TAG, "unable to close output temporary (wave,flac) file due to " + e.getLocalizedMessage());
                        VoiceRecorderService.this.voiceRecorder = null;
                        return;
                    }
                }
                if (state == VoiceRecorder.RECORD_END_BY_USER && stopPolicy == RecordStrategy.POLICY_CANCELED) {
                    String filename = "aimmatic-audio." + recordStrategy.getEncoder().extension();
                    File file = new File(getCacheDir(), filename);
                    // delete the file if user canceled
                    file.delete();
                } else if ((state == VoiceRecorder.RECORD_END_BY_IDLE && recordStrategy.getSpeechTimeoutPolicies() == RecordStrategy.POLICY_SEND_IMMEDIATELY) ||
                        (state == VoiceRecorder.RECORD_END_BY_MAX && recordStrategy.getMaxRecordDurationPolicies() == RecordStrategy.POLICY_SEND_IMMEDIATELY) ||
                        (state == VoiceRecorder.RECORD_END_BY_USER && stopPolicy == RecordStrategy.POLICY_SEND_IMMEDIATELY)) {
                    BackgroundTask bt = new BackgroundTask(recordSampleRate, recordStrategy, getApplicationContext(), listeners);
                    bt.start();
                    bt.sendVoice();
                }
                voiceRecorder = null;
            }
        };
        this.voiceRecorder.setRecorderCallback(eventListener);
        Log.d(TAG, "start voice recorder thread");
        this.voiceRecorder.start();
    }

    /**
     * Stop a voice recorder manually
     *
     * @deprecated As of 1.1.0, replaces by {@link #stopRecordVoice(byte)}
     */
    @Deprecated
    public void stopRecordVoice() {
        this.stopRecordVoice(RecordStrategy.POLICY_SEND_IMMEDIATELY);
    }

    /**
     * Stop a voice recorder manually and provide a policy for whether we send the audio or we canceled.
     * See {@link RecordStrategy#POLICY_SEND_IMMEDIATELY} and {@link RecordStrategy#POLICY_CANCELED}. Giving
     * {@link RecordStrategy#POLICY_USER_CHOICE} will cause a runtime exception.
     *
     * @param policy a policy to process when user stop recording manually
     */
    public void stopRecordVoice(byte policy) {
        if (policy == RecordStrategy.POLICY_USER_CHOICE) {
            throw new IllegalArgumentException("Policy can only be either POLICY_CANCELED or POLICY_SEND_IMMEDIATELY");
        }
        if (voiceRecorder != null) {
            this.stopPolicy = policy;
            voiceRecorder.stop();
            voiceRecorder = null;
            Log.d(TAG, "stop voice recorder thread");
        }
    }

    /**
     * Inform user's choice over the existing record
     *
     * @param policy a policy to define user's choice
     */
    public void onUserChoice(byte policy) {
        if (policy == RecordStrategy.POLICY_SEND_IMMEDIATELY) {
            BackgroundTask bt = new BackgroundTask(recordSampleRate, this.currentStrategy, getApplicationContext(), listeners);
            bt.start();
            bt.sendVoice();
        } else if (policy == RecordStrategy.POLICY_CANCELED) {
            String filename = "aimmatic-audio." + this.currentStrategy.getEncoder().extension();
            File file = new File(getCacheDir(), filename);
            // delete the file if user canceled
            file.delete();
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
        private RecordStrategy recordStrategy;
        private Context ctx;
        private ArrayList<VoiceRecorderCallback> listeners;

        BackgroundTask(int sampleRate, RecordStrategy recordStrategy, Context ctx, ArrayList<VoiceRecorderCallback> listener) {
            super("voice-sender");
            this.recordSampleRate = sampleRate;
            this.recordStrategy = recordStrategy;
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

        private void doInBackground() {
            // set send the file
            String filename = "aimmatic-audio." + recordStrategy.getEncoder().extension();
            File file = new File(ctx.getCacheDir(), filename);
            final File sendFile = new File(ctx.getCacheDir(), System.currentTimeMillis() + "");
            if (file.renameTo(sendFile)) {
                int permission = ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION);
                double lat = 0;
                double lng = 0;
                if (permission == PackageManager.PERMISSION_GRANTED) {
                    LocationManager lm = (LocationManager) ctx.getSystemService(Context.LOCATION_SERVICE);
                    if (lm != null) {
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
                }
                send(sendFile, lat, lng);
            }
        }

        private void send(File sendFile, double lat, double lng) {
            VoiceResponse voiceResponse = null;
            try {
                VoiceSender voiceSender = new VoiceSender(new AndroidAppContext(ctx));
                MediaType mediaType = recordStrategy.getEncoder().contentType();
                if (mediaType == Resources.MEDIA_TYPE_WAVE) {
                    Log.d(TAG, "sending wave voice data");
                } else {
                    Log.d(TAG, "sending flac voice data");
                }
                Response response = voiceSender.sentVoice(sendFile, mediaType, recordStrategy.getLanguage().getBcp47Code(), lat, lng, recordSampleRate);
                if (response.code() >= 400) {
                    voiceResponse = new VoiceResponse(null, new Status(response.code(), "unable to send audio to server", null));
                    return;
                }
                if (response.body() != null) {
                    Gson gson = new GsonBuilder().create();
                    String body = response.body().string();
                    voiceResponse = gson.fromJson(body, VoiceResponse.class);
                }
            } catch (IOException e) {
                Log.d(TAG, "unable to send voice data to backend due to " + e.getLocalizedMessage());
                voiceResponse = new VoiceResponse(null, new Status(-1, e.getMessage(), null));
            } finally {
                if (listeners != null) {
                    if (voiceResponse == null) {
                        voiceResponse = new VoiceResponse(null, new Status(-1, "unable to send audio to server", null));
                    }
                    for (VoiceRecorderCallback vrc : this.listeners) {
                        vrc.onVoiceSent(voiceResponse);
                    }
                }
                sendFile.delete();
            }
        }

    }

}
