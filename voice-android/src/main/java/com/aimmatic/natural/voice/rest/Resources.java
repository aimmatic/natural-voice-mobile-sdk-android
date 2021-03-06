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

import okhttp3.MediaType;

/**
 * This class contain all constant that use for rest api client
 */

public interface Resources {

    String ApiVersion = "/v1";
    String NaturalVoice = "/insights/UploadAudio";
    String NaturalVoiceLanguage = "/insights/langs";

    MediaType MEDIA_TYPE_WAVE = MediaType.parse("audio/wav");
    MediaType MEDIA_TYPE_FLAC = MediaType.parse("audio/flac");

}
