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

package com.aimmatic.natural.oauth;

import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * A base class for Oauth2 and User API. It's provide a function to exchange a token, retrieve
 * new token before expired and get a user profile.
 */
public class AimMatic {

    /**
     * AimMatic account service
     */
    public static final String service = "https://account.aimmatic.com";

    /**
     * Fetch token retrieve a token from an exchange code after user successfully logged using
     * AimMatic account.
     *
     * @param code     a valid exchange code
     * @param apiKey   a valid api key
     * @param callback a callback function to notice when request is success or failure
     */
    public static void fetchToken(String code, String apiKey, final Callback<AccessToken> callback) {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(String.format("%s/v1/exchange?code=%s", service, code))
                .addHeader("Authorization", "AimMatic " + apiKey)
                .build();
        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    TokenResponse tokenResponse = new Gson().fromJson(response.body().string(), TokenResponse.class);
                    callback.onSuccess(tokenResponse.getAccessToken());
                } catch (Exception e) {
                    if (response.code() != 200) {
                        callback.onError(new Exception("Server reply with status " + response.code()));
                    } else {
                        callback.onError(e);
                    }
                }
            }
        });
    }

    /**
     * Renew token process an exchange request using refresh token to get a fresh new token.
     * This method must be called from a background thread.
     *
     * @param refreshToken a valid refresh token
     * @return access token contain token and refresh token
     * @throws IOException
     */
    public static AccessToken renewToken(String refreshToken) throws IOException {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(String.format("%s/v1/exchange/refresh", service))
                .addHeader("Authorization", "Bearer " + refreshToken)
                .build();
        Response response = okHttpClient.newCall(request).execute();
        TokenResponse tokenResponse = new Gson().fromJson(response.body().string(), TokenResponse.class);
        return tokenResponse.getAccessToken();
    }

    /**
     * Fetch user profile
     *
     * @param token    a valid token
     * @param callback a callback function to notice when request is success or failure
     */
    public static void fetchUserProfile(String token, final Callback<Profile> callback) {
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request.Builder()
                .url(String.format("%s/v1/profile", service))
                .addHeader("Authorization", "Bearer " + token)
                .build();
        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    ProfileResponse profileResponse = new Gson().fromJson(response.body().string(), ProfileResponse.class);
                    callback.onSuccess(profileResponse.getProfile());
                } catch (Exception e) {
                    if (response.code() != 200) {
                        callback.onError(new Exception("Server reply with status " + response.code()));
                    } else {
                        callback.onError(e);
                    }
                }
            }
        });
    }

    /**
     * A call back interface to notify the complete of a request
     */
    public interface Callback<T> {
        /**
         * Call when request failed
         *
         * @param throwable
         */
        void onError(Throwable throwable);

        /**
         * Call when request was success
         *
         * @param responseObject a response json object
         */
        void onSuccess(T responseObject);
    }

}
