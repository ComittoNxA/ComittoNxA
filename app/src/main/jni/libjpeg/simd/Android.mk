# Makefile for libjpeg-turbo
 
######################################################
###           libjpeg.a                            ##
######################################################

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# From autoconf-generated Makefile
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
libjpeg_SOURCES_DIST = simd/jsimd_arm_neon.S \
                       simd/jsimd_arm.c 
else ifeq ($(TARGET_ARCH_ABI),armeabi)
libjpeg_SOURCES_DIST = simd/jsimd_arm.c 
else ifeq ($(TARGET_ARCH_ABI),x86)
libjpeg_SOURCES_DIST = simd/jsimdcpu.asm \
                       simd/jfdctflt-3dn.asm \
                       simd/jidctflt-3dn.asm \
                       simd/jquant-3dn.asm \
                       simd/jccolor-mmx.asm \
                       simd/jcgray-mmx.asm \
                       simd/jcsample-mmx.asm \
                       simd/jdcolor-mmx.asm \
                       simd/jdmerge-mmx.asm \
                       simd/jdsample-mmx.asm \
                       simd/jfdctfst-mmx.asm \
                       simd/jfdctint-mmx.asm \
                       simd/jidctfst-mmx.asm \
                       simd/jidctint-mmx.asm \
                       simd/jidctred-mmx.asm \
                       simd/jquant-mmx.asm \
                       simd/jfdctflt-sse.asm \
                       simd/jidctflt-sse.asm \
                       simd/jquant-sse.asm \
                       simd/jccolor-sse2.asm \
                       simd/jcgray-sse2.asm \
                       simd/jcsample-sse2.asm \
                       simd/jdcolor-sse2.asm \
                       simd/jdmerge-sse2.asm \
                       simd/jdsample-sse2.asm \
                       simd/jfdctfst-sse2.asm \
                       simd/jfdctint-sse2.asm \
                       simd/jidctflt-sse2.asm \
                       simd/jidctfst-sse2.asm \
                       simd/jidctint-sse2.asm \
                       simd/jidctred-sse2.asm \
                       simd/jquantf-sse2.asm \
                       simd/jquanti-sse2.asm \
                       simd/jsimd_i386.c
endif

LOCAL_SRC_FILES:= $(libjpeg_SOURCES_DIST)

#LOCAL_ARM_MODE :=arm
#LOCAL_ARM_NEON :=true

#LOCAL_CFLAGS := -DAVOID_TABLES -fstrict-aliasing -fprefetch-loop-arrays  -DANDROID -DANDROID_TILE_BASED_DECODE -DENABLE_ANDROID_NULL_CONVERT
LOCAL_CFLAGS := -DAVOID_TABLES -fstrict-aliasing -DANDROID -DANDROID_TILE_BASED_DECODE -DENABLE_ANDROID_NULL_CONVERT

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
LOCAL_CFLAGS += -D__ARM_HAVE_NEON
endif

LOCAL_MODULE := libjpegsimd

include $(BUILD_STATIC_LIBRARY)
