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
 * A parcelable contain user's information
 */
public class User implements Parcelable {

    private String username;
    private String nickname;
    private String fullname;

    /**
     * Create an empty user object
     */
    public User() {
    }

    protected User(Parcel in) {
        username = in.readString();
        nickname = in.readString();
        fullname = in.readString();
    }

    public static final Creator<User> CREATOR = new Creator<User>() {
        @Override
        public User createFromParcel(Parcel in) {
            return new User(in);
        }

        @Override
        public User[] newArray(int size) {
            return new User[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Get user's Username. The user is an email of the user that use to login with AimMatic Oauth
     *
     * @return user's username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get current user's nickname
     *
     * @return user's nickname
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Get current user's fullname
     * @return user's full name
     */
    public String getFullname() {
        return fullname;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(username);
        dest.writeString(nickname);
        dest.writeString(fullname);
    }
}
