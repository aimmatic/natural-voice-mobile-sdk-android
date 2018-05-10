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
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import com.aimmatic.natural.oauth.AccessToken;
import com.aimmatic.natural.oauth.Profile;
import com.google.gson.Gson;

import okhttp3.OkHttpClient;

/**
 * This class implement app context and use in Android platform
 */

public class AndroidAppContext implements AppContext {

    private static final String pref = "AimMaticPref";
    private static final String token = "AimMaticPref-Token";
    private static final String refreshToken = "AimMaticPref-RefreshToken";
    private static final String userProfile = "AimMaticPref-UserProfile";
    private static final String currentAppId = "AimMaticPref-AppId";

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

    /**
     * {@inheritDoc}
     */
    @Override
    public AccessToken getAccessToken() {
        SharedPreferences sp = context.getSharedPreferences(pref, Context.MODE_PRIVATE);
        AccessToken accessToken = new AccessToken(
                sp.getString(token, null), sp.getString(refreshToken, null));
        return (accessToken.getToken() == null) ? null : accessToken;
    }

    /**
     * Save access token to a share preference
     *
     * @param accessToken user's access token
     */
    public void saveAccessToken(AccessToken accessToken) {
        SharedPreferences sp = context.getSharedPreferences(pref, Context.MODE_PRIVATE);
        sp.edit().putString(token, accessToken.getToken()).putString(refreshToken, accessToken.getRefreshToken()).apply();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Profile getProfile() {
        SharedPreferences sp = context.getSharedPreferences(pref, Context.MODE_PRIVATE);
        String jsonProfile = sp.getString(userProfile, null);
        if (jsonProfile != null) {
            try {
                return new Gson().fromJson(jsonProfile, Profile.class);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Save user's profile
     *
     * @param profile user profile
     */
    public void saveUserProfile(Profile profile) {
        SharedPreferences sp = context.getSharedPreferences(pref, Context.MODE_PRIVATE);
        String jsonProfile = new Gson().toJson(profile);
        sp.edit().putString(userProfile, jsonProfile).apply();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAppId(String appId) {
        SharedPreferences sp = context.getSharedPreferences(pref, Context.MODE_PRIVATE);
        sp.edit().putString(currentAppId, appId).apply();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAppId() {
        SharedPreferences sp = context.getSharedPreferences(pref, Context.MODE_PRIVATE);
        return sp.getString(currentAppId, null);
    }

}
