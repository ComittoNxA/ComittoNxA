#include <malloc.h>
#include <string.h>
#include <android/log.h>
#include "PdfCrypt.h"

DATA_NEST	*gFirstBuffer = NULL;
DATA_NEST	*gNowBuffer = NULL;

BYTE *init_buffer(void) {
#ifdef DEBUG
	LOGD("init_buffer start");
#endif
	free_buffer();

	gFirstBuffer = alloc_buffer();
	if (gFirstBuffer == NULL) {
#ifdef DEBUG
		LOGD("init_buffer: malloc(result == null)");
#endif
		return NULL;
	}
	gNowBuffer = gFirstBuffer;
#ifdef DEBUG
	LOGD("init_buffer end(data=%d)", gNowBuffer->data);
#endif
	return gNowBuffer->data;
}

int set_buffer(int size) {
#ifdef DEBUG
	LOGD("set_buffer = %d", size);
#endif
	gNowBuffer->size = size;
	return 0;
}

BYTE *add_buffer(void) {
#ifdef DEBUG
	LOGD("add_buffer start");
#endif
	gNowBuffer->next = alloc_buffer();
	if (gNowBuffer->next == NULL) {
#ifdef DEBUG
		LOGD("add_buffer: malloc(result == null)");
#endif
		return NULL;
	}

	gNowBuffer = gNowBuffer->next;
#ifdef DEBUG
	LOGD("add_buffer end(data=%d)", gNowBuffer->data);
#endif
	return gNowBuffer->data;
}

BYTE *copy_buffer(int *size) {
#ifdef DEBUG
	LOGD("copy_buffer start");
#endif
	int datasize = size_buffer();
	*size = 0;

#ifdef DEBUG
	LOGD("malloc / size = %d", datasize);
#endif
	BYTE *buff = (BYTE *)malloc(sizeof(BYTE) * datasize);

	if (buff == NULL) {
#ifdef DEBUG
		LOGD("malloc / size = %d", datasize);
#endif
		return NULL;
	}

	DATA_NEST *now = gFirstBuffer;
	int copysize;
	int pos = 0;

	while (now != NULL) {
		copysize = now->size;
		if (copysize > datasize - pos) {
			copysize = datasize - pos;
		}
#ifdef DEBUG
		LOGD("copy (now=%x, size=%d, cs=%d)", now, now->size, copysize);
#endif
		memcpy(&buff[pos], now->data, copysize);
		pos += copysize;
		*size += copysize;
		now = now->next;
	}
#ifdef DEBUG
	LOGD("copy_buffer end buff=%d", buff);
#endif
	return buff;
}

// サイズを合計する
int size_buffer(void) {
#ifdef DEBUG
	LOGD("size_buffer start");
#endif
	DATA_NEST *now;
	int size = 0;

	now = gFirstBuffer;

	while (now != NULL) {
		size += now->size;
		now = now->next;
	}
#ifdef DEBUG
	LOGD("size_buffer end(size=%d)", size);
#endif
	return size;
}

// 構造体確保
DATA_NEST *alloc_buffer(void) {
	DATA_NEST *buff = (DATA_NEST*)malloc(sizeof(DATA_NEST));
	if (buff == NULL) {
#ifdef DEBUG
		LOGD("alloc_buffer: malloc(buff == null)");
#endif
	}
	else {
		memset(buff, 0, sizeof(DATA_NEST));
	}
	return buff;
}

// ネストされているバッファを全部解放
int free_buffer(void) {
#ifdef DEBUG
	LOGD("free_buffer start");
#endif
	DATA_NEST *now;
	DATA_NEST *next;

	now = gFirstBuffer;
	gFirstBuffer = NULL;

	while (now != NULL) {
#ifdef DEBUG
		LOGD("free_buffer: now=%x, next=%x", now, now->next);
#endif
		next = now->next;
		free(now);
		now = next;
	}
#ifdef DEBUG
	LOGD("free_buffer end");
#endif
	return 0;
}
