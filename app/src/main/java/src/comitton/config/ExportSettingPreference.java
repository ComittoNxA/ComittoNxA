package src.comitton.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import src.comitton.common.DEF;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class ExportSettingPreference extends DialogPreference implements OnClickListener {
	private Context mContext;
	private SharedPreferences mSp;

	private EditText mEditView;
	private TextView mMsgView;

	private static final int LAYOUT_PADDING = 10;

	public ExportSettingPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mSp = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	protected View onCreateDialogView() {
		LinearLayout layout = new LinearLayout(mContext);
		layout.setOrientation(LinearLayout.VERTICAL);

		layout.setPadding(LAYOUT_PADDING, LAYOUT_PADDING, LAYOUT_PADDING, LAYOUT_PADDING);
		mMsgView = new TextView(mContext);
		mEditView = new EditText(mContext);
		mEditView.setMaxLines(1);
		mEditView.setHint("Abcde");

		layout.addView(mMsgView, new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
		layout.addView(mEditView, new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));

		String str = (String) getDialogMessage();
		mMsgView.setText(str);

		return layout;
	}

	@Override
	protected void showDialog(Bundle state) {
	    super.showDialog(state);

	    Dialog dialog = getDialog();
	    if (dialog != null) {
	        Button btnOk = (Button)dialog.findViewById(android.R.id.button1);
	        if (btnOk != null) {
	        	btnOk.setOnClickListener(this);
	        }
	    }
	}

	@Override
	public void onClick(View v) {
		// OKボタン処理
		String filepath = getFilePath();
		filepath += mEditView.getText() + DEF.EXTENSION_SETTING;

		FileOutputStream os;
		OutputStreamWriter sw;
		BufferedWriter bw;

		try {
			// ファイルオープン
			os = new FileOutputStream(filepath, false);
			sw = new OutputStreamWriter(os, "UTF-8");
			bw = new BufferedWriter(sw, 8192);
		}
		catch (Exception e) {
			// ファイル作成失敗
			errorToast("ファイル作成エラー", e);
			return;
		}

		try {
			Map<String, ?> keyMap = mSp.getAll();
			if (keyMap != null) {
				String line;
				Set<String> keys = keyMap.keySet();
				// キーをソートする
				keys = new TreeSet<String>(keys);
				for (String key : keys) {
					if (DEF.checkExportKey(key) == false) {
						continue;
					}
					// NxT専用キーチェック
					boolean ex = DEF.checkTonlyExportKey( key );

					Object value = keyMap.get(key);
					String className = value.getClass().getName();
					if (className.equals("java.lang.String")) {
						line = (ex ? "s:" : "S:") + key + "=" + value.toString();
					}
					else if (className.equals("java.lang.Boolean")) {
						line = (ex ? "b:" : "B:") + key + "=" + value.toString();
					}
					else if (className.equals("java.lang.Float")) {
						line = (ex ? "f:" : "F:") + key + "=" + value.toString();
					}
					else if (className.equals("java.lang.Integer")) {
						line = (ex ? "i:" : "I:") + key + "=" + value.toString();
					}
					else if (className.equals("java.lang.Long")) {
						line = (ex ? "l:" : "L:") + key + "=" + value.toString();
					}
					else {
						continue;
					}
					bw.write(line);
					bw.newLine();
				}
			}
		}
		catch (Exception e) {
			errorToast("ファイル書き込みエラー", e);
		}

		try {
			bw.flush();
			bw.close();
		}
		catch (Exception e) {
			// クローズエラー
			errorToast("ファイルクローズエラー", e);
		}

		Toast.makeText(mContext, "Saved to " + filepath, Toast.LENGTH_SHORT).show();
		getDialog().dismiss();
	}

	private String getFilePath() {
		String base = DEF.getBaseDirectory();
		String path = DEF.getConfigDirectory();
		try {
			// ディレクトリがなければ作成
			new File(base).mkdirs();
			new File(path).mkdirs();
		}
		catch (Exception e) {
			Log.e("ExportSettings", e.getMessage());
		}
		return path;
	}

	private void errorToast(String msg, Exception e) {
		// 書き込みエラー
		String errmsg = e.getMessage();
		if (e.getMessage() == null) {
			errmsg = msg;
		}
		else {
			errmsg = msg + "(" + e.getMessage() + ")";
		}
		Toast.makeText(mContext, errmsg, Toast.LENGTH_SHORT).show();
	}
}
