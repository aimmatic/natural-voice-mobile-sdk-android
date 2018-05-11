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

import com.google.gson.annotations.SerializedName;

/**
 * A parcelable contain customer information
 */
public class Customer implements Parcelable {

    @SerializedName("accountId")
    private String customerId;
    @SerializedName("accountName")
    private String customerName;

    /**
     * Create an empty customer object
     */
    public Customer() {
    }

    protected Customer(Parcel in) {
        customerId = in.readString();
        customerName = in.readString();
    }

    public static final Creator<Customer> CREATOR = new Creator<Customer>() {
        @Override
        public Customer createFromParcel(Parcel in) {
            return new Customer(in);
        }

        @Override
        public Customer[] newArray(int size) {
            return new Customer[size];
        }
    };

    /**
     * Get a customer ID
     *
     * @return a customer ID that can be in another API
     */
    public String getCustomerId() {
        return customerId;
    }

    /**
     * Get a customer name
     *
     * @return a customer name
     */
    public String getCustomerName() {
        return customerName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(customerId);
        dest.writeString(customerName);
    }
}
