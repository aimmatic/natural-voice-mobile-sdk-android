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
import android.util.Log
import com.aimmatic.natural.voice.android.VoiceRecorder
import com.aimmatic.natural.voice.android.VoiceRecorderService
import com.aimmatic.natural.voice.rest.Language
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

    private val eventListener: VoiceRecorder.EventListener = object : VoiceRecorder.EventListener() {

        override fun onRecordStart() {
            Log.d(">>>", "Start recorded")
        }

        override fun onRecording(data: ByteArray?, size: Int) {
            Log.d(">>>", "Recorded buff")
        }

        override fun onRecordEnd() {
            Log.d(">>>", "End recorded")
            record.setImageResource(R.drawable.ic_mic_black_24dp)
        }

    }

    private var started = false
    private var voiceRecorderService: VoiceRecorderService? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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
                    // default 29 second and language is "en_US"
                    voiceRecorderService?.startRecordVoice(0,
                            Language.getAllSupportedLanguage()[1].bcp47Code)
                }
            }
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_CODE)
            return
        }
        // default 29 second and language is "en_US"
        voiceRecorderService?.startRecordVoice(0,
                Language.getAllSupportedLanguage()[0].bcp47Code)
    }

    private fun stopListen() {
        voiceRecorderService?.stopRecordVoice()
    }

    override fun onStart() {
        super.onStart()
        bindService(Intent(this, VoiceRecorderService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onResume() {
        super.onResume()
        var permission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
        permission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 1)
        }
    }

    override fun onStop() {
        voiceRecorderService?.removeListener(eventListener)
        unbindService(serviceConnection)
        voiceRecorderService = null
        super.onStop()
    }

}
