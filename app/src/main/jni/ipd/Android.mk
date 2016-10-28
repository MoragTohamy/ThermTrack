# ipd/Android.mk 
# 10.02.2016
# ===============================================================
# Installs the prebuilt rs.thermappsdk_ipd library to libs/librs.thermappsdk_ipd.so 

LOCAL_PATH := jni/prebuilt
include $(CLEAR_VARS)
LOCAL_MODULE:= rs.thermappsdk_ipd
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/librs.thermappsdk_ipd.so 
include $(PREBUILT_SHARED_LIBRARY)