/* Fax G3/G4 decoder */

/* TODO: uncompressed */

/*
<raph> the first 2^(initialbits) entries map bit patterns to decodes
<raph> let's say initial_bits is 8 for the sake of example
<raph> and that the code is 1001
<raph> that means that entries 0x90 .. 0x9f have the entry { val, 4 }
<raph> because those are all the bytes that start with the code
<raph> and the 4 is the length of the code
... if (n_bits > initial_bits) ...
<raph> anyway, in that case, it basically points to a mini table
<raph> the n_bits is the maximum length of all codes beginning with that byte
<raph> so 2^(n_bits - initial_bits) is the size of the mini-table
<raph> peter came up with this, and it makes sense
*/
#include <time.h>
#include <malloc.h>
#include <string.h>
#include <stdio.h>
#include <setjmp.h>
#include <android/log.h>
#include "../zlib/zlib.h"

#include "Image.h"

extern char			*gLoadBuffer;
extern long			gLoadFileSize;

extern jmp_buf		gJmpBuff;

extern BUFFMNG		*gBuffMng;
extern long			gBuffNum;

extern int			gCancel;

typedef struct cfd_node_s cfd_node;

static int unpack_tile(int page, int scale, unsigned char *src, int cx, int cy, int n, int depth);
static int unpack_tile_bitmap(unsigned char *src, int width, int height, int n, int depth, WORD *dst, int dst_stride, int dst_size, int scale);

typedef struct _fax_params
{
	int k;
	int end_of_line;
	int encoded_byte_align;
	int columns;
	int rows;
	int end_of_block;
	int black_is_1;
	
	int stride;
	int ridx;
	
	int bidx;
	unsigned int word;
	
	int stage;
	
	int a, c, dim, eolc;

	unsigned char *ref;
	unsigned char *dst;
	unsigned char *rp;
	unsigned char *wp;

	int inpos;
} FAX_PARAMS;

struct cfd_node_s
{
	short val;
	short nbits;
};

enum
{
	cfd_white_initial_bits = 8,
	cfd_black_initial_bits = 7,
	cfd_2d_initial_bits = 7,
	cfd_uncompressed_initial_bits = 6	/* must be 6 */
};

/* non-run codes in tables */
enum
{
	ERROR = -1,
	ZEROS = -2, /* EOL follows, possibly with more padding first */
	UNCOMPRESSED = -3
};

/* semantic codes for cf_2d_decode */
enum
{
	P = -4,
	H = -5,
	VR3 = 0,
	VR2 = 1,
	VR1 = 2,
	V0 = 3,
	VL1 = 4,
	VL2 = 5,
	VL3 = 6
};

/* White decoding table. */
static const cfd_node cf_white_decode[] = {
	{256,12},{272,12},{29,8},{30,8},{45,8},{46,8},{22,7},{22,7},
	{23,7},{23,7},{47,8},{48,8},{13,6},{13,6},{13,6},{13,6},{20,7},
	{20,7},{33,8},{34,8},{35,8},{36,8},{37,8},{38,8},{19,7},{19,7},
	{31,8},{32,8},{1,6},{1,6},{1,6},{1,6},{12,6},{12,6},{12,6},{12,6},
	{53,8},{54,8},{26,7},{26,7},{39,8},{40,8},{41,8},{42,8},{43,8},
	{44,8},{21,7},{21,7},{28,7},{28,7},{61,8},{62,8},{63,8},{0,8},
	{320,8},{384,8},{10,5},{10,5},{10,5},{10,5},{10,5},{10,5},{10,5},
	{10,5},{11,5},{11,5},{11,5},{11,5},{11,5},{11,5},{11,5},{11,5},
	{27,7},{27,7},{59,8},{60,8},{288,9},{290,9},{18,7},{18,7},{24,7},
	{24,7},{49,8},{50,8},{51,8},{52,8},{25,7},{25,7},{55,8},{56,8},
	{57,8},{58,8},{192,6},{192,6},{192,6},{192,6},{1664,6},{1664,6},
	{1664,6},{1664,6},{448,8},{512,8},{292,9},{640,8},{576,8},{294,9},
	{296,9},{298,9},{300,9},{302,9},{256,7},{256,7},{2,4},{2,4},{2,4},
	{2,4},{2,4},{2,4},{2,4},{2,4},{2,4},{2,4},{2,4},{2,4},{2,4},{2,4},
	{2,4},{2,4},{3,4},{3,4},{3,4},{3,4},{3,4},{3,4},{3,4},{3,4},{3,4},
	{3,4},{3,4},{3,4},{3,4},{3,4},{3,4},{3,4},{128,5},{128,5},{128,5},
	{128,5},{128,5},{128,5},{128,5},{128,5},{8,5},{8,5},{8,5},{8,5},
	{8,5},{8,5},{8,5},{8,5},{9,5},{9,5},{9,5},{9,5},{9,5},{9,5},{9,5},
	{9,5},{16,6},{16,6},{16,6},{16,6},{17,6},{17,6},{17,6},{17,6},
	{4,4},{4,4},{4,4},{4,4},{4,4},{4,4},{4,4},{4,4},{4,4},{4,4},{4,4},
	{4,4},{4,4},{4,4},{4,4},{4,4},{5,4},{5,4},{5,4},{5,4},{5,4},{5,4},
	{5,4},{5,4},{5,4},{5,4},{5,4},{5,4},{5,4},{5,4},{5,4},{5,4},
	{14,6},{14,6},{14,6},{14,6},{15,6},{15,6},{15,6},{15,6},{64,5},
	{64,5},{64,5},{64,5},{64,5},{64,5},{64,5},{64,5},{6,4},{6,4},
	{6,4},{6,4},{6,4},{6,4},{6,4},{6,4},{6,4},{6,4},{6,4},{6,4},{6,4},
	{6,4},{6,4},{6,4},{7,4},{7,4},{7,4},{7,4},{7,4},{7,4},{7,4},{7,4},
	{7,4},{7,4},{7,4},{7,4},{7,4},{7,4},{7,4},{7,4},{-2,3},{-2,3},
	{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},
	{-1,0},{-1,0},{-1,0},{-1,0},{-3,4},{1792,3},{1792,3},{1984,4},
	{2048,4},{2112,4},{2176,4},{2240,4},{2304,4},{1856,3},{1856,3},
	{1920,3},{1920,3},{2368,4},{2432,4},{2496,4},{2560,4},{1472,1},
	{1536,1},{1600,1},{1728,1},{704,1},{768,1},{832,1},{896,1},
	{960,1},{1024,1},{1088,1},{1152,1},{1216,1},{1280,1},{1344,1},
	{1408,1}
};

