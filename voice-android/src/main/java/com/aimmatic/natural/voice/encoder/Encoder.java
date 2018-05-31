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

import okhttp3.MediaType;

/**
 * Encoder interface generic
 */
public abstract class Encoder {

    /**
     * A callback to be call later by encoder when each buffer encoded
     */
    protected EncodingReady encodingReady;

    /**
     * Set a new encoder
     *
     * @param encodingReady a callback
     */
    public void setEncodingReady(EncodingReady encodingReady) {
        if (encodingReady == null)
            throw new IllegalArgumentException("encodingReady is cannot be null");
        this.encodingReady = encodingReady;
    }

    /**
     * Initialize encoder based on the provide audio meta data
     *
     * @param audioMeta an audio meta data
     */
    public abstract void initialize(AudioMeta audioMeta);

    /**
     * Encode the wav pcm 16bit data into an another format.
     *
     * @param buffer a wav pcm 16bit buffer data
     */
    public abstract void encode(byte[] buffer, int size) throws EncodingException;

    /**
     * Finalize encoder information that the encoding operation is done, we should release any resource
     * at this point or depose it. Such as C or C++ resource should released.
     */
    public void release() {
    }

    /**
     * Encoder file extension
     *
     * @return string represent audio file extension
     */
    public abstract String extension();

    /**
     * Audio content type
     *
     * @return string present audio content type
     */
    public abstract MediaType contentType();


}
