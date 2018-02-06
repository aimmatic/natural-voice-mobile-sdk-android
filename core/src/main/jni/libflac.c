#include <jni.h>
#include <FLAC/all.h>

#define READSIZE 1024
static FLAC__int32 pcm[READSIZE/*samples*/ * 2/*channels*/];

static FLAC__StreamEncoderWriteStatus writeCallback(const FLAC__StreamEncoder *encoder,
                                                    const FLAC__byte buffer[],
                                                    size_t bytes, unsigned samples,
                                                    unsigned current_frame,
                                                    void *client_data);

static FLAC__StreamEncoderSeekStatus seekCallback(const FLAC__StreamEncoder *encoder,
                                                  FLAC__uint64 absolute_byte_offset,
                                                  void *client_data);

static FLAC__StreamEncoderTellStatus tellCallback(const FLAC__StreamEncoder *encoder,
                                                  FLAC__uint64 *absolute_byte_offset,
                                                  void *client_data);

static void metadataCallback(const FLAC__StreamEncoder *encoder,
                             const FLAC__StreamMetadata *metadata, void *client_data);

typedef struct {
    jmethodID onEncoded;
    jobject instance;
    JNIEnv *env;
    FLAC__StreamEncoder *encoder;
} CallbackRef;

JNIEXPORT jlong JNICALL
Java_com_aimmatic_natural_voice_android_LibFlac_init(JNIEnv *env, jobject instance) {

    FLAC__StreamEncoder *encoder = 0;
    if ((encoder = FLAC__stream_encoder_new()) == NULL) {
        return 0;
    }

    jobject gInstance = (*env)->NewGlobalRef(env, instance);
    jclass g_clazz = (*env)->GetObjectClass(env, gInstance);
    jmethodID onEncoded = (*env)->GetMethodID(env, g_clazz, "onEncoded", "([BI)V");

    CallbackRef *cbRef = calloc(1, sizeof(CallbackRef));
    cbRef->onEncoded = onEncoded;
    cbRef->instance = gInstance;
    cbRef->env = env;
    cbRef->encoder = encoder;

    return (jlong) cbRef;

}

JNIEXPORT jint JNICALL
Java_com_aimmatic_natural_voice_android_LibFlac_setMetadata(JNIEnv *env, jobject instance,
                                                        jlong cPointer, jint sampleRate,
                                                        jint channel, jint bps, jint totalSample,
                                                        jint compressLevel) {

    FLAC__bool ok = true;
    CallbackRef *cbRef = (CallbackRef *) cPointer;
    FLAC__StreamEncoder *encoder = cbRef->encoder;


    ok &= FLAC__stream_encoder_set_verify(encoder, true);
    ok &= FLAC__stream_encoder_set_compression_level(encoder, compressLevel);
    ok &= FLAC__stream_encoder_set_channels(encoder, channel);
    ok &= FLAC__stream_encoder_set_bits_per_sample(encoder, bps);
    ok &= FLAC__stream_encoder_set_sample_rate(encoder, sampleRate);
    //ok &= FLAC__stream_encoder_set_total_samples_estimate(encoder, totalSample);

    if (!ok) {
        return 0;
    }

    // live stream conversion don't extra callback
    // seek, tell and metadata is suitable for file encoder or pre-encoded
    FLAC__StreamEncoderInitStatus init_status = FLAC__stream_encoder_init_stream(
            encoder, writeCallback,
            /*seek_callback=*/ NULL,
            /*tell_callback=*/ NULL,
            /*metadata_callback=*/ NULL,
            /*client_data=*/ cbRef
    );
    if (init_status != FLAC__STREAM_ENCODER_INIT_STATUS_OK) {
        return 0;
    }
    return 1;
}

JNIEXPORT jboolean JNICALL
Java_com_aimmatic_natural_voice_android_LibFlac_encode(JNIEnv *env, jobject instance, jlong cPointer,
                                                   jint channel, jbyteArray in_) {
    jbyte *in = (*env)->GetByteArrayElements(env, in_, NULL);

    CallbackRef *cbRef = (CallbackRef *) cPointer;
    FLAC__StreamEncoder *encoder = cbRef->encoder;

    size_t i;
    size_t need = sizeof(in) / sizeof(in[0]);
    if (need > READSIZE) {
        return false;
    }
    for (i = 0; i < need * channel; i++) {
        /* inefficient but simple and works on big- or little-endian machines */
        pcm[i] = (FLAC__int32) (((FLAC__int16) (FLAC__int8) in[2 * i + 1] << 8) |
                                (FLAC__int16) in[2 * i]);
    }

    FLAC__bool ok = FLAC__stream_encoder_process_interleaved(encoder, pcm, need);
    (*env)->ReleaseByteArrayElements(env, in_, in, 0);
    return (jboolean) ok;
}

JNIEXPORT void JNICALL
Java_com_aimmatic_natural_voice_android_LibFlac_release(JNIEnv *env, jobject instance, jlong cPointer) {

    CallbackRef *cbRef = (CallbackRef *) cPointer;
    FLAC__StreamEncoder *encoder = cbRef->encoder;
    FLAC__stream_encoder_delete(encoder);
    free(cbRef);

}

JNIEXPORT void JNICALL
Java_com_aimmatic_natural_voice_android_LibFlac_finish(JNIEnv *env, jobject instance, jlong cPointer) {

    CallbackRef *cbRef = (CallbackRef *) cPointer;
    FLAC__StreamEncoder *encoder = cbRef->encoder;
    FLAC__stream_encoder_finish(encoder);

}

FLAC__StreamEncoderWriteStatus writeCallback(const FLAC__StreamEncoder *encoder,
                                             const FLAC__byte buffer[],
                                             size_t bytes, unsigned samples, unsigned current_frame,
                                             void *client_data) {
    (void) encoder, (void) client_data;
    CallbackRef *cbRef = (CallbackRef *) client_data;
    jbyteArray jbytes = (*cbRef->env)->NewByteArray(cbRef->env, bytes);
    if (NULL == jbytes) {
        return FLAC__STREAM_ENCODER_WRITE_STATUS_FATAL_ERROR;
    }
    (*cbRef->env)->SetByteArrayRegion(cbRef->env, jbytes, 0, bytes, buffer);
    (*cbRef->env)->CallVoidMethod(cbRef->env, cbRef->instance, cbRef->onEncoded, jbytes, bytes);

    return FLAC__STREAM_ENCODER_WRITE_STATUS_OK;
}