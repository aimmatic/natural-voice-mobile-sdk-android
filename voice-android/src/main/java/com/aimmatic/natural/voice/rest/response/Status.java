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

package com.aimmatic.natural.voice.rest.response;

import com.google.gson.annotations.SerializedName;

/**
 * A request status, response from the server
 */

public class Status {

    @SerializedName("code")
    private int code;

    @SerializedName("message")
    private String message;

    @SerializedName("requestId")
    private String requestId;

    /**
     * Create request status
     *
     * @param code      response code
     * @param message   response message
     * @param requestId request id
     */
    public Status(int code, String message, String requestId) {
        this.code = code;
        this.message = message;
        this.requestId = requestId;
    }

    /**
     * Get a response code
     *
     * @return a response code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get response message
     *
     * @return a response message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the request id
     *
     * @return a request id
     */
    public String getRequestId() {
        return requestId;
    }
}
