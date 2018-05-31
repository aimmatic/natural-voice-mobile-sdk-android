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

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Random;

/**
 * Mock audio recorder class
 */
abstract class MockAudioRecord {

    private InputStream rawData;
    private InputStream rawHeader;
    private int sampleRate;
    private int totalDuration;
    private int totalByte;

    // get new voice recorder
    VoiceRecorder getVoiceRecorder(Context appContext, RecordStrategy recordStrategy,
                                   final int expectedDuration, final int maxDuration, final int minDuration,
                                   boolean speakTimeOut, boolean stop) throws IOException {
        // sample data
        final int sampleRate = 16000;
        final int sizeBuffer = 1280;
        rawData = appContext.getAssets().open("rawpcm-16bit");
        rawHeader = appContext.getAssets().open("rawpcm-header");
        this.sampleRate = sampleRate;
        this.totalByte += rawHeader.available();
        final int duration = (rawData.available() / (sampleRate * 2));
        final int delayRate = 1000 / ((rawData.available() / duration) / sizeBuffer);
        final int amplify = 20;
        Random random = new Random();
        // delay for speech timeout between 10sec and Max duration
        final int delaySpeechTimeout = speakTimeOut ? random.nextInt(maxDuration + 1 - minDuration) + minDuration : -1;
        final int randomStop = stop ? random.nextInt(maxDuration + 1 - minDuration) + minDuration : -1;
        //
        final VoiceRecorder voiceRecorder = new VoiceRecorder(recordStrategy) {
            @Override
            AudioRecord createAudioRecord() {
                int rate = sampleRate, size = sizeBuffer;
                for (int sampleRate : SAMPLE_RATE_CANDIDATES) {
                    final int sizeInBytes = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                    if (sizeInBytes == AudioRecord.ERROR_BAD_VALUE) {
                        continue;
                    }
                    rate = sampleRate;
                    size = sizeInBytes;
                    break;
                }
                return new AudioRecord(MediaRecorder.AudioSource.MIC,
                        rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size) {

                    @Override
                    public void startRecording() throws IllegalStateException {
                        // Do nothing here prevent instrument test failed to start record
                    }

                    @Override
                    public void stop() throws IllegalStateException {
                        // Do nothing here prevent instrument test failed to start record
                    }

                    @Override
                    public int read(@NonNull byte[] audioData, int offsetInBytes, int sizeInBytes) {
                        try {
                            if (delaySpeechTimeout == -1) {
                                Thread.sleep(delayRate / amplify);
                            } else {
                                Thread.sleep(delayRate);
                            }
                            totalDuration += delayRate;
                            if (delaySpeechTimeout != -1 && totalDuration > delaySpeechTimeout) {
                                // delay 2sec to break the read
                                Arrays.fill(audioData, (byte) 0);
                                totalByte += audioData.length;
                                return sizeInBytes;
                            }
                            if (randomStop != -1 && totalDuration > randomStop) {
                                onStop();
                                return 0;
                            }
                            int size = rawData.read(audioData, offsetInBytes, sizeInBytes);
                            if (size < sizeInBytes ||
                                    (expectedDuration != -1 && totalDuration > (expectedDuration * 1000))) {
                                // the end of talk
                                onStop();
                            } else {
                                totalByte += size;
                            }
                            return size;
                        } catch (Exception e) {
                            onStop();
                            return 0;
                        }
                    }
                };
            }
        };
        voiceRecorder.audioMeta.setSampleRate(sampleRate);
        voiceRecorder.sizeInBytes = sizeBuffer;
        return voiceRecorder;
    }

    void close() throws IOException {
        rawData.close();
        rawHeader.close();
    }

    /**
     * Get sample rate, fixed to 16000 based on wav tested data
     *
     * @return mock sample rate
     */
    int getSampleRate() {
        return sampleRate;
    }

    /**
     * Get total duration of record
     *
     * @return total duration
     */
    int getTotalDuration() {
        return totalDuration;
    }

    /**
     * Get total byte recorded
     *
     * @return raw PCM byte wav that have been read
     */
    int getTotalByte() {
        return totalByte;
    }

    /**
     * Invoke when record stop
     */
    abstract void onStop();

}
