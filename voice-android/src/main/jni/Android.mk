WORKING_DIR := $(call my-dir)

# build libflacJNI.so
include $(CLEAR_VARS)
include $(WORKING_DIR)/flac_sources.mk

LOCAL_PATH := $(WORKING_DIR)
LOCAL_MODULE := libflacJNI
LOCAL_ARM_MODE := arm
LOCAL_CPP_EXTENSION := .cc

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/flac/include \
    $(LOCAL_PATH)/flac/src/libFLAC/include
LOCAL_SRC_FILES := $(FLAC_SOURCES)

LOCAL_CFLAGS += '-DVERSION="1.3.1"' -DFLAC__NO_MD5 -DFLAC__INTEGER_ONLY_LIBRARY -DFLAC__NO_ASM
LOCAL_CFLAGS += -D_REENTRANT -DPIC -DU_COMMON_IMPLEMENTATION -fPIC -DHAVE_SYS_PARAM_H
LOCAL_CFLAGS += -O3 -funroll-loops -finline-functions

LOCAL_LDLIBS := -llog -lz -lm
include $(BUILD_SHARED_LIBRARY)