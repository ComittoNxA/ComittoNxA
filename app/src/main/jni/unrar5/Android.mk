
LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_CFLAGS := -fstrict-aliasing -fprefetch-loop-arrays

ifeq ($(APP_DEBUG), 0)
    LOCAL_CFLAGS += -O3
endif

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_CFLAGS += -mfpu=neon -mfloat-abi=softfp -fPIC -march=armv7-a
    LOCAL_ARM_MODE :=arm
    LOCAL_ARM_NEON :=true
else ifeq ($(TARGET_ARCH_ABI),armeabi)
    LOCAL_CFLAGS += -fPIC
    LOCAL_ARM_MODE :=arm
    LOCAL_ARM_NEON :=false
else ifeq ($(TARGET_ARCH_ABI),x86)
    LOCAL_CFLAGS += -mfpmath=sse -msse2 -m32 -masm=intel
endif

LOCAL_MODULE    := libunrar5

LOCAL_SRC_FILES := crc.cpp crypt.cpp getbits.cpp rarvm.cpp rdwrfn.cpp rijndael.cpp sha1.cpp unpack.cpp \
                   hash.cpp secpassword.cpp blake2s.cpp sha256.cpp unicode.cpp strfn.cpp timefn.cpp

include $(BUILD_STATIC_LIBRARY)
