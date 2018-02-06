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

package com.aimmatic.natural.core.rest;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import okhttp3.OkHttpClient;

/**
 * This class implement app context and use in Android platform
 */

public class AndroidAppContext implements AppContext {

    private Context context;
    private OkHttpClient okHttpClient;

    /**
     * Create Android App Context that return ApiKey from Android Manifest
     */
    public AndroidAppContext(Context context) {
        this.context = context;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHost() {
        return host;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApiKey() throws Exception {
        ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
        String apiKey = appInfo.metaData.getString(apikey);
        if (apiKey == null) {
            throw new Resources.NotFoundException();
        }
        return apiKey;
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

}