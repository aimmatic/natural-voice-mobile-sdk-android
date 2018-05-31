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

import com.aimmatic.natural.voice.rest.response.LangResponse;
import com.google.gson.Gson;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class GetLangTest {

    @Test
    public void testGetLanguage() throws InterruptedException, IOException {
        final AsyncWait cdl = new AsyncWait(1);
        TestAppContext appContext = new TestAppContext();
        Language.loadLanguage(appContext, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                cdl.failed = true;
                cdl.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    String resp = response.body().string();
                    LangResponse lr = new Gson().fromJson(resp, LangResponse.class);
                    cdl.response = new Gson().toJson(lr.getLanguages());
                    cdl.failed = false;
                } else {
                    cdl.failed = true;
                }
                cdl.countDown();
            }
        });
        cdl.await();
        Assert.assertFalse("Loading language failed", cdl.failed);
        File file = new File(appContext.getDataDir(), "aimmatic-speech-lang.json");
        Assert.assertTrue("File not existed", file.exists());
        FileInputStream in = new FileInputStream(file);
        byte[] bf = new byte[in.available()];
        in.read(bf);
        in.close();
        Assert.assertTrue("Response is not identical", new String(bf).compareTo((String) cdl.response) == 0);
    }

}
