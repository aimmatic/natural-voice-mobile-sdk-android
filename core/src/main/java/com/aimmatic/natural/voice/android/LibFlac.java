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

import android.os.AsyncTask;

/**
 * Created by veasna on 2/2/18.
 */

public class LibFlac {

    static {
        System.loadLibrary("flacJNI");
    }

    public static final int STATE_UNINITIALIZED = 0;

    public static final int STATE_INITIALIZED = 1;

    public static final int RECORDSTATE_STOPPED = 1;

    public static final int RECORDSTATE_RECORDING = 3;

    /**
     *
     */
    public interface EncoderCallback {
        void onEncoded(byte[] data, int sized);
    }

    private long cPointer;
    private int channel;
    private int state = STATE_UNINITIALIZED;
    private EncoderCallback encoderCallback;

    /**
     * @param sampleRate
     * @param channel
     * @param bps
     * @param totalSample
     * @param compressLevel
     * @throws IllegalStateException
     */
    public void initialize(int sampleRate, int channel, int bps, int totalSample, int compressLevel) throws IllegalStateException {
        this.channel = channel;
        this.cPointer = this.init();
        state = STATE_INITIALIZED;
        if (setMetadata(cPointer, sampleRate, channel, bps, totalSample, compressLevel) == 0) {
            throw new IllegalStateException("unable to set wave format metadata.");
        }
    }

    public void setFlacEncodeCallback(EncoderCallback encodeCallback) {
        this.encoderCallback = encodeCallback;
    }

    /**
     * @param buffer
     * @return
     */
    public void encode(byte[] buffer) {
        if (!this.encode(cPointer, channel, buffer)) {
            throw new IllegalArgumentException("buffer encode byte array must be smaller or equal 1024 byte");
        }
    }

    /**
     * @param data
     * @param size
     */
    void onEncoded(byte[] data, int size) {
        if (encoderCallback != null) {
            encoderCallback.onEncoded(data, size);
        }
    }

    /**
     *
     */
    public void release() {
        if (state != STATE_UNINITIALIZED) {
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... voids) {
                    LibFlac.this.release(cPointer);
                    return null;
                }
            }.execute();
        }
    }

    /**
     *
     */
    public void finish() {
        this.finish(cPointer);
    }

    /**
     * @param cPointer
     */
    private native void release(long cPointer);

    /**
     *
     * @param cPointer
     */
    private native void finish(long cPointer);

    /**
     * @param cPointer
     * @param in
     */
    private native boolean encode(long cPointer, int channel, byte[] in);

    /**
     * @return
     */
    private native long init();

    /**
     * @param cPointer
     * @param sampleRate
     * @param channel
     * @param bps
     * @param totalSample
     * @param compressLevel
     * @return
     */
    private native int setMetadata(long cPointer, int sampleRate, int channel, int bps, int totalSample, int compressLevel);

}
