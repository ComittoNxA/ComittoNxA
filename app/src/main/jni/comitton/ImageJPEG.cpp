#include <time.h>
#include <malloc.h>
#include <string.h>
#include <stdio.h>
#include <setjmp.h>
#include <android/log.h>

#include "Image.h"

extern char			*gLoadBuffer;
extern long			gLoadFileSize;
extern int			gLoadError;

extern BUFFMNG		*gBuffMng;
extern long			gBuffNum;

extern int			gCancel;
extern jmp_buf		gJmpBuff;

extern "C"
{
//#define XMD_H
#include "../libjpeg/jpeglib.h"
#include "../libjpeg/jerror.h"
}

METHODDEF(void) memory_init_source (j_decompress_ptr cinfo);
METHODDEF(boolean) memory_fill_input_buffer (j_decompress_ptr cinfo);
METHODDEF(void) memory_skip_input_data (j_decompress_ptr cinfo, long num_bytes);
METHODDEF(void) memory_term_source (j_decompress_ptr cinfo);

extern char gDitherX_3bit[8][8];
extern char gDitherX_2bit[4][4];
extern char gDitherY_3bit[8];
extern char gDitherY_2bit[4];

static void _JpegError(j_common_ptr in_info)
{
	char	pszMessage[JMSG_LENGTH_MAX];

	(*in_info->err->format_message)(in_info,pszMessage);

	if (gCancel) {
//		LOGD("JpegError : %s", pszMessage);
	}
	else {
		LOGI("JpegError : %s", pszMessage);

		gLoadError = 1;
		// JPEG処理を抜ける
		longjmp( gJmpBuff, 1 );
	}
}

/* メモリソースからのJPEG展開用マネージャ */
typedef struct {
	struct jpeg_source_mgr pub;	/* public fields */

	JOCTET * buffer;
	unsigned long buffer_length;
} memory_source_mgr;
typedef memory_source_mgr *memory_src_ptr;


GLOBAL(void)
jpeg_memory_src (j_decompress_ptr cinfo, void* data, unsigned long len)
{
	memory_src_ptr src;

	if (cinfo->src == NULL) {	/* first time for this JPEG object? */
		cinfo->src = (struct jpeg_source_mgr *)(*cinfo->mem->alloc_small) ((j_common_ptr) cinfo, JPOOL_PERMANENT, sizeof(memory_source_mgr));
		src = (memory_src_ptr) cinfo->src;
		src->buffer = (JOCTET *)(*cinfo->mem->alloc_small) ((j_common_ptr) cinfo, JPOOL_PERMANENT, len * sizeof(JOCTET));
	}

	src = (memory_src_ptr) cinfo->src;

	src->pub.init_source = memory_init_source;
	src->pub.fill_input_buffer = memory_fill_input_buffer;
	src->pub.skip_input_data = memory_skip_input_data;
	src->pub.resync_to_restart = jpeg_resync_to_restart; /* use default method */
	src->pub.term_source = memory_term_source;

	src->pub.bytes_in_buffer = len;
	src->pub.next_input_byte = (JOCTET*)data;
}

METHODDEF(void) memory_init_source (j_decompress_ptr cinfo)
{
	return;
}


METHODDEF(boolean) memory_fill_input_buffer (j_decompress_ptr cinfo)
{
	memory_src_ptr src = (memory_src_ptr)cinfo->src;

	src->buffer[0] = (JOCTET) 0xFF;
	src->buffer[1] = (JOCTET) JPEG_EOI;
	src->pub.next_input_byte = src->buffer;
	src->pub.bytes_in_buffer = 2;
	return TRUE;
}

METHODDEF(void) memory_skip_input_data (j_decompress_ptr cinfo, long num_bytes)
{
	memory_src_ptr src = (memory_src_ptr) cinfo->src;

	if (num_bytes > 0) {
		src->pub.next_input_byte += (size_t) num_bytes;
		src->pub.bytes_in_buffer -= (size_t) num_bytes;
	}
}

METHODDEF(void) memory_term_source (j_decompress_ptr cinfo)
{
	return;
}

