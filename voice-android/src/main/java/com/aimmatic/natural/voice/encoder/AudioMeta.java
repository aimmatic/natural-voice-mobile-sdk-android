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

/**
 * A basic information about audio
 */
public class AudioMeta {

    private int sampleRate;
    private int channel;
    private int bitPerSecond;

    /**
     * Create an audio meta
     *
     * @param sampleRate   audio sample rate
     * @param channel      audio channel
     * @param bitPerSecond audio data bit per second
     */
    public AudioMeta(int sampleRate, int channel, int bitPerSecond) {
        this.sampleRate = sampleRate;
        this.channel = channel;
        this.bitPerSecond = bitPerSecond;
    }

    /**
     * Set audio sample rate
     *
     * @param sampleRate a sample rate
     * @return self object
     */
    public AudioMeta setSampleRate(int sampleRate) {
        this.sampleRate = sampleRate;
        return this;
    }

    /**
     * Set number of channel
     *
     * @param channel number of channel
     * @return self object
     */
    public AudioMeta setChannel(int channel) {
        this.channel = channel;
        return this;
    }

    /**
     * Set number of bit per second
     *
     * @param bitPerSecond number of bit per second
     * @return self object
     */
    public AudioMeta setBitPerSecond(int bitPerSecond) {
        this.bitPerSecond = bitPerSecond;
        return this;
    }

    /**
     * Get audio sample rate
     *
     * @return a sample rate
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Get audio channel, 1 mean mono and 2 mean stereo other will be different
     *
     * @return integer represent audio channel
     */
    public int getChannel() {
        return channel;
    }

    /**
     * Get audio bit per second value
     *
     * @return integer represent number of bit per second
     */
    public int getBitPerSecond() {
        return bitPerSecond;
    }
}
