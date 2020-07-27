package src.comitton.pdf;

import java.io.IOException;

import src.comitton.pdf.data.PdfCrypt;
import src.comitton.pdf.data.PdfCrypt.PdfCryptFilter;
import src.comitton.stream.CallPdfLibrary;
import src.comitton.stream.ImageManager;


import android.util.Log;

public class PDFLib {
	public static final int OFFSET_PDF_SIGNA_LEN = 0;

	public static byte BYTE_OBJSTART[] = { ' ', 'o', 'b', 'j' };
	public static byte BYTE_OBJEND[] = { 'e', 'n', 'd', 'o', 'b', 'j' };
	public static byte BYTE_STREAM[] = { 's', 't', 'r', 'e', 'a', 'm' };
	public static byte BYTE_STARTXREF[] = { 's', 't', 'a', 'r', 't', 'x', 'r', 'e', 'f' };
//	public static byte BYTE_TRAILER[] = { 't', 'r', 'a', 'i', 'l', 'e', 'r' };

	public final int SIZE_BUFFER = 1024;
	public final int READBYTE_EOF = 0x100;

	private boolean mIsStream;

	protected byte mBuffer[];
	protected int mBufferPos;
	protected int mBufferLen;

	protected byte mFileBuffer[];
	private int mFileBufferPos;
	private int mFileBufferLen;

	protected byte mStreamBuffer[];
	protected int mStreamBufferPos;
	protected int mStreamBufferLen;

	protected int mFileLength;

	private StringBuffer mStringBuffer;

	protected ImageManager mImgMgr;

	public PDFLib(ImageManager imgmgr) {
		mImgMgr = imgmgr;

		mFileBuffer = new byte[SIZE_BUFFER];
		mFileBufferPos = 0;
		mFileBufferLen = 0;

		switchSteramBuffer(false);

		mStringBuffer = new StringBuffer(SIZE_BUFFER);
		mIsStream = false;
	}

	// バッファを切替
	public boolean switchSteramBuffer(boolean isStream) {
		boolean original = mIsStream;
		mIsStream = isStream;
		if (mIsStream) {
			// Fileからの読み込みに戻す
			mBuffer = mStreamBuffer;
			mBufferPos = mStreamBufferPos;
			mBufferLen = mStreamBufferLen;
		}
		else {
			// Fileからの読み込みに戻す
			mBuffer = mFileBuffer;
			mBufferPos = mFileBufferPos;
			mBufferLen = mFileBufferLen;
		}
		return original;
	}

	public void setStreamBuffer() throws Exception {
		int len = CallPdfLibrary.getStreamDataSize();
		byte buff[] = new byte[len];

		if (len > 0) {
			int result = CallPdfLibrary.getStreamData(buff, len);
			if (result != 0) {
				throw new Exception("getStreamData error(" + result + ")");
			}
		}
		setStreamBuffer(buff, len);
	}

	public void setStreamBuffer(byte buff[], int len) {
		mStreamBuffer = buff;
		mStreamBufferPos = 0;
		mStreamBufferLen = len;
	}

	// バッファの使用準備
	protected void openBuffer(byte buff[]) {
		setStreamBuffer(buff, buff.length);
		return;
	}

	protected void openNull(int len, int offset) throws Exception {
		if (len >= 0) {
			mStreamBuffer = new byte[len];
			mStreamBufferLen = len;
		}
		else {
			throw new Exception("xref stream Length error.(" + len + ")");
		}
		seek(offset);
		readBuffer(mStreamBuffer, len);
		CallPdfLibrary.setStreamData(mStreamBuffer, len);
		return;
	}

	/*
	 * PDF 1.7 algorithm 3.1 and ExtensionLevel 3 algorithm 3.1a
	 *
	 * Create filter suitable for de/encrypting a stream.
	 */
	protected void openCrypt(PdfCrypt crypt, PdfCryptFilter stmf, int num, int gen) throws Exception {
		byte key[] = new byte[32];
		int len;

		len = computeObjectKey(crypt, stmf, num, gen, key);

		if (stmf.method == PdfCrypt.PDF_CRYPT_RC4) {
			openArc4(key, len);
			return;
		}

		if (stmf.method == PdfCrypt.PDF_CRYPT_AESV2 || stmf.method == PdfCrypt.PDF_CRYPT_AESV3) {
			openAesd(key, len);
			return;
		}
		return;
	}

	protected void openArc4(byte key[], int keylen) throws Exception {
		int result = CallPdfLibrary.arc4Decode(key, keylen);
		if (result != 0) {
			throw new Exception("arc4Decode : result = " + result);
		}
		return;
	}

	protected void openAesd(byte key[], int keylen) throws Exception {
		int result = CallPdfLibrary.aesDecode(key, keylen);
		if (result != 0) {
			throw new Exception("aesDecode : result = " + result);
		}
		return;
	}

