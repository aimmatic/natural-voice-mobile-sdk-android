# Natural Voice Mobile SDK For Android #

This library allows you to integrate Natural Voice functions into your Android app.

Requires API key. For a free API key you may contact our solution desk.

https://www.aimmatic.com/solution-desk.html

mailto:solution.desk@aimmatic.com

Please allow a few hours for a response.

# Feature #

Example:
- [Natural Voice Mobile](http://www.aimmatic.com/natural-voice.html)

# Usage #

## Setup Natural Voice SDK dependency ##

Natural Voice Mobile SDK requires **Android 5.0+**.

```gradle
dependencies {
    implementation 'com.aimmatic.natural:voice-android:1.0.3'
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

**Note:** The SDK requires permission "**android.permission.RECORD_AUDIO**" to be granted before
you can start the voice recorder.

## Authentication ##

To authentication Oauth2 with AimMatic account using API Key start AimMatic Activity.

```kotlin
startActivity(Intent(this, AimMaticActivity::class.java))
```
To receive AccessToken after authentication success, start activity for result

```kotlin
startActivityForResult(Intent(this, AimMaticActivity::class.java), 1000)
```
then override onActivityResult method
```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    var accessToken = AimMaticLoginFragment.getAccessToken(data)
}
```

AimMatic API will save token and automatically after Oauth2 success. The audio upload
API will use user's access token if available otherwise API Key will be used.

Use the code below to get the current access token

```kotlin
AndroidAppContext(this).accessToken
```

## Using Voice Service ##

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
AudioRecord class which it uses to record audio.

Note: Language can be choose from **com.aimmatic.natural.voice.rest.Language**

The SDK only records the audio data when it detects that there was a voice in
 audio data, otherwise audio data provided by AudioRecord class will
be ignored. The maximum duration of voice recording is not the total duration from
 start recording until the end but it a total duration from hearing the voice until the end.

The SDK will stop the recording if cannot hear the voice for 2 seconds.

### Listening during recording ###

The EventListener provide a callback to allow the application to interact
with a UI.

```kotlin
override fun onRecordStart() {
}
```

The function call that happens immediately as soon as the SDK hears the voice from audio streaming
data provided by AudioRecord class.

```kotlin
override fun onRecording(data: ByteArray?, size: Int) {
}
```

The function call when the SDK detects the voice from the audio streaming data
provided by AudioRecord class. The size represents the actual byte array in the data.
Where the data represents the binary audio format of FLAC or WAV depending on
the setting when you start `startRecordVoice`. By default, The SDK will record audio
as WAV audio format.

This function can be use to update the UI as voice recording is currently happening.

```kotlin
override fun onRecordEnd() {
}
```

The function call when SDK reaches the maximum duration or user stops recording manually
by calling function `voiceRecorderService?.stopRecordVoice()`.

This function can be use to update the UI after voice recording is finished. The SDK will automatically
send the audio data the server.

```kotlin
override fun onVoiceSent(response: VoiceResponse?) {
}
```

The function call after SDK sends the voice data to the server. If voice is successfully received by
the server the response will contain the voice id otherwise a none 0 response code is provided which
indicates the server is unable to receive the voice data.