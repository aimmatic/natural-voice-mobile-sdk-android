#include <jni.h>
#include <FLAC/all.h>
#include <android/log.h>
#include <memory.h>

#define READSIZE 960

static FLAC__byte buffer[READSIZE/*samples*/ * 2/*bytes_per_sample*/ *
                         1/*channels*/]; /* we read the WAVE data into here */

static FLAC__int32 pcm[READSIZE/*samples*/ * 1/*channels*/];

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
                                                            jint channel, jint bps,
                                                            jint compressLevel) {

    FLAC__bool ok = true;
    CallbackRef *cbRef = (CallbackRef *) cPointer;
    FLAC__StreamEncoder *encoder = cbRef->encoder;


    ok &= FLAC__stream_encoder_set_verify(encoder, true);
    ok &= FLAC__stream_encoder_set_compression_level(encoder, compressLevel);
    ok &= FLAC__stream_encoder_set_channels(encoder, channel);
    ok &= FLAC__stream_encoder_set_bits_per_sample(encoder, bps);
    ok &= FLAC__stream_encoder_set_sample_rate(encoder, sampleRate);

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
        return false;
    }
    return true;
}

JNIEXPORT jboolean JNICALL
Java_com_aimmatic_natural_voice_android_LibFlac_encode(JNIEnv *env, jobject instance,
                                                       jlong cPointer,
                                                       jint channel, jbyteArray in_) {
    jbyte *in = (*env)->GetByteArrayElements(env, in_, NULL);

    CallbackRef *cbRef = (CallbackRef *) cPointer;
    FLAC__StreamEncoder *encoder = cbRef->encoder;

    size_t i;
    size_t need = (size_t) (*env)->GetArrayLength(env, in_) / 2;

    FLAC__byte *flacBytes = (FLAC__byte *) in;

    for (i = 0; i < need * channel; i++) {
        pcm[i] = (FLAC__int32) (((FLAC__int16) (FLAC__int8) flacBytes[2 * i + 1] << 8) |
                                (FLAC__int16) flacBytes[2 * i]);
    }

    FLAC__bool ok = FLAC__stream_encoder_process_interleaved(encoder, pcm, (unsigned int) need);
    (*env)->ReleaseByteArrayElements(env, in_, in, 0);
    return (jboolean) ok;
}

JNIEXPORT void JNICALL
Java_com_aimmatic_natural_voice_android_LibFlac_release(JNIEnv *env, jobject instance,
                                                        jlong cPointer) {

    CallbackRef *cbRef = (CallbackRef *) cPointer;
    FLAC__StreamEncoder *encoder = cbRef->encoder;
    FLAC__stream_encoder_delete(encoder);
    free(cbRef);

}

JNIEXPORT void JNICALL
Java_com_aimmatic_natural_voice_android_LibFlac_finish(JNIEnv *env, jobject instance,
                                                       jlong cPointer) {

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
    (*cbRef->env)->SetByteArrayRegion(cbRef->env, jbytes, 0, (jsize) bytes, (const jbyte *) buffer);
    (*cbRef->env)->CallVoidMethod(cbRef->env, cbRef->instance, cbRef->onEncoded, jbytes, bytes);

    return FLAC__STREAM_ENCODER_WRITE_STATUS_OK;
}

JNIEXPORT jboolean JNICALL
Java_com_aimmatic_natural_voice_android_LibFlac_convertRawPCM(JNIEnv *env, jclass type,
                                                              jstring fileIn_, jstring fileOut_,
                                                              jint channel, jint sampleRate,
                                                              jint bitPerSecond, jint compression) {
    const char *fileIn = (*env)->GetStringUTFChars(env, fileIn_, 0);
    const char *fileOut = (*env)->GetStringUTFChars(env, fileOut_, 0);

    FLAC__bool ok = true;
    FLAC__StreamEncoder *encoder = 0;
    FLAC__StreamEncoderInitStatus init_status;
    FILE *fin;
    unsigned sample_rate = (unsigned int) sampleRate;
    unsigned channels = (unsigned int) channel;
    unsigned bps = (unsigned int) bitPerSecond;

    if ((fin = fopen(fileIn, "rb")) == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "libflac", "opening %s for output\n", fileIn);
        return false;
    }

    /* allocate the encoder */
    if ((encoder = FLAC__stream_encoder_new()) == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "libflac", "allocating encoder\n");
        fclose(fin);
        return false;
    }

    ok &= FLAC__stream_encoder_set_verify(encoder, true);
    ok &= FLAC__stream_encoder_set_compression_level(encoder, (unsigned int) compression);
    ok &= FLAC__stream_encoder_set_channels(encoder, channels);
    ok &= FLAC__stream_encoder_set_bits_per_sample(encoder, bps);
    ok &= FLAC__stream_encoder_set_sample_rate(encoder, sample_rate);

    /* initialize encoder */
    if (ok) {
        init_status = FLAC__stream_encoder_init_file(encoder, fileOut, /*process_callback=*/
                                                     NULL, /*client_data=*/NULL);
        if (init_status != FLAC__STREAM_ENCODER_INIT_STATUS_OK) {
            __android_log_print(ANDROID_LOG_ERROR, "libflac", "initializing encoder: %s\n",
                                FLAC__StreamEncoderInitStatusString[init_status]);
            ok = false;
        }
    }

    /* read blocks of samples from WAVE file and feed to encoder */
    if (ok) {
        size_t need = (size_t) READSIZE;
        size_t read;
        int notBreak = true;
        while (ok && notBreak) {
            read = fread(buffer, channels * (bps / 8), need, fin);
            if (read != need) {
                need = read;
                notBreak = false;
            }
            /* convert the packed little-endian 16-bit PCM samples from WAVE into an interleaved FLAC__int32 buffer for libFLAC */
            size_t i;
            for (i = 0; i < need * channels; i++) {
                /* inefficient but simple and works on big- or little-endian machines */
                pcm[i] = (FLAC__int32) (((FLAC__int16) (FLAC__int8) buffer[2 * i + 1] << 8) |
                                        (FLAC__int16) buffer[2 * i]);
            }
            /* feed samples to encoder */
            ok = FLAC__stream_encoder_process_interleaved(encoder, pcm, (unsigned int) need);
        }
    }

    ok &= FLAC__stream_encoder_finish(encoder);

    FLAC__stream_encoder_delete(encoder);
    fclose(fin);

    (*env)->ReleaseStringUTFChars(env, fileIn_, fileIn);
    (*env)->ReleaseStringUTFChars(env, fileOut_, fileOut);
    return (jboolean) ok;
}