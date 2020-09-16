#include <jni.h>
#include <time.h>
#include <malloc.h>
#include <string.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <stdio.h>
#include <setjmp.h>

#include "Image.h"

//#define DEBUG

// 繧､繝｡繝ｼ繧ｸ邂｡逅・
IMAGEDATA	*gImageData = NULL;
long		gTotalPages = 0;
long		gLoadBuffSize = 0;
char		*gLoadBuffer = NULL;
WORD		**gLinesPtr;
WORD		**gDsLinesPtr;
WORD		**gSclLinesPtr;	// 出力先ラインポインタ配列

long		gLoadFileSize;
long		gLoadFilePos;
long		gLoadPage;
int			gLoadError;
int			gCancel;
jmp_buf		gJmpBuff;
int			gMaxThreadNum = 1;

BUFFMNG		*gBuffMng = NULL;
long		gBuffNum = 0;

BUFFMNG		*gSclBuffMng = NULL;
long		gSclBuffNum = 0;

int			gSclLoopCnt;
int			gSclLoopWidth;
int			gSclLoopHeight;

char gDitherX_3bit[8][8] = {{0, 0, 0, 0, 0, 0, 0, 0},
							{8, 0, 0, 0, 0, 0, 0, 0},
							{8, 0, 0, 0, 8, 0, 0, 0},
							{8, 0, 0, 8, 0, 0, 8, 0},
							{8, 0, 8, 0, 8, 0, 8, 0},
							{8, 8, 8, 0, 8, 0, 8, 0},
							{8, 8, 8, 0, 8, 8, 8, 0},
							{8, 8, 8, 8, 8, 8, 8, 0}};
char gDitherX_2bit[4][4] = {{0, 0, 0, 0},
							{4, 0, 0, 0},
							{4, 0, 4, 0},
							{4, 4, 4, 0}};
char gDitherY_3bit[8] = {0, 2, 4, 6, 0, 2, 4, 6};
char gDitherY_2bit[4] = {0, 2, 0, 2};

extern "C" {
// 繧ｵ繝阪う繝ｫ縺ｮ蛻晄悄蛹・
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ThumbnailInitialize (JNIEnv *env, jclass obj, jlong id, jint pagesize, jint pagenum, jint imagenum)
{
#ifdef DEBUG
	LOGD("ThumbnailInitialize : id=%lld, pagesize=%d, pagenum=%d, imagenum=%d", id, pagesize, pagenum, imagenum);
#endif

	int ret = ThumbnailAlloc(id, pagesize, pagenum, imagenum);
	return ret;
}

// 繧ｵ繝阪う繝ｫ縺ｮNoImage險ｭ螳・
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ThumbnailSetNone (JNIEnv *env, jclass obj, jlong id, jint index)
{
#ifdef DEBUG
	LOGD("ThumbnailSetNone : id=%lld, index=%d", id, index);
#endif

	int ret = ThumbnailSetNone(id, index);
	return ret;
}

// 繧ｵ繝阪う繝ｫ縺ｮ谿九ｊ鬆伜沺遒ｺ隱・
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ThumbnailCheck (JNIEnv *env, jclass obj, jlong id, jint index)
{
#ifdef DEBUG
	LOGD("ThumbnailCheck : id=%lld, index=%d", id, index);
#endif

	int ret = ThumbnailCheck(id, index);
	return ret;
}

// 繧ｵ繝阪う繝ｫ縺ｮ谿九ｊ鬆伜沺遒ｺ隱・
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ThumbnailSizeCheck (JNIEnv *env, jclass obj, jlong id, jint width, jint height)
{
#ifdef DEBUG
	LOGD("ThumbnailSizeCheck : id=%lld, width=%d, height=%d", id, width, height);
#endif

	int ret = ThumbnailSizeCheck(id, width, height);
	return ret;
}

// 繧ｵ繝阪う繝ｫ繧呈紛逅・＠縺ｦ螳ｹ驥冗｢ｺ菫・
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ThumbnailImageAlloc (JNIEnv *env, jclass obj, jlong id, jint blocks, jint index)
{
#ifdef DEBUG
	LOGD("ThumbnailImageAlloc : id=%lld, blocks=%d, index=%d", id, blocks, index);
#endif

	int ret = ThumbnailImageAlloc(id, blocks, index);
	return ret;
}

// 繧ｵ繝阪う繝ｫ縺悟・縺ｦ險ｭ螳壹＆繧後※縺・ｋ縺九ｒ繝√ぉ繝・け
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ThumbnailCheckAll (JNIEnv *env, jclass obj, jlong id)
{
#ifdef DEBUG
	LOGD("ThumbnailCheckAll : id=%lld", id);
#endif

	int ret = ThumbnailCheckAll(id);
	return ret;
}

// 繧ｵ繝阪う繝ｫ菫晏ｭ・
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ThumbnailSave (JNIEnv *env, jclass obj, jlong id, jobject bitmap, jint index)
{
#ifdef DEBUG
	LOGD("ThumbnailSave : id=%lld, index=%d", id, index);
#endif
	if (bitmap == NULL) {
		return 0;
	}

	// 繝薙ャ繝医・繝・・諠・펡叙蠕・
	AndroidBitmapInfo	info;
	int					ret;
	void				*canvas;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return -2;
	}

	if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
		LOGE("Bitmap format is not RGB_565 !");
		return -3;
	}

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &canvas)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return -4;
	}

	ret = ThumbnailSave(id, index, info.width, info.height, info.stride, (BYTE*)canvas);

	AndroidBitmap_unlockPixels(env, bitmap);
	return ret;
}

