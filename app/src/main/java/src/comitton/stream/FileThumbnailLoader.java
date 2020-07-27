package src.comitton.stream;

import java.util.ArrayList;

import src.comitton.common.DEF;
import src.comitton.common.FileAccess;
import src.comitton.common.ImageAccess;
import src.comitton.common.WaitFor;
import src.comitton.data.FileData;


import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Message;
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

	private boolean mOut_of_memory = false;

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

	public FileThumbnailLoader(String uri, String path, Handler handler, long id, ArrayList<FileData> files, int sizeW, int sizeH, int cachenum) {
		super(uri, path, handler, id, files, sizeW, sizeH, cachenum);
	}

	// スレッド開始
	public void run() {
		Log.d("FileThumbnailLoader"," 0 DirName=" + mPath);

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
				for (int loop = 0; loop < 2; loop++) {
					// 1週目キャッシュからのみ、2週目は実体も
					for (int i = firstindex; i <= lastindex && firstindex == mFirstIndex; i++) {
						if (i < 0 || i >= fileNum) {
							// 範囲内だけ処理する
							continue;
						}

						Log.d("FileThumbnailLoader","index=" + i + " " + (loop+1) + "周目 run");
						// 1周目は新たに読み込みしない
						loadBitmap(i, thum_cx, thum_cy, loop == 0, true);
						if (mThreadBreak == true) {
							Log.d("FileThumbnailLoader", "index=" + i + " " + (loop+1) + "周目 run 中断されました。");
							return;
						}
					}
				}
				if (firstindex != mFirstIndex) {
					// 選択範囲が変わったら再チェック
					continue;
				}

				// 前後をキャッシュから読み込み
				int range = (lastindex - firstindex) * 2;
				boolean isBreak = false;
				boolean prevflag = false;
				boolean nextflag = false;
				for (int count = 1; count <= range && isBreak == false; count++) {
					if ((prevflag && nextflag) || firstindex != mFirstIndex) {
						// 範囲オーバー、選択範囲変更
						break;
					}
					for (int way = 0; way < 2 && firstindex == mFirstIndex; way++) {
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
						} else {
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

				// 前後をキャッシュと実体から読み込み
				range = (lastindex - firstindex);
				isBreak = false;
				prevflag = false;
				nextflag = false;
				for (int count = 1; count <= range && firstindex == mFirstIndex; count++) {
					if ((prevflag && nextflag) || firstindex != mFirstIndex) {
						// 範囲オーバー、選択範囲変更
						break;
					}
					for (int way = 0; way < 2 && firstindex == mFirstIndex; way++) {
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
						} else {
							// 後側
							index = lastindex + count;
							if (index >= fileNum) {
								// 範囲内だけ処理する
								nextflag = true;
								continue;
							}
						}
						// キャッシュと実体からのみ読み込み
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
			} else {
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
		int result = CallImgLibrary.ThumbnailCheck(mID, index);
		boolean ret = false;
		if (result > 0) {
			// 既に読み込み済み
			Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap 読み込み済みです。");
			return true;
		}
		// ファイル情報取得
		if (index >= mFiles.size()) {
			return false;
		}
		FileData file = mFiles.get(index);
		Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap Filename=" + file.getName());
		String filename = file.getName();
		String filepath = mUri + mPath + filename;
		String pathcode = DEF.makeCode(filepath, thum_cx, thum_cy);
		Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap Filename=" + file.getName() + ", pathcode=" + pathcode);
		int filetype = file.getType();

		if (filename.equals("..")) {
			// 対象外のファイル
			CallImgLibrary.ThumbnailSetNone(mID, index);
			ret = true;
		}
		else {
			String ext = DEF.getExtension(filename);
			if (ext.equals(".txt")) {
				// 対象外のファイル
				CallImgLibrary.ThumbnailSetNone(mID, index);
				ret = true;
			}
			else {
				ret = loadBitmap2(filename, index, thum_cx, thum_cy, firstloop, priority, pathcode);
			}
		}

		if (firstloop == false && ret == false) {
			// 2周目で画像セーブに失敗していたら
			CallImgLibrary.ThumbnailSetNone(mID, index);
			Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap 空で登録しました。");
		}

		if ((firstloop == true && ret == true) || firstloop == false) {
			// 1周目で画像セーブに成功しているか、2周目が終わったら
			// 通知
			Message message = new Message();
			message.what = DEF.HMSG_THUMBNAIL;
			message.arg1 = ret == true ? index : DEF.THUMBSTATE_ERROR;
			message.obj = getFilename(index);
			mHandler.sendMessage(message);
			Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap 通知しました arg1=" + message.arg1);
		}
		return !mOut_of_memory;
	}

	private boolean loadBitmap2(String filename, int index, int thum_cx, int thum_cy, boolean firstloop, boolean priority, String pathcode) {
		Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap2  Filename=" + filename);
		boolean ret = false;
		Bitmap bm = null;

		if (filename.equals("..")) {
			// 対象外のファイル
			//CallImgLibrary.ThumbnailSetNone(mID, index);
			return false;
		}
		String ext = DEF.getExtension(filename);
		if (ext.equals(".txt")) {
			// 対象外のファイル
			//CallImgLibrary.ThumbnailSetNone(mID, index);
			return false;
		}

		// キャッシュから読込
		if (checkThumbnailCache(pathcode)) {
			Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap2 キャッシュに登録済みです。");
			bm = loadThumbnailCache(pathcode);
			if (bm == null) {
				Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap2 キャッシュが空でした。スキップします。");
				return true;
			}
			else {
				Log.d("FileThumbnailLoader", "index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap2 キャッシュがありました。");
				loadMemory(index, thum_cx, thum_cy, bm, priority, pathcode);
				//saveBitmap(bm, pathcode);
				return true;
			}
		}
		else {
			Log.d("FileThumbnailLoader", "index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap2 キャッシュに登録済されていません。");
		}

		if (mThreadBreak == true) {
			Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap2 中断されました。");
			return false;
		}

		if (firstloop && bm == null) {
			// 初回ループはキャッシュからのみ読み込み
			Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap2 スキップします。");
			return false;
		}

		ArrayList<String> infilename = new ArrayList<String>();

		if (bm == null) {
			// ディレクトリの場合は中のファイルを参照
			if (filename.endsWith("/")) {
				Log.d("FileThumbnailLoader","index=" + index + " " + (firstloop ? 1 : 2) + "周目 loadBitmap2 ディレクトリの中を検索します。");
				infilename = FileAccess.getInnerFile(mUri, mPath + filename, mUser, mPass);

				if (infilename == null) {
					return false;
				}
				for (int i = 0; i < infilename.size(); i++) {
					if (infilename.get(i).endsWith("/")) {
						if (loadBitmap2(filename + infilename.get(i), index, thum_cx, thum_cy, firstloop, priority, pathcode)) {
								return true;
						}
					} else if (infilename.get(0) != null) {
						if (loadBitmap3(filename + infilename.get(i), index, thum_cx, thum_cy, priority, pathcode)) {
							return true;
						}
					} else {
						continue;
					}
				}
			}
			else {
				if (loadBitmap3(filename, index, thum_cx, thum_cy, priority, pathcode)) {
					return true;
				}
			}
		}
		return ret;
	}

	private boolean loadBitmap3(String filename, int index, int thum_cx, int thum_cy, boolean priority, String pathcode) {
		Log.d("FileThumbnailLoader","index=" + index + " loadBitmap3 Filename=" + filename);
		// ビットマップ読み込み
		Bitmap bm = null;
		BitmapFactory.Options option = new BitmapFactory.Options();
		String uripath = mUri + mPath;
		String pathfile = mPath + filename;
		String ext = DEF.getExtension(filename);
		Log.d("FileThumbnailLoader","index=" + index + " loadBitmap3 拡張子を取得します。 ext=" + ext);

		if (pathfile != null) {
			int type = FILETYPE_IMG;
			if (ext.equals(".zip") || ext.equals(".cbz") || ext.equals(".epub")) {
				Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 Filename=" + filename + ", type=zip");
				type = FILETYPE_ZIP;
			}
			else if (ext.equals(".rar") || ext.equals(".cbr")) {
				Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 Filename=" + filename + ", type=rar");
				type = FILETYPE_RAR;
			}
			else if (ext.equals(".pdf")) {
				Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 Filename=" + filename + ", type=pdf");
				type = FILETYPE_PDF;
			}
			else {
				return false;
			}
			try {
				if (type != FILETYPE_IMG) {
					Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 圧縮ファイルを開きます。");
					int openmode = 0;
					// ファイルリストの読み込み
					if (mThumbSort) {
						openmode = ImageManager.OPENMODE_THUMBSORT;
					} else {
						openmode = ImageManager.OPENMODE_THUMBNAIL;
					}
					mImageMgr = new ImageManager(uripath, filename, mUser, mPass, mFileSort, mHandler, mCharset, mHidden, openmode, 1);
					mImageMgr.LoadImageList(0, 0, 0);
					try {
						bm = mImageMgr.loadThumbnailFromStream(0, mThumbSizeW, mThumbSizeH);
					} catch (Exception ex) {
						Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 エラーになりました。");
						String s = "exception";
						if (ex != null && ex.getMessage() != null) {
							s = ex.getMessage();
							return false;
						}
						Log.i("Thumbnail", s);
					} finally {
						Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 圧縮ファイルを開きました。");
						mImageMgr.close();
						synchronized (mImageMgrLock) {
							mImageMgr = null;
						}
					}

					if (bm == null) {
						// NoImageであればステータス設定
						//CallImgLibrary.ThumbnailSetNone(mID, index);
						Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 取得できませんでした");
						return false;
					}
				} else {
					Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 イメージファイルを開きます。 mUri=" + mUri + ", pathfile=" + pathfile + ", mUser=" + mUser + ", mPass=" + mPass);
					WorkStream ws = new WorkStream(mUri, pathfile, mUser, mPass, type == FILETYPE_ZIP);

					// サイズのみ取得
					option.inJustDecodeBounds = true;
					BitmapFactory.decodeStream(ws, null, option);

					Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 イメージファイルのサイズ。 outWidth=" + option.outWidth + ", outoutheight=" + option.outHeight);
					if (option.outHeight != -1 && option.outWidth != -1) {
						// 縮小してファイル読込
						option.inJustDecodeBounds = false;
						option.inSampleSize = DEF.calcThumbnailScale(option.outWidth, option.outHeight, mThumbSizeW, mThumbSizeH);
						ws.seek(0);
						bm = BitmapFactory.decodeStream(new BufferedInputStream(ws, 100 * 1024), null, option);

						if (bm == null) {
							Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 イメージファイルを取得できませんでした");
							// NoImageであればステータス設定
							return false;
							//CallImgLibrary.ThumbnailSetNone(mID, index);
						}
					}
					Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 イメージファイルを開きました。");
				}
			} catch (Exception e) {
				Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 エラーになりました。");
				String s = "exception";
				if (e != null && e.getMessage() != null) {
					s = e.getMessage();
					return false;
				}
				Log.e("Thumbnail/Load", s);
			}
			if (mThreadBreak == true) {
				// 読み込み中断
				Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 中断されました");
				return true;
			}

			loadMemory(index, thum_cx, thum_cy, bm, priority, pathcode);
			saveCache(bm, pathcode);

		}

		return true;
	}

	private void loadMemory(int index, int thum_cx, int thum_cy, Bitmap bm, boolean priority, String pathcode) {
		int result;
		boolean save = false;

		if (bm != null) {
			Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 NULLじゃないです");
		} else {
			Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 NULLです");
		}
		// ビットマップをサムネイルサイズぴったりにリサイズする
		if (bm != null) {
			Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 リサイズします");
			bm = ImageAccess.resizeTumbnailBitmap(bm, thum_cx, thum_cy, ImageAccess.BMPALIGN_AUTO);
		}
		if (bm != null) {
			Log.d("FileThumbnailLoader", "index=" + index + " loadBitmap3 切り出します");
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

		if (bm != null) {
			// 空きメモリがあるかをチェック
			result = CallImgLibrary.ThumbnailSizeCheck(mID, bm.getWidth(), bm.getHeight());
			if (result == 0) {
				// メモリあり
				Log.d("FileThumbnailLoader", "index=" + index + " loadCache 空きメモリがありました");
				save = true;
			} else if (result > 0 && priority) {
				// 表示の中心から外れたものを解放してメモリを空ける
				result = CallImgLibrary.ThumbnailImageAlloc(mID, result, (mFirstIndex + mLastIndex) / 2);
				if (result == 0) {
					// メモリ獲得成功
					Log.d("FileThumbnailLoader", "index=" + index + " loadCache メモリを解放しました");
					save = true;
				} else {
					// メモリなし
					Log.d("FileThumbnailLoader", "index=" + index + " loadCache 空きメモリがありません");
					mOut_of_memory = true;
					save = false;
				}
			}

			if (bm != null && save) {
				result = CallImgLibrary.ThumbnailSave(mID, bm, index);
				if (result != CallImgLibrary.RESULT_OK) {
					// メモリ保持失敗
					Log.d("FileThumbnailLoader", "index=" + index + " loadCache メモリに保持できません");
					mOut_of_memory = true;
					save = false;
				}
			}
		}
		return;
	}

	private void saveCache(Bitmap bm, String pathcode) {
		int result;
		if (bm != null) {
			Log.d("FileThumbnailLoader", "saveBitmap  キャッシュにセーブします pathcode=" + pathcode);
			saveThumbnailCache(pathcode, bm);
			return;
		}
		return;
	}

	private String getFilename(int index) {
		String filename = "";
		boolean ret = false;
		// ファイル情報取得
		if (index >= mFiles.size()) {
			return filename;
		}
		FileData file = mFiles.get(index);
		filename = file.getName();
		return filename;
	}

}

