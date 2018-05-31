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

package com.aimmatic.natural.sample.java;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.aimmatic.natural.voice.android.RecordStrategy;
import com.aimmatic.natural.voice.android.VoiceRecorder;
import com.aimmatic.natural.voice.android.VoiceRecorderService;
import com.aimmatic.natural.voice.encoder.AudioMeta;
import com.aimmatic.natural.voice.encoder.WavEncoder;
import com.aimmatic.natural.voice.rest.Language;
import com.aimmatic.natural.voice.rest.response.VoiceResponse;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int REQUEST_RECORD_AUDIO_CODE = 100;

    private RecordStrategy recordStrategy;
    private VoiceRecorderService voiceRecorderService;

    private FloatingActionButton record;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            voiceRecorderService = VoiceRecorderService.from(service);
            voiceRecorderService.addListener(eventListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            voiceRecorderService = null;
        }
    };

    private VoiceRecorderService.VoiceRecorderCallback eventListener = new VoiceRecorderService.VoiceRecorderCallback() {
        @Override
        public void onRecordStart(AudioMeta audioMeta) {
        }

        @Override
        public void onRecording(byte[] data, int size) {
        }

        @Override
        public void onRecordError(Throwable throwable) {
        }

        @Override
        public void onRecordEnd(byte state) {
            if ((state == VoiceRecorder.RECORD_END_BY_IDLE && recordStrategy.getSpeechTimeoutPolicies() == RecordStrategy.POLICY_USER_CHOICE) ||
                    (state == VoiceRecorder.RECORD_END_BY_MAX && recordStrategy.getMaxRecordDurationPolicies() == RecordStrategy.POLICY_USER_CHOICE)) {
                // Ask user whether they want to send audio to the code or they want to canceled the current record
                // then call "voiceRecorderService.onUserChoice(RecordStrategy.POLICY_SEND_IMMEDIATELY or RecordStrategy.POLICY_CANCELED)"
                Log.d("Natural Voice", "You should ask user to make a decision!");
            }
            record.setImageResource(R.drawable.ic_mic_black_24dp);
        }

        @Override
        public void onVoiceSent(VoiceResponse response) {
        }
    };

    boolean started;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recordStrategy = new RecordStrategy()
                .setEncoder(new WavEncoder())
                .setMaxRecordDuration(59 * 1000)
                .setSpeechTimeoutPolicies(RecordStrategy.POLICY_SEND_IMMEDIATELY)
                .setMaxRecordDurationPolicies(RecordStrategy.POLICY_SEND_IMMEDIATELY)
                .setLanguage(Language.getLanguage(getBaseContext(), "en-US"));
        record = findViewById(R.id.record);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!started) {
                    record.setImageResource(R.drawable.ic_mic_off_black_24dp);
                    startListen();
                } else {
                    stopListen();
                }
                started = !started;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_RECORD_AUDIO_CODE &&
                grantResults.length != 0 &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            voiceRecorderService.startRecordVoice(recordStrategy);
        }
    }

    private void startListen() {
        int permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORD_AUDIO_CODE);
            return;
        }
        // default 29 second and language is "en_US"
        voiceRecorderService.startRecordVoice(recordStrategy);
    }

    private void stopListen() {
        voiceRecorderService.stopRecordVoice(RecordStrategy.POLICY_SEND_IMMEDIATELY);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, VoiceRecorderService.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        unbindService(serviceConnection);
        voiceRecorderService.removeListener(eventListener);
        voiceRecorderService = null;
        super.onStop();
    }

}
