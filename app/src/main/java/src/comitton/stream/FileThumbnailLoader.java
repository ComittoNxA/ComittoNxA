package src.comitton.stream;

import java.io.IOException;
import java.util.ArrayList;

import jp.dip.muracoro.comittona.ImageActivity;
import src.comitton.common.DEF;
import src.comitton.common.FileAccess;
import src.comitton.common.ImageAccess;
import src.comitton.common.WaitFor;
import src.comitton.config.SetCacheActivity;
import src.comitton.data.FileData;


import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import java.io.BufferedInputStream;

public class FileThumbnailLoader extends ThumbnailLoader implements Runnable {
	private final int FILETYPE_IMG = 0;
	private final int FILETYPE_ZIP = 1;
	private final int FILETYPE_RAR = 2;
	private final int FILETYPE_PDF = 3;

	private String mUser;
	private String mPass;
	private int mFileSort;
	private String mCharset;
	private boolean mHidden;
	private boolean mThumbSort;

	private ImageManager mImageMgr;
	private Object mImageMgrLock;

	private WaitFor mWaitFor;
	
	public FileThumbnailLoader(String uri, String path, String user, String pass, Handler handler, long id, ArrayList<FileData> files, int sizeW, int sizeH, int cachenum, int filesort, String charset, boolean hidden, boolean thumbsort) {
		super(uri, path, handler, id, files, sizeW, sizeH, cachenum);

		mUser = user;
		mPass = pass;
		mFileSort = filesort;
		mCharset = charset;
		mHidden = hidden;
		mThumbSort = thumbsort;

		mImageMgrLock = new Object();

		mWaitFor = new WaitFor(60000);

		// スレッド起動
		Thread thread = new Thread(this);
		thread.start();
		return;
	}

	// 解放
	public void releaseThumbnail() {
		super.releaseThumbnail();
		return;
	}

	// スレッド停止
	public void breakThread() {
		super.breakThread();

		// 読み込み終了
		synchronized (mImageMgrLock) {
			if (mImageMgr != null) {
				mImageMgr.setBreakTrigger();
			}
		}
		return;
	}

	protected void interruptThread() {
		if (mWaitFor != null) {
			mWaitFor.interrupt();
		}
	}

	// スレッド開始
	public void run() {
//		synchronized (mLock) {
			if (mThreadBreak == true || mCachePath == null || mFiles == null) {
				return;
			}
			int fileNum = mFiles.size();
			if (fileNum <= 0) {
				return;
			}
			// サムネイル保持領域初期化
			int ret = CallImgLibrary.ThumbnailInitialize(mID, DEF.THUMBNAIL_PAGESIZE, DEF.THUMBNAIL_MAXPAGE, fileNum);
			if (ret < 0) {
				return;
			}
			
			int thum_cx = mThumbSizeW;
			int thum_cy = mThumbSizeH;
			int firstindex = -1;
			int lastindex;

			while (mThreadBreak == false) {
				// 初回又は変化があるかのチェック
				if (firstindex != mFirstIndex) {
					// 表示範囲
					firstindex = mFirstIndex;
					lastindex = mLastIndex;
					
					// 最初は表示範囲を優先
					for (int loop = 0 ; loop < 2 ; loop ++) {
						// 1週目キャッシュからのみ、2週目は実体も
						for (int i = firstindex ; i <= lastindex && firstindex == mFirstIndex ; i++) {
							if (i < 0 || i >= fileNum) {
								// 範囲内だけ処理する
								continue;
							}

							// 1周目は新たに読み込みしない
							loadBitmap(i, thum_cx, thum_cy, loop == 0, true);
							if (mThreadBreak == true) {
								return;
							}
						}
					}
					if (firstindex != mFirstIndex) {
						// 選択範囲が変わったら再チェック
						continue;
					}

					// 前後をキャッシュから読み込み
					// 前後をキャッシュから読み込み
					int range = (lastindex - firstindex) * 2;
					boolean isBreak = false;
					boolean prevflag = false;
					boolean nextflag = false;
					for (int count = 1 ; count <= range && isBreak == false ; count ++) {
						if ((prevflag && nextflag) || firstindex != mFirstIndex) {
							// 範囲オーバー、選択範囲変更 
							break;
						}
						for (int way = 0 ; way < 2 && firstindex == mFirstIndex ; way ++) {
							if (mThreadBreak == true) {
								return;
							}
							// キャッシュからのみ
							int index;
							if (way == 0) {
								// 前側
								index = firstindex - count;
								if (index < 0) {
									// 範囲内だけ処理する
									prevflag = true;
									continue;
								}
							}
							else {
								// 後側
								index = lastindex + count;
								if (index >= fileNum) {
									// 範囲内だけ処理する
									nextflag = true;
									continue;
								}
							}
							// キャッシュからのみ読み込み
							if (loadBitmap(index, thum_cx, thum_cy, true, false) == false) {
								// メモリ不足で中断
								isBreak = true;
								break;
							}
						}
					}
					if (firstindex != mFirstIndex) {
						continue;
					}

					// 前後をキャッシュから読み込み
					range = (lastindex - firstindex);
					isBreak = false;
					prevflag = false;
					nextflag = false;
					for (int count = 1 ; count <= range && firstindex == mFirstIndex ; count ++) {
						if ((prevflag && nextflag) || firstindex != mFirstIndex) {
							// 範囲オーバー、選択範囲変更 
							break;
						}
						for (int way = 0 ; way < 2 && firstindex == mFirstIndex ; way ++) {
							if (mThreadBreak == true) {
								return;
							}
							// 実体も読み込み
							int index;
							if (way == 0) {
								// 前側
								index = firstindex - count;
								if (index < 0) {
									// 範囲内だけ処理する
									prevflag = true;
									continue;
								}
							}
							else {
								// 後側
								index = lastindex + count;
								if (index >= fileNum) {
									// 範囲内だけ処理する
									nextflag = true;
									continue;
								}
							}
							// キャッシュからのみ読み込み
							if (loadBitmap(index, thum_cx, thum_cy, false, true) == false) {
								break;
							}
						}
					}
					if (firstindex != mFirstIndex) {
						continue;
					}

				}
				
				if (CallImgLibrary.ThumbnailCheckAll(mID) == 0) {
					// 全部読み込めた
					break;
				}
				else {
					// ページ選択待ちに入る
					mWaitFor.sleep();
				}
			}

			if (mThreadBreak == false) {
				// サムネイルキャッシュ削除
				if (mCachePath != null) {
					deleteThumbnailCache(mThumbCacheNum);
				}
			}
//		}
	}