/* Black decoding table. */
static const cfd_node cf_black_decode[] = {
	{128,12},{160,13},{224,12},{256,12},{10,7},{11,7},{288,12},{12,7},
	{9,6},{9,6},{8,6},{8,6},{7,5},{7,5},{7,5},{7,5},{6,4},{6,4},{6,4},
	{6,4},{6,4},{6,4},{6,4},{6,4},{5,4},{5,4},{5,4},{5,4},{5,4},{5,4},
	{5,4},{5,4},{1,3},{1,3},{1,3},{1,3},{1,3},{1,3},{1,3},{1,3},{1,3},
	{1,3},{1,3},{1,3},{1,3},{1,3},{1,3},{1,3},{4,3},{4,3},{4,3},{4,3},
	{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},
	{4,3},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},
	{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},
	{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},{3,2},
	{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},
	{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},
	{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},{2,2},
	{-2,4},{-2,4},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},
	{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-3,5},{1792,4},
	{1792,4},{1984,5},{2048,5},{2112,5},{2176,5},{2240,5},{2304,5},
	{1856,4},{1856,4},{1920,4},{1920,4},{2368,5},{2432,5},{2496,5},
	{2560,5},{18,3},{18,3},{18,3},{18,3},{18,3},{18,3},{18,3},{18,3},
	{52,5},{52,5},{640,6},{704,6},{768,6},{832,6},{55,5},{55,5},
	{56,5},{56,5},{1280,6},{1344,6},{1408,6},{1472,6},{59,5},{59,5},
	{60,5},{60,5},{1536,6},{1600,6},{24,4},{24,4},{24,4},{24,4},
	{25,4},{25,4},{25,4},{25,4},{1664,6},{1728,6},{320,5},{320,5},
	{384,5},{384,5},{448,5},{448,5},{512,6},{576,6},{53,5},{53,5},
	{54,5},{54,5},{896,6},{960,6},{1024,6},{1088,6},{1152,6},{1216,6},
	{64,3},{64,3},{64,3},{64,3},{64,3},{64,3},{64,3},{64,3},{13,1},
	{13,1},{13,1},{13,1},{13,1},{13,1},{13,1},{13,1},{13,1},{13,1},
	{13,1},{13,1},{13,1},{13,1},{13,1},{13,1},{23,4},{23,4},{50,5},
	{51,5},{44,5},{45,5},{46,5},{47,5},{57,5},{58,5},{61,5},{256,5},
	{16,3},{16,3},{16,3},{16,3},{17,3},{17,3},{17,3},{17,3},{48,5},
	{49,5},{62,5},{63,5},{30,5},{31,5},{32,5},{33,5},{40,5},{41,5},
	{22,4},{22,4},{14,1},{14,1},{14,1},{14,1},{14,1},{14,1},{14,1},
	{14,1},{14,1},{14,1},{14,1},{14,1},{14,1},{14,1},{14,1},{14,1},
	{15,2},{15,2},{15,2},{15,2},{15,2},{15,2},{15,2},{15,2},{128,5},
	{192,5},{26,5},{27,5},{28,5},{29,5},{19,4},{19,4},{20,4},{20,4},
	{34,5},{35,5},{36,5},{37,5},{38,5},{39,5},{21,4},{21,4},{42,5},
	{43,5},{0,3},{0,3},{0,3},{0,3}
};

