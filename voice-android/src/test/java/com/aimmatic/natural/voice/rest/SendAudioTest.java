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

package com.aimmatic.natural.voice.rest;

import com.aimmatic.natural.voice.rest.response.VoiceResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import okhttp3.Response;

public class SendAudioTest {

    @Test
    public void testSendAudio() throws InterruptedException, IOException {
        TestAppContext appContext = new TestAppContext();
        VoiceSender voiceSender = new VoiceSender(appContext);
        File file = Paths.get("src/androidTest/assets/rawpcm-16bit").toFile();
        Response response = voiceSender.sentVoice(file, Resources.MEDIA_TYPE_WAVE, "en-US", 10.11, 100.11, 160000);
        //
        Assert.assertTrue("Send audio failed expect 200 got " + response.code(), response.code() == 200);
        Assert.assertTrue("Server response empty body", response.body() != null);
        Gson gson = new GsonBuilder().create();
        String body = response.body().string();
        VoiceResponse voiceResponse = gson.fromJson(body, VoiceResponse.class);
        Assert.assertTrue("Cannot decode response to Voice response object", voiceResponse != null);
        Assert.assertTrue("Server response with no audio id", voiceResponse.getID() != null);
        Assert.assertTrue("Server response with no voice result", voiceResponse.getVoiceResult() != null);
        Assert.assertTrue("Server response with 0 confidence", voiceResponse.getVoiceResult().getConfidence() != 0);
    }

}
