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

import com.aimmatic.natural.core.rest.AppContext;

import java.io.File;
import java.io.IOException;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * This class represent a rest client which use to send a byte wave audio data to Placenext Api.
 */

public class VoiceSender {

    private AppContext appContext;

    /**
     * Create a voice sender from the given app context
     *
     * @param appContext a app context which provide a way to get the ApiKey
     */
    public VoiceSender(AppContext appContext) {
        this.appContext = appContext;
    }

    /**
     * Send a voice to Placenext Api. The binary must be a wav format of PCM 16 bits.
     * This method execute synchronous mode so don't use this method if you're on main thread
     * or UI Thread.
     *
     * @param file       a binary file audio in format of PCM 16 bit
     * @param deviceLang a default language of the device
     * @param lat        a latitude of the device, this value is optional
     * @param lng        a longitude of the device, this value is optional
     * @return Response from Placenext Api
     * @throws IOException
     */
    public Response sentVoice(File file, String deviceLang, double lat, double lng, int sampleRate) throws IOException {
        OkHttpClient client = appContext.getOkHttpClient();
        MultipartBody.Builder buidler = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("uploadFile", "natural-voice.wav", RequestBody.create(Resources.MEDIA_TYPE_WAVE, file))
                .addFormDataPart("deviceLanguage", deviceLang)
                .addFormDataPart("sampleRate", "" + sampleRate);
        // don't add it lat,lng 0
        if (lat != 0 && lng != 0) {
            buidler = buidler.addFormDataPart("deviceLocation", lat + "," + lng);
        }
        RequestBody requestBody = buidler.build();

        Request request = new Request.Builder()
                .url(appContext.getHost() + Resources.ApiVersion + Resources.NaturalVoice)
                .post(requestBody)
                .build();
        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        return response;
    }

}
