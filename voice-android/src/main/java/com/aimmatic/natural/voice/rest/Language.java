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

package com.aimmatic.natural.voice.rest;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.aimmatic.natural.core.rest.AndroidAppContext;
import com.aimmatic.natural.core.rest.AppContext;
import com.aimmatic.natural.voice.rest.response.LangResponse;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * This class private all support language for natural voice processing
 */

public class Language {

    @SerializedName("name")
    private String displayLanguage;

    @SerializedName("fullname")
    private String langEn;

    @SerializedName("speechlang")
    private String bcp47Code;

    @SerializedName("nlplang")
    private String langCode;

    private static Language[] supportLang;

    private static void initialDefaultLanguages() {
        String allLang = "Deutsch (Deutschland),German (Germany),de-DE,de\n" +
                "English (United States),English (United States),en-US,en\n" +
                "English (Philippines),English (Philippines),en-PH,en\n" +
                "English (Australia),English (Australia),en-AU,en\n" +
                "English (Canada),English (Canada),en-CA,en\n" +
                "English (Ghana),English (Ghana),en-GH,en\n" +
                "English (Great Britain),English (United Kingdom),en-GB,en\n" +
                "English (India),English (India),en-IN,en\n" +
                "English (Ireland),English (Ireland),en-IE,en\n" +
                "English (Kenya),English (Kenya),en-KE,en\n" +
                "English (New Zealand),English (New Zealand),en-NZ,en\n" +
                "English (Nigeria),English (Nigeria),en-NG,en\n" +
                "English (South Africa),English (South Africa),en-ZA,en\n" +
                "English (Tanzania),English (Tanzania),en-TZ,en\n" +
                "Español (Argentina),Spanish (Argentina),es-AR,es\n" +
                "Español (Bolivia),Spanish (Bolivia),es-BO,es\n" +
                "Español (Chile),Spanish (Chile),es-CL,es\n" +
                "Español (Colombia),Spanish (Colombia),es-CO,es\n" +
                "Español (Costa Rica),Spanish (Costa Rica),es-CR,es\n" +
                "Español (Ecuador),Spanish (Ecuador),es-EC,es\n" +
                "Español (El Salvador),Spanish (El Salvador),es-SV,es\n" +
                "Español (España),Spanish (Spain),es-ES,es\n" +
                "Español (Estados Unidos),Spanish (United States),es-US,es\n" +
                "Español (Guatemala),Spanish (Guatemala),es-GT,es\n" +
                "Español (Honduras),Spanish (Honduras),es-HN,es\n" +
                "Español (México),Spanish (Mexico),es-MX,es\n" +
                "Español (Nicaragua),Spanish (Nicaragua),es-NI,es\n" +
                "Español (Panamá),Spanish (Panama),es-PA,es\n" +
                "Español (Paraguay),Spanish (Paraguay),es-PY,es\n" +
                "Español (Perú),Spanish (Peru),es-PE,es\n" +
                "Español (Puerto Rico),Spanish (Puerto Rico),es-PR,es\n" +
                "Español (República Dominicana),Spanish (Dominican Republic),es-DO,es\n" +
                "Español (Uruguay),Spanish (Uruguay),es-UY,es\n" +
                "Español (Venezuela),Spanish (Venezuela),es-VE,es\n" +
                "Français (France),French (France),fr-FR,fr\n" +
                "Français (Canada),French (Canada),fr-CA,fr\n" +
                "Italiano (Italia),Italian (Italy),it-IT,it\n" +
                "日本語（日本）,Japanese (Japan),ja-JP,ja\n" +
                "한국어 (대한민국),Korean (South Korea),ko-KR,ko\n" +
                "Português (Brasil),Portuguese (Brazil),pt-BR,pt\n" +
                "Português (Portugal),Portuguese (Portugal),pt-PT,pt\n" +
                "普通話 (香港),\"Chinese, Mandarin (Simplified Hong Kong)\",cmn-Hans-HK,zh\n" +
                "普通话 (中国大陆),\"Chinese, Mandarin (Simplified China)\",cmn-Hans-CN,zh\n" +
                "廣東話 (香港),\"Chinese, Cantonese (Traditional Hong Kong)\",yue-Hant-HK,zh-Hant\n" +
                "國語 (台灣),\"Chinese, Mandarin (Traditional Taiwan)\",cmn-Hant-TW,zh-Hant";
        String[] eachLine = allLang.split("\n");
        supportLang = new Language[eachLine.length];
        for (int i = 0; i < eachLine.length; i++) {
            String[] column = eachLine[i].split(",");
            supportLang[i] = new Language(column[0], column[1], column[2], column[3]);
        }
    }

    private Language(String displayLanguage, String langEn, String bcp47Code, String langCode) {
        this.displayLanguage = displayLanguage;
        this.langEn = langEn;
        this.bcp47Code = bcp47Code;
        this.langCode = langCode;
    }

