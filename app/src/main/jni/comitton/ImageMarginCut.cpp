#include <malloc.h>
#include <string.h>
#include <math.h>
#include <pthread.h>
#ifdef _WIN32
#include <stdio.h>
#else
#include <android/log.h>
#endif
//#include <unistd.h>
#include "Image.h"

extern WORD			**gLinesPtr;
extern WORD			**gSclLinesPtr;
extern int			gCancel;

extern int			gMaxThreadNum;

void *ImageMarginCut_ThreadFunc(void *param)
{
	int *range = (int*)param;
	int stindex   = range[0];
	int edindex   = range[1];
	int SclWidth  = range[2];
	int SclHeight = range[3];
	int OrgWidth  = range[4];
	int OrgHeight = range[5];
	int CutT      = range[6];
	int CutL      = range[7];

	WORD *orgbuff1;
	WORD *buffptr;

	int		xx;	// サイズ変更後のx座標
	int		yy;	// サイズ変更後のy座標

	for (yy = stindex ; yy < edindex ; yy ++) {
//		LOGD("ImageMarginCut : loop yy=%d", yy);
		if (gCancel) {
//			LOGD("ImageRotate : cancel.");
//			ReleaseBuff(Page, 1, Half);
			return (void*)-1;
		}

		// バッファ位置
		buffptr = gSclLinesPtr[yy];
//		LOGD("ImageRotate : buffindex=%d, buffpos=%d, linesize=%d", buffindex, buffpos, linesize);

		orgbuff1 = gLinesPtr[yy + CutT + HOKAN_DOTS / 2];
		memcpy(buffptr, &orgbuff1[CutL + HOKAN_DOTS / 2], SclWidth * sizeof(WORD));

		// 補完用の余裕
		buffptr[-2] = buffptr[0];
		buffptr[-1] = buffptr[0];
		buffptr[SclWidth + 0] = buffptr[SclWidth - 1];
		buffptr[SclWidth + 1] = buffptr[SclWidth - 1];
	}
	return 0;
}