/* 2-D decoding table. */
static const cfd_node cf_2d_decode[] = {
	{128,11},{144,10},{6,7},{0,7},{5,6},{5,6},{1,6},{1,6},{-4,4},
	{-4,4},{-4,4},{-4,4},{-4,4},{-4,4},{-4,4},{-4,4},{-5,3},{-5,3},
	{-5,3},{-5,3},{-5,3},{-5,3},{-5,3},{-5,3},{-5,3},{-5,3},{-5,3},
	{-5,3},{-5,3},{-5,3},{-5,3},{-5,3},{4,3},{4,3},{4,3},{4,3},{4,3},
	{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},{4,3},
	{2,3},{2,3},{2,3},{2,3},{2,3},{2,3},{2,3},{2,3},{2,3},{2,3},{2,3},
	{2,3},{2,3},{2,3},{2,3},{2,3},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},
	{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},
	{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},
	{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},
	{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},
	{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},{3,1},
	{3,1},{3,1},{3,1},{-2,4},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},
	{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},
	{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-1,0},{-3,3}
};

/* Uncompressed decoding table. */
static const cfd_node cf_uncompressed_decode[] = {
	{64,12},{5,6},{4,5},{4,5},{3,4},{3,4},{3,4},{3,4},{2,3},{2,3},
	{2,3},{2,3},{2,3},{2,3},{2,3},{2,3},{1,2},{1,2},{1,2},{1,2},{1,2},
	{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},
	{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},
	{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},
	{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},{0,1},
	{-1,0},{-1,0},{8,6},{9,6},{6,5},{6,5},{7,5},{7,5},{4,4},{4,4},
	{4,4},{4,4},{5,4},{5,4},{5,4},{5,4},{2,3},{2,3},{2,3},{2,3},{2,3},
	{2,3},{2,3},{2,3},{3,3},{3,3},{3,3},{3,3},{3,3},{3,3},{3,3},{3,3},
	{0,2},{0,2},{0,2},{0,2},{0,2},{0,2},{0,2},{0,2},{0,2},{0,2},{0,2},
	{0,2},{0,2},{0,2},{0,2},{0,2},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},
	{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2},{1,2}
};

/* bit magic */

static inline int getbit(const unsigned char *buf, int x)
{
	return ( buf[x >> 3] >> ( 7 - (x & 7) ) ) & 1;
}

static int
find_changing(const unsigned char *line, int x, int w)
{
	int a, b;

	if (!line)
		return w;

	if (x == -1)
	{
		a = 0;
		x = 0;
	}
	else
	{
		a = getbit(line, x);
		x++;
	}

	while (x < w)
	{
		b = getbit(line, x);
		if (a != b)
			break;
		x++;
	}

	return x;
}

static int
find_changing_color(const unsigned char *line, int x, int w, int color)
{
	if (!line)
		return w;

	x = find_changing(line, (x > 0 || !color) ? x : -1, w);

	if (x < w && getbit(line, x) != color)
		x = find_changing(line, x, w);

	return x;
}

static const unsigned char lm[8] = {
	0xFF, 0x7F, 0x3F, 0x1F, 0x0F, 0x07, 0x03, 0x01
};

static const unsigned char rm[8] = {
	0x00, 0x80, 0xC0, 0xE0, 0xF0, 0xF8, 0xFC, 0xFE
};

static inline void setbits(unsigned char *line, int x0, int x1)
{
	int a0, a1, b0, b1, a;

	a0 = x0 >> 3;
	a1 = x1 >> 3;

	b0 = x0 & 7;
	b1 = x1 & 7;

	if (a0 == a1)
	{
		if (b1)
			line[a0] |= lm[b0] & rm[b1];
	}
	else
	{
		line[a0] |= lm[b0];
		for (a = a0 + 1; a < a1; a++)
			line[a] = 0xFF;
		if (b1)
			line[a1] |= rm[b1];
	}
}

enum
{
	STATE_NORMAL,	/* neutral state, waiting for any code */
	STATE_MAKEUP,	/* got a 1d makeup code, waiting for terminating code */
	STATE_EOL,		/* at eol, needs output buffer space */
	STATE_H1, STATE_H2,	/* in H part 1 and 2 (both makeup and terminating codes) */
	STATE_DONE		/* all done */
};

static inline void eat_bits(FAX_PARAMS *fax, int nbits)
{
	fax->word <<= nbits;
	fax->bidx += nbits;
}

