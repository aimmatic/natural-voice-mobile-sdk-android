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

import okhttp3.OkHttpClient;

/**
 * This class represent abstract class to provide accessible to host and apikey.
 */

public interface AppContext {

    /**
     * a package name use as key to define in Android Manifest.
     */
    String apikey = "com.aimmatic.placenext.apikey";

    /**
     * a default Placenext host domain
     */
    String host = "https://api.aimmatic.com";

    /**
     * Get host address of Placenext server. By default, https://placenext.aimmatic.com will be return.
     *
     * @return a string represent Placenext address
     */
    public String getHost();

    /**
     * Get apikey to access to Placenext API. This value will be reading from Android Manifest file
     * with the key com.aimmatic.placenext.apikey
     *
     * @return a string represent Placenext ApiKey
     * @throws Exception if key was not found or unable to read ApiKey from Android Manifest file
     */
    public String getApiKey() throws Exception;

    /**
     * Get okhttp client where interceptor is added by default
     * @return okhttp client instance
     */
    public OkHttpClient getOkHttpClient();

}
