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

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.rule.ServiceTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.aimmatic.natural.voice.encoder.FlacEncoder;
import com.aimmatic.natural.voice.rest.Language;
import com.aimmatic.natural.voice.rest.response.VoiceResponse;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
public class ServiceRecorderFlacEncodeTest {

    @Rule
    public final ServiceTestRule serviceRule = new ServiceTestRule();

    // required Android API 23+
    public final GrantPermissionRule grantPermissionRule = GrantPermissionRule
            .grant(Manifest.permission.RECORD_AUDIO);


    private VoiceRecorderService.VoiceRecorderCallback callback = new VoiceRecorderService.VoiceRecorderCallback() {
        @Override
        public void onVoiceSent(VoiceResponse response) {
            super.onVoiceSent(response);
        }
    };

    @Test
    public void testFlacRecordFullDuration() throws Exception {
        testFlacRecordService(serviceRule, "Max 59 ", 59,
                59 * 1000, 59 * 1000, false, false);
        testFlacRecordService(serviceRule, "Max 29 ", 29,
                29 * 1000, 29 * 1000, false, false);
    }

    @Test
    public void testFlacRecordRandomStop() throws Exception {
        testFlacRecordService(serviceRule, "Max 29 speech timeout ", -1,
                29 * 1000, 10 * 1000, true, false);
        testFlacRecordService(serviceRule, "Max 29 random stop ", -1,
                29 * 1000, 10 * 1000, false, true);
    }

    /**
     * @param serviceRule      service rule for this test case
     * @param tcase            a string define test case
     * @param expectedDuration a duration in second
     * @param maxDuration      a duration in millisecond
     * @param minDuration      a duration in millisecond
     * @param speakTimeOut     a duration in millisecond
     * @param stop             a state for random stop
     * @throws Exception
     */
    void testFlacRecordService(ServiceTestRule serviceRule, String tcase,
                               final int expectedDuration, final int maxDuration, final int minDuration,
                               boolean speakTimeOut, boolean stop) throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();
        // Create the service Intent.
        Intent serviceIntent = new Intent(appContext, VoiceRecorderService.class);
        // Bind the service and grab a reference to the binder.
        IBinder binder = serviceRule.bindService(serviceIntent);
        final VoiceRecorderService service = VoiceRecorderService.from(binder);
        //
        RecordStrategy recordStrategy = new RecordStrategy()
                .setEncoder(new FlacEncoder())
                .setMaxRecordDuration(maxDuration)
                // speech timeout 2sec
                .setSpeechTimeout(2000)
                // no need to send data just make audio data from recording is correct
                .setSpeechTimeoutPolicies(RecordStrategy.POLICY_CANCELED)
                // no need to send data just make audio data from recording is correct
                .setMaxRecordDurationPolicies(RecordStrategy.POLICY_CANCELED)
                // does not use either
                .setLanguage(Language.getLanguage(appContext, "en-US"));
        service.addListener(callback);
        //
        final MockAudioRecord mockAudioRecord = new MockAudioRecord() {
            @Override
            void onStop() {
                service.stopRecordVoice(RecordStrategy.POLICY_CANCELED);
            }
        };
        //
        final CountDownLatch object = new CountDownLatch(1);
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        service.addListener(new VoiceRecorderService.VoiceRecorderCallback() {
            @Override
            public void onRecording(byte[] data, int size) {
                bout.write(data, 0, size);
            }

            @Override
            public void onRecordEnd(byte state) {
                try {
                    mockAudioRecord.close();
                } catch (IOException e) {
                }
                object.countDown();
            }
        });
        // start recording
        service.startTestRecordVoice(recordStrategy, mockAudioRecord.getVoiceRecorder(
                appContext, recordStrategy, expectedDuration, maxDuration, minDuration, speakTimeOut, stop
        ));
        object.await();
        serviceRule.unbindService();
        //
        byte[] data = bout.toByteArray();
        bout.close();
        FlacVerifier flacVerifier = FlacVerifier.getFlacVerifier(data);
        // testing
        Assert.assertFalse(tcase + "Empty record data", data.length == 0);
        // TODO: estimate size of wav to flac
        // Assert.assertTrue(tcase + "Wrong record data size expect " + mockAudioRecord.getTotalByte() + " got " + data.length, data.length == mockAudioRecord.getTotalByte());
        Assert.assertTrue(tcase + "Wrong wav header", flacVerifier.isValid());
        Assert.assertTrue(tcase + "Wrong sample rate expect " + mockAudioRecord.getSampleRate() + " got " + flacVerifier.getSampleRate(), flacVerifier.getSampleRate() == mockAudioRecord.getSampleRate());
        int verifyDuration = expectedDuration;
        if (expectedDuration == -1) {
            verifyDuration = mockAudioRecord.getTotalDuration() / 1000;
        }
        Assert.assertTrue(tcase + "Wrong duration expect " + verifyDuration + " got " + flacVerifier.getDuration(), flacVerifier.getDuration() == verifyDuration);
    }

}
