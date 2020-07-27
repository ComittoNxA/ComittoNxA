#include <jni.h>
#include <time.h>
#include <malloc.h>
#include <string.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <stdio.h>
#include <setjmp.h>

#include "text.h"

//#define DEBUG

// 繧ｵ繝繝阪う繝ｫ邂｡逅・
BYTE		*gTextImages[MAX_TEXTPAGE] = {NULL, NULL, NULL, NULL, NULL};
int			gTextImagePages[MAX_TEXTPAGE] = {-1, -1, -1, -1, -1};
int			gTextImageSize = 0;

extern "C" {
// 繧､繝｡繝ｼ繧ｸ菫晏ｭ・
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallTxtLibrary_SetTextImage (JNIEnv *env, jclass obj, jobject bitmap, jint page, jint current_page)
{
//	LOGD("SetTextImage : pg=%d, cp=%d", page, current_page);

	// ID縺ｮ荳閾ｴ繝√ぉ繝・け
	if (page < 0 || page < current_page - 2 || page > current_page + 2) {
		// 繝壹・繧ｸ荳肴ｭ｣
//		LOGD("SetTextImage : Illegal page(pg=%d, cp=%d)", page, current_page);
		return -1;
	}

	// 繝｡繝｢繝ｪ迯ｲ蠕・
	AndroidBitmapInfo	info;
	int					ret;
	void				*canvas;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return -2;
	}
//	LOGD("BitmaInfo : w=%d, h=%d, s=%d, fm=%d, fl=%d, pg=%d, cpg=%d", info.width, info.height, info.stride, info.format, info.flags, page, current_page);

//	if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
//		LOGE("Bitmap format is not RGB_565 !");
//		return -3;
//	}

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &canvas)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return -4;
	}

	// 繧､繝｡繝ｼ繧ｸ繧ｵ繧､繧ｺ
//	int size = sizeof(WORD) * info.width * info.height;
	int size = info.stride * info.height;
	if (gTextImageSize != size) {
		// 繝｡繝｢繝ｪ蜀咲佐蠕・
		if (TextImagesAlloc(size) != 0) {
			return -5;
		}
	}

	int index = TextImageGetFree(current_page);
	if (index == -1) {
		return -6;
	}

	// 繝｡繝｢繝ｪ縺ｫ菫晏ｭ・
	memcpy(gTextImages[index], (BYTE*)canvas, size);
	gTextImagePages[index] = page;

	AndroidBitmap_unlockPixels(env, bitmap);
	return ret;
}

// 繧､繝｡繝ｼ繧ｸ蜿門ｾ・
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallTxtLibrary_GetTextImage(JNIEnv *env, jclass obj, jobject bitmap, jint page)
{
//	LOGD("GetTextImage : pg=%d", page);
	// 繝｡繝｢繝ｪ迯ｲ蠕・
	AndroidBitmapInfo	info;
	int					ret;
	void				*canvas;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return -2;
	}

//	if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
//		LOGE("Bitmap format is not RGB_565 !");
//		return -3;
//	}

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &canvas)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return -4;
	}

	int size = info.stride * info.height;
	int index = TextImageFindPage(page);
	
//	LOGD("getTextImage : page=%d, size=%d/%d, index=%d", page, gTextImageSize, size, index);
	if (gTextImageSize == size && index != -1) {
		// 繝薙ャ繝医・繝・・縺ｫ霑斐☆
		memcpy(canvas, gTextImages[index], gTextImageSize);
		ret = 0;
	}
	else {
		ret = -1;
	}
	AndroidBitmap_unlockPixels(env, bitmap);
	return ret;
}

// 繧､繝｡繝ｼ繧ｸ蟄伜惠繝√ぉ繝・け
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallTxtLibrary_CheckTextImage(JNIEnv *env, jclass obj, jint page)
{
//	LOGD("CheckTextImage : pg=%d", page);
	// 繝｡繝｢繝ｪ迯ｲ蠕・
	int		ret = 0;

	int index = TextImageFindPage(page);
//	LOGD("checkTextImage : page=%d, index=%d", page, index);
	
	if (index != -1) {
		// 繝薙ャ繝医・繝・・縺ｫ霑斐☆
		ret = 1;
	}
	return ret;
}

// 繧､繝｡繝ｼ繧ｸ隗｣謾ｾ
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallTxtLibrary_FreeTextImage(JNIEnv *env, jclass obj)
{
//	LOGD("FreeTextImage");
	TextImagesFree();
	return 0;
}
}
