package src.comitton.stream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

import src.comitton.common.FileAccess;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbRandomAccessFile;
import src.comitton.exception.FileAccessException;

public class TextInputStream extends InputStream {
	public static final int HOSTTYPE_ERROR = 0;
	public static final int HOSTTYPE_LOCAL = 1;
	public static final int HOSTTYPE_SAMBA = 2;

	private SmbRandomAccessFile mSambaRnd;
	private RandomAccessFile mLocalRnd;
	private int mHostType;

	public TextInputStream() {
		mHostType = HOSTTYPE_LOCAL;
	}

	/*************************** FileAccess ***************************/
	public void fileAccessInit(String path, String user, String pass) throws IOException {
		// 参照先
		if (path.length() >= 1 && path.substring(0, 1).equals("/")) {
			// ローカルパス
			mHostType = HOSTTYPE_LOCAL;
			mLocalRnd = new RandomAccessFile(path, "r");
		}
		else if (path.length() >= 6 && path.substring(0, 6).equals("smb://")) {
			// サーバパス
			mHostType = HOSTTYPE_SAMBA;
			boolean exists = false;
			try {
				exists = FileAccess.exists(path, user, pass);
			} catch (FileAccessException e) {
				e.printStackTrace();
			}
			if (!exists) {
				throw new IOException("File not found.");
			}
			mSambaRnd = FileAccess.smbRandomAccessFile(path, user, pass);
		}
		else {
			mHostType = HOSTTYPE_ERROR;
			throw new IOException("Illegal path");
		}
		return;
	}

	
	@Override
	public int read() throws IOException {
		// デフォルト使用しない
		return 0;
	}

	@Override
	public int read(byte buf[], int off, int len) throws IOException {
		int ret = 0;
		if (mHostType == HOSTTYPE_SAMBA) {
			ret = mSambaRnd.read(buf, off, len);
		}
		else if (mHostType == HOSTTYPE_LOCAL) {
			ret = mLocalRnd.read(buf, off, len);
		}
		return ret;
	}

	public void fileDirectSeek(long pos) throws IOException {
		// エントリーサイズ
		if (mHostType == HOSTTYPE_SAMBA) {
			mSambaRnd.seek(pos);
		}
		else if (mHostType == HOSTTYPE_LOCAL) {
			mLocalRnd.seek(pos);
		}
	}

	public long fileLength() throws IOException {
		long fileLength = 0;

		// エントリーサイズ
		if (mHostType == HOSTTYPE_SAMBA) {
			fileLength = mSambaRnd.length();
		}
		else if (mHostType == HOSTTYPE_LOCAL) {
			fileLength = mLocalRnd.length();
		}
		if ((fileLength & 0xFFFFFFFF00000000L) == fileLength) {
			fileLength = (fileLength >> 32) & 0x00000000FFFFFFFFL;
		}
		return fileLength;
	}

	public void fileClose() throws IOException {
		if (mSambaRnd != null) {
			mSambaRnd.close();
			mSambaRnd = null;
		}
		if (mLocalRnd != null) {
			mLocalRnd.close();
			mLocalRnd = null;
		}
		return;
	}
}
