package src.comitton.dialog;

import java.io.File;
import java.io.FileOutputStream;

import src.comitton.common.DEF;
import src.comitton.common.FileAccess;

import jcifs.smb.SmbFile;
import jcifs.smb.SmbRandomAccessFile;
import jp.dip.muracoro.comittona.R;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadDialog extends Dialog implements Runnable, Handler.Callback, OnClickListener, OnDismissListener {
	public static final int MSG_MESSAGE = 1;
	public static final int MSG_SETMAX = 2;
	public static final int MSG_PROGRESS = 3;
	public static final int MSG_ERRMSG = 4;

	private String mFullPath;
	private String mUser;
	private String mPass;
	private String mItem;
	private String mLocal;
	private Thread mThread;
	private boolean mBreak;
	private Handler mHandler;
	private Context mContext;

	private TextView mMsgText;
	private TextView mProgressText;
	private ProgressBar mProgress;
	private Button mBtnCancel;

	public DownloadDialog(Context context, String uri, String path, String user, String pass, String item, String local) {
		super(context);
		Window dlgWindow = getWindow();

		// 画面をスリープ有効
		dlgWindow.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		// タイトルなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Activityを暗くしない
		dlgWindow.setFlags(0 , WindowManager.LayoutParams.FLAG_DIM_BEHIND);

		// 背景を透明に
		PaintDrawable paintDrawable = new PaintDrawable(0xC0000000);
		dlgWindow.setBackgroundDrawable(paintDrawable);

		setCanceledOnTouchOutside(false);
		setOnDismissListener(this);

		mContext = context.getApplicationContext();

		mFullPath = uri + path;
		mUser = user;
		mPass = pass;
		mItem = item;
		mLocal = local;
		mBreak = false;

		mHandler = new Handler(this);
		return;
	}

	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
		// ディスプレイのインスタンス生成
		Display disp = wm.getDefaultDisplay();
		int cx = disp.getWidth();
		int cy = disp.getHeight();
		int width = Math.min(cx, cy);

		setContentView(R.layout.downloaddialog);

		mMsgText = (TextView)this.findViewById(R.id.text_msg);
		mMsgText.setWidth(width);
		mProgressText = (TextView)this.findViewById(R.id.text_progress);
		mBtnCancel  = (Button)this.findViewById(R.id.btn_cancel);
		mProgress = (ProgressBar)this.findViewById(R.id.progress);
		// 最大値/初期値
		mProgress.setMax(1);
		mProgress.incrementProgressBy(0);

		// キャンセル
		mBtnCancel.setOnClickListener(this);

		mThread = new Thread(this);
		mThread.start();
	}

	public void run() {
		// コピー開始
		try {
			downloadFile("", mItem);
		}
		catch (Exception e) {
			String msg = e.getMessage();
			if (msg == null) {
				msg = "Download Error.";
			}
			sendMessage(MSG_ERRMSG, msg, 0, 0);
		}
		// プログレス終了
		this.dismiss();
	}

	public boolean downloadFile(String path, String item) throws Exception {
		SmbFile file = FileAccess.authSmbFile(mFullPath + path + item, mUser, mPass);
		if (file.isDirectory()) {
			// ローカルにディレクトリ作成
			File lfile = new File(mLocal + path + item);
			lfile.mkdir();

			// 再帰呼び出し
			String nextpath = path + item;
			SmbFile sfile = FileAccess.authSmbFile(mFullPath + nextpath, mUser, mPass);
			SmbFile[] sfiles = null;

			sfiles = sfile.listFiles();
			int filenum = sfiles.length;
			if (sfiles == null || filenum <= 0) {
				// ファイルなし
				return true;
			}
			// ディレクトリ内のファイル
			for (int i = 0; i < filenum; i++) {
				String name = sfiles[i].getName();
				downloadFile(nextpath, name);
				if (mBreak) {
					// 中断
					break;
				}
			}
		}
		else {
			// ファイル拡張子チェック
			if (item.length() <= 4) {
				return false;
			}
			String ext = DEF.getFileExt(item);
			if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") && !ext.equals(".zip") && !ext.equals(".rar") && !ext.equals(".cbz") && !ext.equals(".cbr") && !ext.equals(".pdf") && !ext.equals(".txt") && !ext.equals(".gif") && !ext.equals(".epub") && !ext.equals(".xhtml") && !ext.equals(".html")) {
				// 対象外
				return false;
			}

			/* || ext.equals(".bmp") || ext.equals(".gif") ) {*/
			// ダウンロード実行
			try {
				FileOutputStream localFile = new FileOutputStream(mLocal + path + item + "_dl");
				SmbFile sambaFile = FileAccess.authSmbFile(mFullPath + path + item, mUser, mPass);
				if (!sambaFile.exists()) {
					throw new Exception("File not found.");
				}
				SmbRandomAccessFile sambaFileRnd = new SmbRandomAccessFile(sambaFile, "r");

				// ファイルサイズ取得
				long fileSize = sambaFileRnd.length();
				if ((fileSize & 0xFFFFFFFF00000000L) != 0 && (fileSize & 0x00000000FFFFFFFFL) == 0) {
					fileSize >>= 32;
				}

				// メッセージを設定
				sendMessage(MSG_MESSAGE, path + item, 0, 0);
				sendMessage(MSG_SETMAX, null, 0, (int)fileSize);

				byte buff[] = new byte[1024 * 16];
				int size;
				long total = 0;
				while (true) {
					// 読み込み
					size = sambaFileRnd.read(buff, 0, buff.length);
					if (mBreak) {
						// 中断
						localFile = null;
						File removeFile = new File(mLocal + path + item + "_dl");
						removeFile.delete();
						return false;
					}
					if (size <= 0) {
						break;
					}
					// 書き込み
					localFile.write(buff, 0, size);

					total += size;
					sendMessage(MSG_PROGRESS, null, (int)total, (int)fileSize);
				}
				// クローズ
				localFile.close();
				sambaFileRnd.close();
				localFile = null;
				sambaFile = null;

				// リネーム
				File renameFrom = new File(mLocal + path + item + "_dl");
				File renameTo = null;
				String filename = item.substring(0, item.length() - 4);
				String extname = item.substring(item.length() - 4);

				for (int i = 0; i < 10; i++) {
					if (i == 0) {
						renameTo = new File(mLocal + path + item);
					}
					else {
						renameTo = new File(mLocal + path + filename + "(" + i + ")" + extname);
					}
					if (!renameTo.exists()) {
						break;
					}
				}
				if (!renameFrom.renameTo(renameTo)) {
					// リネーム失敗ならダウンロードしたファイルを削除
					renameFrom.delete();
				}
			}
			catch (Exception e) {
				throw new Exception(e);
			}
		}
		return true;
	}

	private void sendMessage(int msg_id, String obj, int arg1, int arg2) {
		Message message = new Message();
		message.what = msg_id;
		message.arg1 = arg1;
		message.arg2 = arg2;
		message.obj = obj;
		mHandler.sendMessage(message);
	}

	public boolean handleMessage(Message msg) {
		// 受信
		switch (msg.what) {
			case MSG_MESSAGE:
				mMsgText.setText((String) msg.obj);
				return true;
			case MSG_SETMAX:
				mProgress.setMax(msg.arg2);
			case MSG_PROGRESS:
				mProgress.setProgress(msg.arg1);
				mProgressText.setText(msg.arg1 + " / " + msg.arg2);
				return true;
			case MSG_ERRMSG:
				String msgstr = (String)msg.obj;
				Toast.makeText(mContext, msgstr, Toast.LENGTH_LONG).show();
				return true;
		}
		return false;
	}

	@Override
	public void onClick(View v) {
		// キャンセルクリック
		dismiss();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		// 画面をスリープ有効
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		mBreak = true;
	}
}
