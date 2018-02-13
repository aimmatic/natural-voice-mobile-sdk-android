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
 * LibFlac java native jni interface to convert raw wav pcm 16 bit binary into flac format.
 */

public class LibFlac {

    static {
        System.loadLibrary("flacJNI");
    }

    /**
     * State flac list hasn't initialized yet
     */
    public static final int STATE_UNINITIALIZED = 0;

    /**
     * State flac has initialized
     */
    public static final int STATE_INITIALIZED = 1;

    /**
     * Encoder callback
     */
    public interface EncoderCallback {
        /**
         * call when flac successfully encode wav to a binary flac.
         *
         * @param data  flac binary file data
         * @param sized size of data in byte
         */
        void onEncoded(byte[] data, int sized);
    }

    private long cPointer;
    private int channel;
    private int state = STATE_UNINITIALIZED;
    private EncoderCallback encoderCallback;

    /**
     * initialize the flac lib.
     *
     * @param sampleRate    sample rate of audio
     * @param channel       channel of audio like digital or mono
     * @param bps           bit per second. Usually 16
     * @param compressLevel a compression level for flac
     * @throws IllegalStateException throw when configuration is not valid
     */
    public void initialize(int sampleRate, int channel, int bps, int compressLevel) throws IllegalStateException {
        this.channel = channel;
        this.cPointer = this.init();
        state = STATE_INITIALIZED;
        if (setMetadata(cPointer, sampleRate, channel, bps, compressLevel) == 0) {
            throw new IllegalStateException("unable to set wave format metadata.");
        }
    }

    /**
     * Set Flac encode callback
     *
     * @param encodeCallback flac encoder callback
     */
    public void setFlacEncodeCallback(EncoderCallback encodeCallback) {
        this.encoderCallback = encodeCallback;
    }

    /**
     * Send a wav PCM 16 bit raw data
     *
     * @param buffer a binary of raw wav pcm
     */
    public void encode(byte[] buffer) {
        if (!this.encode(cPointer, channel, buffer)) {
            throw new IllegalArgumentException("buffer encode byte array must be smaller or equal 1024 byte");
        }
    }

    /**
     * Called by native c/c++ code when encode to flac is done.
     *
     * @param data flac binary data
     * @param size size of data in byte
     */
    void onEncoded(byte[] data, int size) {
        if (encoderCallback != null) {
            encoderCallback.onEncoded(data, size);
        }
    }

    /**
     * free native resource
     */
    public void release() {
        if (state != STATE_UNINITIALIZED) {
            new ReleaseTask(this).execute();
        }
    }

    /**
     * mark encode as done. Flac lib will send addition data to finalize the flac binary data.
     */
    public void finish() {
        this.finish(cPointer);
    }

    /**
     * convert raw wave pcm into flac file. Input file must be a raw pcm, a wave raw data with header format.
     * In Android this raw PCM should take directly from AudioRecode class in Java.
     *
     * @param fileIn       path to a file with a raw pcm binary data
     * @param fileOut      a path where flac should be written to
     * @param channel      a number of channel of raw pcm. e.g Mono or Stereo
     * @param sampleRate   a sample rate of raw pcm data
     * @param bitPerSecond a number of bit per second
     * @param compression  flac compression level
     */
    public static native void convertRawPCM(String fileIn, String fileOut,
                                            int channel, int sampleRate, int bitPerSecond, int compression);

    /*
     * release native resource with given pointer
     */
    private native void release(long cPointer);

    /*
     * finish encode resource with given pointer
     */
    private native void finish(long cPointer);

    /*
     * encode the wav to flac using native code by passing wave binary data, number of channel
     * and the reference to C pointer.
     */
    private native boolean encode(long cPointer, int channel, byte[] in);

    /*
     * initialize flac native resource
     */
    private native long init();

    /*
     * set metadata of current audio wave
     */
    private native int setMetadata(long cPointer, int sampleRate, int channel, int bps, int compressLevel);

    private static class ReleaseTask extends AsyncTask<Void, Void, Void> {
        private LibFlac libFlac;

        ReleaseTask(LibFlac libFlac) {
            this.libFlac = libFlac;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            this.libFlac.release(libFlac.cPointer);
            return null;
        }
    }

}
