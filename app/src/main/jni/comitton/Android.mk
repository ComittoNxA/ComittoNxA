LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_DISABLE_FATAL_LINKER_WARNINGS := true

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

LOCAL_MODULE    := comitton
LOCAL_SRC_FILES := callText.cpp TextCommon.cpp callUnrar.cpp callImage.cpp ImageBlur.cpp ImageScaleHalf.cpp \
                                ImageCommon.cpp ImageThumbnail.cpp ImageJPEG.cpp ImagePDF.cpp ImagePNG.cpp ImageGIF.cpp \
                                ImageScaleLinear.cpp ImageScaleCubic.cpp ImageScaleNear.cpp ImageScale.cpp ImageBright.cpp \
                                ImageRotate.cpp ImageMarginCut.cpp ImageSharpen.cpp ImageInvert.cpp ImageGray.cpp ImageColoring.cpp ImageHalf.cpp \
                                callPdf.cpp PdfFlate.cpp PdfPredict.cpp PdfCommon.cpp PdfArc4.cpp PdfMd5.cpp PdfSha256.cpp PdfAes.cpp PdfCrypt.cpp
LOCAL_LDLIBS    := -ljnigraphics

LOCAL_C_INCLUDES += $(LOCAL_PATH)/../zlib
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../libpng
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../giflib

LOCAL_STATIC_LIBRARIES += libunrar5
LOCAL_STATIC_LIBRARIES += libjpeg
LOCAL_STATIC_LIBRARIES += libpng
LOCAL_STATIC_LIBRARIES += libzip
LOCAL_STATIC_LIBRARIES += libgif

LOCAL_IS_SUPPORT_LOG := true
ifeq ($(LOCAL_IS_SUPPORT_LOG),true)
	LOCAL_LDLIBS += -llog
endif

include $(BUILD_SHARED_LIBRARY)