    /**
     * A display language
     *
     * @return string unicode represent a language
     */
    public String getDisplayLanguage() {
        return displayLanguage;
    }

    /**
     * A language in english text
     *
     * @return string of language name in english
     */
    public String getLangEn() {
        return langEn;
    }

    /**
     * A BCP-47 code as describe in https://tools.ietf.org/html/bcp47
     *
     * @return BCP-47 code to use in voice api
     */
    public String getBcp47Code() {
        return bcp47Code;
    }

    /**
     * A language code
     *
     * @return a language code
     */
    public String getLangCode() {
        return langCode;
    }

    /**
     * get all language supported by natural voice
     *
     * @return array of language
     * @deprecated As of 1.1.0, it replace by {@link #getAllSupportedLanguage(Context)}
     */
    @Deprecated
    public static Language[] getAllSupportedLanguage() {
        if (supportLang == null) {
            initialDefaultLanguages();
        }
        return supportLang;
    }

    /**
     * Get language from the give string lang
     *
     * @param language a string can be either language code (ISO 639-1) or BCP 47 Code
     * @return a language object
     * @see <a href="https://tools.ietf.org/rfc/bcp/bcp47.txt">BCP 47</a>
     * @see <a href="https://en.wikipedia.org/wiki/ISO_639-1">ISO 639-1</a>
     * @deprecated As of 1.1.0, it replace by {@link #getLanguage(Context, String)}
     */
    @Deprecated
    public static Language getLanguage(String language) {
        Language[] supportLang = getAllSupportedLanguage();
        for (Language lang : supportLang) {
            if (lang.bcp47Code.equalsIgnoreCase(language)) {
                return lang;
            } else if (lang.langCode.equalsIgnoreCase(language)) {
                return lang;
            }
        }
        return null;
    }

    /**
     * get all language supported by natural voice
     *
     * @param context android context
     * @return array of language
     */
    public static Language[] getAllSupportedLanguage(Context context) {
        if (supportLang == null) {
            File file = new File(context.getFilesDir(), "aimmatic-speech-lang.json");
            if (file.exists()) {
                try {
                    FileInputStream in = new FileInputStream(file);
                    int size = in.available();
                    byte[] buffer = new byte[size];
                    in.read(buffer);
                    in.close();
                    supportLang = new Gson().fromJson(new String(buffer, "UTF-8"), Language[].class);
                    if (supportLang != null) {
                        return supportLang;
                    }
                    loadLanguage(new AndroidAppContext(context));
                } catch (Exception e) {
                    // ignore the error
                }
            } else {
                loadLanguage(new AndroidAppContext(context));
            }
            initialDefaultLanguages();
        }
        return supportLang;
    }

    /**
     * Get language from the give string lang
     *
     * @param language a string can be either language code (ISO 639-1) or BCP 47 Code
     * @param context  android context
     * @return a language object
     * @see <a href="https://tools.ietf.org/rfc/bcp/bcp47.txt">BCP 47</a>
     * @see <a href="https://en.wikipedia.org/wiki/ISO_639-1">ISO 639-1</a>
     */
    public static Language getLanguage(Context context, String language) {
        Language[] supportLang = getAllSupportedLanguage(context);
        for (Language lang : supportLang) {
            if (lang.bcp47Code.equalsIgnoreCase(language)) {
                return lang;
            } else if (lang.langCode.equalsIgnoreCase(language)) {
                return lang;
            }
        }
        return null;
    }

    static void loadLanguage(final AppContext appContext) {
        loadLanguage(appContext, null);
    }

    @VisibleForTesting
    static void loadLanguage(final AppContext appContext, final Callback callback) {
        OkHttpClient client = appContext.getOkHttpClient();
        final Request request = new Request.Builder()
                .url(appContext.getHost() + Resources.ApiVersion + Resources.NaturalVoiceLanguage)
                .build();
        final File dir = appContext.getDataDir();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                if (callback != null) callback.onFailure(call, e);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.code() == 200 && response.body() != null) {
                    ResponseBody responseBody = response.body();
                    BufferedSource source = responseBody.source();
                    source.request(Long.MAX_VALUE); // request the entire body.
                    Buffer buffer = source.buffer();
                    String resp = buffer.clone().readString(Charset.forName("UTF-8"));
                    LangResponse lr = new Gson().fromJson(resp, LangResponse.class);
                    File file = new File(dir, "aimmatic-speech-lang.json");
                    FileOutputStream out = new FileOutputStream(file);
                    String data = new Gson().toJson(lr.getLanguages());
                    out.write(data.getBytes());
                    out.close();
                }
                if (callback != null) callback.onResponse(call, response);
            }
        });
    }

}
