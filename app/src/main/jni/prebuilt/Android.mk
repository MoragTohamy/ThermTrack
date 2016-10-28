# prebuilt/Android.mk 
# 15.06.2015
# ===============================================================
# Compiles both usb and native static libraries into 
# a Shared Library, then installs it to libs/libusbthermapp.so

# 'native' library is thermapp communication methods with libusb
# 'usb' library is an open source C library known as libusb ( http://libusb.info/ )

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := native
LOCAL_SRC_FILES := $(TARGET_ARCH_ABI)/libnative.a

include $(PREBUILT_STATIC_LIBRARY) 

LOCAL_PATH := $(call my-dir)  
  
include $(CLEAR_VARS)  	 
LOCAL_MODULE := usbthermapp 
	 
LOCAL_LDLIBS := -llog
LOCAL_WHOLE_STATIC_LIBRARIES := libnative libusb

include $(BUILD_SHARED_LIBRARY)  