	private boolean loadBitmap(int index, int thum_cx, int thum_cy, boolean firstloop, boolean priority) {
		// 読み込み済みチェック
		int result = CallImgLibrary.ThumbnailCheck(mID, index);
		if (result > 0) {
			// 既に読み込み済み
			return true;
		}

		// ファイル情報取得
		if (index >= mFiles.size()) {
			return false;
		}
		FileData data = mFiles.get(index);
		int filetype = data.getType();
		// 拡張子分の文字列長がない
		if (filetype == FileData.FILETYPE_TXT || filetype == FileData.FILETYPE_PARENT) {
			// 対象外のファイル
			CallImgLibrary.ThumbnailSetNone(mID, index);
			return true;
		}
		
		// ビットマップ読み込み
		Bitmap bm = null;
		BitmapFactory.Options option = new BitmapFactory.Options();
		String filename = data.getName();
		String filepath = mUri + mPath + filename;
		String ext = DEF.getExtension(filename);
		String pathcode = DEF.makeCode(filepath, thum_cx, thum_cy);
		boolean skipFile = false;
		boolean retval = true;

		// Log.d("ThumbnailFile", filepath);
		
		// キャッシュから読込
		if (checkThumbnailCache(pathcode)) {
			bm = loadThumbnailCache(pathcode);
			if (bm == null) {
				skipFile = true;
			}
		}

		if (mThreadBreak == true) {
			return true;
		}

		if (firstloop && bm == null) {
			// 初回ループはキャッシュからのみ読み込み
			skipFile = true;
		}
		if (skipFile == false) {
			String uripath = "";
			String pathfile = "";
			String infilename = "";
			if (bm == null) {
				// ディレクトリの場合は中のファイルを参照
				if (filename.endsWith("/")) {
					infilename = FileAccess.getInnerFile(mUri, mPath + filename, mUser, mPass);
					if (infilename != null) {
						ext = DEF.getExtension(infilename);
						uripath = mUri + mPath + filename;
						pathfile = mPath + filename + infilename;
					}
					else {
						// ディレクトリ内に画像ファイルがない
						skipFile = true;
						uripath = null;
						pathfile = null;

						// NoImageであればステータス設定
						CallImgLibrary.ThumbnailSetNone(mID, index);
					}
				}
				else {
					uripath = mUri + mPath;
					pathfile = mPath + filename;
					infilename = filename;
				}

				if (pathfile != null) {
					int type = FILETYPE_IMG;
					if (ext.equals(".zip") || ext.equals(".cbz") || ext.equals(".epub")) {
						type = FILETYPE_ZIP;
					}
					else if (ext.equals(".rar") || ext.equals(".cbr")) {
						type = FILETYPE_RAR;
					}
					else if (ext.equals(".pdf")) {
						type = FILETYPE_PDF;
					}

					try {
						if (type != FILETYPE_IMG) {
							int openmode = 0;
							// ファイルリストの読み込み
							if (mThumbSort) {
								openmode = ImageManager.OPENMODE_THUMBSORT;
							}
							else {
								openmode = ImageManager.OPENMODE_THUMBNAIL;
							}
							mImageMgr = new ImageManager(uripath, infilename, mUser, mPass, mFileSort, mHandler, mCharset, mHidden, openmode, 1);
							mImageMgr.LoadImageList(0, 0, 0);
							try {
								bm = mImageMgr.loadThumbnailFromStream(0, mThumbSizeW, mThumbSizeH);
							}
							catch (Exception ex) {
								String s = "exception";
								if (ex != null && ex.getMessage() != null) {
									s = ex.getMessage();
								}
								Log.i("Thumbnail", s);
							}
							finally {
								mImageMgr.close();
								synchronized (mImageMgrLock) {
									mImageMgr = null;
								}
							}

							if (bm == null) {
								// NoImageであればステータス設定
								CallImgLibrary.ThumbnailSetNone(mID, index);
							}
						}
						else {
							WorkStream ws = new WorkStream(mUri, pathfile, mUser, mPass, type == FILETYPE_ZIP);

							// サイズのみ取得
							option.inJustDecodeBounds = true;
							BitmapFactory.decodeStream(ws, null, option);

							if (option.outHeight != -1 && option.outWidth != -1) {
								// 縮小してファイル読込
								option.inJustDecodeBounds = false;
								option.inSampleSize = DEF.calcThumbnailScale(option.outWidth, option.outHeight, mThumbSizeW, mThumbSizeH);
								ws.seek(0);
								bm = BitmapFactory.decodeStream(new BufferedInputStream(ws, 100*1024), null, option);

								if (bm == null) {
									// NoImageであればステータス設定
									CallImgLibrary.ThumbnailSetNone(mID, index);
								}
							}
						}
					}
					catch (Exception e) {
						String s = "exception";
						if (e != null && e.getMessage() != null) {
							s = e.getMessage();
						}
						Log.e("Thumbnail/Load", s);
					}
				}
			}

			if (mThreadBreak == true) {
				// 読み込み中断
				return true;
			}

			// ビットマップをサムネイルサイズぴったりにリサイズする
			if (bm != null) {
				bm = ImageAccess.resizeTumbnailBitmap(bm, thum_cx, thum_cy, ImageAccess.BMPALIGN_AUTO);
			}
			if (bm != null) {
				int w = bm.getWidth();
				int h = bm.getHeight();
				boolean chg = false;
				if (w > mThumbSizeW) {
					w = mThumbSizeW;
					chg = true;
				}
				if (h > mThumbSizeH) {
					h = mThumbSizeH;
					chg = true;
				}

				// ビットマップを切り出す
				if (chg || bm.getConfig() != Config.RGB_565) {
					Bitmap bm2 = Bitmap.createBitmap(w, h, Config.RGB_565);
					bm = Bitmap.createBitmap(bm, 0, 0, w, h);
					Paint drawBmp = new Paint();
					Canvas offScreen = new Canvas(bm2);
					drawBmp.setColor(0xFFFFFFFF);
					drawBmp.setStyle(Style.FILL);
					offScreen.drawRect(0, 0, w, h, drawBmp);
					offScreen.drawBitmap(bm, 0, 0, null);
					bm = bm2;
				}
			}

			boolean save = false;
			if (bm != null) {
				// 空きメモリがあるかをチェック
				result = CallImgLibrary.ThumbnailSizeCheck(mID, bm.getWidth(), bm.getHeight());
				if (result == 0) {
					// メモリあり
					save = true;
				}
				else if (result > 0 && priority) {
					// 表示の中心から外れたものを解放してメモリを空ける
					result = CallImgLibrary.ThumbnailImageAlloc(mID, result, (mFirstIndex + mLastIndex) / 2);
					if (result == 0) {
						// メモリ獲得成功
						save = true;	
					}
					else {
						// メモなし
						retval = false;
					}
				}
			}
			if (bm != null && save) {
				result = CallImgLibrary.ThumbnailSave(mID, bm, index);
				if (result != CallImgLibrary.RESULT_OK) {
					// メモリ保持成功
					retval = false;
				}
			}
		}
		if (bm != null) {
			saveThumbnailCache(pathcode, bm);
			if (retval == false) {
				// メモリ保持失敗の場合は描画しない
				bm = null;
			}
		}
		if ((firstloop == true && bm != null) || firstloop == false) {
			// 通知
			Message message = new Message();
			message.what = DEF.HMSG_THUMBNAIL;
			message.arg1 = bm != null ? index : DEF.THUMBSTATE_ERROR;
			message.obj = filename;
			mHandler.sendMessage(message);
		}
		return retval;
	}
}
