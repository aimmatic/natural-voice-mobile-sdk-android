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
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

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

    /**
     * encode audio as wave pcm 16 bit
     */
    public static int VOICE_ENCODE_AS_WAVE = 1;

    /**
     * encode audio as flac from pcm wave 16 bit
     */
    public static int VOICE_ENCODE_AS_FLAC = 2;

    /**
     * event audio recorder listener
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

    //
    private int samplreRate;
    private int sizeInBytes;
    //
    private HandlerThread thread;
    private TransferFromAudioRecorder readAudioBuffer;
    private final Object lock = new Object();
    private boolean stop;
    // internal callback
    private final EventListener eventListener;
    //
    private final int encodingType;

    private long maxSpeech;
    private long voiceHeardMillis = Long.MAX_VALUE;
    private long voiceStartStartedMillis;

    /**
     * Create a new voice recorder object. It you decide to use this class directly you make sure to
     * stop the recorder during device rotation otherwise leak or crash can happen.
     *
     * @param speechLength  a maximum speech length in second, must be greater than 0 otherwise default length 29 second is set.
     * @param encodingType  encode type
     * @param eventListener voice recorder event listener
     */
    VoiceRecorder(int speechLength, int encodingType, EventListener eventListener) {
        if (speechLength <= 0) {
            this.maxSpeech = 29 * 1000;
        } else {
            this.maxSpeech = speechLength * 1000;
        }
        this.encodingType = encodingType;
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
        thread = new HandlerThread("read-audio-buffer");
        Log.d(TAG, "start audio recorder");
        thread.start();
        Handler handler = new Handler(thread.getLooper());
        readAudioBuffer = new TransferFromAudioRecorder(encodingType);
        handler.post(readAudioBuffer);
    }

    /**
     * Stops recording audio.
     */
    public void stop() {
        synchronized (lock) {
            if (thread != null) {
                stop = true;
                thread.quit();
                thread = null;
            }
            if (audioRecord != null) {
                audioRecord.stop();
                audioRecord.release();
                audioRecord = null;
            }
            Log.d(TAG, "stop audio recorder");
        }
    }

    /**
     * Retrieves the sample rate currently used to record audio.
     *
     * @return The sample rate of recorded audio.
     */
    int getSampleRate() {
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
                this.sizeInBytes = sizeInBytes;
                if (encodingType == VOICE_ENCODE_AS_FLAC) {
                    // 1 CHANNEL mono
                    // 16 ENCODING_PCM_16BIT
                    this.samplreRate = sampleRate;
                    this.sizeInBytes = sizeInBytes;
                }
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

        private LibFlac libFlac;

        public TransferFromAudioRecorder(int encodingType) {
            if (encodingType == VOICE_ENCODE_AS_FLAC) {
                libFlac = new LibFlac();
                libFlac.setFlacEncodeCallback(new LibFlac.EncoderCallback() {
                    @Override
                    public void onEncoded(byte[] data, int sized) {
                        synchronized (lock) {
                            if (eventListener != null && !stop) {
                                eventListener.onRecording(data, sized);
                            }
                        }
                    }
                });
            }
        }

        @Override
        public void run() {
            Log.d(TAG, "read from audio record buffer");
            byte[] buffer = new byte[sizeInBytes];
            String tmp = "";
            while (true) {
                tmp = tmp + stop;
                synchronized (lock) {
                    if (stop) {
                        endRecording();
                        return;
                    }
                    final int size = audioRecord.read(buffer, 0, buffer.length);
                    if (size > 0 && !stop) {
                        final long now = System.currentTimeMillis();
                        if (isHearingVoice(buffer, size)) {
                            if (voiceHeardMillis == Long.MAX_VALUE) {
                                voiceStartStartedMillis = now;
                                eventListener.onRecordStart();
                                if (VoiceRecorder.this.encodingType == VOICE_ENCODE_AS_FLAC) {
                                    libFlac.initialize(VoiceRecorder.this.getSampleRate(), 1,
                                            16, 5);
                                }
                            }
                            if (encodingType == VOICE_ENCODE_AS_FLAC) {
                                libFlac.encode(buffer);
                            } else {
                                eventListener.onRecording(buffer, size);
                            }
                            voiceHeardMillis = now;
                            if (now - voiceStartStartedMillis > maxSpeech) {
                                end();
                                endRecording();
                                return;
                            }
                        } else if (voiceHeardMillis != Long.MAX_VALUE) {
                            if (encodingType == VOICE_ENCODE_AS_FLAC) {
                                libFlac.encode(buffer);
                            } else {
                                eventListener.onRecording(buffer, size);
                            }
                            if (now - voiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
                                end();
                                endRecording();
                                return;
                            }
                        }
                    }
                }
            }
        }

        private void endRecording() {
            if (voiceHeardMillis != Long.MAX_VALUE) {
                voiceHeardMillis = Long.MAX_VALUE;
                if (encodingType == VOICE_ENCODE_AS_FLAC) {
                    libFlac.finish();
                }
                if (libFlac != null) {
                    libFlac.release();
                    libFlac = null;
                }
            }
            eventListener.onRecordEnd();
        }

        // end the record
        private void end() {
            stop();
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
