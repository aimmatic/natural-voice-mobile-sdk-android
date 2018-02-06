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

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.aimmatic.natural.BuildConfig;

/**
 * This class represent an audio recorder. It record the speech into a wave format PCM 16 bit.
 * The voice recorder only record wave data if it detect any speech on the byte stream
 */

public class VoiceRecorder {

    private static final String TAG = "VoiceRecorder";

    private static final int[] SAMPLE_RATE_CANDIDATES = new int[]{16000, 11025, 22050, 44100};

    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    //
    private static final int AMPLITUDE_THRESHOLD = 1500;
    // 2 second if no speech detected if it will automatically end the record
    private static final int SPEECH_TIMEOUT_MILLIS = 2000;
    // 29 second after continue to header the speech, the recorder will automatically end the record
    private static final int MAX_SPEECH_LENGTH_MILLIS = 29 * 1000;

    /**
     * Voice record event listener
     */
    public static abstract class EventListener {

        /**
         * Called when the recorder starts hearing voice.
         */
        public void onRecordStart() {
        }

        /**
         * Called when the recorder is hearing voice.
         *
         * @param data The audio data in {@link AudioFormat#ENCODING_PCM_16BIT}.
         * @param size The size of the actual data in {@code data}.
         */
        public void onRecording(byte[] data, int size) {
        }

        /**
         * Called when the recorder stops hearing voice or exceed 29 second
         */
        public void onRecordEnd() {
        }

    }

    //
    private AudioRecord audioRecord;
    private Thread thread;
    private byte[] buffer;
    private final Object lock = new Object();
    // internal callback
    private final EventListener eventListener;

    private long voiceHeardMillis = Long.MAX_VALUE;
    private long voiceStartStartedMillis;

    /**
     * Create a new voice recorder object. It you decide to use this class directly you make sure to
     * stop the recorder during device rotation otherwise leak or crash can happen.
     *
     * @param eventListener voice recorder event listener
     */
    public VoiceRecorder(EventListener eventListener) {
        this.eventListener = eventListener;
    }

    /**
     * Starts recording voice and caller must call stop later.
     */
    public void start() {
        stop();
        audioRecord = createAudioRecord();
        if (audioRecord == null) {
            throw new RuntimeException("Cannot instantiate VoiceRecorder");
        }
        // Start recording.
        audioRecord.startRecording();
        // Start processing the captured audio.
        thread = new Thread(new TransferFromAudioRecorder());
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "start audio recorder");
        }
        thread.start();
    }

    /**
     * Stops recording audio.
     */
    public void stop() {
        synchronized (lock) {
            dismiss();
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            buffer = null;
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "stop audio recorder");
            }
        }
    }

    /**
     * Dismisses the currently ongoing utterance.
     */
    public void dismiss() {
        if (voiceHeardMillis != Long.MAX_VALUE) {
            voiceHeardMillis = Long.MAX_VALUE;
            eventListener.onRecordEnd();
        }
    }

    /**
     * Retrieves the sample rate currently used to record audio.
     *
     * @return The sample rate of recorded audio.
     */
    public int getSampleRate() {
        if (audioRecord != null) {
            return audioRecord.getSampleRate();
        }
        return 0;
    }

    /**
     * Creates a new {@link AudioRecord}.
     *
     * @return A newly created {@link AudioRecord}, or null if it cannot be created due to no permission
     * or no microphone available.
     */
    private AudioRecord createAudioRecord() {
        for (int sampleRate : SAMPLE_RATE_CANDIDATES) {
            final int sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, CHANNEL, ENCODING);
            if (sizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
                continue;
            }
            final AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleRate, CHANNEL, ENCODING, sizeInBytes);
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                buffer = new byte[sizeInBytes];
                return audioRecord;
            } else {
                audioRecord.release();
            }
        }
        return null;
    }

    /**
     * Continuously processes the captured audio and notifies {@link #eventListener} of corresponding
     * events.
     */
    private class TransferFromAudioRecorder implements Runnable {

        @Override
        public void run() {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "read from audio record buffer");
            }
            while (true) {
                synchronized (lock) {
                    if (Thread.currentThread().isInterrupted()) {
                        break;
                    }
                    final int size = audioRecord.read(buffer, 0, buffer.length);
                    final long now = System.currentTimeMillis();
                    if (isHearingVoice(buffer, size)) {
                        if (voiceHeardMillis == Long.MAX_VALUE) {
                            voiceStartStartedMillis = now;
                            eventListener.onRecordStart();
                        }
                        eventListener.onRecording(buffer, size);
                        voiceHeardMillis = now;
                        if (now - voiceStartStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
                            end();
                        }
                    } else if (voiceHeardMillis != Long.MAX_VALUE) {
                        eventListener.onRecording(buffer, size);
                        if (now - voiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
                            end();
                        }
                    }
                }
            }
        }

        // end the record
        private void end() {
            voiceHeardMillis = Long.MAX_VALUE;
            eventListener.onRecordEnd();
        }

        // detect if we can hear the voice
        private boolean isHearingVoice(byte[] buffer, int size) {
            for (int i = 0; i < size - 1; i += 2) {
                // The buffer has LINEAR16 in little endian.
                int s = buffer[i + 1];
                if (s < 0) s *= -1;
                s <<= 8;
                s += Math.abs(buffer[i]);
                if (s > AMPLITUDE_THRESHOLD) {
                    return true;
                }
            }
            return false;
        }

    }

}
