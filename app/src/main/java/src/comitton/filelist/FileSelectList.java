package src.comitton.filelist;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import src.comitton.common.DEF;
import src.comitton.common.FileAccess;
import src.comitton.data.FileData;
import src.comitton.dialog.LoadingDialog;

import jcifs.smb.SmbFile;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Environment;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

@SuppressLint("DefaultLocale")
public class FileSelectList implements Runnable, Callback, DialogInterface.OnDismissListener {
	private static final int LISTMODE_LOCAL = 0;
	private static final int LISTMODE_SERVER = 1;
	private static final int LISTMODE_WEBDAV = 2;

	//標準のストレージパスを保存
	private static final String mStaticRootDir = Environment.getExternalStorageDirectory().getAbsolutePath() +"/";

	private ArrayList<FileData> mFileList = null;

	private String mUri;
	private String mPath;
	private String mUser;
	private String mPass;
	private int mListMode = LISTMODE_LOCAL;
	private int mSortMode = 0;
	private boolean mParentMove;
	private boolean mHidden;
	private boolean mFilter;
	private boolean mApplyDir;
	private String mMarker;

	public LoadingDialog mDialog;
	private Handler mHandler;
	private Handler mActivityHandler;
	private Context mContext;
	private SharedPreferences mSp;
	private Thread mThread;

	public FileSelectList(Handler handler, Context context, SharedPreferences sp) {
		mActivityHandler = handler;
		mHandler = new Handler(this);
		mContext = context;
		mSp = sp;
		return;
	}

	// パス
	public void setPath(String uri, String path, String user, String pass) {
		mUri = uri;
		mPath = path;
		mUser = user;
		mPass = pass;
		if (uri != null && uri.startsWith("smb://")) {
			mListMode = LISTMODE_SERVER;
		}
		else if (uri != null && uri.startsWith("http")) {
			mListMode = LISTMODE_WEBDAV;
		}
		else {
			mListMode = LISTMODE_LOCAL;
		}
	}

	// ソートモード
	public void setMode(int mode) {
		mSortMode = mode;
		if (mFileList != null) {
			// ソートあり設定の場合
			Collections.sort(mFileList, new MyComparator());
		}
	}

	// リストモード
	public void setParams(boolean hidden, String marker, boolean filter, boolean applydir, boolean parentmove) {
		mHidden = hidden;
		mMarker = marker;
		mFilter = filter;
		mApplyDir = applydir;
		mParentMove = parentmove;
	}

	public ArrayList<FileData> getFileList() {
		return mFileList;
	}

	public void setFileList(ArrayList<FileData> filelist) {
		mFileList = filelist; 
	}

	public void loadFileList() {
		mDialog = new LoadingDialog(mContext);
		mDialog.setOnDismissListener(this);
		mDialog.show();

		// サムネイルスレッド開始
		if (mThread != null) {
			// 起動中のスレッドあり
			return;
		}

		mThread = new Thread(this);
		mThread.start();
		return;
	}

