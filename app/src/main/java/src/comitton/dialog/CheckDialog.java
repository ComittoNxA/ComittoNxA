package src.comitton.dialog;

import java.util.EventListener;

import jp.dip.muracoro.comittona.R;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.graphics.drawable.PaintDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.view.View.OnClickListener;

@SuppressLint("NewApi")
public class CheckDialog extends Dialog implements OnClickListener, OnDismissListener {
	private CheckListener mListener = null;
	private Activity mContext;
	private float mDensity;

	private TextView mTitleText;
	private Button mBtnOk;
	private Button mBtnCancel;
	private ListView mListView;

	private String mTitle;
	private boolean mStates[];
	private String mItems[];

	private ItemArrayAdapter mItemArrayAdapter;

	public CheckDialog(Activity context, String title, boolean states[], String[] items, CheckListener listener) {
		super(context);
		Window dlgWindow = getWindow();

		// タイトルなし
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Activityを暗くしない
		dlgWindow.setFlags(0 , WindowManager.LayoutParams.FLAG_DIM_BEHIND);

		// 背景を透明に
		PaintDrawable paintDrawable = new PaintDrawable(0xC0000000);
		dlgWindow.setBackgroundDrawable(paintDrawable);

		setCanceledOnTouchOutside(true);
		setOnDismissListener(this);

		mContext = context;
		mTitle = title;
		mStates = states;
		mItems = items;
		mListener = listener;
		mDensity = context.getResources().getDisplayMetrics().scaledDensity;
	}

	protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);

		setContentView(R.layout.checkdialog);

		mTitleText = (TextView)this.findViewById(R.id.text_title);
		mBtnOk  = (Button)this.findViewById(R.id.btn_ok);
		mBtnCancel  = (Button)this.findViewById(R.id.btn_cancel);
		mListView = (ListView)this.findViewById(R.id.listview);

		mTitleText.setText(mTitle);
		// リストの設定
		mListView.setScrollingCacheEnabled(false);
		mItemArrayAdapter = new ItemArrayAdapter(mContext, -1, mItems);
		mListView.setAdapter(mItemArrayAdapter);

		// デフォルトはしおりを記録する
		mBtnOk.setOnClickListener(this);
		mBtnCancel.setOnClickListener(this);
	}

	// ダイアログを表示してもIMMERSIVEが解除されない方法
	// http://stackoverflow.com/questions/22794049/how-to-maintain-the-immersive-mode-in-dialogs
	/**
	 * An hack used to show the dialogs in Immersive Mode (that is with the NavBar hidden). To
	 * obtain this, the method makes the dialog not focusable before showing it, change the UI
	 * visibility of the window like the owner activity of the dialog and then (after showing it)
	 * makes the dialog focusable again.
	 */
	@Override
	public void show() {
		// Set the dialog to not focusable.
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
		// 設定をコピー
		copySystemUiVisibility();

		// Show the dialog with NavBar hidden.
		super.show();

		// Set the dialog to focusable again.
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
	}

	/**
	 * Copy the visibility of the Activity that has started the dialog {@link mActivity}. If the
	 * activity is in Immersive mode the dialog will be in Immersive mode too and vice versa.
	 */
	@SuppressLint("NewApi")
	private void copySystemUiVisibility() {
	    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
	        getWindow().getDecorView().setSystemUiVisibility(
	                mContext.getWindow().getDecorView().getSystemUiVisibility());
	    }
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
				case KeyEvent.KEYCODE_MENU:
					dismiss();
					break;
			}
		}
		// 自動生成されたメソッド・スタブ
		return super.dispatchKeyEvent(event);
	}

	public class ItemArrayAdapter extends ArrayAdapter<String>
	{
		private String[] mItems; // ファイル情報リスト

		// コンストラクタ
		public ItemArrayAdapter(Context context, int resId, String[] items)
		{
			super(context, resId, items);
			mItems = items;
		}

		// 一要素のビューの生成
		@Override
		public View getView(int index, View view, ViewGroup parent)
		{
			// レイアウトの生成
			CheckBox checkbox;
			if(view == null) {
				Context context = getContext();
				int marginW = (int)(4 * mDensity);
				int marginH = (int)(0 * mDensity);
				// レイアウト
				LinearLayout layout = new LinearLayout(context);
				layout.setOrientation(LinearLayout.HORIZONTAL);
				layout.setBackgroundColor(0);
				view = layout;

				LinearLayout inLayout = new LinearLayout(context);
				layout.setOrientation(LinearLayout.HORIZONTAL);
				layout.setBackgroundColor(0);

				checkbox = new CheckBox(context);
				checkbox.setId(0);
				checkbox.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
				inLayout.addView(checkbox);
				inLayout.setPadding(marginW, marginH, 0, marginH);
				layout.addView(inLayout);

				checkbox.setOnCheckedChangeListener(new OnCheckedChangeListener(){
					@Override
					public void onCheckedChanged(CompoundButton view, boolean state) {
						Integer index = (Integer)view.getTag();
						if (index != null) {
    						if (0 <= index && index < mStates.length) {
    							mStates[index] = state;
    						}
						}
					}
				});
			}
			else {
				checkbox = (CheckBox)view.findViewById(0);
			}

			// 値の指定
			checkbox.setTag(index);
			checkbox.setChecked(mStates[index]);
			checkbox.setText(mItems[index]);
			return view;
		}
	}

	@Override
	public void onClick(View v) {
		// キャンセルクリック
		if (v.getId() == R.id.btn_ok) {
			mListener.onSelected(mStates);
		}
		dismiss();
	}

	@Override
	public void onDismiss(DialogInterface dialog) {
		mListener.onClose();
	}

	public interface CheckListener extends EventListener {
	    // メニュー選択された
	    public void onSelected(boolean states[]);
	    public void onClose();
	}
}