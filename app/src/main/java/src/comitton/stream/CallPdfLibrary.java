package src.comitton.stream;

public class CallPdfLibrary {
	static {
		// JNIライブラリのロード
		System.loadLibrary("comitton");
	}

	public static native int setStreamData(byte buff[], int len);
	public static native int getStreamDataSize();
	public static native int getStreamData(byte buff[], int len);
	public static native int flateDecompress();
	public static native int predictDecode(int pred, int clmns, int colors, int bpc);
	public static native int arc4Decode(byte key[], int keylen);
	public static native int aesDecode(byte key[], int keylen);
	public static native int computeObjectKey(int method, byte crypt_key[], int crypt_len, int num, int gen, byte res_key[]);
	public static native int authenticatePassword(byte id[], int v, int length, int r, byte o[], byte u[], byte oe[], byte ue[], int p, boolean encrypt_metadata, byte key[], String pass);

    public static native int pdfAlloc(int maxcmplen);
	public static native int pdfInit(int cmplen, int maxlen);
    public static native int pdfWrite(byte data[], int off, int len);
    public static native int pdfRead(byte data[], int off, int len);
    public static native int pdfInitSeek();
    public static native void pdfClose();
}
