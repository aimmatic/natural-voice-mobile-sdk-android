# Natural Voice Mobile SDK For Android #

This library allows you to integrate Natural Voice functions into your Android app.

Requires API key. For a free API key you may contact our solution desk. Please allow up to 24 hours for response.
solution.desk@aimmatic.com

# Feature #

Link to frontend mockup:
- [Natural Voice Mobile](http://www.aimmatic.com/natural-voice.html)

# Usage #

## Setup Natural Voice SDK dependency ##

```gradle
dependencies {
    compile 'com.aimmatic.natural:voice-android:0.1.0'
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

## Add Kotlin Code ##

Declare variable voice recorder service

```kotlin
private var voiceRecorderService: VoiceRecorderService? = null
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

Start audio recorder

```kotlin
voiceRecorderService?.startRecordVoice()
```
