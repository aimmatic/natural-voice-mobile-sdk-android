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

/**
 * This class private all support language for natural voice processing
 */

public class Language {

    private String displayLanguage;
    private String langEn;
    private String bcp47Code;
    private String langCode;

    private static Language[] supportLang;

    static {
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
     */
    public static Language[] getAllSupportedLanguage() {
        return supportLang;
    }

}
