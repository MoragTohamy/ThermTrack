# ipn/Android.mk 
# 10.02.2016
# ===============================================================
# Installs the prebuilt rs.thermappsdk_ipn library to libs/librs.thermappsdk_ipn.so 

LOCAL_PATH := jni/prebuilt
include $(CLEAR_VARS)
LOCAL_MODULE:= rs.thermappsdk_ipn
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/librs.thermappsdk_ipn.so 
include $(PREBUILT_SHARED_LIBRARY)