	@Override
	public void run() {
		boolean flag = false;
		String name = "";
		short type;
		short exttype;
		int state;
		long size = 0;
		long date = 0;
		boolean hit;

		Thread thread = mThread;
		boolean hidden = mHidden;
		String marker = mMarker.toUpperCase();
		if (marker != null && marker.equals("")) {
			// 空文字列ならnullにする
			marker = null;
		}
		
		File lfiles[] = null;
		SmbFile sfile = null;
		SmbFile[] sfiles = null;
//		WDFileData[] wdfiles = null;

		ArrayList<FileData> fileList = null;
		mFileList = null;

		try {
			if (mListMode == LISTMODE_LOCAL) {
				// ローカルの場合のファイル一覧取得
				lfiles = new File(mPath).listFiles();
				if (lfiles == null) {
					flag = true;
				}
			}
			else if (mListMode == LISTMODE_SERVER) {
				// サーバの場合のファイル一覧取得
				// サーバの場合のファイル一覧取得
				sfile = FileAccess.authSmbFile(mUri + mPath, mUser, mPass);
				sfiles = sfile.listFiles();
				if (sfiles == null) {
					flag = true;
				}
			}
			else {
				// WebDAVサーバの場合のファイル一覧取得
//				wdfile = new WDFile(mUri + mPath, mUser, mPass);
//				wdfiles = wdfile.listFiles();
//				if (wdfiles == null) {
//					flag = true;
//				}
			}
			if (thread.isInterrupted()) {
				// 処理中断
				return;
			}

			if (flag) {
				Log.d("FileSelectList", "run ファイルがありません。");
				// ファイル無しの場合は .. を表示
				fileList = new ArrayList<FileData>();
				if (!mPath.equals("/") && mParentMove) {
					// ツールバーがある場合は不要
					FileData fileData = new FileData();
					fileData.setName("..");
					fileData.setType(FileData.FILETYPE_PARENT);
					fileData.setState(0);
					fileList.add(fileData);
				}
				// 初期フォルダより上のフォルダの場合
				if (mStaticRootDir.startsWith(mPath) && !mStaticRootDir.equals(mPath)) {
					int pos = mStaticRootDir.indexOf("/", mPath.length());
					String dir = mStaticRootDir.substring(mPath.length(), pos + 1);

					//途中のフォルダを表示対象に追加
					FileData fileData = new FileData();
					fileData.setExtType(FileData.EXTTYPE_NONE);
					fileData.setName(dir);
					fileData.setType(FileData.FILETYPE_DIR);
					fileData.setState(-1);
					fileList.add(fileData);
				}
				// 処理中断
				sendResult(true, thread);
				mFileList = fileList;
				return;
			}

			int length = 0;
			if (mListMode == LISTMODE_LOCAL) {
				length = lfiles.length;
			} else if (mListMode == LISTMODE_SERVER) {
				length = sfiles.length;
    		} else {
//    			length = wdfiles.length;
    		}
			fileList = new ArrayList<FileData>(length + 1);

			if (!mPath.equals("/") && mParentMove) {
				// ツールバーがある場合は不要
				FileData fileData = new FileData();
				fileData.setName("..");
				fileData.setType(FileData.FILETYPE_PARENT);
				fileData.setState(0);
				fileList.add(fileData);
			}

			// ファイル名のリストを作る
			for (int i = 0; i < length; i++) {
				if (mListMode == LISTMODE_LOCAL) {
					name = lfiles[i].getName();
					flag = lfiles[i].isDirectory();
					size = lfiles[i].length();
					date = lfiles[i].lastModified();
				} else if (mListMode == LISTMODE_SERVER) {
					name = sfiles[i].getName();
					Log.d("FileSelectList", "run name=" + name);
					int len = name.length();
					if (name != null && len >= 1 && name.substring(len - 1).equals("/")) {
						flag = true;
					} else {
						flag = false;
					}
					size = sfiles[i].length();
					date = sfiles[i].lastModified();
				}
				else {
					// WebDAV
//					name = wdfiles[i].getName();
//					flag = wdfiles[i].isDirectory();
//					size = wdfiles[i].length();
//					date = wdfiles[i].lastModified();
				}

				if (flag) {
					// ディレクトリの場合
					int len = name.length();
					if (len >= 1 && !name.substring(len - 1).equals("/")) {
						name += "/";
					}
					type = FileData.FILETYPE_DIR;
					exttype = FileData.EXTTYPE_NONE;
					state = mSp.getInt(FileAccess.createUrl(mUri + mPath + name, mUser, mPass), -1);
				} else {
					// 通常のファイル
					int len = name.length();
					if (len < 5) {
						continue;
					}
					if (hidden == true && DEF.checkHiddenFile(name)) {
						continue;
					}
					String ext = DEF.getFileExt(name);
					if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".gif")/* || ext.equals(".bmp")*/) {
//						if (mUri != null && !mUri.equals("")) {
//							// ネットワーク上のファイル単体はサポートしない
//							continue;
//						}
						type = FileData.FILETYPE_IMG;
						if (ext.equals(".jpg") || ext.equals(".jpeg")) {
							exttype = FileData.EXTTYPE_JPG;
						}
						else if (ext.equals(".png")) {
							exttype = FileData.EXTTYPE_PNG;
						}
						else {
							exttype = FileData.EXTTYPE_GIF;
						}
						state = 0;
					}
					else if (ext.equals(".zip") || ext.equals(".rar") || ext.equals(".cbz") || ext.equals(".cbr") || ext.equals(".pdf") || ext.equals(".epub")) {
						type = FileData.FILETYPE_ARC;
						if (ext.equals(".zip") || ext.equals(".cbz") || ext.equals(".epub")) {
							exttype = FileData.EXTTYPE_ZIP;
						}
						else if (ext.equals(".rar") || ext.equals(".cbr")) {
							exttype = FileData.EXTTYPE_RAR;
						}
						else {
							exttype = FileData.EXTTYPE_PDF;
						}
						state = mSp.getInt(FileAccess.createUrl(mUri + mPath + name, mUser, mPass), -1);
					}
					else if (ext.equals(".txt") || ext.equals(".xhtml") || ext.equals(".html")) {
						type = FileData.FILETYPE_TXT;
						exttype = FileData.EXTTYPE_TXT;
						state = mSp.getInt(FileAccess.createUrl(mUri + mPath + name, mUser, mPass), -1);
					}
					else {
						continue;
					}
				}
				hit = false;
				if (marker != null) {
					if (name.toUpperCase().indexOf(marker) != -1) {
						// 検索文字列が含まれる
						hit = true;
					}
				}

				FileData fileData = new FileData();
				fileData.setType(type);
				fileData.setExtType(exttype);
				fileData.setName(name);
				fileData.setState(state);
				fileData.setSize(size);
				fileData.setDate(date);
				fileData.setMarker(hit);
				//マークではなくフィルタに
				if(mFilter) {
					//マーカー未設定orディレクトリに適用しない場合のディレクトリは無条件で追加
					if (marker == null || (mApplyDir == false && flag) ) {
						fileList.add(fileData);
					}else if(hit) {
						fileList.add(fileData);
					}
				}else
					fileList.add(fileData);

				if (thread.isInterrupted()) {
					// 処理中断
					return;
				}
			}
		}
		catch (Exception e) {
			String s = null;
			if (e != null) {
				s = e.getMessage();
				if (s != null) {
					Log.e("FileSelectList", s);
				}
				else {
					s = "error.";
				}
				e.printStackTrace();
			}
			sendResult(false, s, thread);
			return;
		} finally {
			if (mListMode == LISTMODE_LOCAL) {
				// ローカルの場合のファイル一覧取得
				lfiles = null;
			} else if (mListMode == LISTMODE_SERVER) {
				// サーバの場合のファイル一覧取得
				sfiles = null;
				sfile = null;
    		} else if (mListMode == LISTMODE_WEBDAV) {
    			// WebDAVサーバの場合のファイル一覧取得
//    			wdfiles = null;
//    			wdfile = null;
    		}
		}

		if (thread.isInterrupted()) {
			// 処理中断
			return;
		}

		// sort
		if (mSortMode != 0) {
			// ソートあり設定の場合
			Collections.sort(fileList, new MyComparator());
		}

		if (thread.isInterrupted()) {
			// 処理中断
			return;
		}
		mFileList = fileList;
		sendResult(true, thread);
	}

	public class MyComparator implements Comparator<FileData> {
		public int compare(FileData file1, FileData file2) {

			int result;
			// ディレクトリ/ファイルタイプ
			int type1 = file1.getType();
			int type2 = file2.getType();
			if (type1 == FileData.FILETYPE_PARENT || type2 == FileData.FILETYPE_PARENT) {
				return type1 - type2;
			}
			else if (mSortMode == DEF.ZIPSORT_FILESEP || mSortMode == DEF.ZIPSORT_NEWSEP || mSortMode == DEF.ZIPSORT_OLDSEP) {
				// IMAGEとZIPのソート優先度は同じにする
				if (type1 == FileData.FILETYPE_IMG || type1 == FileData.FILETYPE_TXT) {
					type1 = FileData.FILETYPE_ARC;
				}
				if (type2 == FileData.FILETYPE_IMG || type2 == FileData.FILETYPE_TXT) {
					type2 = FileData.FILETYPE_ARC;
				}

				result = type1 - type2;
				if (result != 0) {
					return result;
				}
			}
			switch (mSortMode) {
				case DEF.ZIPSORT_FILEMGR:
				case DEF.ZIPSORT_FILESEP:
//					return file1.getName().toUpperCase().compareTo(file2.getName().toUpperCase());
					return DEF.compareFileName(file1.getName().toLowerCase(), file2.getName().toLowerCase());
				case DEF.ZIPSORT_NEWMGR:
				case DEF.ZIPSORT_NEWSEP:
				{
					long val = file2.getDate() - file1.getDate(); 
					return val == 0 ? 0 : (val > 0 ? 1 : -1);
				}
				case DEF.ZIPSORT_OLDMGR:
				case DEF.ZIPSORT_OLDSEP:
				{
					long val = file1.getDate() - file2.getDate();
					return val == 0 ? 0 : (val > 0 ? 1 : -1);
				}
			}
			return 0;
		}
	}
	
	private void sendResult(boolean result, Thread thread) {
		sendResult(result, result ? null : "User Cancelled.", thread);
	}

	private void sendResult(boolean result, String str, Thread thread) {
		if (mThread != null) {
			if (mThread == thread) {
				if (result == false) {
					mFileList = new ArrayList<FileData>();
					if (mParentMove) {
    					FileData fileData = new FileData();
    					fileData.setName("..");
    					fileData.setType(FileData.FILETYPE_PARENT);
    					fileData.setState(0);
    					mFileList.add(fileData);
					}
				}

				Message message;
				message = new Message();
				message.what = DEF.HMSG_LOADFILELIST;
				message.arg1 = result ? 1 : 0;
				mActivityHandler.sendMessage(message);

				message = new Message();
				message.what = DEF.HMSG_LOADFILELIST;
				message.arg1 = result ? 1 : 0;
				message.obj = str;
				mHandler.sendMessage(message);
			}
			mThread = null;
		}
	}

	public void closeDialog() {
		if (mDialog != null) {
			try {
				mDialog.dismiss();
			}
			catch (IllegalArgumentException e) {
				;
			}
			mDialog = null;
		}
	}

	@Override
	public void onDismiss(DialogInterface di) {
		// 閉じる
		if (mDialog != null) {
			mDialog = null;
			// 割り込み
			if (mThread != null) {
				mThread.interrupt();

				// キャンセル時のみ
				sendResult(false, mThread);
			}
		}
	}

	@Override
	public boolean handleMessage(Message msg) {
		// 終了
		closeDialog();
		if (msg.obj != null) {
			Toast.makeText(mContext, (String)msg.obj, Toast.LENGTH_LONG).show();
		}
		return false;
	}
}
