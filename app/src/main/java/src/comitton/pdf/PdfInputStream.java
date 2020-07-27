package src.comitton.pdf;

import java.io.IOException;
import java.io.InputStream;

import src.comitton.pdf.data.PdfCrypt;
import src.comitton.pdf.data.PdfCrypt.PdfCryptFilter;
import src.comitton.stream.CallPdfLibrary;
import src.comitton.stream.FileListItem;


public class PdfInputStream extends InputStream {
	InputStream	mInputStream;
	int mPage;

	public PdfInputStream(InputStream is, int page, FileListItem fi, src.comitton.pdf.data.PdfCrypt crypt, int maxlen) throws IOException {
		mInputStream = is;
		mPage = page;
		int cmplen = fi.cmplen;
		int num = fi.param1;
		int gen = fi.param2;
		boolean hascrypt = (fi.param3 == 0 ? false : true);

		byte buff[] = new byte[1024 * 8];
		int retsize;
		int readsize;
		int datasize = cmplen;
		int ret;

		// イメージのストリームサイズを設定
		ret = CallPdfLibrary.pdfInit(datasize, maxlen);
		if (ret != 0) {
			throw new IOException("Illegal function call.");
		}

		while (datasize > 0) {
			readsize = buff.length;
			if (readsize > datasize) {
				readsize = datasize;
			}
			retsize = is.read(buff, 0, readsize);
			if (retsize <= 0) {
				break;
			}
			datasize -= retsize;
			CallPdfLibrary.pdfWrite(buff, 0, retsize);
		}

		if (crypt != null && !hascrypt) {
			// 複合化
			try {
				openCrypt(crypt, crypt.stmf, num, gen);
			}
			catch (Exception e) {
				// 
				throw new IOException("Crypt error.");
			}
		}
	}

	// 読み込み済みのページを返す
	public int getLoadPage() {
		return mPage;
	}
	
	// 現在の展開ファイルをもう一度最初から返す
	public void initSeek() {
		CallPdfLibrary.pdfInitSeek();
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

	/*
	 * PDF 1.7 algorithm 3.1 and ExtensionLevel 3 algorithm 3.1a
	 * 
	 * Using the global encryption key that was generated from the password, create a new key that is used to decrypt individual objects and streams. This key is based on the object and generation numbers.
	 */

	private int computeObjectKey(PdfCrypt crypt, PdfCryptFilter cf, int num, int gen, byte key[]) {
		int result = CallPdfLibrary.computeObjectKey(cf.method, crypt.key, crypt.length, num, gen, key);
		return result;
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

	@Override
	public int read() throws IOException {
		// 自動生成されたメソッド・スタブ
		return 0;
	}
	
	@Override
	public int read(byte buf[], int off, int len) throws IOException {
		int ret = CallPdfLibrary.pdfRead(buf, off, len);
		if (ret == 0) {
			ret = -1;
		}
		return ret;
	}

	public void close() {
		CallPdfLibrary.pdfClose();
	}
}
