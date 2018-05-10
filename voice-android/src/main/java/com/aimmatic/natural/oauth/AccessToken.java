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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * A parcelable object contain access token and refresh token
 */
public class AccessToken implements Parcelable {

    private String token;
    private String refreshToken;

    /**
     * Create new AccessToken from the given token and refresh token
     *
     * @param token        a valid token
     * @param refreshToken a valid refresh token
     */
    public AccessToken(String token, String refreshToken) {
        this.token = token;
        this.refreshToken = refreshToken;
    }

    protected AccessToken(Parcel in) {
        token = in.readString();
        refreshToken = in.readString();
    }

    public static final Creator<AccessToken> CREATOR = new Creator<AccessToken>() {
        @Override
        public AccessToken createFromParcel(Parcel in) {
            return new AccessToken(in);
        }

        @Override
        public AccessToken[] newArray(int size) {
            return new AccessToken[size];
        }
    };

    /**
     * Get current valid token
     *
     * @return a token
     */
    public String getToken() {
        return token;
    }

    /**
     * Get current valid refresh token
     *
     * @return a refresh token
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(token);
        dest.writeString(refreshToken);
    }
}
