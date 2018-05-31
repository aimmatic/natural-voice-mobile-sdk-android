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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Abstract Verifier
 */
abstract class Verifier {

    int duration;
    boolean valid;
    int sampleRate;

    /**
     * Convert 4 byte into integer. (4 bytes)
     *
     * @param buf   a buffer
     * @param start a start of byte to convert
     * @return integer
     */
    static int toInt(byte[] buf, int start) {
        ByteBuffer buffer = ByteBuffer.wrap(buf, start, 4).order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getInt();
    }

    /**
     * Convert 4 byte into short. (2 bytes)
     *
     * @param buf   a buffer
     * @param start a start of byte to convert
     * @return short
     */
    static short toShort(byte[] buf, int start) {
        ByteBuffer buffer = ByteBuffer.wrap(buf, start, 2).order(ByteOrder.LITTLE_ENDIAN);
        return buffer.getShort();
    }

    /**
     * Check if binary media is valid
     *
     * @return true if valid otherwise false
     */
    boolean isValid() {
        return valid;
    }

    /**
     * Get media sample rate
     *
     * @return sample rate
     */
    int getSampleRate() {
        return sampleRate;
    }

    /**
     * Get duration of media
     *
     * @return duration in second
     */
    int getDuration() {
        return duration;
    }

    /**
     * Verify if binary header is valid
     *
     * @return true if valid otherwise false
     */
    abstract boolean validateHeader();

}