int LoadImageJpeg(IMAGEDATA *pData, int page, int scale, int quality)
{
//	LOGI("Jpeg : start1(%d, %d)", page, quality);
	/* Now fill the values with a nice little plasma */
	int		yy, xx, sx, yd3, yd2;
	int		rr, gg, bb;
	

	if (gLoadBuffer == NULL) {
		LOGD("LoadImageJpeg : gLoadBuffer is null");
		return -1;
	}

	jpeg_decompress_struct		in_info;
	jpeg_error_mgr				jpeg_error;

	JSAMPROW	buffer[1];

	in_info.err = jpeg_std_error(&jpeg_error);		//エラーハンドラ設定
	jpeg_error.error_exit = _JpegError;					//エラーハンドラ設定

	jpeg_create_decompress(&in_info);							//
	jpeg_memory_src(&in_info, gLoadBuffer, gLoadFileSize);		//読込ファイル設定
	jpeg_read_header(&in_info,TRUE);							//ヘッダー読込

	if (quality == QUALITY_LOW) {
		// 高速化
		in_info.dct_method = JDCT_IFAST;
		in_info.do_fancy_upsampling = FALSE;
		in_info.dither_mode = JDITHER_NONE;	// 関係ないっぽいけど一応
		in_info.two_pass_quantize    = FALSE;
		in_info.quantize_colors      = FALSE;
//		LOGI("Jpeg : Low");
	}
	else {
		// 画質優先
		in_info.dct_method = JDCT_ISLOW;
		in_info.do_fancy_upsampling = TRUE;
		in_info.dither_mode = JDITHER_FS;	// 関係ないっぽいけど一応
//		LOGI("Jpeg : High");
	}

	// 倍率
	in_info.scale_num = 1;
	in_info.scale_denom = 1;
	if (scale % 8 == 0) {
		in_info.scale_denom = 8;
		scale /= 8;
	}
	else if (scale % 4 == 0) {
		in_info.scale_denom = 4;
		scale /= 4;
	}
	else if (scale % 2 == 0) {
		in_info.scale_denom = 2;
		scale /= 2;
	}

	jpeg_start_decompress(&in_info);					//デコードスタート

	//JPEG 1ラインのバイト数
	int width  = ROUNDUP_DIV(in_info.output_width, scale);
	int height = ROUNDUP_DIV(in_info.output_height, scale);
	int components = in_info.output_components;

	// イメージサイズを設定
	pData->OrgWidth = width;
	pData->OrgHeight = height;

#ifdef DEBUG
 	LOGD("LoadImageJpeg : scl=%d, sn=%d, sd=%d, w=%d, h=%d, sw=%d, sh=%d, comp=%d", scale, in_info.scale_num, in_info.scale_denom, in_info.output_width, in_info.output_height, width, height, components);
#endif

	int buffsize = in_info.output_width * components + 10;
	JSAMPLE *pSample = (JSAMPLE*)malloc(buffsize * sizeof(JSAMPLE));
	if (pSample == NULL) {
		LOGD("LoadImageJpeg : MAlloc Error. size=%d", buffsize * sizeof(JSAMPLE));
		return -7;
	}
	buffer[0] = pSample;

	int buffindex = -1;
	int buffpos = 0;
	int linesize = (width + HOKAN_DOTS);
	int ret = 0;
	WORD *buffptr = NULL;

	yy = 0;
	while(yy < height) {
		if (gCancel) {
			LOGD("LoadImageJpeg : cancel.");
			ReleaseBuff(page, -1, -1);
			ret = -10;
			break;
		}

		// ライン毎のバッファの位置を保存
		jpeg_read_scanlines(&in_info, buffer, 1);		//Jpegを1ライン読み込む
//		LOGD("DEBUG1:oh=%d, sh=%d, yy=%d, sc=%d, bpos=%d", in_info.output_height, height, yy, in_info.output_scanline, buffpos);

		if (buffindex < 0 || BLOCKSIZE - buffpos < linesize) {
			for (buffindex ++ ; buffindex < gBuffNum ; buffindex ++) {
				if (gBuffMng[buffindex].Page == -1) {
					break;
				}
			}
			if (buffindex >= gBuffNum) {
				// 領域不足
				ret = -5;
				break;
			}
			buffpos = 0;
			gBuffMng[buffindex].Page = page;
			gBuffMng[buffindex].Type = 0;
			gBuffMng[buffindex].Half = 0;
			gBuffMng[buffindex].Size = 0;
			gBuffMng[buffindex].Index = 0;
		}

		buffptr = gBuffMng[buffindex].Buff + buffpos + HOKAN_DOTS / 2;

		// データセット
		yd3 = gDitherY_3bit[yy & 0x07];
		yd2 = gDitherY_2bit[yy & 0x03];
		if (components == 3) {
			for (xx = 0, sx = 0 ; xx < width ; xx ++, sx += 3/*+= scale * 3*/) {
				rr = pSample[sx];
				gg = pSample[sx + 1];
				bb = pSample[sx + 2];

				// 切り捨ての値を分散
				if (rr < 0xF8) {
					rr = rr + gDitherX_3bit[rr & 0x07][(xx + yd3) & 0x07];
				}
				if (gg < 0xFC) {
					gg = gg + gDitherX_2bit[gg & 0x03][(xx + yd2) & 0x03];
				}
				if (bb < 0xF8) {
					bb = bb + gDitherX_3bit[bb & 0x07][(xx + yd3) & 0x07];
				}

				buffptr[xx] = MAKE565(rr, gg, bb);
			}
		}
		else {
			for (xx = 0, sx = 0 ; xx < width ; xx ++, sx ++/*+= scale*/) {
				rr = gg = pSample[sx];

				// 切り捨ての値を分散
				if (rr < 0xF8) {
					rr = rr + gDitherX_3bit[rr & 0x07][(xx + yd3) & 0x07];
				}
				if (gg < 0xFC) {
					gg = gg + gDitherX_2bit[gg & 0x03][(xx + yd2) & 0x03];
				}

				buffptr[xx] = MAKE565(rr, gg, rr);	// rrとbbは同値なので
			}
		}

		// 補完時に範囲外を参照するのを防ぐため余分につけとく
		buffptr[-2] = buffptr[0];
		buffptr[-1] = buffptr[0];
		buffptr[width + 0] = buffptr[width - 1];
		buffptr[width + 1] = buffptr[width - 1];

		// go to next line
		buffpos += linesize;
		gBuffMng[buffindex].Size += linesize;
		yy++;

		// 縮小対応
//		for (int i = 1 ; i < scale ; i ++) {
//			jpeg_read_scanlines(&in_info, buffer, 1);		//Jpegを縮小分ラインを読み飛ばし
//		}
	}

	// 残りのラインを読み飛ばし
	while (in_info.output_scanline < in_info.output_height) {
		jpeg_read_scanlines(&in_info, buffer, 1);
	}

//	LOGI("Jpeg : end3");

	if (ret == 0) {
		jpeg_finish_decompress(&in_info);		// 読み込み終了処理
	}
	jpeg_destroy_decompress(&in_info);

//	LOGI("Jpeg : end2");

	pData->UseFlag = 1;
	free (pSample);
//	LOGI("Jpeg : end1");
	return ret;
}
