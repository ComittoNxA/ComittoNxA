//#define DEBUG

#include <time.h>
#include <malloc.h>
#include <string.h>
#include <stdio.h>
#include <android/log.h>
#include <png.h>

#include "Image.h"

extern char			*gLoadBuffer;
extern long			gLoadFileSize;
extern int			gLoadError;
static int			gLoadOffset;

extern BUFFMNG		*gBuffMng;
extern long			gBuffNum;

extern int			gCancel;

extern char gDitherX_3bit[8][8];
extern char gDitherX_2bit[4][4];
extern char gDitherY_3bit[8];
extern char gDitherY_2bit[4];

void png_memread_func(png_structp png_ptr, png_bytep buf, png_size_t size) {
	if (gLoadOffset + size <= gLoadFileSize) {
		// ファイルサイズを超えない間コピー
		memcpy(buf, gLoadBuffer + gLoadOffset, size);
		gLoadOffset += size;
	} else {
		png_error(png_ptr,"data error.");
	}
}


//void png_data_read(png_structp png_ptr, PNG_BUFFER *png_buff) {
//	png_set_read_fn(png_ptr, (png_voidp) png_buff, (png_rw_ptr)png_memread_func);
//}

int LoadImagePng(IMAGEDATA *pData, int page, int scale)
{
	int				fPngCheck;
	png_uint_32		width;
	png_uint_32		height;
	int				bpp;
	int				color_type;
    int				interlace_type;
    int				compression_type;
    int				filter_type;
	png_uint_32		image_bytes;
	png_uint_32		row_bytes;

    int				num_palette = 0;
    png_color		*palette;

	int				components;

	int buffindex;
	int buffpos;
	int linesize;
	WORD *buffptr;

    int				ret = 0;
    int chgflag = 0;

	png_structp	pPng = NULL;
	png_infop	pInfo = NULL;
	png_bytepp	ppRowImage = NULL;

	gLoadOffset = 0;

	fPngCheck = png_check_sig((png_bytep)gLoadBuffer, 8);
	if (!fPngCheck) {
		LOGE("LoadImagePng : Illigal PNG Format.");
		return -5;
	}

	pPng = png_create_read_struct(PNG_LIBPNG_VER_STRING, NULL,NULL,NULL);
	if (!pPng) {
		LOGE("LoadImagePng : png_create_read_struct == NULL.");
		ret = -6;
		goto ERROREND;
	}

	pInfo = png_create_info_struct(pPng);
	if (!pInfo) {
		LOGE("LoadImagePng : png_create_info_struct == NULL.");
		ret = -7;
		goto ERROREND;
	}

//	png_data_read(pPng, NULL);
	png_set_read_fn(pPng, (png_voidp)NULL, (png_rw_ptr)png_memread_func);
	png_read_info(pPng, pInfo);
	png_get_IHDR(pPng, pInfo, &width, &height, &bpp, &color_type
						, &interlace_type, &compression_type, &filter_type);

	row_bytes	= png_get_rowbytes(pPng,pInfo);
	image_bytes	= height * row_bytes;
	components = row_bytes / width;

//	LOGI("LoadImagePng : stap1 pg=%d, w=%d, h=%d bpp=%d ct=%d, rb=%d, np=%d", page, width, height, bpp, color_type, row_bytes, num_palette);

	if (color_type == PNG_COLOR_TYPE_PALETTE) {
		png_get_PLTE(pPng, pInfo, &palette, &num_palette);

//		png_set_palette_to_rgb(pPng);
//		// png_set_expand(pPng);
//
//		// 設定反映＆再取得
//		png_read_update_info(pPng, pInfo);
//		png_get_IHDR(pPng, pInfo, &width, &height, &bpp, &color_type
//							, &interlace_type, &compression_type, &filter_type);
//		row_bytes	= png_get_rowbytes(pPng, pInfo);
//		image_bytes	= height * row_bytes;
//		components = row_bytes / width;
//
//		LOGI("LoadImagePng : stap2 pg=%d, w=%d, h=%d bpp=%d ct=%d, rb=%d, np=%d", page, width, height, bpp, color_type, row_bytes, num_palette);
	}
	else {
//		if (color_type == PNG_COLOR_TYPE_GRAY && bpp < 8) {
//			// png_set_gray_1_2_4_to_8(pPng);
//			png_set_expand(pPng);
//			chgflag = 1;
//		}
	
		if (png_get_valid(pPng, pInfo, PNG_INFO_tRNS)) {
			// png_set_tRNS_to_alpha(pPng);
			png_set_expand(pPng);
			chgflag = 1;
		}
	
		if (bpp > 8) {
			png_set_strip_16(pPng);
			chgflag = 1;
		}
	
		if (color_type & PNG_COLOR_MASK_ALPHA) {
			png_set_strip_alpha(pPng);
			// png_set_add_alpha(pPng, 0xff, PNG_FILLER_AFTER);
			chgflag = 1;
		}
	
//		if (color_type == PNG_COLOR_TYPE_GRAY) {
//			png_set_gray_to_rgb(pPng);
//			chgflag = 1;
//		}
	
		if (chgflag) {
			// 設定反映＆再取得
			png_read_update_info(pPng, pInfo);
			png_get_IHDR(pPng, pInfo, &width, &height, &bpp, &color_type
								, &interlace_type, &compression_type, &filter_type);
			row_bytes	= png_get_rowbytes(pPng, pInfo);
			image_bytes	= height * row_bytes;
			components = row_bytes / width;
	
//			LOGI("LoadImagePng : step3 pg=%d, w=%d, h=%d bpp=%d ct=%d, rb=%d, np=%d", page, width, height, bpp, color_type, row_bytes, num_palette);
		}
	}

	buffindex = -1;
	buffpos   = 0;
	linesize  = (width + HOKAN_DOTS);
	buffptr   = NULL;

	//画像データ用バッファーの確保
	ppRowImage = (png_bytepp)malloc(sizeof(png_bytep) * height + image_bytes);
	if (ppRowImage == NULL) {
		ret = -8;
		goto ERROREND;
	}

	int		yy, xx, alpha;
	//画像データ用バッファーの初期化
	{
		png_bytep	pRowImage;

		pRowImage = (png_bytep)&(ppRowImage[height]);
		for(yy = 0; yy < height; yy++)
		{
			ppRowImage[yy] = pRowImage;
			pRowImage += row_bytes;
		}
	}

	//画像ファイル読み込み
	png_read_image(pPng, ppRowImage);

	yy = 0;
	for(yy = 0; yy < height; yy++)
	{
		if (gCancel) {
			LOGD("LoadImagePng : cancel.");
			ReleaseBuff(page, -1, -1);
			ret = -9;
			break;
		}

		// ライン毎のバッファの位置を保存
		if (buffindex < 0 || BLOCKSIZE - buffpos < linesize) {
			for (buffindex ++ ; buffindex < gBuffNum ; buffindex ++) {
				if (gBuffMng[buffindex].Page == -1) {
					break;
				}
			}
			if (buffindex >= gBuffNum) {
				// 領域不足
				ret = -10;
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
//		LOGD("DEBUG2:yy=%d, idx=%d, pos=%d", yy, buffindex, buffpos);

		unsigned char*		pImagePixel;
		pImagePixel = (unsigned char*)(ppRowImage[yy]);		//画像ファイルの行データ

		int yd3 = gDitherY_3bit[yy & 0x07];
		int yd2 = gDitherY_2bit[yy & 0x03];
		int rr, gg, bb;

		if (color_type == PNG_COLOR_TYPE_PALETTE) {
			if (bpp == 1) {
				int palIndex;
				for (xx = 0 ; xx < width ; xx += 8) {
					palIndex = (pImagePixel[xx / 8] & 0x80) ? 1 : 0;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				for (xx = 1 ; xx < width ; xx += 8) {
					palIndex = (pImagePixel[xx / 8] & 0x40) ? 1 : 0;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				for (xx = 2 ; xx < width ; xx += 8) {
					palIndex = (pImagePixel[xx / 8] & 0x20) ? 1 : 0;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				for (xx = 3 ; xx < width ; xx += 8) {
					palIndex = (pImagePixel[xx / 8] & 0x10) ? 1 : 0;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				for (xx = 4 ; xx < width ; xx += 8) {
					palIndex = (pImagePixel[xx / 8] & 0x08) ? 1 : 0;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				for (xx = 5 ; xx < width ; xx += 8) {
					palIndex = (pImagePixel[xx / 8] & 0x04) ? 1 : 0;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				for (xx = 6 ; xx < width ; xx += 8) {
					palIndex = (pImagePixel[xx / 8] & 0x02) ? 1 : 0;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				for (xx = 7 ; xx < width ; xx += 8) {
					palIndex = (pImagePixel[xx / 8] & 0x01) ? 1 : 0;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
			}
			else if (bpp == 2) {
				// 4色
				// パレット参照
				int palIndex;
				for (xx = 0 ; xx < width ; xx += 4) {
					palIndex = (pImagePixel[xx / 4] >> 6) & 0x03;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				for (xx = 1 ; xx < width ; xx += 4) {
					palIndex = (pImagePixel[xx / 4] >> 4) & 0x03;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				for (xx = 2 ; xx < width ; xx += 4) {
					palIndex = (pImagePixel[xx / 4] >> 2) & 0x03;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				for (xx = 3 ; xx < width ; xx += 4) {
					palIndex = pImagePixel[xx / 4] & 0x03;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
			}
			else if (bpp == 4) {
				// 16色
				// パレット参照
				int palIndex;
				// 偶数位置
				for (xx = 0 ; xx < width ; xx += 2) {
					palIndex = (pImagePixel[xx / 2] >> 4) & 0x0F;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
				// 奇数位置
				for (xx = 1 ; xx < width ; xx += 2) {
					palIndex = pImagePixel[xx / 2] & 0x0F;
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
			}
			else if (bpp == 8) {
				// 256色
				// パレット参照
				int palIndex;
				for (xx = 0 ; xx < width ; xx ++) {
					palIndex = pImagePixel[xx];
					buffptr[xx] = MAKE565(palette[palIndex].red, palette[palIndex].green, palette[palIndex].blue);
				}
			}
			else {
				memset(&buffptr[2], 0x80, width * sizeof(WORD));
			}
		}
		else if (color_type == PNG_COLOR_TYPE_GRAY) {
			if (bpp == 1) {
				// 1bit グレースケール
				int color;
				for (xx = 0 ; xx < width ; xx += 8) {
					color = (pImagePixel[xx / 8] & 0x80) ? 255 : 0;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 1 ; xx < width ; xx += 8) {
					color = (pImagePixel[xx / 8] & 0x40) ? 255 : 0;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 2 ; xx < width ; xx += 8) {
					color = (pImagePixel[xx / 8] & 0x20) ? 255 : 0;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 3 ; xx < width ; xx += 8) {
					color = (pImagePixel[xx / 8] & 0x10) ? 255 : 0;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 4 ; xx < width ; xx += 8) {
					color = (pImagePixel[xx / 8] & 0x08) ? 255 : 0;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 5 ; xx < width ; xx += 8) {
					color = (pImagePixel[xx / 8] & 0x04) ? 255 : 0;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 6 ; xx < width ; xx += 8) {
					color = (pImagePixel[xx / 8] & 0x02) ? 255 : 0;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 7 ; xx < width ; xx += 8) {
					color = (pImagePixel[xx / 8] & 0x01) ? 255 : 0;
					buffptr[xx] = MAKE565(color, color, color);
				}
			}
			else if (bpp == 2) {
				// 2bit グレースケール
				int color;
				for (xx = 0 ; xx < width ; xx += 4) {
					color = (pImagePixel[xx / 4] >> 6) & 0x03;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 1 ; xx < width ; xx += 4) {
					color = (pImagePixel[xx / 4] >> 4) & 0x03;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 2 ; xx < width ; xx += 4) {
					color = (pImagePixel[xx / 4] >> 2) & 0x03;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 3 ; xx < width ; xx += 4) {
					color = (pImagePixel[xx / 4] >> 0) & 0x03;
					buffptr[xx] = MAKE565(color, color, color);
				}
			}
			else if (bpp == 4) {
				// 4bit グレースケール
				int color;
				for (xx = 0 ; xx < width ; xx += 2) {
					color = (pImagePixel[xx / 2] >> 0) & 0xF0;
					buffptr[xx] = MAKE565(color, color, color);
				}
				for (xx = 1 ; xx < width ; xx += 2) {
					color = (pImagePixel[xx / 2] << 4) & 0xF0;
					buffptr[xx] = MAKE565(color, color, color);
				}
			}
			else if (bpp == 8) {
				// 8bit グレースケール
				for (xx = 0 ; xx < width ; xx ++) {
					buffptr[xx] = MAKE565(pImagePixel[xx], pImagePixel[xx], pImagePixel[xx]);
				}
			}
			else if (bpp == 16) {
				// 16bit グレースケール
				// どっちが上位ビットかわからん
				for (xx = 0 ; xx < width ; xx ++) {
					buffptr[xx] = MAKE565(pImagePixel[xx * 2], pImagePixel[xx * 2], pImagePixel[xx * 2]);
				}
			}
		}
//		if (color_type == PNG_COLOR_TYPE_GRAY_ALPHA) {
//			if (components == 2) {
//				// 
//				for (xx = 0 ; xx < width ; xx ++) {
//					alpha = pImagePixel[xx * 2 + 1];
//					rr = pImagePixel[xx * 2] * alpha / 255;
//					gg = pImagePixel[xx * 2] * alpha / 255;
//					bb = pImagePixel[xx * 2] * alpha / 255;
//
//					// 切り捨ての値を分散
//					if (rr < 0xF8) {
//						rr = rr + gDitherX_3bit[rr & 0x07][(xx + yd3) & 0x07];
//					}
//					if (gg < 0xFC) {
//						gg = gg + gDitherX_2bit[gg & 0x03][(xx + yd2) & 0x03];
//					}
//					if (bb < 0xF8) {
//						bb = bb + gDitherX_3bit[bb & 0x07][(xx + yd3) & 0x07];
//					}
//					buffptr[xx] = MAKE565(rr, gg, bb);
//				}
//			}
//		}
//		if (color_type == PNG_COLOR_TYPE_RGB_ALPHA) {
//			if (components == 4) {
//				for (xx = 0 ; xx < width ; xx ++) {
//					alpha = pImagePixel[xx * 4 + 3];
//					rr = pImagePixel[xx * 4] * alpha / 255 + (255 - alpha);
//					gg = pImagePixel[xx * 4 + 1] * alpha / 255 + (255 - alpha);
//					bb = pImagePixel[xx * 4 + 2] * alpha / 255 + (255 - alpha);
//
//					// 切り捨ての値を分散
//					if (rr < 0xF8) {
//						rr = rr + gDitherX_3bit[rr & 0x07][(xx + yd3) & 0x07];
//					}
//					if (gg < 0xFC) {
//						gg = gg + gDitherX_2bit[gg & 0x03][(xx + yd2) & 0x03];
//					}
//					if (bb < 0xF8) {
//						bb = bb + gDitherX_3bit[bb & 0x07][(xx + yd3) & 0x07];
//					}
//					buffptr[xx] = MAKE565(rr, gg, bb);
////					buffptr[xx] = MAKE565(pImagePixel[xx * 4] * alpha / 255, pImagePixel[xx * 4 + 1] * alpha / 255, pImagePixel[xx * 4 + 2] * alpha / 255);
//				}
//			}
//		}
		else if (color_type == PNG_COLOR_TYPE_RGB) {
			if (components == 3) {
				for (xx = 0 ; xx < width ; xx ++) {
					rr = pImagePixel[xx * 3];
					gg = pImagePixel[xx * 3 + 1];
					bb = pImagePixel[xx * 3 + 2];

// LOGD("RGB : rr=%02x, gg=%02x, bb=%02x", (int)pImagePixel[xx * 4], (int)pImagePixel[xx * 4 + 1], (int)pImagePixel[xx * 4 + 2]);

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
//					buffptr[xx] = MAKE565(pImagePixel[xx * 3], pImagePixel[xx * 3 + 1], pImagePixel[xx * 3 + 2]);
				}
			}
//			else if (components == 2) {
//				for (xx = 0 ; xx < width ; xx ++) {
//					buffptr[xx] = pImagePixel[xx * 2];
//				}
//			}
		}
		// 補完用の余裕
		buffptr[-2] = buffptr[0];
		buffptr[-1] = buffptr[0];
		buffptr[width + 0] = buffptr[width - 1];
		buffptr[width + 1] = buffptr[width - 1];

		// go to next line
		buffpos += linesize;
		gBuffMng[buffindex].Size += linesize;
	}

	pData->UseFlag = 1;
	pData->OrgWidth = width;
	pData->OrgHeight = height;

ERROREND:
	if(ppRowImage)
	{
		free(ppRowImage);
		ppRowImage = NULL;
	}

	if(pPng)
	{
		if (pInfo) {
			png_destroy_read_struct(&pPng, &pInfo, NULL);
		}
		else {
			png_destroy_read_struct(&pPng, NULL, NULL);
		}

		pPng = NULL;
		pInfo = NULL;
	}
	return ret;
}
