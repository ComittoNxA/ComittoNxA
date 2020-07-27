package src.comitton.stream;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.UnknownHostException;

import src.comitton.common.DEF;
import src.comitton.common.FileAccess;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbRandomAccessFile;

public class WorkStream extends InputStream {
	public static final int OFFSET_LCL_FNAME_LEN = 26;
	public static final int SIZE_LOCALHEADER = 30;

	private String mURI;
	private SmbRandomAccessFile mSambaFile;
	private RandomAccessFile mLocalFile;
	private int mPos;
	private boolean mZipFlag;

	public WorkStream(String uri, String path, String user, String pass, boolean zipflag) throws IOException, SmbException, UnknownHostException, MalformedURLException, FileNotFoundException {
		mURI = uri;
		if (uri != null && uri.length() > 0) {
			SmbFile file;
			file = FileAccess.authSmbFile(uri + path, user, pass);
			if (!file.exists()) {
				throw new IOException("File not found.");
			}
			mSambaFile = new SmbRandomAccessFile(file, "r");
		}
		else {
			mLocalFile = new RandomAccessFile(path, "r");
		}
		mPos = 0;
		mZipFlag = zipflag;
	}

	public void seek(long pos) throws IOException {
		if (mURI != null && mURI.length() > 0) {
			mSambaFile.seek(pos);
		}
		else {
			mLocalFile.seek(pos);
		}
		mPos = 0;
	}

	@Override
	public int read() throws IOException {
		// 読み込み
		return 0;
	}

	public int read(byte buf[], int off, int size) throws IOException {
		int ret;
		if (mURI != null && mURI.length() > 0) {
			ret = mSambaFile.read(buf, off, size);
		}
		else {
			ret = mLocalFile.read(buf, off, size);
		}
		if (mPos == 0 && mZipFlag) {
			if (ret >= OFFSET_LCL_FNAME_LEN + 2) {
				int lenFName = DEF.getShort(buf, OFFSET_LCL_FNAME_LEN);

				if (ret >= SIZE_LOCALHEADER + lenFName) {
					for (int i = 0; i < lenFName - 4; i++) {
						buf[off + SIZE_LOCALHEADER + i] = '0';
					}
				}
			}
		}
		if (ret > 0) {
			mPos += ret;
		}
		return ret;
	}
}

