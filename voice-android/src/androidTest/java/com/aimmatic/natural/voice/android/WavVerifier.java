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

import android.util.Log;

import java.util.Arrays;

/**
 * Wav encoder verifier
 */
public class WavVerifier extends Verifier {

    private byte[] chunkID = new byte[4];
    private int chunkSize;
    private byte[] format = new byte[4];
    private byte[] subChunk1ID = new byte[4];
    private int subChunk1Size;
    private short audioFormat;
    private short numChannels;
    private int byteRate;
    private short blockAlign;
    private short bitsPerSample;
    private byte[] subChunk2ID = new byte[4];
    private int subChunk2Size;

    /**
     * Get new wav verifier
     * @param buf a wav binrary data
     * @return WavVerifier
     */
    static WavVerifier getWavVerifier(byte[] buf) {
        WavVerifier wavVerifier = new WavVerifier();
        if (buf.length < 44) {
            wavVerifier.valid = false;
            return wavVerifier;
        }
        wavVerifier.chunkID = Arrays.copyOfRange(buf, 0, 4);
        wavVerifier.chunkSize = toInt(buf, 4);
        wavVerifier.format = Arrays.copyOfRange(buf, 8, 12);
        wavVerifier.subChunk1ID = Arrays.copyOfRange(buf, 12, 16);
        wavVerifier.subChunk1Size = toInt(buf, 16);
        wavVerifier.audioFormat = toShort(buf, 20);
        wavVerifier.numChannels = toShort(buf, 22);
        wavVerifier.sampleRate = toInt(buf, 24);
        wavVerifier.byteRate = toInt(buf, 28);
        wavVerifier.blockAlign = toShort(buf, 32);
        wavVerifier.bitsPerSample = toShort(buf, 34);
        wavVerifier.subChunk2ID = Arrays.copyOfRange(buf, 36, 40);
        wavVerifier.subChunk2Size = toInt(buf, 40);
        wavVerifier.validateHeader();
        Log.d(">>>", "--> " + (buf.length - 44));
        wavVerifier.duration = (buf.length - 44) / (wavVerifier.sampleRate * 2);
        return wavVerifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean validateHeader() {
        valid = Arrays.equals(chunkID, new byte[]{(byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F'}) &&
                chunkSize == 0 &&   // 0 for now. backend does not care
                Arrays.equals(format, new byte[]{(byte) 'W', (byte) 'A', (byte) 'V', (byte) 'E'}) &&
                Arrays.equals(subChunk1ID, new byte[]{(byte) 'f', (byte) 'm', (byte) 't', (byte) ' '}) &&
                subChunk1Size == 16 &&
                audioFormat == 1 &&
                numChannels == 1 &&
                bitsPerSample == 16 &&
                byteRate == (sampleRate * numChannels * (bitsPerSample / 8)) &&
                blockAlign == (numChannels * (bitsPerSample / 8)) &&
                Arrays.equals(subChunk2ID, new byte[]{(byte) 'd', (byte) 'a', (byte) 't', (byte) 'a'}) &&
                subChunk2Size == 0;
        return valid;
    }

}
