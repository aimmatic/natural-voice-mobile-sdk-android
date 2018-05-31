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

import com.aimmatic.natural.voice.android.LibFlac;
import com.aimmatic.natural.voice.rest.Resources;

import okhttp3.MediaType;

/**
 * A Flac audio encoder, it's encode from wav to flac format.
 * Flac naturally support streaming encoding
 */
public class FlacEncoder extends Encoder {

    private LibFlac libFlac;
    private int compression;

    /**
     * Create Flac encoder with default compression to 5
     */
    public FlacEncoder() {
        this(5);
    }

    /**
     * Create flac encoder
     *
     * @param compression a compression level range from 0 to 8
     */
    public FlacEncoder(int compression) {
        this.compression = compression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(AudioMeta audioMeta) {
        if (libFlac == null) {
            this.libFlac = new LibFlac();
            libFlac.setFlacEncodeCallback(new LibFlac.EncoderCallback() {
                @Override
                public void onEncoded(byte[] data, int sized) {
                    encodingReady.onEncoded(data, sized);
                }
            });
        }
        libFlac.initialize(audioMeta.getSampleRate(), audioMeta.getChannel(), audioMeta.getBitPerSecond(), compression);
    }

    /**
     * Encode pcm 16bit into flac audio format
     *
     * @param buffer a wav pcm 16bit buffer data
     */
    @Override
    public void encode(byte[] buffer, int size) throws EncodingException {
        if (libFlac == null) {
            throw new IllegalStateException("initialize has not been called");
        }
        libFlac.encode(buffer, size);
    }

    /**
     * Release all C & C++ resource
     */
    @Override
    public void release() {
        if (libFlac != null) {
            libFlac.finish();
            libFlac.release();
            libFlac = null;
        }
    }

    /**
     * Get flac file extension
     *
     * @return flac file extension "flac"
     */
    @Override
    public String extension() {
        return "flac";
    }

    /**
     * Get flac content type, can be use with Http Content type
     *
     * @return flac content type "audio/flac"
     */
    @Override
    public MediaType contentType() {
        return Resources.MEDIA_TYPE_FLAC;
    }

}
