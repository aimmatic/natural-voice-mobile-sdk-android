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

import flac.decode.FlacDecoder;
import flac.decode.StreamInfo;

/**
 * Flac encode verifier
 */
class FlacVerifier extends Verifier {

    private StreamInfo streamInfo;

    /**
     * Get new flac verifier
     * @param buf a flac binrary data
     * @return FlacVerifier
     */
    static FlacVerifier getFlacVerifier(byte[] buf) {
        FlacVerifier flacVerifier = new FlacVerifier();
        if (buf.length < 4) {
            flacVerifier.valid = false;
            return flacVerifier;
        }
        try {
            FlacDecoder fd = new FlacDecoder(buf);
            fd.decode();
            flacVerifier.streamInfo = fd.getStreamInfo();
            flacVerifier.sampleRate = flacVerifier.streamInfo.getSampleRate();
            flacVerifier.duration = (int) (flacVerifier.streamInfo.getTotalSample() / flacVerifier.streamInfo.getBitPerSecond());
            flacVerifier.duration /= 1000;
        } catch (Exception e) {
            flacVerifier.valid = false;
        }
        flacVerifier.validateHeader();
        return flacVerifier;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    boolean validateHeader() {
        if (streamInfo == null) {
            return false;
        }
        valid = streamInfo.getChannels() == 1 &&
                streamInfo.getBitPerSecond() == 16;
        return valid;
    }

}
