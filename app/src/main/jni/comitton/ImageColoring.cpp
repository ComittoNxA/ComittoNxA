#include <malloc.h>
#include <string.h>
#include <math.h>
#include <pthread.h>
#ifdef _WIN32
#include <stdio.h>
#else
#include <android/log.h>
#endif

#include "Image.h"

extern WORD			**gLinesPtr;
extern int			gCancel;

extern int			gMaxThreadNum;

static int colorCurve[] = {
	0,0,1,1,1,2,2,2,2,3,3,3,4,4,4,4,5,5,5,6,6,7,7,7,8,8,9,9,10,10,11,11,
	12,13,13,14,14,15,16,16,17,18,19,19,20,21,22,22,23,24,25,26,27,28,29,29,30,31,32,33,34,35,36,37,
	39,40,41,42,43,45,46,47,48,50,51,52,54,55,56,58,59,61,62,63,65,66,68,69,71,72,73,75,76,78,79,80,
	82,84,85,87,88,90,91,93,94,96,97,99,100,102,103,105,106,108,109,111,113,114,116,117,119,120,122,124,125,127,128,129,
	131,133,134,136,138,139,141,142,144,146,147,149,150,152,154,155,157,158,160,161,163,165,166,168,169,171,172,174,175,177,178,179,
	181,183,184,186,187,188,190,191,193,194,196,197,199,200,201,203,204,206,207,208,210,211,212,213,215,216,217,218,219,220,221,222,
	224,225,226,226,227,228,229,230,231,232,233,233,234,235,236,236,237,238,239,239,240,241,241,242,242,243,244,244,245,245,246,246,
	247,247,248,248,248,249,249,249,250,250,250,251,251,251,251,252,252,252,252,253,253,253,253,253,254,254,254,254,254,255,255,255
};
static int colorCurveR[] = {
	8,17,26,33,41,47,53,55,52,46,41,40,45,54,66,79,99,121,143,160,172,180,186,193,202,210,218,225,233,241,248,255
};
static int colorCurveG[] = {
	8,18,28,38,46,54,61,65,67,67,65,63,60,58,57,57,59,62,66,70,74,78,83,86,90,95,99,103,107,110,113,114,
	114,112,110,107,104,101,100,99,99,99,100,102,104,106,110,114,121,128,136,145,154,164,173,181,191,200,209,219,228,237,247,255
};
static int colorCurveB[] = {
	14,29,45,61,83,106,126,138,139,133,124,118,116,115,113,109,99,87,74,64,53,43,36,38,50,69,92,117,150,184,221,255
};

void *ImageColoring_ThreadFunc(void *param)
{
	int *range = (int*)param;
	int stindex   = range[0];
	int edindex   = range[1];
	int OrgWidth  = range[2];
	int OrgHeight = range[3];

	// 使用するバッファを保持
	WORD *orgbuff;

	int		xx;	// x座標
	int		yy;	// y座標

	int cc, rr, gg, bb;

	// ラインサイズ
	for (yy = stindex ; yy < edindex ; yy ++) {
//		LOGD("ImageColoring : loop yy=%d", yy);
		if (gCancel) {
			LOGD("ImageColoring : cancel.");
//			ReleaseBuff(Page, 1, Half);
			return (void*)-1;
		}

		orgbuff = gLinesPtr[yy + HOKAN_DOTS / 2];

		for (xx =  0 ; xx < OrgWidth + HOKAN_DOTS ; xx++) {
			// 色の変換
			rr = colorCurve[colorCurveR[RGB565_RED(orgbuff[xx])]];
			gg = colorCurve[colorCurveG[RGB565_GREEN(orgbuff[xx])]];
			bb = colorCurve[colorCurveB[RGB565_BLUE(orgbuff[xx])]];
			orgbuff[xx] = MAKE565(rr, gg, bb);
		}

		// 補完用の余裕
		orgbuff[-2] = orgbuff[0];
		orgbuff[-1] = orgbuff[0];
		orgbuff[OrgWidth + 0] = orgbuff[OrgWidth - 1];
		orgbuff[OrgWidth + 1] = orgbuff[OrgWidth - 1];
	}
	return 0;
}

// 自動着色
int ImageColoring(int Page, int Half, int Index, int OrgWidth, int OrgHeight)
{
//	LOGD("ImageColoring : p=%d, h=%d, i=%d, ow=%d, oh=%d", Page, Half, Index, OrgWidth, OrgHeight);
	int ret = 0;

	pthread_t thread[gMaxThreadNum];
	int start = 0;
	int param[gMaxThreadNum][4];
	void *status[gMaxThreadNum];

	for (int i = 0 ; i < gMaxThreadNum ; i ++) {
		param[i][0] = start;
		param[i][1] = start = OrgHeight * (i + 1)  / gMaxThreadNum;
		param[i][2] = OrgWidth;
		param[i][3] = OrgHeight;
		
		if (i < gMaxThreadNum - 1) {
			/* スレッド起動 */
			if (pthread_create(&thread[i], NULL, ImageColoring_ThreadFunc, (void*)param[i]) != 0) {
				LOGE("pthread_create()");
			}
		}
		else {
			// ループの最後は直接実行
			status[i] = ImageColoring_ThreadFunc((void*)param[i]);
		}
	}

	for (int i = 0 ; i < gMaxThreadNum ; i ++) {
		/*thread_func()スレッドが終了するのを待機する。thread_func()スレッドが終了していたら、この関数はすぐに戻る*/
		if (i < gMaxThreadNum - 1) {
			pthread_join(thread[i], &status[i]);
		}
		if (status[i] != 0) {
//			LOGD("CreateScaleCubic : cancel");
			ret = -10;
		}
	}
//	LOGD("ImageColoring : complete");
	return ret;
}