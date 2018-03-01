# Natural Voice Mobile SDK For Android #

This library allows you to integrate Natural Voice functions into your Android app.

Requires API key. For a free API key you may contact our solution desk.

https://www.aimmatic.com/solution-desk.html

mailto:solution.desk@aimmatic.com

Please allow a few hours for a response.

# Feature #

Link to frontend mockup:
- [Natural Voice Mobile](http://www.aimmatic.com/natural-voice.html)

# Usage #

## Setup Natural Voice SDK dependency ##

Natural Voice Mobile SDK required **Android 4.1+**.

```gradle
dependencies {
    implementation 'com.aimmatic.natural:voice-android:1.0.1'
}
```

## Setup Manifest File ##

Add below metadata underneath the application tag.

```xml
<meta-data
    android:name="com.aimmatic.natural.voice.apikey"
    android:value="YOUR API KEY" />
```

Add Service into your application

```xml
<service android:name="com.aimmatic.natural.voice.android.VoiceRecorderService" />
```

**Note:** The SDK required permission "**android.permission.RECORD_AUDIO**" to be granted before
you can start record the voice.

## Add Kotlin Code ##

Declare variable voice recorder service

```kotlin
private var voiceRecorderService: VoiceRecorderService? = null
```

Create voice recorder listener

```kotlin
private val eventListener: VoiceRecorderService.VoiceRecorderCallback = object : VoiceRecorderService.VoiceRecorderCallback() {
    override fun onRecordStart() {
    }

    override fun onRecording(data: ByteArray?, size: Int) {
    }

    override fun onRecordEnd() {
    }

    override fun onVoiceSent(response: VoiceResponse?) {
    }
}
```

Create service connection

```kotlin
private val serviceConnection: ServiceConnection = object : ServiceConnection {

    override fun onServiceDisconnected(name: ComponentName?) {
        voiceRecorderService = null
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        voiceRecorderService = VoiceRecorderService.from(service)
        voiceRecorderService?.addListener(eventListener)
    }

}
```

Bind VoiceRecorderService onStart

```kotlin
override fun onStart() {
    super.onStart()
    bindService(Intent(this, VoiceRecorderService::class.java), serviceConnection, Context.BIND_AUTO_CREATE)
}
```

Unbind VoiceRecorderService onStop

```kotlin
override fun onStop() {
    voiceRecorderService?.removeListener(eventListener)
    unbindService(serviceConnection)
    voiceRecorderService = null
    super.onStop()
}
```

Start a maximum 24 seconds of voice recoding with english

```kotlin
voiceRecorderService?.startRecordVoice(24, "en_US")
```

Voice Recording service will throw a RuntimeException if it cannot initiate
AudioRecord class which use to record audio.

Note: Language can be choose from **com.aimmatic.natural.voice.rest.Language**

The SDK only record the audio data when it detected that there was a voice in
 audio data otherwise audio data that provided by AudioRecord class will
be ignored. The maximum duration of voice recording is not the total duration from
 start recording until the end but it a total duration from hearing the voice until the end.

The SDK will stop the record it cannot hear the voice for 2 seconds.

### Listening to recording ###

The EventListener provide a callback that allow application to interacted
with UI.

```kotlin
override fun onRecordStart() {
}
```

The function call immediately as soon as SDK hearing the voice from audio streaming
data provided by AudioRecord class.

```kotlin
override fun onRecording(data: ByteArray?, size: Int) {
}
```

The function call when SDK detected the voice from audio streaming data
provided by AudioRecord class. The size represent the actual byte array in the data.
Where the data represent the binary audio format of FLAC or WAVE depending on
the setting when you start `startRecordVoice`. By default, The SDK will record audio
as FLAC audio format.

This function can be use to update the UI as voice record is currently happening.

```kotlin
override fun onRecordEnd() {
}
```

The function call when SDK reach the maximum duration or user stop recording manually
by calling function `voiceRecorderService?.stopRecordVoice()`.

This function can be use to update the UI as voice record finish. The SDK will automatically
send the audio data the server.

```kotlin
override fun onVoiceSent(response: VoiceResponse?) {
}
```

The function call after SDK send the voice data to the server. If voice successfully receive by
the server the response will contain the voice id otherwise a none 0 response code is provided which
indicate the server is unable to receive the voice data.
