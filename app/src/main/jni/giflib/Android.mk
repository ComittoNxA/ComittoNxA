# Makefile for libjpeg-turbo
 
######################################################
###           libjpeg.a                            ##
######################################################

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# From autoconf-generated Makefile
libjpeg_SOURCES_DIST = dev2gif.c egif_lib.c dgif_lib.c getarg.c gif_err.c gif_font.c gif_hash.c gifalloc.c qprintf.c quantize.c

LOCAL_SRC_FILES:= $(libjpeg_SOURCES_DIST)

LOCAL_CFLAGS := -DHAVE_FCNTL_H -DHAVE_STDARG_H -fstrict-aliasing -fprefetch-loop-arrays  -DANDROID -DANDROID_TILE_BASED_DECODE -DENABLE_ANDROID_NULL_CONVERT

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

LOCAL_MODULE := libgif

include $(BUILD_STATIC_LIBRARY)
