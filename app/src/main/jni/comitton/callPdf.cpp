#include <jni.h>
#include <malloc.h>
#include <string.h>
#include <android/log.h>
#include "PdfCrypt.h"

//#define DEBUG 

BYTE	*gStreamBuffer = NULL;
int		gStreamBufferLen = 0;
int		gStreamBufferMaxLen = 0;
int		gStreamBufferReadPos = 0;
int		gStreamBufferWritePos = 0;

extern "C" {

int allocStreamBuffer(int length);
void freeStreamBuffer(void);

// flate展開するデータを設定
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_setStreamData (JNIEnv *env, jclass obj, jbyteArray dataArray, jint size)
{
#ifdef DEBUG
	LOGD("setStreamData start");
#endif
	int result = 0;
	int datasize = sizeof(BYTE) * size;

	jbyte *data = env->GetByteArrayElements(dataArray, NULL);

	// 領域が不足していたら再確保
	freeStreamBuffer();

#ifdef DEBUG
	LOGD("malloc: size = %d", datasize);
#endif
	gStreamBuffer = (BYTE *)malloc(datasize);

	if (gStreamBuffer == NULL) {
		LOGE("malloc: error");
		gStreamBufferLen = 0;
		result = -20;
		goto error;
	}

	// セットしたサイズを超えないように
	memcpy(gStreamBuffer, data, datasize);
	gStreamBufferLen = datasize;
#ifdef DEBUG
	LOGD("newStreamBuffer: ptr=%x, size=%d", gStreamBuffer, gStreamBufferLen);
#endif

error:
	env->ReleaseByteArrayElements(dataArray, data, 0);
#ifdef DEBUG
	LOGD("setStreamData end");
#endif
	return result;
}

// flate展開後のデータサイズ返却
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_getStreamDataSize (JNIEnv *env, jclass obj)
{
#ifdef DEBUG
	LOGD("getStreamDataSize: size=%d", gStreamBufferLen);
#endif
	return gStreamBufferLen;
}

// flate展開後のデータをコピー
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_getStreamData (JNIEnv *env, jclass obj, jbyteArray dataArray, jint size)
{
	int result = 0;
	int copysize = 0;

#ifdef DEBUG
	LOGD("copyFlateData: start(sb=%x, sl=%d, size=%d)", gStreamBuffer, gStreamBufferLen, size);
#endif
	jbyte *data = env->GetByteArrayElements(dataArray, NULL);

	if (gStreamBuffer == NULL || gStreamBufferLen < 0) {
		LOGE("copyFlateData: gStreamBuffer = NULL");
		result = -21;
		goto error;
	}

	if (size < 0) {
		LOGE("copyFlateData: size <= 0");
		result = -22;
		goto error;
	}

	copysize = size;
	if (gStreamBufferLen < copysize) {
		copysize = gStreamBufferLen;
	}
#ifdef DEBUG
	LOGD("getStreamData: copysize = %d", copysize);
#endif
	memcpy(data, gStreamBuffer, copysize);

error:
	env->ReleaseByteArrayElements(dataArray, data, 0);
#ifdef DEBUG
	LOGD("copyFlateData: end");
#endif
	return result;
}

// flate展開後のデータをコピー
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_flateDecompress (JNIEnv *env, jclass obj)
{
	int result = 0;
	int size = 0;

#ifdef DEBUG
	LOGD("flateDecompress: start");
#endif

	result = FlateDecompress(gStreamBuffer, gStreamBufferLen);

	// バッファ解放
	freeStreamBuffer();

	if (result == 0) {
		// セットしたサイズを超えないように
		BYTE *buff;
		buff = copy_buffer(&size);
		if (buff != NULL && size > 0) {
			gStreamBuffer = buff;
			gStreamBufferLen = size;
#ifdef DEBUG
			LOGD("newStreamBuffer: ptr=%x, size=%d", gStreamBuffer, gStreamBufferLen);
#endif
		}
		free_buffer();
	}

#ifdef DEBUG
	LOGD("flateDecompress: end(st=%x, sl=%d)", gStreamBuffer, gStreamBufferLen);
#endif
	return result;
}

// flate展開後のデータをコピー
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_predictDecode (JNIEnv *env, jclass obj, jint predict, jint columns, jint colors, jint bpc)
{
	int result = 0;
	int size = 0;

#ifdef DEBUG
	LOGD("predictDecode: start pre=%d, clm=%d, clr=%d, bpc=%d", predict, columns, colors, bpc);
#endif

	result = PredictDecode(gStreamBuffer, gStreamBufferLen, predict, columns, colors, bpc);

	// バッファ解放
	freeStreamBuffer();

	if (result == 0) {
		// セットしたサイズを超えないように
		BYTE *buff;
		buff = copy_buffer(&size);
		if (buff != 0 && size > 0) {
			gStreamBuffer = buff;
			gStreamBufferLen = size;
		}
		free_buffer();
	}

#ifdef DEBUG
	LOGD("predictDecode: end(st=%x, sl=%d)", gStreamBuffer, gStreamBufferLen);
#endif
	return result;
}

// arc4で複合
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_arc4Decode (JNIEnv *env, jclass obj, jbyteArray dataArray, jint size)
{
	int result = 0;
	int copysize = 0;

#ifdef DEBUG
	LOGD("arc4Decode: start(size=%x)", size);
#endif
	jbyte *data = env->GetByteArrayElements(dataArray, NULL);

	result = Arc4Decode(gStreamBuffer, gStreamBufferLen, (BYTE*)data, size);

	env->ReleaseByteArrayElements(dataArray, data, 0);
#ifdef DEBUG
	LOGD("arc4Decode: end");
#endif
	return result;
}

// AESで複合
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_aesDecode (JNIEnv *env, jclass obj, jbyteArray dataArray, jint size)
{
	int result = 0;
	int copysize = 0;

#ifdef DEBUG
	LOGD("aesDecode: start(size=%x)", size);
#endif
	jbyte *data = env->GetByteArrayElements(dataArray, NULL);

	result = AesDecode(gStreamBuffer, gStreamBufferLen, (BYTE*)data, size);

	env->ReleaseByteArrayElements(dataArray, data, 0);
#ifdef DEBUG
	LOGD("aesDecode: end");
#endif
	return result;
}

// keyの作成
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_computeObjectKey(JNIEnv *env, jclass obj, jint method, jbyteArray srcArray, jint src_len, jint num, jint gen, jbyteArray dstArray)
{
	int result = 0;
	int copysize = 0;

#ifdef DEBUG
	LOGD("computeObjectKey: start(method=%d, slen=%d, num=%d, gen=%d ", method, src_len, num, gen);
#endif
	jbyte *src_key = env->GetByteArrayElements(srcArray, NULL);
	jbyte *dst_key = env->GetByteArrayElements(dstArray, NULL);

	result = computeObjectKey(method, (BYTE*)src_key, src_len, num, gen, (BYTE*)dst_key);

	env->ReleaseByteArrayElements(srcArray, src_key, 0);
	env->ReleaseByteArrayElements(dstArray, dst_key, 0);
#ifdef DEBUG
	LOGD("computeObjectKey: end");
#endif
	return result;
}

// keyの作成
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_authenticatePassword(JNIEnv *env, jclass obj, jbyteArray idArray, jint v, jint length, jint r, jbyteArray oArray, jbyteArray uArray, jbyteArray oeArray, jbyteArray ueArray, jint p, jboolean encrypt_metadata, jbyteArray keyArray, jstring password/*jbyteArray pasArray*/)
{
#ifdef DEBUG
	LOGD("authenticatePassword: start(v=%d, length=%d, r=%d, p=%d, enc=%d, password=%x", v, length, r, p, (int)encrypt_metadata, password);
#endif
	int result = 0;
	pdf_crypt	crypt;
	memset(&crypt, 0, sizeof(crypt));

	crypt.v = v;
	crypt.length = length;
	crypt.r = r;
	crypt.p = p;
	crypt.encrypt_metadata = encrypt_metadata;

	BYTE *id  = (BYTE*)env->GetByteArrayElements(idArray, NULL);
	BYTE *o   = (BYTE*)env->GetByteArrayElements(oArray, NULL);
	BYTE *u   = (BYTE*)env->GetByteArrayElements(uArray, NULL);
	BYTE *oe  = (BYTE*)env->GetByteArrayElements(oeArray, NULL);
	BYTE *ue  = (BYTE*)env->GetByteArrayElements(ueArray, NULL);
	BYTE *key = (BYTE*)env->GetByteArrayElements(keyArray, NULL);
//	char *pas = (char*)env->GetByteArrayElements(pasArray, NULL);

	int id_len  = env->GetArrayLength(idArray);
	int o_len   = env->GetArrayLength(oArray);
	int u_len   = env->GetArrayLength(uArray);
	int oe_len  = env->GetArrayLength(oeArray);
	int ou_len  = env->GetArrayLength(ueArray);
	int key_len = env->GetArrayLength(keyArray);

	memcpy(crypt.id, id, id_len);
	memcpy(crypt.o, o,  o_len);
	memcpy(crypt.u, u,  u_len);
	memcpy(crypt.oe, oe, oe_len);
	memcpy(crypt.ue, ue, ou_len);
	memcpy(crypt.key, key, key_len);

	crypt.id_len = id_len;

	char *pas = NULL;
	if (password != NULL) {
		pas = (char*)env->GetStringUTFChars(password, NULL);
	}
//	int pas_len = env->GetArrayLength(pasArray);
//	int pas_len = strlen(pas);

#ifdef DEBUG
	LOGD("authenticatePassword: param id=%d, o=%d, u=%d, oe=%d, ou=%d, key=%d, pas=%s", id_len, o_len, u_len, oe_len, ou_len, key_len, pas);
#endif

	result = pdf_authenticate_password(&crypt, pas);

	memcpy(key, crypt.key, key_len);

	env->ReleaseByteArrayElements(idArray, (jbyte*)id, 0);
	env->ReleaseByteArrayElements(oArray, (jbyte*)o, 0);
	env->ReleaseByteArrayElements(uArray, (jbyte*)u, 0);
	env->ReleaseByteArrayElements(oeArray, (jbyte*)oe, 0);
	env->ReleaseByteArrayElements(ueArray, (jbyte*)ue, 0);
	env->ReleaseByteArrayElements(keyArray, (jbyte*)key, 0);
//	env->ReleaseByteArrayElements(pasArray, (jbyte*)pas, 0);

	if (pas != NULL) {
		env->ReleaseStringUTFChars(password, pas);
	}

	// 文字列バッファ解放
#ifdef DEBUG
	LOGD("authenticatePassword: end");
#endif
	return result;
}

/*
 * Class:     src_comitton_stream_callLibrary
 * Method:    pdfAlloc
 * Signature: (I[II)V
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_pdfAlloc(JNIEnv *env, jclass obj, jint length)
{
#ifdef DEBUG
	LOGD("pdfAlloc start(len=%d)", length);
#endif

	int result = allocStreamBuffer(length);

#ifdef DEBUG
	LOGD("pdfAlloc end");
#endif
	return result;
}

/*
 * Class:     src_comitton_stream_callLibrary
 * Method:    pdfInit
 * Signature: (I[II)V
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_pdfInit(JNIEnv *env, jclass obj, jint cmplen, jint maxlen)
{
#ifdef DEBUG
	LOGD("pdfInit : cmplen=%d", cmplen);
#endif
	if (gStreamBuffer == NULL) {
		LOGE("pdfInit : re-alloc (%d)", maxlen);
		int result = allocStreamBuffer(maxlen);
		if (result != 0) {
			LOGE("pdfInit : alloc error");
			return result;
		}
//		LOGE("pdfInit : gStreamBuffer == null");
//		return -1;
	}
	if (gStreamBufferMaxLen < cmplen) {
		LOGE("pdfInit : gStreamBufferMaxLen < cmplen");
		return -2;
	}

	gStreamBufferLen = cmplen;
	gStreamBufferReadPos = 0;
	gStreamBufferWritePos = 0;
	return 0;
}

/*
 * Class:     src_comitton_stream_callLibrary
 * Method:    pdfWrite
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_pdfWrite(JNIEnv *env, jclass obj, jbyteArray cmpArray, jint offset, jint size)
{
	if (gStreamBuffer == NULL) {
		return -1;
	}

	jbyte *data = env->GetByteArrayElements(cmpArray, NULL);

	if (gStreamBufferLen - gStreamBufferWritePos < size) {
		// バッファサイズまでしか書き込まない
		size = gStreamBufferLen - gStreamBufferWritePos;
	}

	memcpy(&gStreamBuffer[gStreamBufferWritePos], &data[offset], size);
	gStreamBufferWritePos += size;

	env->ReleaseByteArrayElements(cmpArray, data, 0);
	return size;
}

/*
 * Class:     src_comitton_stream_callLibrary
 * Method:    pdfRead
 * Signature: ([BI)I
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_pdfRead(JNIEnv *env, jclass obj, jbyteArray orgArray, jint offset, jint size)
{
	if (gStreamBuffer == NULL) {
		return -1;
	}

	jbyte *data = env->GetByteArrayElements(orgArray, NULL);

	if (gStreamBufferLen - gStreamBufferReadPos < size) {
		// バッファサイズまでしか読み込まない
		size = gStreamBufferLen - gStreamBufferReadPos;
	}

	memcpy(&data[offset], &gStreamBuffer[gStreamBufferReadPos], size);
	gStreamBufferReadPos += size;

	env->ReleaseByteArrayElements(orgArray, data, 0);
	return size;
}

/*
 * Class:     src_comitton_stream_callLibrary
 * Method:    pdfInitSeek
 * Signature: (I[II)V
 */
JNIEXPORT jint JNICALL Java_src_comitton_stream_CallPdfLibrary_pdfInitSeek(JNIEnv *env, jclass obj)
{
	if (gStreamBuffer == NULL) {
		return -1;
	}

	gStreamBufferReadPos = 0;
	return 0;
}

/*
 * Class:     src_comitton_stream_callLibrary
 * Method:    pdfClose
 * Signature: ([BI)I
 */
JNIEXPORT void JNICALL Java_src_comitton_stream_CallPdfLibrary_pdfClose(JNIEnv *env, jclass obj)
{
#ifdef DEBUG
	LOGD("pdfClose");
#endif
	freeStreamBuffer();
	return;
}

int allocStreamBuffer(int length) {

	int result = 0;
	int datasize = sizeof(BYTE) * length;

	freeStreamBuffer();

#ifdef DEBUG
	LOGD("malloc: size = %d", datasize);
#endif
	gStreamBuffer = (BYTE *)malloc(datasize);

	if (gStreamBuffer == NULL) {
		LOGE("malloc: error");
		gStreamBufferLen = 0;
		result = -20;
	}
	gStreamBufferMaxLen = length;
	return result;
}

void freeStreamBuffer(void) {
	// 前回使用バッファが残っていれば解放
	if (gStreamBuffer != NULL) {
#ifdef DEBUG
		LOGD("freeStreamBuffer: ptr=%x, size=%d", gStreamBuffer, gStreamBufferLen);
#endif
		free(gStreamBuffer);
		gStreamBuffer = NULL;
		gStreamBufferLen = 0;
		gStreamBufferMaxLen = 0;
		gStreamBufferReadPos = 0;
		gStreamBufferWritePos = 0;
	}
}

}