// Margin     : 画像の何%まで余白チェックするか(0～20%)
// pOrgWidth  : 余白カット後の幅を返す
// pOrgHeight : 余白カット後の高さを返す
int ImageMeasureMarginCut(int Page, int Half, int Index, int OrgWidth, int OrgHeight, int Margin, int *pOrgWidth, int *pOrgHeight, int *pCutT, int *pCutL)
{
    int ret = 0;

    // 使用するバッファを保持
    int CutL = 0;
    int CutR = 0;
    int CutT = 0;
    int CutB = 0;
    int CutCX = 0;
    int CutCY = 0;
    int SclWidth = 0;
    int SclHeight = 0;

    //LOGD("ImageMarginCut Page=%d, Half=%d, 元サイズ Index=%d, OrgWidth=%d, OrgHeight=%d, Margin=%d", Page, Half, Index, OrgWidth, OrgHeight, Margin);

    int limit;
    int space;
    int range;

    // パラメタ設定
    switch (Margin) {
        case 0:		// なし
            return 0;
        case 1:		// 弱
            limit = 5;
            space = 70;
            range = 15;
            break;
        case 2:		// 中
            limit = 6;
            space = 80;
            range = 15;
            break;
        case 3:		// 強
            limit = 7;
            space = 90;
            range = 20;
            break;
        default:	// 最強
            limit = 8;
            space = 100;
            range = 30;
            break;
    }

    if (Margin > 0) {
        // 元データ配列化
        ret = SetLinesPtr(Page, Half, Index, OrgWidth, OrgHeight);
        if (ret < 0) {
            return ret;
        }
    }

    WORD *buffptr = NULL;
    WORD *orgbuff1;

    int		xx;	// サイズ変更後のx座標
    int		yy;	// サイズ変更後のy座標

    int CheckCX = OrgWidth * range / 100;
    int CheckCY = OrgHeight * range / 100;
    int overcnt;

    //LOGD("ImageMarginCut : CheckCX=%d, CheckCY=%d", CheckCX, CheckCY);
    for (yy = 0 ; yy < CheckCY ; yy ++) {
//		LOGD("ImageMarginCut : yy=%d", yy);
        orgbuff1 = gLinesPtr[yy + HOKAN_DOTS / 2];
        overcnt = 0;	// 白でないカウンタ
        CutT = yy;
        for (xx = 0 ; xx < OrgWidth ; xx ++) {
            // 白チェック
            if (!WHITE_CHECK(orgbuff1[xx + HOKAN_DOTS / 2])) {
                overcnt ++;
            }
        }
        // 0.5%以上がオーバーしたら余白ではないとする
        if (overcnt >= OrgWidth * limit / 1000) {
            // 5%以上
            break;
        }
    }
    for (int yy = OrgHeight - 1 ; yy >= OrgHeight - CheckCY ; yy --) {
//		LOGD("ImageMarginCut : yy=%d", yy);
        orgbuff1 = gLinesPtr[yy + HOKAN_DOTS / 2];
        overcnt = 0;	// 白でないカウンタ
        CutB = OrgHeight - 1 - yy;
        for (xx = 0 ; xx < OrgWidth ; xx ++) {
            // 白チェック
            if (!WHITE_CHECK(orgbuff1[xx + HOKAN_DOTS / 2])) {
                overcnt ++;
            }
        }
        // 0.5%以上がオーバーしたら余白ではないとする
        if (overcnt >= OrgWidth * limit / 1000) {
            // 5%以上
            break;
        }
    }
    //LOGD("ImageMarginCut : CutT=%d, CutB=%d", CutT, CutB);
    if (Margin < 5 && CutT * space / 100 + CutB * space / 100 <= 0) {
        // 0になったら抜ける(0除算もあるし)
        return 0;
    }

    for (xx = 0 ; xx < CheckCX ; xx ++) {
//		LOGD("ImageMarginCut : xx=%d", xx);
        overcnt = 0;	// 白でないカウンタ
        CutL = xx;
        for (yy = CutT + 1 ; yy < OrgHeight - CutB ; yy ++) {
            // 白チェック
            if (!WHITE_CHECK(gLinesPtr[yy + HOKAN_DOTS / 2][xx + HOKAN_DOTS / 2])) {
                overcnt ++;
            }
        }
        // 0.5%以上がオーバーしたら余白ではないとする
        if (overcnt >= OrgWidth * limit / 1000) {
            // 5%以上
            break;
        }
    }
    for (int xx = OrgWidth - 1 ; xx >= OrgWidth - CheckCX ; xx --) {
//		LOGD("ImageMarginCut : xx=%d", xx);
        overcnt = 0;	// 白でないカウンタ
        CutR = OrgWidth - 1 - xx;
        for (yy = CutT + 1 ; yy < OrgHeight - CutB ; yy ++) {
            // 白チェック
            if (!WHITE_CHECK(gLinesPtr[yy + HOKAN_DOTS / 2][xx + HOKAN_DOTS / 2])) {
                overcnt ++;
            }
        }
        // 0.5%以上がオーバーしたら余白ではないとする
        if (overcnt >= OrgWidth * limit / 1000) {
            // 5%以上
            break;
        }
    }

    CutL = CutL * space / 100;
    CutR = CutR * space / 100;
    CutT = CutT * space / 100;
    CutB = CutB * space / 100;

    //LOGD("ImageMarginCut : CutL=%d, CutR=%d", CutL, CutR);
    if (Margin >= 5) {
        // 強制or縦横比無視の場合
        CutCX = (CutL + CutR);
        CutCY = (CutT + CutB);
        if (CutCX <= 0 && CutCY <= 0) {
            // 余白無し
            return 0;
        }
    }
    else {
        // 縦横比保持の場合
        if (CutL + CutR <= 0) {
            // 0になったら抜ける(0除算もあるし)
            //		LOGD("ImageMarginCut : return=0 cl=%d, cr=%d", CutL, CutR);
            return 0;
        }

        if ((CutL + CutR) * 1000 / OrgWidth < (CutT + CutB) * 1000 / OrgHeight) {
            // 幅の余白率の方が小さい
            CutCX = (CutL + CutR);
            CutCY = (CutL + CutR) * OrgHeight / OrgWidth;
            CutT = CutCY * CutT / (CutT + CutB);
            CutB = CutCY - CutT;
        }
        else {
            // 高さの余白率の方が小さい
            CutCX = (CutT + CutB) * OrgWidth / OrgHeight;
            CutCY = (CutT + CutB);
            CutL = CutCX * CutL / (CutL + CutR);
            CutR = CutCX - CutL;
        }
        if (CutCX <= 0 || CutCY <= 0) {
            // 余白無し
            return 0;
        }
    }

    SclWidth  = OrgWidth - CutCX;
    SclHeight = OrgHeight - CutCY;
    *pOrgWidth = SclWidth;
    *pOrgHeight = SclHeight;
    *pCutT = CutT;
    *pCutL = CutL;

    //LOGD("ImageMarginCut Page=%d, Half=%d, 出力サイズ SclWidth=%d, SclHeight=%d, CutLeft=%d, CutRight=%d, CutTop=%d, CutBottom=%d", Page, Half, SclWidth, SclHeight, CutL, CutR, CutT, CutB);
    return 1;
}

