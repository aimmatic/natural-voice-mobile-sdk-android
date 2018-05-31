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
import com.aimmatic.natural.core.rest.Interceptor;
import com.aimmatic.natural.oauth.AccessToken;
import com.aimmatic.natural.oauth.Profile;

import java.io.File;

import okhttp3.OkHttpClient;

/**
 * Test context
 */
public class TestAppContext implements AppContext {

    private static final String ENV_HOST = "AIMMATIC_HOST";
    private static final String ENV_APIKEY = "AIMMATIC_APIKEY";

    OkHttpClient okHttpClient;
    Profile profile;
    String accessToken;
    String refreshToken;
    String appId;
    String customerId;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHost() {
        return System.getenv(ENV_HOST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApiKey() throws Exception {
        return System.getenv(ENV_APIKEY);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            okHttpClient = new OkHttpClient().newBuilder()
                    .addInterceptor(new Interceptor(this)).build();
        }
        return okHttpClient;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken getAccessToken() {
        if (accessToken == null && refreshToken == null) return null;
        return new AccessToken(accessToken, refreshToken);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Profile getProfile() {
        return profile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAppId(String appId) {
        this.appId = appId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAppId() {
        return appId;
    }

    @Override
    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCustomerId() {
        return customerId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public File getDataDir() {
        return new File("/tmp/");
    }

}