// 繧ｵ繝阪う繝ｫ謠冗判
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ThumbnailDraw(JNIEnv *env, jclass obj, jlong id, jobject bitmap, jint index)
{
#ifdef DEBUG
	LOGD("ThumbnailDraw : id=%lld, index=%d", id, index);
#endif

	if (bitmap == NULL) {
		return 0;
	}

	// 繝｡繝｢繝ｪ迯ｲ蠕・
	AndroidBitmapInfo	info;
	int					ret;
	void				*canvas;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return -2;
	}

	if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
		LOGE("Bitmap format is not RGB_565 !");
		return -3;
	}

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &canvas)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return -4;
	}

	ret = ThumbnailDraw(id, index, info.width, info.height, info.stride, (BYTE*)canvas);

	AndroidBitmap_unlockPixels(env, bitmap);
	return ret;
}

// 繧ｵ繝阪う繝ｫ隗｣謾ｾ
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ThumbnailFree(JNIEnv *env, jclass obj, jlong id)
{
#ifdef DEBUG
	LOGD("ThumbnailFree : id=%lld", id);
#endif
//	if (gThumbnailId != id) {
//		// 蛻晄悄蛹悶＠縺櫑D縺ｨ逡ｰ縺ｪ繧・
//		return -1;
//	}
	ThumbnailFree(id);
	return 0;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageInitialize
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ImageInitialize (JNIEnv *env, jclass obj, jint loadsize, jint buffsize, jint totalpage, jint threadnum)
{
#ifdef DEBUG
	LOGD("Initialize : buffsize=%d * 4, page=%d", buffsize, totalpage);
#endif

	// 隱ｭ縺ｿ霎ｼ縺ｿ逕ｨ鬆伜沺遒ｺ菫・
	MemFree();

	gLoadBuffSize  = loadsize;
	gTotalPages  = totalpage;
	gCancel = 0;

	jint ret = 0;

	gLoadPage = -1;
	ret = MemAlloc(buffsize);

	if (threadnum > 0) {
		gMaxThreadNum = threadnum;
	}
	else {
		LOGE("SetParameter : Illegal Param.(%d)", threadnum);
	}
	return ret;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageSetSize
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ImageSetPage (JNIEnv *env, jclass obj, jint page, jint size)
{
#ifdef DEBUG
	LOGD("ImageSetSize : page=%d, size=%d", page, size);
#endif
	
	if (gLoadBuffSize < size) {
		// 繝ｭ繝ｼ繝蛾셅沺荳崎ｶｳ
		// ロード????不足
		return -2;
	}

	if (page < 0 || gTotalPages <= page) {
		// 繝壹・繧ｸ逡ｪ蜿ｷ荳肴ｭ｣
        // ページ番号不正
		return -3;
	}

	gLoadPage = page;
	gLoadFileSize = size;
	gLoadFilePos = 0;
	gImageData[page].SclFlag[0] = 0;
	gImageData[page].SclFlag[1] = 0;
	gImageData[page].SclFlag[2] = 0;
	return 0;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageSetData
 * Signature: ([BI)V
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ImageSetData (JNIEnv *env, jclass obj, jbyteArray dataArray, jint size)
{
	jbyte *data = env->GetByteArrayElements(dataArray, NULL);

	if (gLoadFileSize - gLoadFilePos < size) {
		// 繧ｻ繝・ヨ縺励◆繧ｵ繧､繧ｺ繧定ｶ・∴縺ｪ縺・ｈ縺・↓
		// セチE??したサイズを趁E??なぁE??ぁE??
		size = gLoadFileSize - gLoadFilePos;
	}
	memcpy(&gLoadBuffer[gLoadFilePos], data, size);
	gLoadFilePos += size;

	env->ReleaseByteArrayElements(dataArray, data, 0);
//	LOGD("setdata end");
	return size;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageConvert
 * Signature: ()V
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ImageConvert (JNIEnv *env, jclass obj, jint type, jint scale, jintArray paramArray)
{
	if (gLoadPage < 0 || gTotalPages <= gLoadPage) {
		LOGE("ImageConvert : Illegal Page.(%d)", (int)gLoadPage);
		return - 3;
	}
#ifdef DEBUG
	LOGD("ImageConvert : page=%d, filesize=%d, type=%d, scale=%d", (int)gLoadPage, (int)gLoadFilePos, type, scale);
#endif

	int ret = 0;
	gLoadError = 0;

	jint *p = NULL;
	if (paramArray != NULL) {
		p = (jint*)env->GetIntArrayElements(paramArray, NULL);
	}

    if (setjmp(gJmpBuff) == 0) {
		if (type == 4){
#ifdef DEBUG
			LOGD("ImageConvert : PDF-CCITT(%d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d)", p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10]);
#endif
			ret = LoadImageCCITT(&gImageData[gLoadPage], gLoadPage, scale, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], NULL, 0, 0);
			if (ret == 0 && gLoadError) {
				ret = -4;
			}
		}
		else if (type == 5){
#ifdef DEBUG
			LOGD("ImageConvert : PDF-Flate(%d, %d, %d, %d)", p[0], p[1], p[2], p[3]);
#endif
			ret = LoadImageFlate(&gImageData[gLoadPage], gLoadPage, scale, p[0], p[1], p[2], p[3], NULL, 0, 0);
			if (ret == 0 && gLoadError) {
				ret = -4;
			}
		}
		else {
			if (gLoadBuffer[6] == 'J' && gLoadBuffer[7] == 'F' && gLoadBuffer[8] == 'I' && gLoadBuffer[9] == 'F') {
//				LOGD("ImageConvert : Judge - JPEG");
				type = 1;
			}
			else if (gLoadBuffer[1] == 'P' && gLoadBuffer[2] == 'N' && gLoadBuffer[3] == 'G') {
//				LOGD("ImageConvert : Judge - PNG");
				type = 2;
			}
			else if (gLoadBuffer[0] == 'G' && gLoadBuffer[1] == 'I' && gLoadBuffer[2] == 'F') {
//				LOGD("ImageConvert : Judge - GIF");
				type = 6;
			}
			else {
//				LOGD("ImageConvert : Judge - ELSE(%d)", type);
			}

			if (type == 1) {
#ifdef DEBUG
				LOGD("ImageConvert : JPEG - quality=%d", p[0]);
#endif
				ret = LoadImageJpeg(&gImageData[gLoadPage], gLoadPage, scale, p[0]);
				if (ret == 0 && gLoadError) {
					ret = -4;
				}
			}
			else if (type == 2){
				ret = LoadImagePng(&gImageData[gLoadPage], gLoadPage, scale);
				if (ret == 0 && gLoadError) {
					ret = -4;
				}
			}
			else if (type == 6){
				ret = LoadImageGif(&gImageData[gLoadPage], gLoadPage, scale);
				if (ret == 0 && gLoadError) {
					ret = -4;
				}
			}
		}
	}
	else {
		ret = -1;
	}

	if (paramArray != NULL) {
		env->ReleaseIntArrayElements(paramArray, p, 0);
	}

//	if (ret == 0) {
//		int width = gImageData[gLoadHandle].OrgWidth;
//		int height = gImageData[gLoadHandle].OrgHeight;
//		gImageData[gLoadHandle].BuffScale = &gImageData[gLoadHandle].Buff[width * height];
//	}
	gLoadPage = -1;
#ifdef DEBUG
	LOGD("ImageConvert : end - result=%d", ret);
#endif
	return ret;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageConvertBitmap
 * Signature: ()V
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ImageConvertBitmap (JNIEnv *env, jclass obj, jint type, jint scale, jintArray paramArray, jobject bitmap)
{
	if (gLoadPage < 0 || gTotalPages <= gLoadPage) {
		LOGE("ImageConvertBitmap : Illegal Page.(%d)", (int)gLoadPage);
		return - 3;
	}
#ifdef DEBUG
	LOGD("ImageConvertBitmap : page=%d, filesize=%d, type=%d, scale=%d", (int)gLoadPage, (int)gLoadFilePos, type, scale);
#endif
	if (type != 4 && type != 5){
		return -1;
	}

	int 				ret = 0;
	AndroidBitmapInfo	info;
	void				*canvas;

	// 繝ｭ繝ｼ繝・ぅ繝ｳ繧ｰ荳ｭ縺ｮ繧ｨ繝ｩ繝ｼ諠・틆	gLoadError = 0;
    // ローチE??ング中のエラー惁E?

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return -2;
	}

	if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
		LOGE("Bitmap format is not RGB_565 !");
		return -3;
	}

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &canvas)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return -4;
	}

	jint *p = NULL;
	if (paramArray != NULL) {
		p = (jint*)env->GetIntArrayElements(paramArray, NULL);
	}

    if (setjmp(gJmpBuff) == 0) {
		if (type == 4){
#ifdef DEBUG
			LOGD("ImageConvertBitmap : PDF-CCITT(%d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d)", p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10]);
#endif
			ret = LoadImageCCITT(NULL, gLoadPage, scale, p[0], p[1], p[2], p[3], p[4], p[5], p[6], p[7], p[8], p[9], p[10], (WORD*)canvas, info.stride / sizeof(WORD), info.height);
			if (ret == 0 && gLoadError) {
				ret = -4;
			}
		}
		else if (type == 5){
#ifdef DEBUG
			LOGD("ImageConvertBitmap : PDF-Flate(%d, %d, %d, %d)", p[0], p[1], p[2], p[3]);
#endif
			ret = LoadImageFlate(NULL, gLoadPage, scale, p[0], p[1], p[2], p[3], (WORD*)canvas, info.stride / sizeof(WORD), info.height);
			if (ret == 0 && gLoadError) {
				ret = -4;
			}
		}
	}
	else {
		ret = -1;
	}

	AndroidBitmap_unlockPixels(env, bitmap);

	if (paramArray != NULL) {
		env->ReleaseIntArrayElements(paramArray, p, 0);
	}

	gLoadPage = -1;