static int
fill_bits(FAX_PARAMS *fax)
{
	while (fax->bidx >= 8)
	{
		if (gLoadFileSize == fax->inpos) {
			return EOF;
		}
		int c = gLoadBuffer[(fax->inpos)++] & 0x000000FF;
		fax->bidx -= 8;
		fax->word |= c << fax->bidx;
	}
	return 0;
}

static int
get_code(FAX_PARAMS *fax, const cfd_node *table, int initialbits)
{
	unsigned int word = fax->word;
	int tidx = word >> (32 - initialbits);
	int val = table[tidx].val;
	int nbits = table[tidx].nbits;

	if (nbits > initialbits)
	{
		int mask = (1 << (32 - initialbits)) - 1;
		tidx = val + ((word & mask) >> (32 - nbits));
		val = table[tidx].val;
		nbits = initialbits + table[tidx].nbits;
	}

	eat_bits(fax, nbits);

	return val;
}

/* decode one 1d code */
static int dec1d(FAX_PARAMS *fax)
{
	int code;

	if (fax->a == -1)
		fax->a = 0;

	if (fax->c)
		code = get_code(fax, cf_black_decode, cfd_black_initial_bits);
	else
		code = get_code(fax, cf_white_decode, cfd_white_initial_bits);

	if (code == UNCOMPRESSED) {
		LOGE("uncompressed data in faxd");
		return false;
	}

	if (code < 0) {
		LOGE("negative code in 1d faxd");
		return false;
	}

	if (fax->a + code > fax->columns) {
		LOGE("overflow in 1d faxd");
		return false;
	}

	if (fax->c)
		setbits(fax->dst, fax->a, fax->a + code);

	fax->a += code;

	if (code < 64) {
		fax->c = !fax->c;
		fax->stage = STATE_NORMAL;
	}
	else {
		fax->stage = STATE_MAKEUP;
	}

	return true;
}

/* decode one 2d code */
static int dec2d(FAX_PARAMS *fax)
{
	int code, b1, b2;

	if (fax->stage == STATE_H1 || fax->stage == STATE_H2)
	{
		if (fax->a == -1)
			fax->a = 0;

		if (fax->c)
			code = get_code(fax, cf_black_decode, cfd_black_initial_bits);
		else
			code = get_code(fax, cf_white_decode, cfd_white_initial_bits);

		if (code == UNCOMPRESSED) {
			LOGE("uncompressed data in faxd");
			return false;
		}

		if (code < 0) {
			LOGE("negative code in 2d faxd");
			return false;
		}

		if (fax->a + code > fax->columns) {
			LOGE("overflow in 2d faxd");
			return false;
		}

		if (fax->c)
			setbits(fax->dst, fax->a, fax->a + code);

		fax->a += code;

		if (code < 64)
		{
			fax->c = !fax->c;
			if (fax->stage == STATE_H1)
				fax->stage = STATE_H2;
			else if (fax->stage == STATE_H2)
				fax->stage = STATE_NORMAL;
		}

		return true;
	}

	code = get_code(fax, cf_2d_decode, cfd_2d_initial_bits);

	switch (code)
	{
	case H:
		fax->stage = STATE_H1;
		break;

	case P:
		b1 = find_changing_color(fax->ref, fax->a, fax->columns, !fax->c);
		if (b1 >= fax->columns)
			b2 = fax->columns;
		else
			b2 = find_changing(fax->ref, b1, fax->columns);
		if (fax->c) setbits(fax->dst, fax->a, b2);
		fax->a = b2;
		break;

	case V0:
		b1 = find_changing_color(fax->ref, fax->a, fax->columns, !fax->c);
		if (fax->c) setbits(fax->dst, fax->a, b1);
		fax->a = b1;
		fax->c = !fax->c;
		break;

	case VR1:
		b1 = 1 + find_changing_color(fax->ref, fax->a, fax->columns, !fax->c);
		if (b1 >= fax->columns) b1 = fax->columns;
		if (fax->c) setbits(fax->dst, fax->a, b1);
		fax->a = b1;
		fax->c = !fax->c;
		break;

	case VR2:
		b1 = 2 + find_changing_color(fax->ref, fax->a, fax->columns, !fax->c);
		if (b1 >= fax->columns) b1 = fax->columns;
		if (fax->c) setbits(fax->dst, fax->a, b1);
		fax->a = b1;
		fax->c = !fax->c;
		break;

	case VR3:
		b1 = 3 + find_changing_color(fax->ref, fax->a, fax->columns, !fax->c);
		if (b1 >= fax->columns) b1 = fax->columns;
		if (fax->c) setbits(fax->dst, fax->a, b1);
		fax->a = b1;
		fax->c = !fax->c;
		break;

	case VL1:
		b1 = -1 + find_changing_color(fax->ref, fax->a, fax->columns, !fax->c);
		if (b1 < 0) b1 = 0;
		if (fax->c) setbits(fax->dst, fax->a, b1);
		fax->a = b1;
		fax->c = !fax->c;
		break;

	case VL2:
		b1 = -2 + find_changing_color(fax->ref, fax->a, fax->columns, !fax->c);
		if (b1 < 0) b1 = 0;
		if (fax->c) setbits(fax->dst, fax->a, b1);
		fax->a = b1;
		fax->c = !fax->c;
		break;

	case VL3:
		b1 = -3 + find_changing_color(fax->ref, fax->a, fax->columns, !fax->c);
		if (b1 < 0) b1 = 0;
		if (fax->c) setbits(fax->dst, fax->a, b1);
		fax->a = b1;
		fax->c = !fax->c;
		break;

	case UNCOMPRESSED:
		LOGE("uncompressed data in faxd");
		return false;

	case ERROR:
		LOGE("invalid code in 2d faxd");
		return false;

	default:
		LOGE("invalid code in 2d faxd (%d)", code);
		return false;
	}
	return true;
}

