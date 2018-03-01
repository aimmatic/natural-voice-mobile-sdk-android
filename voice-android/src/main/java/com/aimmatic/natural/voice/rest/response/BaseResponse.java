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
 * Base response contain a status of the request response from the server.
 */

public class BaseResponse {

    @SerializedName("status")
    private Status status;

    /**
     * Create base response object
     * @param status request status
     */
    public BaseResponse(Status status) {
        this.status = status;
    }

    /**
     * Get status of the request
     *
     * @return request status
     */
    public Status getStatus() {
        return status;
    }

}