// Margin     : 画像の何%まで余白チェックするか(0～20%)
// pOrgWidth  : 余白カット後の幅を返す
// pOrgHeight : 余白カット後の高さを返す
int ImageMarginCut(int Page, int Half, int Index, int OrgWidth, int OrgHeight, int Margin, int *pOrgWidth, int *pOrgHeight)
{
	// 使用するバッファを保持
	int CutL = 0;
	int CutR = 0;
	int CutT = 0;
	int CutB = 0;
	int CutCX = 0;
	int CutCY = 0;
	int SclWidth = 0;
	int SclHeight = 0;

    //LOGD("ImageMarginCut Page=%d, Half=%d, 元サイズ Index=%d, OrgWidth=%d, OrgHeight=%d, Margin=%d", Page, Half, Index, OrgWidth, OrgHeight, Margin);

    int limit;
    int space;
    int range;

    // パラメタ設定
    switch (Margin) {
        case 0:		// なし
            return 0;
        case 1:		// 弱
            limit = 5;
            space = 70;
            range = 15;
            break;
        case 2:		// 中
            limit = 6;
            space = 80;
            range = 15;
            break;
        case 3:		// 強
            limit = 7;
            space = 90;
            range = 20;
            break;
        default:	// 最強
            limit = 8;
            space = 100;
            range = 30;
            break;
    }

    WORD *buffptr = NULL;
    WORD *orgbuff1;

    int		xx;	// サイズ変更後のx座標
    int		yy;	// サイズ変更後のy座標

    int CheckCX = OrgWidth * range / 100;
    int CheckCY = OrgHeight * range / 100;
    int overcnt;

    //LOGD("ImageMarginCut : CheckCX=%d, CheckCY=%d", CheckCX, CheckCY);

    for (yy = 0 ; yy < CheckCY ; yy ++) {
//		LOGD("ImageMarginCut : yy=%d", yy);
        orgbuff1 = gLinesPtr[yy + HOKAN_DOTS / 2];
        overcnt = 0;	// 白でないカウンタ
        CutT = yy;
        for (xx = 0 ; xx < OrgWidth ; xx ++) {
            // 白チェック
            if (!WHITE_CHECK(orgbuff1[xx + HOKAN_DOTS / 2])) {
                overcnt ++;
            }
        }
        // 0.5%以上がオーバーしたら余白ではないとする
        if (overcnt >= OrgWidth * limit / 1000) {
            // 5%以上
            break;
        }
    }
    for (int yy = OrgHeight - 1 ; yy >= OrgHeight - CheckCY ; yy --) {
//		LOGD("ImageMarginCut : yy=%d", yy);
        orgbuff1 = gLinesPtr[yy + HOKAN_DOTS / 2];
        overcnt = 0;	// 白でないカウンタ
        CutB = OrgHeight - 1 - yy;
        for (xx = 0 ; xx < OrgWidth ; xx ++) {
            // 白チェック
            if (!WHITE_CHECK(orgbuff1[xx + HOKAN_DOTS / 2])) {
                overcnt ++;
            }
        }
        // 0.5%以上がオーバーしたら余白ではないとする
        if (overcnt >= OrgWidth * limit / 1000) {
            // 5%以上
            break;
        }
    }
    //LOGD("ImageMarginCut : CutT=%d, CutB=%d", CutT, CutB);
    if (Margin < 5 && CutT * space / 100 + CutB * space / 100 <= 0) {
        // 0になったら抜ける(0除算もあるし)
        return 0;
    }

    for (xx = 0 ; xx < CheckCX ; xx ++) {
//		LOGD("ImageMarginCut : xx=%d", xx);
        overcnt = 0;	// 白でないカウンタ
        CutL = xx;
        for (yy = CutT + 1 ; yy < OrgHeight - CutB ; yy ++) {
            // 白チェック
            if (!WHITE_CHECK(gLinesPtr[yy + HOKAN_DOTS / 2][xx + HOKAN_DOTS / 2])) {
                overcnt ++;
            }
        }
        // 0.5%以上がオーバーしたら余白ではないとする
        if (overcnt >= OrgWidth * limit / 1000) {
            // 5%以上
            break;
        }
    }
    for (int xx = OrgWidth - 1 ; xx >= OrgWidth - CheckCX ; xx --) {
//		LOGD("ImageMarginCut : xx=%d", xx);
        overcnt = 0;	// 白でないカウンタ
        CutR = OrgWidth - 1 - xx;
        for (yy = CutT + 1 ; yy < OrgHeight - CutB ; yy ++) {
            // 白チェック
            if (!WHITE_CHECK(gLinesPtr[yy + HOKAN_DOTS / 2][xx + HOKAN_DOTS / 2])) {
                overcnt ++;
            }
        }
        // 0.5%以上がオーバーしたら余白ではないとする
        if (overcnt >= OrgWidth * limit / 1000) {
            // 5%以上
            break;
        }
    }

    CutL = CutL * space / 100;
    CutR = CutR * space / 100;
    CutT = CutT * space / 100;
    CutB = CutB * space / 100;

    if (Margin >= 5) {
        // 強制or縦横比無視の場合
        CutCX = (CutL + CutR);
        CutCY = (CutT + CutB);
        if (CutCX <= 0 && CutCY <= 0) {
            // 余白無し
            return 0;
        }
    }
    else {
        // 縦横比保持の場合
        if (CutL + CutR <= 0) {
            // 0になったら抜ける(0除算もあるし)
            //		LOGD("ImageMarginCut : return=0 cl=%d, cr=%d", CutL, CutR);
            return 0;
        }

        if ((CutL + CutR) * 1000 / OrgWidth < (CutT + CutB) * 1000 / OrgHeight) {
            // 幅の余白率の方が小さい
            CutCX = (CutL + CutR);
            CutCY = (CutL + CutR) * OrgHeight / OrgWidth;
            CutT = CutCY * CutT / (CutT + CutB);
            CutB = CutCY - CutT;
        }
        else {
            // 高さの余白率の方が小さい
            CutCX = (CutT + CutB) * OrgWidth / OrgHeight;
            CutCY = (CutT + CutB);
            CutL = CutCX * CutL / (CutL + CutR);
            CutR = CutCX - CutL;
        }
        if (CutCX <= 0 || CutCY <= 0) {
            // 余白無し
            return 0;
        }
    }

    SclWidth  = OrgWidth - CutCX;
    SclHeight = OrgHeight - CutCY;

    //LOGD("ImageMarginCut Page=%d, Half=%d, 出力サイズ SclWidth=%d, SclHeight=%d, CutLeft=%d, CutRight=%d, CutTop=%d, CutBottom=%d", Page, Half, SclWidth, SclHeight, CutL, CutR, CutT, CutB);

		// 縮小画像から取得
	int linesize  = SclWidth + HOKAN_DOTS;

	//  サイズ変更画像待避用領域確保
	if (ScaleMemAlloc(linesize, SclHeight) < 0) {
		return -6;
	}

	// データの格納先ポインタリストを更新
	if (RefreshSclLinesPtr(Page, Half, Index, SclHeight, linesize) < 0) {
		return -7;
	}

	int ret = 1;

	pthread_t thread[gMaxThreadNum];
	int start = 0;
	int param[gMaxThreadNum][8];
	void *status[gMaxThreadNum];

	for (int i = 0 ; i < gMaxThreadNum ; i ++) {
		param[i][0] = start;
		param[i][1] = start = SclHeight * (i + 1)  / gMaxThreadNum;
		param[i][2] = SclWidth;
		param[i][3] = SclHeight;
		param[i][4] = OrgWidth;
		param[i][5] = OrgHeight;
		param[i][6] = CutT;
		param[i][7] = CutL;
		
		if (i < gMaxThreadNum - 1) {
			/* スレッド起動 */
			if (pthread_create(&thread[i], NULL, ImageMarginCut_ThreadFunc, (void*)param[i]) != 0) {
				LOGE("pthread_create()");
			}
		}
		else {
			// ループの最後は直接実行
			status[i] = ImageMarginCut_ThreadFunc((void*)param[i]);
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
	*pOrgWidth = SclWidth;
	*pOrgHeight = SclHeight;

	//LOGD("ImageMarginCut : complete");
	return ret;
}