	protected void openFlated() throws Exception {
		int result = CallPdfLibrary.flateDecompress();
		if (result != 0) {
			throw new Exception("flateDecompress : result = " + result);
		}
		return;
	}

	protected void openPredict(int predictor, int columns, int colors, int bpc) throws Exception {
		int result = CallPdfLibrary.predictDecode(predictor, columns, colors, bpc);
		if (result != 0) {
			throw new Exception("predictDecode : result = " + result);
		}
		return;
	}

	/*
	 * PDF 1.7 algorithm 3.1 and ExtensionLevel 3 algorithm 3.1a
	 *
	 * Using the global encryption key that was generated from the password, create a new key that is used to decrypt individual objects and streams. This key is based on the object and generation numbers.
	 */

	protected int computeObjectKey(PdfCrypt crypt, PdfCryptFilter cf, int num, int gen, byte key[]) {
		int result = CallPdfLibrary.computeObjectKey(cf.method, crypt.key, crypt.length, num, gen, key);
		return result;
	}

	protected boolean authenticatePassword(PdfCrypt crypt, String password)
	{
		if (crypt == null) {
			return false;
		}
		int result = CallPdfLibrary.authenticatePassword(crypt.id.s, crypt.v, crypt.length, crypt.r, crypt.o, crypt.u, crypt.oe, crypt.ue, crypt.p, crypt.encrypt_metadata, crypt.key, password);
		return (result == 0);
	}

	// off - 検索開始位置
	// size - データ全体のサイズ
	static public int searchBinary(byte buf[], int off, int size, byte keyword[]) {
		// 評価以前の問題
		if (buf == null || keyword == null) {
			return -2;
		}
		// パラメタチェック
		if (buf.length == 0 || keyword.length == 0 || size - off < keyword.length) {
			return -2;
		}

		// 評価する限界値
		int limitpos = size - keyword.length + 1;

		int i, j;
		for (i = off; i < limitpos; i++) {
			for (j = 0; j < keyword.length; j++) {
				if (buf[i + j] != keyword[j]) {
					break;
				}
			}
			// 最後まで一致したら戻り値に設定する
			if (j == keyword.length) {
				return i;
			}
		}
		// 最後まで見つからなかった
		return -1;
	}

	// off - 検索開始位置
	// size - データ全体のサイズ
	static public void moveBinary(byte buf[], int off, int size) {
		// 評価以前の問題
		if (buf == null) {
			return;
		}

		for (int i = 0; i < size - off; i++) {
			buf[i] = buf[off + i];
		}
		// 最後までコピー
		return;
	}

	// 指定位置から数字が出てくるまで
	static public int getPDFValue(String param, int startpos) {
		int value = -1;
		int flag = 0;

		for (int i = startpos; i < param.length(); i++) {
			int ch = param.charAt(i);
			if (flag == 0) {
				// 空白を飛ばす
				if (ch == ' ' || ch == '\t') {
					continue;
				}
				else if (ch < '0' || '9' < ch) {
					break;
				}
				flag = 1;
				value = 0;
			}
			if (flag == 1) {
				if ('0' <= ch && ch <= '9') {
					value = value * 10 + ch - '0';
				}
				else {
					break;
				}
			}
		}
		return value;
	}

	public int skipEnterCode(byte buff[], int pos) {
		if (buff[pos] == (byte) 0x0d) {
			// 0x0d0a分進める
			return 2;
		}
		else if (buff[pos] == (byte) 0x0a) {
			// 0x0a分進める
			return 1;
		}
		return 0;
	}

	// 空白チェック
	public boolean isWhiteCode(int buff) {
		switch (buff) {
			case '\n':
			case '\r':
			case '\t':
			case ' ':
			case '\0':
			case '\f':
			case '\b':
			case 0177:
				return true;
		}
		return false;
	}

	// 空白チェック
	public boolean isDelimiterCode(int buff) {
		switch (buff) {
			case '(':
			case ')':
			case '<':
			case '>':
			case '[':
			case ']':
			case '{':
			case '}':
			case '/':
			case '%':
				return true;
		}
		return false;
	}

	// 空白チェック
	public boolean isWhiteCode(byte buff[], int pos) {
		switch (buff[pos]) {
			case '\n':
			case '\r':
			case '\t':
			case ' ':
			case '\0':
			case '\f':
			case '\b':
			case 0177:
				return true;
		}
		return false;
	}

