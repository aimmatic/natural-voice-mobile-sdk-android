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

import com.aimmatic.natural.oauth.AccessToken;
import com.aimmatic.natural.oauth.AimMatic;

import java.io.IOException;

import okhttp3.Request;
import okhttp3.Response;

/**
 * This class handle interceptor of OKHttp to inject the apikey and necessary header
 * in request header. Its also include a custom user-agent for the request.
 */

public class Interceptor implements okhttp3.Interceptor {

    private static final String authorization = "Authorization";
    private static final String userAgent = "User-Agent";
    private static final String xAppId = "X-App-Id";
    private static final String xCustomerId = "X-Customer-Id";

    // interface app context
    private AppContext appContext;

    /**
     * Create Interceptor instance from the given app context
     *
     * @param appContext app context instance
     */
    public Interceptor(AppContext appContext) {
        this.appContext = appContext;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        AccessToken accessToken = appContext.getAccessToken();
        request = rebuildRequest(accessToken, request);
        Response response = chain.proceed(request);
        if (response.code() == 401 && accessToken != null) {
            accessToken = AimMatic.renewToken(accessToken.getRefreshToken());
            if (accessToken != null) {
                if (appContext instanceof AndroidAppContext) {
                    ((AndroidAppContext) appContext).saveAccessToken(accessToken);
                }
                request = rebuildRequest(accessToken, request);
                response = chain.proceed(request);
            }
        }
        return response;
    }

    private Request rebuildRequest(AccessToken accessToken, Request request) {
        try {
            Request.Builder builder = request.newBuilder();
            builder.addHeader(userAgent, "AimMatic 1.0");
            if (accessToken != null) {
                builder.addHeader(authorization, "Bearer " + accessToken.getToken());
            } else {
                builder.addHeader(authorization, "AimMatic " + appContext.getApiKey());
            }
            String appId = appContext.getAppId();
            if (appId != null) {
                builder.addHeader(xAppId, appId);
            }
            String customerId = appContext.getCustomerId();
            if (customerId != null) {
                builder.addHeader(xCustomerId, customerId);
            }
            return builder.build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
