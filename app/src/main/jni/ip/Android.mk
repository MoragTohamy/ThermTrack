# ip/Android.mk 
# 15.06.2015
# ===============================================================
# Installs the prebuilt ipthermapp library to libs/libipthermapp.so

# 'ipthermapp' is ThermApp Image Processing library

LOCAL_PATH := jni/prebuilt
include $(CLEAR_VARS)
LOCAL_MODULE:= ipthermapp 
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libipthermapp.so 
include $(PREBUILT_SHARED_LIBRARY)