int LoadImageCCITT(IMAGEDATA *pData, int page, int scale,
	int width, int height, int n, int depth,
	int k, int end_of_line, int encoded_byte_align,
	int columns, int rows, int end_of_block, int black_is_1,
	WORD *bitmap, int bmp_stride, int bmp_height)
{
#ifdef DEBUG
	LOGD("LoadImageCCITT : start (%d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d, %d)"
		, width, height, k, end_of_line, encoded_byte_align
			, columns, rows, end_of_block, black_is_1, n, depth
				, bmp_stride, bmp_height);
#endif

	if (gLoadBuffer == NULL) {
		LOGD("LoadImageCCITT : gLoadBuffer is null");
		return -1;
	}

	if (pData != NULL) {
		pData->OrgWidth  = ROUNDUP_DIV(width, scale);
		pData->OrgHeight = ROUNDUP_DIV(height, scale);
	}

	FAX_PARAMS fax;
	fax.k = k;
	fax.end_of_line = end_of_line;
	fax.encoded_byte_align = encoded_byte_align;
	fax.columns = columns;
	fax.rows = rows;
	fax.end_of_block = end_of_block;
	fax.black_is_1 = black_is_1;

	fax.stride = ((fax.columns - 1) >> 3) + 1;
	fax.ridx = 0;
	fax.bidx = 32;
	fax.word = 0;

	fax.stage = STATE_NORMAL;
	fax.a = -1;
	fax.c = 0;
	fax.dim = fax.k	 < 0 ? 2 : 1;
	fax.eolc = 0;

	fax.ref = (unsigned char *)malloc(fax.stride);
	fax.dst = (unsigned char *)malloc(fax.stride);
	fax.rp = fax.dst;
	fax.wp = fax.dst + fax.stride;
	fax.inpos = 0;

	memset(fax.ref, 0, fax.stride);
	memset(fax.dst, 0, fax.stride);

	// 
	int outsize = fax.stride * height;
	unsigned char *outbuff = (unsigned char *)malloc(outsize);

	// デコード処理
	unsigned char *p = outbuff;
	unsigned char *ep = outbuff + outsize;
	unsigned char *tmp;

	int result = 0;

	while (true) {
loop:

		if (fill_bits(&fax))
		{
			if (fax.bidx > 31)
			{
				if (fax.a > 0)
					goto eol;
				goto rtc;
			}
		}

		if ((fax.word >> (32 - 12)) == 0)
		{
			eat_bits(&fax, 1);
			continue;
		}

		if ((fax.word >> (32 - 12)) == 1)
		{
			eat_bits(&fax, 12);
			fax.eolc ++;

			if (fax.k > 0)
			{
				if (fax.a == -1)
					fax.a = 0;
				if ((fax.word >> (32 - 1)) == 1)
					fax.dim = 1;
				else
					fax.dim = 2;
				eat_bits(&fax, 1);
			}
		}
		else if (fax.k > 0 && fax.a == -1)
		{
			fax.a = 0;
			if ((fax.word >> (32 - 1)) == 1)
				fax.dim = 1;
			else
				fax.dim = 2;
			eat_bits(&fax, 1);
		}
		else if (fax.dim == 1)
		{
			fax.eolc = 0;
			if (dec1d(&fax) == false) {
				result = -3;
				break;
			}
		}
		else if (fax.dim == 2)
		{
			fax.eolc = 0;
			if (dec2d(&fax) == false) {
				result = -4;
				break;
			}
		}

		/* no eol check after makeup codes nor in the middle of an H code */
		if (fax.stage == STATE_MAKEUP || fax.stage == STATE_H1 || fax.stage == STATE_H2)
			continue;

		/* check for eol conditions */
		if (fax.eolc || fax.a >= fax.columns)
		{
			if (fax.a > 0)
				goto eol;
			if (fax.eolc == (fax.k < 0 ? 2 : 6))
				goto rtc;
		}
		continue;

eol:
		fax.stage = STATE_EOL;

		if (fax.black_is_1)
		{
			while (fax.rp < fax.wp && p < ep)
				*p++ = *fax.rp++;
		}
		else
		{
			while (fax.rp < fax.wp && p < ep)
				*p++ = *fax.rp++ ^ 0xff;
		}

		if (fax.rp < fax.wp) {
			result = -2;
			goto error_end;
		}

		tmp = fax.ref;
		fax.ref = fax.dst;
		fax.dst = tmp;
		memset(fax.dst, 0, fax.stride);

		fax.rp = fax.dst;
		fax.wp = fax.dst + fax.stride;

		fax.stage = STATE_NORMAL;
		fax.c = 0;
		fax.a = -1;
		fax.ridx ++;

		if (!fax.end_of_block && fax.rows)
		{
			if (fax.ridx >= fax.rows)
				goto rtc;
		}

		/* we have not read dim from eol, make a guess */
		if (fax.k > 0 && !fax.eolc && fax.a == -1)
		{
			if (fax.ridx % fax.k == 0)
				fax.dim = 1;
			else
				fax.dim = 2;
		}

		/* if end_of_line & encoded_byte_align, EOLs are *not* optional */
		if (fax.encoded_byte_align)
		{
			if (fax.end_of_line)
				eat_bits(&fax, (12 - fax.bidx) & 7);
			else
				eat_bits(&fax, (8 - fax.bidx) & 7);
		}

		/* no more space in output, don't decode the next row yet */
		if (p == outbuff + outsize) {
			break;
		}
	}

rtc:
error_end:
	if (fax.ref != NULL) {
		free(fax.ref);
	}
	if (fax.dst != NULL) {
		free(fax.dst);
	}

	fax.stage = STATE_DONE;

	if (result == 0) {
		if (pData != NULL) {
			result = unpack_tile(page, scale, outbuff, width, height, n, depth);
		}
		else {
			result = unpack_tile_bitmap(outbuff, width, height, n, depth, bitmap, bmp_stride, bmp_height, scale);
		}
	}
	if (outbuff != NULL) {
		free(outbuff);
	}
	
	if (result == 0 && pData != NULL) {
		pData->UseFlag = 1;
	}
	return result;
}