	// 数字チェック
	public boolean isDigitCode(int c) {
		switch (c) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				return true;
		}
		return false;
	}

	// 数字と±チェック
	public boolean isNumber(int c) {
		switch (c) {
			case '+':
			case '-':
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
				return true;
		}
		return false;
	}

	public boolean isHexCode(int c) {
		switch (c) {
			case '0':
			case '1':
			case '2':
			case '3':
			case '4':
			case '5':
			case '6':
			case '7':
			case '8':
			case '9':
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
			case 'a':
			case 'b':
			case 'c':
			case 'd':
			case 'e':
			case 'f':
				return true;
		}
		return false;
	}

	// Hex(小文字)チェック
	public boolean isHexLowerCode(int c) {
		switch (c) {
			case 'a':
			case 'b':
			case 'c':
			case 'd':
			case 'e':
			case 'f':
				return true;
		}
		return false;
	}

	// Hex(大文字)チェック
	public boolean isHexUpperCode(int c) {
		switch (c) {
			case 'A':
			case 'B':
			case 'C':
			case 'D':
			case 'E':
			case 'F':
				return true;
		}
		return false;
	}

	// 空白を飛ばす
	public int skipWhiteCode(byte buff[], int offset) {
		int pos = offset;

		while (pos < buff.length) {
			switch (buff[pos]) {
				case '\n':
				case '\r':
				case '\t':
				case ' ':
				case '\0':
				case '\f':
				case '\b':
				case 0177:
					pos++;
					break;
				default:
					return pos - offset;
			}
		}
		return pos - offset;
	}

	// 空白以外を飛ばす
	public int skipNotWhiteCode(byte buff[], int offset) {
		int pos = offset;

		while (pos < buff.length) {
			switch (buff[pos]) {
				case '\n':
				case '\r':
				case '\t':
				case ' ':
				case '\0':
				case '\f':
				case '\b':
				case 0177:
					return pos - offset;
			}
			pos++;
		}
		return pos - offset;
	}

	public int unhex(int ch) {
		if (ch >= '0' && ch <= '9')
			return ch - '0';
		if (ch >= 'A' && ch <= 'F')
			return ch - 'A' + 0xA;
		if (ch >= 'a' && ch <= 'f')
			return ch - 'a' + 0xA;
		return 0;
	}

	// 逆方向で検索
	static public int searchBinaryRevers(byte buf[], int off, byte keyword[]) {
		// 評価以前の問題
		if (buf == null || keyword == null) {
			return -2;
		}
		// パラメタチェック
		if (buf.length == 0 || keyword.length == 0 || off + keyword.length >= buf.length) {
			return -2;
		}

		// 評価する限界値
		int i, j;
		for (i = off; i >= 0; i--) {
			for (j = 0; j < keyword.length; j++) {
				if (buf[i + j] != keyword[j]) {
					break;
				}
			}
			// 最後まで一致したら戻り値に設定する
			if (j == keyword.length) {
				return i;
			}
		}
		// 最後まで見つからなかった
		return -1;
	}

	// 改行までを分解してString配列で返す
	// 復帰値 : 改行の次の位置
	public int getParamsStr(byte buff[], int offset, String params[]) {
		int num = 0;
		int mode = 0;
		int pos, stpos;

		// 返却領域初期化
		for (int i = 0; i < params.length; i++) {
			params[i] = null;
		}

		// 文字列切り出し
		for (pos = stpos = offset;; pos++) {
			if (mode == 0) {
				if (pos < buff.length && buff[pos] != (byte) ' ' && buff[pos] != (byte) '\t') {
					// 文字列の開始
					mode = 1;
					stpos = pos;
				}
			}
			else {
				if (pos >= buff.length || buff[pos] == (byte) ' ' || buff[pos] == (byte) '\t' || buff[pos] == (byte) 0x0d || buff[pos] == (byte) 0x0a) {
					// 文字列の終了
					params[num++] = new String(buff, stpos, pos - stpos);
					mode = 0;
				}
			}
			if (pos >= buff.length || buff[pos] == (byte) 0x0d || buff[pos] == (byte) 0x0a) {
				// 1行の終わり
				pos += skipEnterCode(buff, pos);
				break;
			}
		}
		return pos - offset;
	}

	static public int getObjLength(String str, int objlens[]) {
		int length = 0;

		// データサイズを探す
		int parampos = str.indexOf("/length");
		if (parampos >= 0) {
			// データサイズの設定あり
			parampos += "/length".length();
			int i;
			for (i = parampos; i < str.length(); i++) {
				char ch = str.charAt(i);
				if (ch != ' ' && ch != '\t') {
					// 先頭空白削除
					break;
				}
			}
			int stpos = i;
			for (; i < str.length(); i++) {
				char ch = str.charAt(i);
				if (ch == '<' || ch == '/' || ch == 0x0d || ch == 0x0a) {
					// 区切り記号まで切り出し
					break;
				}
			}
			if (i < str.length()) {
				str = str.substring(stpos, i);
			}

			String params[] = str.split(" ");
			if (params.length == 1) {
				// データ長
				try {
					length = Integer.parseInt(params[0]);
				}
				catch (NumberFormatException e) {
					;
				}
			}
			else if (params.length == 3 && params[2].equals("r")) {
				// データ長
				int index = Integer.parseInt(params[0]);
				if (index >= 0 && index < objlens.length) {
					length = objlens[index];
				}
			}
		}
		return length;
	}

	// byte配列の数字を数値に変換
	public int convertDigitStr(byte buf[], int offset, int len) {
		int val = 0;
		for (int i = 0; i < len; i++) {
			int work = ((int) buf[offset + i]) & 0xFF;
			if (work < 0x30 || 0x39 < work) {
				// 数字のみ
				break;
			}
			val = val * 10 + (work - 0x30);
		}
		return val;
	}

	public String strsep(String str, char delim) {
		String result = null;

		int pos = str.indexOf(delim);
		if (pos > 0) {
			result = str.substring(pos);
		}
		return result;
	}

	public int atoi(String str) {
		try {
			return Integer.parseInt(str);
		}
		catch (Exception e) {
			return 0;
		}
	}

	protected void setFilter() {

	}

	// バッファを意識しない読み込みの初期化
	protected void peekInit() {
		if (mIsStream == false) {
			mBufferPos = 0;
			mBufferLen = 0;
		}
	}

	// バッファを意識せずに1バイトずつ読み込み
	protected int peekByte() throws IOException {
		if (mBufferPos >= mBufferLen) {
			// 読み込んだサイズを超えた場合は続きを読み込み
			readBuffer();
		}
		if (mBufferPos >= mBufferLen) {
			return READBYTE_EOF;
		}
		return mBuffer[mBufferPos];
	}

	// バッファを意識せずに1バイトずつ読み込み
	protected int readByte() throws IOException {
		if (mBufferPos >= mBufferLen) {
			// 読み込んだサイズを超えた場合は続きを読み込み
			readBuffer();
		}
		if (mBufferPos >= mBufferLen) {
			return READBYTE_EOF;
		}
		return mBuffer[mBufferPos++];
	}

	// 1バイト戻す
	protected void unreadByte() {
		if (mBufferPos > 0) {
			// バッファがある場合戻す
			mBufferPos--;
		}
		return;
	}

	protected String readLine() throws IOException {
		StringBuffer s = mStringBuffer;
		int c = READBYTE_EOF;

		s.setLength(0);
		while (true) {
			c = readByte();
			if (c == READBYTE_EOF) {
				break;
			}
			if (c == '\r') {
				c = peekByte();
				if (c == '\n') {
					readByte();
				}
				break;
			}
			if (c == '\n') {
				break;
			}
			s.append((char) c);
		}
		return s.toString();
	}

	protected String read(int size) throws IOException {
		StringBuffer s = mStringBuffer;
		int c = READBYTE_EOF;

		s.setLength(0);
		for (int i = 0; i < size; i++) {
			c = readByte();
			if (c == READBYTE_EOF) {
				break;
			}
			s.append((char) c);
		}
		return s.toString();
	}

	// 現在のファイルオフセット取得
	protected int tell() throws IOException {
//		if (mIsStream) {
//			return mBufferPos;
//		}
//		else {
			int pos = (int) mImgMgr.cmpDirectTell();
			if (pos >= 0) {
				pos -= (mBufferLen - mBufferPos);
			}
			return pos;
//		}
	}

	// 現在のファイルオフセット取得
	protected void seek(int pos) throws IOException {
		if (mIsStream) {
			;
		}
		else {
			mImgMgr.cmpDirectSeek(pos);
			mBufferPos = 0;
			mBufferLen = 0;
		}
		return;
	}

	protected boolean isEOF() throws IOException {
		if (mIsStream) {
			return (mBufferPos >= mBufferLen);
		}
		else {
			return (tell() >= mFileLength);
		}
	}

	protected void readBuffer() throws IOException {
		if (mIsStream) {
			return;
		}
		try {
			// ファイルから読み込み
			mBufferPos = 0;
			mBufferLen = mImgMgr.cmpDirectRead(mBuffer, 0, mBuffer.length);
		}
		catch (IOException e) {
			// 読み込みエラー
			mBufferLen = 0;
			Log.e("pdf/readBuffer", e.getMessage());
			throw e;
		}
	}

	protected int readBuffer(byte buff[], int len) throws IOException {
		int ret = 0;
		try {
			// ファイルから読み込み
			ret = mImgMgr.cmpDirectRead(buff, 0, len);
		}
		catch (IOException e) {
			// 読み込みエラー
			Log.e("pdf/readBuffer", e.getMessage());
			throw e;
		}
		return ret;
	}
}
