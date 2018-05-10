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

package com.aimmatic.natural.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.ProgressBar;

import com.aimmatic.natural.R;
import com.aimmatic.natural.core.rest.AndroidAppContext;
import com.aimmatic.natural.oauth.AccessToken;
import com.aimmatic.natural.oauth.AimMatic;
import com.aimmatic.natural.oauth.Profile;

import java.util.HashMap;

/**
 * A fragment that hold a webview to Oauth2 login
 */
public class AimMaticLoginFragment extends Fragment {

    private static final String aimmaticCode = "AimMatic-Code";

    private WebView webView;
    private AndroidAppContext appContext;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        appContext = new AndroidAppContext(inflater.getContext());
        return inflater.inflate(R.layout.aimmatic_login_fragment, container, false);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        webView = view.findViewById(R.id.webview);
        webView.clearCache(true);
        webView.clearFormData();
        webView.clearHistory();
        webView.clearMatches();
        CookieManager.getInstance().removeAllCookie();
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebInterface(), "aimmatic");
        try {
            HashMap<String, String> header = new HashMap<>();
            header.put("Authorization", "AimMatic " + appContext.getApiKey());
            webView.loadUrl(AimMatic.service + "?callback=aimmatic://login", header);
        } catch (Exception e) {
            final Snackbar bar = Snackbar.make(getView(), R.string.oauth2_failed, Snackbar.LENGTH_INDEFINITE);
            bar.show();
        }
    }

    class WebInterface {
        @JavascriptInterface
        public void code(String code) throws Exception {
            final Snackbar bar = Snackbar.make(getView(), R.string.authenticating, Snackbar.LENGTH_INDEFINITE);
            ViewGroup contentLay = (ViewGroup) bar.getView().findViewById(android.support.design.R.id.snackbar_text).getParent();
            ProgressBar item = new ProgressBar(getContext());
            contentLay.addView(item, 0);
            bar.show();
            AimMatic.fetchToken(code, appContext.getApiKey(), new AimMatic.Callback<AccessToken>() {
                @Override
                public void onError(Throwable throwable) {
                    bar.dismiss();
                    Snackbar.make(getView(), R.string.oauth2_failed, Snackbar.LENGTH_SHORT).show();
                }

                @Override
                public void onSuccess(final AccessToken accessToken) {
                    appContext.saveAccessToken(accessToken);
                    AimMatic.fetchUserProfile(accessToken.getToken(), new AimMatic.Callback<Profile>() {
                        @Override
                        public void onError(Throwable throwable) {
                            bar.dismiss();
                            getActivity().setResult(Activity.RESULT_OK, newIntentResult(accessToken));
                            getActivity().finish();
                        }

                        @Override
                        public void onSuccess(Profile profile) {
                            bar.dismiss();
                            getActivity().setResult(Activity.RESULT_OK, newIntentResult(accessToken));
                            if (profile != null) {
                                appContext.saveUserProfile(profile);
                            }
                            getActivity().finish();
                        }
                    });
                }
            });
        }
    }

    static Intent newIntentResult(AccessToken accessToken) {
        return new Intent().putExtra(aimmaticCode, accessToken);
    }

    /**
     * Return access token from the intent
     *
     * @param intent
     * @return user's access token
     */
    public static AccessToken getAccessToken(Intent intent) {
        return intent.getParcelableExtra(aimmaticCode);
    }

}
