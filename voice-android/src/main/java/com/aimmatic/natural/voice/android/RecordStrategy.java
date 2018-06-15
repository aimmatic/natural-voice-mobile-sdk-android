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

import com.aimmatic.natural.voice.encoder.Encoder;
import com.aimmatic.natural.voice.encoder.FlacEncoder;
import com.aimmatic.natural.voice.rest.Language;

/**
 * The record strategy define a rule how the recording should work
 */
public class RecordStrategy {

    /**
     * This policy define an action that any recording will be canceled if certain criteria are met
     * without notice the user to make a decision. For example, if user stop speaking, see {@link RecordStrategy#setSpeechTimeout(int)},
     * the recording will be stopped after reaching the speech timeout and audio will not sent to the cloud.
     */
    public static final byte POLICY_CANCELED = 1;

    /**
     * This policy define an action that any recording will be processed forward if certain criteria are met
     * without notice the user to make a decision. For example, if user stop speaking, see {@link RecordStrategy#setSpeechTimeout(int)},
     * the recording will be stopped after reaching the speech timeout and audio will be sent to the cloud.
     */
    public static final byte POLICY_SEND_IMMEDIATELY = 2;

    /**
     * This policy define an action that any recording will be stopped and the process is paused if certain criteria are met
     * and the user have to make a decision. For example, if user stop speaking, see {@link RecordStrategy#setSpeechTimeout(int)},
     * the recording will be stopped after reaching the speech timeout and a callback will be called to inform user to make a decision.
     */
    public static final byte POLICY_USER_CHOICE = 3;

    private Encoder encoder;
    private int[] sampleRatesCandidate;
    private int speechTimeout;
    private int maxRecordDuration;
    private byte speechTimeoutPolicies;
    private byte maxRecordDurationPolicies;
    private Language language;

    /**
     * Create record strategy
     */
    public RecordStrategy() {
        encoder = new FlacEncoder();
        sampleRatesCandidate = VoiceRecorder.SAMPLE_RATE_CANDIDATES;
        speechTimeout = VoiceRecorder.SPEECH_TIMEOUT_MILLIS;
        speechTimeoutPolicies = POLICY_USER_CHOICE;
        maxRecordDuration = 1 * 29 * 1000;  // 59s in millisecond
        maxRecordDurationPolicies = POLICY_USER_CHOICE;
    }

    /**
     * Set encoder to encoder the audio data
     *
     * @param encoder a class implement Encoder
     * @return a record strategy object
     * @see {@link com.aimmatic.natural.voice.encoder.Encoder}
     * @see {@link com.aimmatic.natural.voice.encoder.WavEncoder}
     * @see {@link com.aimmatic.natural.voice.encoder.FlacEncoder}
     */
    public RecordStrategy setEncoder(Encoder encoder) {
        this.encoder = encoder;
        return this;
    }

    /**
     * Set speech timeout in millisecond. This duration use to measure and terminate or stop the on going
     * recording after we don't hear the voice from user
     *
     * @param speechTimeout a speech timeout in millisecond
     * @return a record strategy object
     */
    public RecordStrategy setSpeechTimeout(int speechTimeout) {
        this.speechTimeout = speechTimeout;
        return this;
    }

    /**
     * Set speech timeout policy, this policy define a behavior when the speech timeout occurs.
     *
     * @param speechTimeoutPolicies a policy to be use when a timeout occurs
     * @return a record strategy object
     * @see {@link #POLICY_CANCELED}
     * @see {@link #POLICY_SEND_IMMEDIATELY}
     * @see {@link #POLICY_USER_CHOICE}
     */
    public RecordStrategy setSpeechTimeoutPolicies(byte speechTimeoutPolicies) {
        this.speechTimeoutPolicies = speechTimeoutPolicies;
        return this;
    }

    /**
     * Set a maximum duration to record the audio. By default, it set to 29 second. The maximum duration
     * that can be recording 59 second. You can set any value between 0s to 59s. If you set value longer than
     * 59 second, the {@link InvalidRecordStrategy} is raised
     *
     * @param maxRecordDuration a duration in millisecond
     * @return a record strategy object
     */
    public RecordStrategy setMaxRecordDuration(int maxRecordDuration) throws InvalidRecordStrategy {
        this.maxRecordDuration = maxRecordDuration;
        if (maxRecordDuration > 59 * 1000) {
            throw new InvalidRecordStrategy("Maximum record duration out of range. Duration can be set between 0s to 59s");
        }
        return this;
    }

    /**
     * Set max record duration policy, this policy define a behavior when the recording reach a maximum duration allowed
     * either by developer define or default duration 29s.
     *
     * @param maxRecordDurationPolicies a policy to be use when a record reach maximum allowed duration
     * @return a record strategy object
     * @see {@link #POLICY_CANCELED}
     * @see {@link #POLICY_SEND_IMMEDIATELY}
     * @see {@link #POLICY_USER_CHOICE}
     */
    public RecordStrategy setMaxRecordDurationPolicies(byte maxRecordDurationPolicies) {
        this.maxRecordDurationPolicies = maxRecordDurationPolicies;
        return this;
    }

    /**
     * Set a language that by the user
     *
     * @param language language speech by user
     * @return a record strategy object
     */
    public RecordStrategy setLanguage(Language language) {
        this.language = language;
        return this;
    }

    /**
     * Get audio encoder
     *
     * @return current Encoder
     * @see {@link Encoder}
     */
    public Encoder getEncoder() {
        return encoder;
    }

    /**
     * Get a list of sample rate
     *
     * @return current prefer sample rate
     */
    public int[] getSampleRatesCandidate() {
        return sampleRatesCandidate;
    }

    /**
     * Get a speech timeout in millisecond
     *
     * @return current speech timeout
     */
    public int getSpeechTimeout() {
        return speechTimeout;
    }

    /**
     * Get speech timeout policy
     *
     * @return current speech timeout policy
     */
    public byte getSpeechTimeoutPolicies() {
        return speechTimeoutPolicies;
    }

    /**
     * Get maximum duration allowed
     *
     * @return current maximum duration
     */
    public int getMaxRecordDuration() {
        return maxRecordDuration;
    }

    /**
     * Get maximum duration policy
     *
     * @return current maximum duration policy
     */
    public byte getMaxRecordDurationPolicies() {
        return maxRecordDurationPolicies;
    }

    /**
     * Get speech language set by either user or developer
     *
     * @return current speech language
     */
    public Language getLanguage() {
        return language;
    }
}