int LoadImageFlate(IMAGEDATA *pData, int page, int scale,
	int width, int height, int n, int depth,
	WORD *bitmap, int bmp_stride, int bmp_height)
{
#ifdef DEBUG
	LOGD("LoadImageFlate : start (%d, %d, %d, %d, %d, %d)", width, height, n, depth, bmp_stride, bmp_height);
#endif

	if (gLoadBuffer == NULL) {
		LOGD("LoadImageCCITT : gLoadBuffer is null");
		return -1;
	}

	if (pData != NULL) {
		pData->OrgWidth  = ROUNDUP_DIV(width, scale);
		pData->OrgHeight = ROUNDUP_DIV(height, scale);
	}

	int stride = (width * n * depth + 7) / 8;
	int outsize = stride * height;
	int memsize = (outsize + (sizeof(long long int) - 1)) / sizeof(long long int) * sizeof(long long int);
	unsigned char *outbuff = (unsigned char *)malloc(memsize);
	long long int *ptr = (long long int*)outbuff;

#ifdef DEBUG
	LOGD("LoadImageFlate : stride=%d, outsize=%d, insize=%d", stride, outsize, (int)gLoadFileSize);
#endif

	int result = 0;
	int status;

	/* すべてのメモリ管理をライブラリに任せる */
	z_stream z;
	z.zalloc = Z_NULL;
	z.zfree = Z_NULL;
	z.opaque = Z_NULL;

	/* 初期化 */
	z.next_in = Z_NULL;
	z.avail_in = 0;
	if (inflateInit(&z) != Z_OK) {
		LOGE("LoadImageFlate : %s\n", (z.msg) ? z.msg : "???");
		result = -1;
		goto error_end;
	}

	z.next_out = outbuff;		/* 出力ポインタ */
	z.avail_out = outsize;		/* 出力バッファ残量 */
	status = Z_OK;

	z.next_in = (BYTE*)gLoadBuffer;  /* 入力ポインタを入力バッファの先頭に */
	z.avail_in = gLoadFileSize; /* データを読み込む */

	while (status != Z_STREAM_END) {
#ifdef DEBUG
		LOGD("LoadImageFlate : st=%d, in=%d, out=%d", status, z.avail_in, z.avail_out);
#endif

		if (z.avail_in == 0) {		/* 入力残量がゼロになれば */
			LOGE("inflate: avail_in == 0");
			result = -2;
			goto error_end;
		}
		status = inflate(&z, Z_NO_FLUSH); /* 展開 */

		if (status == Z_STREAM_END) {
#ifdef DEBUG
			LOGD("LoadImageFlate : End - st=%d, in=%d, out=%d", status, z.avail_in, z.avail_out);
#endif
			/* 完了 */
			break;
		}
		if (status != Z_OK) {   /* エラー */
			LOGE("LoadImageFlate : inflate(%s)", (z.msg) ? z.msg : "???");
			result = -3;
			goto error_end;
		}
		if (z.avail_out == 0) {
			// 出力バッファが尽きた
			LOGE("LoadImageFlate: add_buffer(result == NULL)");
			result = -4;
			goto error_end;
		}
	}
	for (int i = 0 ; i < memsize / sizeof(long long int) ; i ++) {
		ptr[i] = ptr[i] ^ 0xFFFFFFFFFFFFFFFFLL;
	}

error_end:
	/* 後始末 */
	if (inflateEnd(&z) != Z_OK) {
		LOGE("LoadImageFlate : inflateEnd(%s)", (z.msg) ? z.msg : "???");
		result = -6;
	}

	if (result == 0) {
		if (pData != NULL) {
			result = unpack_tile(page, scale, outbuff, width, height, n, depth);
		}
		else {
			result = unpack_tile_bitmap(outbuff, width, height, n, depth, bitmap, bmp_stride, bmp_height, scale);
		}
	}
	if (outbuff != NULL) {
		free(outbuff);
	}
	if (result == 0 && pData != NULL) {
		pData->UseFlag = 1;
	}
	return result;
}

