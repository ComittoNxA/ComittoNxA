#include <malloc.h>
#include <string.h>
#include <android/log.h>
#include "../zlib/zlib.h"
#include "PdfCrypt.h"

void predict_png(BYTE *out, BYTE *in, int len, int predictor, int bpp, BYTE *ref);
void predict_tiff(BYTE *out, BYTE *in, int len, int columns, int colors, int bpc, int stride);
inline int getcomponent(unsigned char *line, int x, int bpc);
inline void putcomponent(BYTE *buf, int x, int bpc, int value);
inline int paeth(int a, int b, int c);

enum { MAXC = 32 };

int PredictDecode(BYTE *inbuf, int insize, int predictor, int columns, int colors, int bpc)
{
	int result = 0;

	if (predictor != 1  && predictor != 2
		&& predictor != 10 && predictor != 11
			&& predictor != 12 && predictor != 13
				&& predictor != 14 && predictor != 15) {
		LOGW("invalid predictor: %d", predictor);
		predictor = 1;
	}

	int stride = (bpc * colors * columns + 7) / 8;
	int bpp = (bpc * colors + 7) / 8;

	BYTE *outbuf = init_buffer();
	int outpos = 0;

	// 読み込み処理
	// read_predict(fz_stream *stm, unsigned char *buf, int len)
	int ispng = predictor >= 10;
	int n;
	int inpos = 0;

	BYTE *out = (BYTE*)malloc(sizeof(BYTE) * stride);
	BYTE *ref = (BYTE*)malloc(sizeof(BYTE) * stride);
	if (out == NULL || ref == NULL) {
		result = -1;
		goto error_end;
	}
	memset(ref, 0, stride);

//	LOGD("insize=%d, stride=%d, ispng=%d", insize, stride, ispng);

	while (true) {	// 全部読み込み
		n = stride + ispng;
		if (n > insize - inpos) {
			n = insize - inpos;
		}
//		LOGD("insize=%d, inpos=%d, n=%d, outpos=%d", insize, inpos, n, outpos);

		if (n == 0) {
			// 最後まで読み込み
			set_buffer(outpos);
			break;
		}

		if (predictor == 1) {
			// 無変換
			memcpy(out, &inbuf[inpos], n);
		}
		else if (predictor == 2) {
			// tiff型式?
			predict_tiff(out, &inbuf[inpos], n, columns, colors, bpc, stride);
		}
		else
		{
			predict_png(out, &inbuf[inpos + 1], n - 1, inbuf[inpos], bpp, ref);
			memcpy(ref, out, stride);
		}

		for (int i = 0 ; i < n - ispng ; i ++) {
			if (outpos >= BUFFER_SIZE) {
				set_buffer(BUFFER_SIZE);
				outbuf = add_buffer();
				if (outbuf == NULL) {
					LOGE("PredictDecode: add_buffer(result == NULL)");
					result = -2;
					goto error_end;
				}
				outpos = 0;
			}
			outbuf[outpos++] = out[i];
		}
		inpos += n;
	}

error_end:
	if (out != NULL) {
		free(out);
	}
	if (ref != NULL) {
		free(ref);
	}
	return result;
}

void predict_png(BYTE *out, BYTE *in, int len, int predictor, int bpp, BYTE *ref)
{
//	LOGD("predict_png: len=%d, pred=%d, bpp=%d", len, predictor, bpp);
	int i;

	switch (predictor)
	{
	case 0:
		memcpy(out, in, len);
		break;
	case 1:
		for (i = bpp; i > 0; i--)
		{
			*out++ = *in++;
		}
		for (i = len - bpp; i > 0; i--)
		{
			*out = *in++ + out[-bpp];
			out++;
		}
		break;
	case 2:
		for (i = bpp; i > 0; i--)
		{
			*out++ = *in++ + *ref++;
		}
		for (i = len - bpp; i > 0; i--)
		{
			*out++ = *in++ + *ref++;
		}
		break;
	case 3:
		for (i = bpp; i > 0; i--)
		{
			*out++ = *in++ + (*ref++) / 2;
		}
		for (i = len - bpp; i > 0; i--)
		{
			*out = *in++ + (out[-bpp] + *ref++) / 2;
			out++;
		}
		break;
	case 4:
		for (i = bpp; i > 0; i--)
		{
			*out++ = *in++ + paeth(0, *ref++, 0);
		}
		for (i = len - bpp; i > 0; i --)
		{
			*out = *in++ + paeth(out[-bpp], *ref, ref[-bpp]);
			ref++;
			out++;
		}
		break;
	}
}

void predict_tiff(BYTE *out, BYTE *in, int len, int columns, int colors, int bpc, int stride)
{
	int left[MAXC];
	int i, k;
	const int mask = (1 << bpc)-1;

	for (k = 0; k < colors; k++)
		left[k] = 0;
	memset(out, 0, stride);

	for (i = 0; i < columns; i++)
	{
		for (k = 0; k < colors; k++)
		{
			int a = getcomponent(in, i * colors + k, bpc);
			int b = a + left[k];
			int c = b & mask;
			putcomponent(out, i * colors + k, bpc, c);
			left[k] = c;
		}
	}
}

inline int getcomponent(unsigned char *line, int x, int bpc)
{
	switch (bpc)
	{
	case 1: return (line[x >> 3] >> ( 7 - (x & 7) ) ) & 1;
	case 2: return (line[x >> 2] >> ( ( 3 - (x & 3) ) << 1 ) ) & 3;
	case 4: return (line[x >> 1] >> ( ( 1 - (x & 1) ) << 2 ) ) & 15;
	case 8: return line[x];
	case 16: return (line[x<<1]<<8)+line[(x<<1)+1];
	}
	return 0;
}

inline void putcomponent(unsigned char *buf, int x, int bpc, int value)
{
	switch (bpc)
	{
	case 1: buf[x >> 3] |= value << (7 - (x & 7)); break;
	case 2: buf[x >> 2] |= value << ((3 - (x & 3)) << 1); break;
	case 4: buf[x >> 1] |= value << ((1 - (x & 1)) << 2); break;
	case 8: buf[x] = value; break;
	case 16: buf[x<<1] = value>>8; buf[(x<<1)+1] = value; break;
	}
}

inline int paeth(int a, int b, int c)
{
	/* The definitions of ac and bc are correct, not a typo. */
	int ac = b - c, bc = a - c, abcc = ac + bc;
	int pa = fz_absi(ac);
	int pb = fz_absi(bc);
	int pc = fz_absi(abcc);
	return pa <= pb && pa <= pc ? a : pb <= pc ? b : c;
}