#ifdef DEBUG
	LOGD("ImageConvertBitmap : end - result=%d", ret);
#endif
	return ret;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageTerminate
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_src_comitton_stream_CallImgLibrary_ImageTerminate (JNIEnv *env, jclass obj)
{
//	LOGD("Terminate");
	MemFree();
	return;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageGetFreeSize
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ImageGetFreeSize (JNIEnv *env, jclass obj)
{
//	LOGD("ImageGetFreeSize : Start");
	int  count = 0, i;
	for (i = 0 ; i < gBuffNum ; i ++) {
		if (gBuffMng[i].Page == -1) {
			count ++;
		}
	}
#ifdef DEBUG
	LOGD("ImageGetFreeSize : %d / %d", count, (int)gBuffNum);
#endif
	return (count);
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    GetMarginSize
 * Signature: ()V
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_GetMarginSize (JNIEnv *env, jclass obj, jint page, jint half, jint index, jint width, jint height, jint margin, jintArray size)
{
	if (page < 0 || gTotalPages <= page) {
		LOGE("ImageMeasureMarginCut : Illegal Page.(%d)", page);
		return -1;
	}
#ifdef DEBUG
	LOGD("ImageMeasureMarginCut : page=%d, half=%d, width=%d, height=%d", page, half, width, height);
#endif

	jint *retsize = env->GetIntArrayElements(size, NULL);
	int ret = GetMarginSize(page, half, index, width, height, margin, &retsize[0], &retsize[1], &retsize[2], &retsize[3]);
	env->ReleaseIntArrayElements(size, retsize, 0);
	return ret;
//	return
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageScale
 * Signature: ()V
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ImageScale (JNIEnv *env, jclass obj, jint page, jint half, jint width, jint height, jint left, jint right, jint top, jint bottom, jint algorithm, jint rotate, jint margin, jint bright, jint gamma, jint param, jintArray size)
{
	if (page < 0 || gTotalPages <= page) {
		LOGE("ImageScale : Illegal Page.(%d)", page);
		return -1;
	}
#ifdef DEBUG
	LOGD("ImageScale : page=%d, half=%d, width=%d, height=%d", page, half, width, height);
#endif

    jint *retsize = env->GetIntArrayElements(size, NULL);
	int ret = CreateScale(page, half, width, height, left, right, top, bottom, algorithm, rotate, margin, bright, gamma, param, retsize);
    env->ReleaseIntArrayElements(size, retsize, 0);
	return ret;
//	return 
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageFree
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_src_comitton_stream_CallImgLibrary_ImageFree (JNIEnv *env, jclass obj, jint page)
{
	if (page < 0 || gTotalPages <= page) {
		LOGE("ImnageFree : Illegal Page.(%d)", page);
		return;
	}
#ifdef DEBUG
	LOGD("ImageFree : page=%d", page);
#endif
	gImageData[page].UseFlag = 0;
	// 逕ｻ蜒上・鬆伜沺蜈ｨ驛ｨ繧定ｧ｣謾ｾ
	ReleaseBuff(page, -1, -1);
//	gImageData[page].LoadSize = 0;
//	gImageData[page].LoadPos = 0;
	return;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageScaleFree
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_src_comitton_stream_CallImgLibrary_ImageScaleFree (JNIEnv *env, jclass obj, jint page, jint half)
{
	if (page != -1 && (page < 0 || gTotalPages <= page)) {
		LOGE("ImageScaleFree : Illegal Page.(%d)", page);
		return;
	}
#ifdef DEBUG
	LOGD("ImageScaleFree : page=%d, half=%d", page, half);
#endif

	// 逕ｻ蜒上・邵ｮ蟆ｺ繧定ｧ｣謾ｾ
	ReleaseBuff(page, 1, half);
	return;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageDraw
 * Signature: (IIILandroid/graphics/Bitmap;)V
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ImageDraw (JNIEnv *env, jclass obj, jint page, jint half, jint x, jint y, jobject bitmap)
{
	if (page < 0 || gTotalPages <= page) {
		LOGE("ImageDraw : Illegal Page.(%d)", page);
		return -1;
	}
//	if (gImageData[page].SclFlag != 1) {
//		LOGE("ImageDraw : no Scale Image.(%d)", page);
//		return -2;
//	}

#ifdef DEBUG
	LOGD("DrawBitmap : page=%d, half=%d, x=%d, y=%d / sflg=%d, ow=%d, oh=%d, sw=%d, sh=%d"
			, page, half, x, y
			, (int)gImageData[page].SclFlag[half]
			, (int)gImageData[page].OrgWidth, (int)gImageData[page].OrgHeight
			, (int)gImageData[page].SclWidth[half], (int)gImageData[page].SclHeight[half]);
#endif

	AndroidBitmapInfo	info;
	int					ret;
	void				*canvas;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return -2;
	}

	if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
		LOGE("Bitmap format is not RGB_565 !");
		return -3;
	}

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &canvas)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return -4;
	}

//	memset(canvas, 0, info.width * info.height * sizeof(uint16_t));
	ret = DrawBitmap(page, half, x, y, canvas, info.width, info.height, info.stride, &gImageData[page]);

	AndroidBitmap_unlockPixels(env, bitmap);
#ifdef DEBUG
	LOGD("DrawBitmap : end");
#endif
	return ret;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageScaleDraw
 * Signature: (IIILandroid/graphics/Bitmap;)V
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallImgLibrary_ImageScaleDraw (JNIEnv *env, jclass obj, jint page, jint rotate, jint s_x, jint s_y, jint s_cx, jint s_cy, jint d_x, jint d_y, jint d_cx, jint d_cy, jint psel, jobject bitmap, jint cut_left, jint cut_right, jint cut_top, jint cut_bottom)
{
	if (page < 0 || gTotalPages <= page) {
		LOGE("ImageDraw : Illegal Page.(%d)", page);
		return -1;
	}
	if (gImageData[page].UseFlag != 1) {
		LOGE("ImageDraw : no Scale Image.(%d)", page);
		return -2;
	}

//	LOGD("ImageScaleDraw : page=%d, x=%d, y=%d / sflg=%d, ow=%d, oh=%d, sw=%d, sh=%d"
//			, page, x, y
//			, gImageData[page].SclFlag
//			, gImageData[page].OrgWidth, gImageData[page].OrgHeight
//			, gImageData[page].SclWidth, gImageData[page].SclHeight);

	AndroidBitmapInfo	info;
	int					ret;
	void				*canvas;

	if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
		LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
		return -3;
	}

	if (info.format != ANDROID_BITMAP_FORMAT_RGB_565) {
		LOGE("Bitmap format is not RGB_565 !");
		return -4;
	}

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, &canvas)) < 0) {
		LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
		return -5;
	}

//	memset(canvas, 0, info.width * info.height * sizeof(uint16_t));
	ret = DrawScaleBitmap(page, rotate, s_x, s_y, s_cx, s_cy, d_x, d_y, d_cx, d_cy, canvas, info.width, info.height, info.stride, psel, &gImageData[page], cut_left, cut_right, cut_top, cut_bottom);

	AndroidBitmap_unlockPixels(env, bitmap);
	return ret;
}

/*
 * Class:     src_comitton_stream_CallImgLibrary
 * Method:    ImageCancel
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_src_comitton_stream_CallImgLibrary_ImageCancel (JNIEnv *env, jclass obj, jint flag)
{
//	LOGD("ImageCancel : flag=%d", flag);
	gCancel = flag;

	return;
}
}
