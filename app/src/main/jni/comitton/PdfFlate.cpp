#include <android/log.h>
#include "../zlib/zlib.h"
#include "PdfCrypt.h"

int FlateDecompress(BYTE *inbuf, int insize)
{
	int result = 0;
	int count, status;

	BYTE *outbuf = init_buffer();
	if (outbuf == NULL) {
		return -10;
	}

	/* すべてのメモリ管理をライブラリに任せる */
	z_stream z;
	z.zalloc = Z_NULL;
	z.zfree = Z_NULL;
	z.opaque = Z_NULL;

	/* 初期化 */
	z.next_in = Z_NULL;
	z.avail_in = 0;
	if (inflateInit(&z) != Z_OK) {
		LOGE("inflateInit: %s\n", (z.msg) ? z.msg : "???");
		result = -1;
		goto error_end;
	}

	z.next_out = outbuf;		/* 出力ポインタ */
	z.avail_out = BUFFER_SIZE;	/* 出力バッファ残量 */
	status = Z_OK;

	z.next_in = inbuf;  /* 入力ポインタを入力バッファの先頭に */
	z.avail_in = insize; /* データを読み込む */

	while (status != Z_STREAM_END) {
#ifdef DEBUG
		LOGD("inflate: st=%d, in=%d, out=%d", status, z.avail_in, z.avail_out);
#endif

		if (z.avail_in == 0) {		/* 入力残量がゼロになれば */
			LOGE("inflate: avail_in == 0");
			result = -2;		/* 入力ポインタを元に戻す */
			goto error_end;
		}
		status = inflate(&z, Z_NO_FLUSH); /* 展開 */

		if (status == Z_STREAM_END) {
			/* 完了 */
			break;
		}
		if (status != Z_OK) {   /* エラー */
			LOGE("FlateDecompress : inflate(%s)", (z.msg) ? z.msg : "???");
			result = -3;
			goto error_end;
		}
		if (z.avail_out == 0) { /* 出力バッファが尽きれば */
			/* まとめて書き出す */
			set_buffer(BUFFER_SIZE);
			outbuf = add_buffer();
			if (outbuf == NULL) {
				LOGE("FlateDecompress: add_buffer(result == NULL)");
				result = -4;
				goto error_end;
			}
			z.next_out = outbuf; /* 出力ポインタを元に戻す */
			z.avail_out = BUFFER_SIZE; /* 出力バッファ残量を元に戻す */
		}
	}

	/* 残りを吐き出す */
	if ((count = BUFFER_SIZE - z.avail_out) != 0) {
		set_buffer(count);
	}

error_end:
	/* 後始末 */
	if (inflateEnd(&z) != Z_OK) {
		LOGE("FlateDecompress : inflateEnd(%s)", (z.msg) ? z.msg : "???");
		result = -6;
	}

	if (result != 0) {
		free_buffer();
	}
	return result;
}

