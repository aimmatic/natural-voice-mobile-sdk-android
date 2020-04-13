# Natural Voice Mobile SDK For Android #

This library allows you to integrate voice reply functions into your Android app.

Requires API key. For an API key you may contact our solution desk.

https://www.aimmatic.com/solution-desk.html

mailto:solution.desk@aimmatic.com

Please allow up to 24 hours for a response.

# Usage #

## Setup Natural Voice SDK dependency ##

Natural Voice Mobile SDK requires **Android 5.0+**.

```gradle
dependencies {
    implementation 'com.aimmatic.natural:voice-android:1.1.2'
}
```

## Setup Manifest File ##

Add the following metadata underneath the application tag.

```xml
<meta-data
    android:name="com.aimmatic.natural.voice.apikey"
    android:value="YOUR API KEY" />
```

Add the Service into your application

```xml
<service android:name="com.aimmatic.natural.voice.android.VoiceRecorderService" />
```

**Note:** The SDK requires permission "**android.permission.RECORD_AUDIO**" to be granted before
you can start the voice recorder.

## Authentication ##

For Oauth2 authentication with an AimMatic account using an API Key, start AimMaticActivity.

```kotlin
startActivity(Intent(this, AimMaticActivity::class.java))
```
To receive AccessToken after successful authentication, start activity to get the result.

```kotlin
startActivityForResult(Intent(this, AimMaticActivity::class.java), 1000)
```
then override the onActivityResult method
```kotlin
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    var accessToken = AimMaticLoginFragment.getAccessToken(data)
}
```

The API will save the token automatically after Oauth2 success. The API will use the access token if available, otherwise the API Key will be used.

Use the code below to get the current access token.

```kotlin
AndroidAppContext(this).accessToken
```

## Apply Customer ID ##

The API requires the application to provide a customer ID when you authenticate by token. We provide
profile data which automatically loads after Oauth2 authentication success. The profile
contains a list of available customers.

```kotlin
AndroidAppContext(baseContext).profile.customers
```

then before perform the action on the voice service you must set the customer id first.

```kotlin
AndroidAppContext(baseContext).customerId = "Your Select customer ID"
```

## Using Voice Service ##

Declare the variable VoiceRecorderService

```kotlin
private var voiceRecorderService: VoiceRecorderService? = null
```

Create the eventListener

```kotlin
private val eventListener: VoiceRecorderService.VoiceRecorderCallback = object : VoiceRecorderService.VoiceRecorderCallback() {
    override fun onRecordStart(audioMeta: AudioMeta) {
    }

    override fun onRecording(data: ByteArray?, size: Int) {
    }

    override fun onRecordError(throwable: Throwable?) {
    }

    override fun onRecordEnd(state: Byte) {
    }

    override fun onVoiceSent(response: VoiceResponse?) {
    }
}
```

Create a service connection

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

Create a RecordStrategy

```kotlin
val recordStrategy = RecordStrategy()
            // maximum record duration available from 1sec to 59sec
            .setMaxRecordDuration(59 * 1000)
            // If user stops talking for the timeout duration, the recording will stop
            // and we can choose to send audio as soon as recording has stopped.
            .setSpeechTimeoutPolicies(RecordStrategy.POLICY_SEND_IMMEDIATELY)
            // same policy as Speech Timeout
            .setMaxRecordDurationPolicies(RecordStrategy.POLICY_SEND_IMMEDIATELY)
            // set language of speech
            .setLanguage(Language.getLanguage(baseContext,"en-US"))
```

Start a voice recording with a maximum length of 59 seconds

```kotlin
voiceRecorderService?.startRecordVoice(recordStrategy)
```

VoiceRecorderService will throw a RuntimeException if it cannot initiate
AudioRecord class which it uses to record audio.

Note: Language can be set from **com.aimmatic.natural.voice.rest.Language**

The SDK only records the audio data when it detects that there was a natural voice in
 the audio data, otherwise audio data provided by AudioRecord class will
be ignored. The maximum duration of voice recording is not the total duration from
 start recording until the end but it a total duration from when it detects the voice until the end.

The SDK will stop the recording if cannot hear a natural voice for 2 seconds by default.

To stop recording manually, the application must provide a policy either `RecordStrategy.POLICY_CANCELED` or
`RecordStrategy.POLICY_SEND_IMMEDIATELY`

```kotlin
voiceRecorderService?.stopRecordVoice(RecordStrategy.POLICY_CANCELED)
```

### Listening during recording ###

The EventListener provides a callback to allow the application to interact with a UI.

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

The function is called when the SDK detects a natural voice from the audio streaming data
provided by AudioRecord class. The size represents the actual byte array in the data.
Where the data represents the binary audio format of FLAC or WAV depending on
the setting when you start `startRecordVoice`. By default, The SDK will record audio
as FLAC audio format.

This function can be used to update the UI as voice recording is currently happening.

```kotlin
override fun onRecordEnd() {
}
```

The function is called when SDK reaches the maximum duration or user stops recording manually
by calling function `voiceRecorderService?.stopRecordVoice()`.

This function can be use to update the UI after voice recording is finished. The SDK will automatically
send the audio data the server.

```kotlin
override fun onVoiceSent(response: VoiceResponse?) {
}
```

The function is called after SDK sends the audio data to the server. If voice is successfully received by
the server the response will contain the audioId otherwise a none 0 response code is provided which
indicates the server is unable to receive the audio data.

Note that the response status `response.status.code` now returns a proper code related to http status code.
It's provided for the developer to check the status, such as Unauthorized or Forbidden, and display a proper response to the user.
