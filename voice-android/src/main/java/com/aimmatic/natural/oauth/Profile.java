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

import java.util.ArrayList;

/**
 * A parcelable contain user's profile information
 */
public class Profile implements Parcelable {

    private User user;
    private ArrayList<Customer> customers;

    /**
     * create empty user's profile
     */
    public Profile() {
    }

    protected Profile(Parcel in) {
        user = in.readParcelable(User.class.getClassLoader());
        customers = in.createTypedArrayList(Customer.CREATOR);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(user, flags);
        dest.writeTypedList(customers);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    /**
     * Get profile's user information
     *
     * @return a user information
     */
    public User getUser() {
        return user;
    }

    /**
     * Get available customers
     *
     * @return a list of customers
     */
    public ArrayList<Customer> getCustomers() {
        return customers;
    }

    public static final Creator<Profile> CREATOR = new Creator<Profile>() {
        @Override
        public Profile createFromParcel(Parcel in) {
            return new Profile(in);
        }

        @Override
        public Profile[] newArray(int size) {
            return new Profile[size];
        }
    };
}
