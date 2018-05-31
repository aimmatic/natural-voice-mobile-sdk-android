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

package com.aimmatic.natural.voice.encoder;

import com.aimmatic.natural.voice.rest.Resources;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import okhttp3.MediaType;

/**
 * Wave encoder, this class just does include any specific encode algorithm
 * Current it keep the origin buffer data format of PCM 16bit.
 */
public class WavEncoder extends Encoder {

    /**
     * Create wav header
     */
    @Override
    public void initialize(AudioMeta audioMeta) {
        byte[] littleBytes = ByteBuffer
                .allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort((short) audioMeta.getChannel())
                .putInt(audioMeta.getSampleRate())
                .putInt(audioMeta.getSampleRate() * audioMeta.getChannel() * (audioMeta.getBitPerSecond() / 8))
                .putShort((short) (audioMeta.getChannel() * (audioMeta.getBitPerSecond() / 8)))
                .putShort((short) audioMeta.getBitPerSecond())
                .array();
        byte[] buffer = new byte[]{
                // RIFF header
                'R', 'I', 'F', 'F', // ChunkID
                0, 0, 0, 0, // ChunkSize (0 for now since backend pretty much does care)
                'W', 'A', 'V', 'E', // Format
                // fmt subchunk
                'f', 'm', 't', ' ', // Subchunk1ID
                16, 0, 0, 0, // Subchunk1Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // NumChannels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                littleBytes[10], littleBytes[11], // BlockAlign
                littleBytes[12], littleBytes[13], // BitsPerSample
                // data subchunk
                'd', 'a', 't', 'a', // Subchunk2ID
                0, 0, 0, 0, // Subchunk2Size (0 for now since backend pretty much does care)
        };
        this.encodingReady.onEncoded(buffer, buffer.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void encode(byte[] buffer, int size) {
        this.encodingReady.onEncoded(buffer, size);
    }


    /**
     * Get a string wav extension
     *
     * @return a string of wave audio extension "wav"
     */
    @Override
    public String extension() {
        return "wav";
    }

    /**
     * Get a string represent audio content type, can be use with Http Request
     *
     * @return a okhttp media type of wave audio content type "audio/wav"
     */
    @Override
    public MediaType contentType() {
        return Resources.MEDIA_TYPE_WAVE;
    }

}
