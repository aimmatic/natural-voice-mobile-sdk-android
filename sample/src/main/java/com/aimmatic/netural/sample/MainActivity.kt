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

package com.aimmatic.netural.sample

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import com.aimmatic.natural.voice.android.RecordStrategy
import com.aimmatic.natural.voice.android.VoiceRecorder
import com.aimmatic.natural.voice.android.VoiceRecorderService
import com.aimmatic.natural.voice.encoder.AudioMeta
import com.aimmatic.natural.voice.rest.Language
import com.aimmatic.natural.voice.rest.response.VoiceResponse
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private val REQUEST_RECORD_AUDIO_CODE = 100

    private val serviceConnection: ServiceConnection = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            voiceRecorderService = null
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            voiceRecorderService = VoiceRecorderService.from(service)
            voiceRecorderService?.addListener(eventListener)
        }

    }

    private val eventListener: VoiceRecorderService.VoiceRecorderCallback = object : VoiceRecorderService.VoiceRecorderCallback() {

        override fun onRecordStart(audioMeta: AudioMeta) {
        }

        override fun onRecording(data: ByteArray?, size: Int) {
        }

        override fun onRecordError(throwable: Throwable?) {
        }

        override fun onRecordEnd(state: Byte) {
            if ((state == VoiceRecorder.RECORD_END_BY_IDLE && recordStrategy?.speechTimeoutPolicies == RecordStrategy.POLICY_USER_CHOICE) ||
                    (state == VoiceRecorder.RECORD_END_BY_MAX && recordStrategy?.maxRecordDurationPolicies == RecordStrategy.POLICY_USER_CHOICE)) {
                // Ask user whether they want to send audio to the code or they want to canceled the current record
                // then call "voiceRecorderService?.onUserChoice(RecordStrategy.POLICY_SEND_IMMEDIATELY or RecordStrategy.POLICY_CANCELED)"
            }
            record.setImageResource(R.drawable.ic_mic_black_24dp)
        }

        override fun onVoiceSent(response: VoiceResponse?) {
        }

    }

    private var started = false
    private var recordStrategy: RecordStrategy? = null
    private var voiceRecorderService: VoiceRecorderService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recordStrategy = RecordStrategy()
                .setMaxRecordDuration(59 * 1000)
                .setSpeechTimeoutPolicies(RecordStrategy.POLICY_SEND_IMMEDIATELY)
                .setMaxRecordDurationPolicies(RecordStrategy.POLICY_SEND_IMMEDIATELY)
                .setLanguage(Language.getLanguage(baseContext,"en-US"))
        record.setOnClickListener {
            if (!started) {
                record.setImageResource(R.drawable.ic_mic_off_black_24dp)
                startListen()
            } else {
                stopListen()
            }
            started = !started
        }

    }

    private fun startListen() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.OnRequestPermissionsResultCallback { requestCode, permissions, grantResults ->
                if (requestCode == REQUEST_RECORD_AUDIO_CODE &&
                        !grantResults.isEmpty() &&
                        grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // default 59 second and language is "en_US"
                    voiceRecorderService?.startRecordVoice(recordStrategy)
                }
            }
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_CODE)
            return
        }
        // default 59 second and language is "en_US"
        voiceRecorderService?.startRecordVoice(recordStrategy)
    }

    private fun stopListen() {
        voiceRecorderService?.stopRecordVoice(RecordStrategy.POLICY_SEND_IMMEDIATELY)
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, VoiceRecorderService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        voiceRecorderService?.removeListener(eventListener)
        unbindService(serviceConnection)
        voiceRecorderService = null
        super.onStop()
    }

}