// 1バイトあたり8ドット
#define get1(buf,x) (((buf[x >> 3] >> (7 - (x & 7))) & 0x01) * 255)
// 1バイトあたり4ドット
#define get2(buf,x) (((buf[x >> 2] >> ((3 - (x & 3)) * 2)) & 0x03) * 85)
// 1バイトあたり2ドット
#define get4(buf,x) (((buf[x >> 1] >> ((1 - (x & 1)) * 4)) & 0x0F) * 17)
// 1バイトあたり1ドット
#define get8(buf,x) (buf[x])
// 2バイトあたり1ドット
#define get16(buf,x) (buf[x << 1])

#define MAKE565(red, green, blue) (((red<<8) & 0xf800) | ((green<<3) & 0x07e0) | ((blue >> 3) & 0x001f))

static WORD get1_tab[256][8];

static void init_get1_tables(void);

static int unpack_tile(int page, int scale, unsigned char *src, int width, int height, int n, int depth)
{
#ifdef DEBUG
	LOGD("unpack_tile : start (%d, %d, %d, %d, %d, %d)", page, scale, width, height, n, depth);
#endif
	int ret = 0;

	// n : 1ドットあたりの色数？
	// depth : 1ドットあたりのビット数
	int stride = (width * n * depth + 7) / 8;

	if (depth == 1)
		init_get1_tables();

	width  = ROUNDUP_DIV(width, scale);
	height = ROUNDUP_DIV(height, scale);

	int buffindex = -1;
	int buffpos   = 0;
	int linesize  = (width + HOKAN_DOTS);
	WORD *buffptr   = NULL;

	int yy, xx;
	for (yy = 0; yy < height * scale ; yy += scale)
	{
		if (gCancel) {
			LOGD("LoadImagePDF : cancel.");
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
		buffptr = gBuffMng[buffindex].Buff + buffpos;
		unsigned char *sp = src + (unsigned int)(yy * stride);

		/* Specialized loops */

		if (n == 1 && depth == 1 && scale == 1) {
			// 1バイトあたり8ドット
			int w3 = width >> 3;
			WORD *dp = &buffptr[2];
			for (xx = 0 ; xx < w3 ; xx ++) {
				memcpy(dp, get1_tab[*sp++], 8 * sizeof(WORD));
				dp += 8;
			}
			xx = xx << 3;
			if (xx < width) {
				memcpy(dp, get1_tab[*sp], (width - xx) * sizeof(WORD));
			}
		}
		else if (n == 1 && depth == 1 && scale != 1) {
			// 1バイトあたり8ドット、縮小あり
			WORD *dp = &buffptr[2];
			for (xx = 0 ; xx < width * scale ; xx += scale) {
				// 3色分をまとめ
				int cc = get1(sp, xx);
				*dp++ = MAKE565(cc, cc, cc);
			}
		}
		else if (n == 1 && depth == 8) {
			// 1バイトあたり1ドット * 1色
			for (xx = 0 ; xx < width * scale ; xx += scale) {
				// 3色分をまとめ
				buffptr[xx + 2] = MAKE565(sp[xx], sp[xx], sp[xx]);
			}
		}
		else if (n == 3 && depth == 8) {
			// 1バイトあたり1ドット * 3色
			for (xx = 0 ; xx < width * 3 * scale ; xx += 3 * scale) {
				// 3色分をまとめ
				buffptr[xx + 2] = MAKE565(sp[xx], sp[xx + 1], sp[xx + 2]);
			}
		}
		else if (n == 1) {
			WORD *dp = &buffptr[2];
			for (xx = 0; xx < width * scale; xx += scale) {
				// 1色
				int cc = 0;
				switch (depth)
				{
					case 1:
						cc = get1(sp, xx);
						break;
					case 2:
						cc = get2(sp, xx);
						break;
					case 4:
						cc = get4(sp, xx);
						break;
					case 8:
						cc = get8(sp, xx);
						break;
					case 16:
						cc = get16(sp, xx);
						 break;
					default:
						break;
				}
				*dp++ = MAKE565(cc, cc, cc);
			}
		}
		// 補完用の余裕
		buffptr[0] = buffptr[2];
		buffptr[1] = buffptr[2];
		buffptr[width + 2] = buffptr[width + 1];
		buffptr[width + 3] = buffptr[width + 1];

		// go to next line
		buffpos += linesize;
		gBuffMng[buffindex].Size += linesize;
	}
	return ret;
}

static int unpack_tile_bitmap(unsigned char *src, int width, int height, int n, int depth, WORD *dst, int dst_stride, int dst_height, int scale)
{
#ifdef DEBUG
	LOGD("unpack_tile_bmp : start (%d, %d, %d, %d, %d, %d, %d)", width, height, n, depth, dst_stride, dst_height, scale);
#endif
	int ret = 0;

	// n : 1ドットあたりの色数？
	// depth : 1ドットあたりのビット数
	int src_stride = (width * n * depth + 7) / 8;

	if (depth == 1)
		init_get1_tables();

	width  = ROUNDUP_DIV(width, scale);
	height = ROUNDUP_DIV(height, scale);

	int buffindex = -1;
	int buffpos   = 0;
	int linesize  = (width + HOKAN_DOTS);
	WORD *buffptr   = dst;

	int loop_height;
	if (height <= dst_height) {
		loop_height = height * scale;
	}
	else {
		loop_height = dst_height * scale;
	}

	int yy, xx;
	for (yy = 0; yy < loop_height ; yy += scale)
	{
		// ライン毎のバッファの位置を求める
		unsigned char *sp = src + (unsigned int)(yy * src_stride);
		WORD *dp = buffptr;

		/* Specialized loops */

		if (n == 1 && depth == 1 && scale == 1) {
			// 1バイトあたり8ドット
			int w3 = width >> 3;
			for (xx = 0 ; xx < w3 ; xx ++) {
				memcpy(dp, get1_tab[*sp++], 8 * sizeof(WORD));
				dp += 8;
			}
			xx = xx << 3;
			if (xx < width) {
				memcpy(dp, get1_tab[*sp], (width - xx) * sizeof(WORD));
			}
		}
		else if (n == 1 && depth == 1 && scale != 1) {
			// 1バイトあたり8ドット、縮小あり
			for (xx = 0 ; xx < width * scale ; xx += scale) {
				// 3色分をまとめ
				int cc = get1(sp, xx);
				*dp++ = MAKE565(cc, cc, cc);
			}
		}
		else if (n == 1 && depth == 8) {
			// 1バイトあたり1ドット * 1色
			for (xx = 0 ; xx < width * scale ; xx += scale) {
				// 3色分をまとめ
				buffptr[xx] = MAKE565(sp[xx], sp[xx], sp[xx]);
			}
		}
		else if (n == 3 && depth == 8) {
			// 1バイトあたり1ドット * 3色
			for (xx = 0 ; xx < width * 3 * scale ; xx += 3 * scale) {
				// 3色分をまとめ
				buffptr[xx] = MAKE565(sp[xx], sp[xx + 1], sp[xx + 2]);
			}
		}
		else if (n == 1) {
			for (xx = 0; xx < width * scale; xx += scale) {
				// 1色
				int cc = 0;
				switch (depth)
				{
					case 1:
						cc = get1(sp, xx);
						break;
					case 2:
						cc = get2(sp, xx);
						break;
					case 4:
						cc = get4(sp, xx);
						break;
					case 8:
						cc = get8(sp, xx);
						break;
					case 16:
						cc = get16(sp, xx);
						 break;
					default:
						break;
				}
				*dp++ = MAKE565(cc, cc, cc);
			}
		}
		// go to next line
		buffptr += dst_stride;
	}
	return ret;
}

static void init_get1_tables(void)
{
	static int once = 0;
	unsigned char bits[1];
	int i, k;

	/* TODO: mutex lock here */

	if (once)
		return;

	for (i = 0; i < 256; i++)
	{
		bits[0] = i;
		for (k = 0; k < 8; k++)
		{
			unsigned char cc = get1(bits, k);
			get1_tab[i][k] = MAKE565(cc, cc, cc);
		}
	}

	once = 1;
}
