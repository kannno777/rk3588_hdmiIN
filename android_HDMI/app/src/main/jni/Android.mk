LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
TARGET_PLATFORM := android-3
LOCAL_MODULE    := hdmiinput_jni
LOCAL_SRC_FILES := $(LOCAL_PATH)/native.cpp
LOCAL_LDLIBS    := -lm -llog
LOCAL_MODULE_TAGS := optional
LOCAL_ALLOW_UNDEFINED_SYMBOLS := true
include $(BUILD_SHARED_LIBRARY)