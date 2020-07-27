package src.comitton.filelist;

import java.net.URLEncoder;

import src.comitton.data.ServerData;

import jp.dip.muracoro.comittona.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;

public class ServerSelect {
	public static final int MAX_SERVER = 10;
	public static final int INDEX_LOCAL = -1;
	public static final String SERVER_NAME_UNDEFINE = "Undefined";

	private int mSelect = INDEX_LOCAL;
	private SharedPreferences mSharedPrefer = null;

	private ServerData mServer[];
	private String mLocalPath;
	private String mLocalName;

	public ServerSelect(SharedPreferences sharedPreferences, Context context) {
		mSharedPrefer = sharedPreferences;
		mServer = new ServerData[MAX_SERVER];
		mSelect = INDEX_LOCAL;
		Resources res = context.getResources();
		mLocalName = res.getString(R.string.localStrage);

		// パス設定
		mLocalPath = sharedPreferences.getString("path", "/");
		if (mLocalPath == null || mLocalPath.length() < 1 || !mLocalPath.substring(0, 1).equals("/")) {
			mLocalPath = "/";
		}

		for (int i = 0 ; i < MAX_SERVER ; i ++) {
			mServer[i] = new ServerData();
			String name = sharedPreferences.getString("smb-name" + i, "");
			String host = sharedPreferences.getString("smb-host" + i, "");
			String path = sharedPreferences.getString("smb-path" + i, "");
			String user = sharedPreferences.getString("smb-user" + i, "");
			String pass = sharedPreferences.getString("smb-pass" + i, "");
			if (host != null && !host.equals("")) {
				mServer[i].setName(name);
				mServer[i].setHost(host);
				mServer[i].setPath(path);
				mServer[i].setUser(user);
				mServer[i].setPass(pass);
			}
		}
		return;
	}

	// サーバ選択
	public boolean select(int index) {
		if (index == INDEX_LOCAL || 0 <= index && index < MAX_SERVER) {
			mSelect = index;
			return true;
		}
		return false;
	}

	// サーバ選択
	public boolean select(String code) {
		if (code == null || code.equals("")) {
			mSelect = INDEX_LOCAL;
			return true;
		}
		else {
			for (int i = 0 ; i < MAX_SERVER ; i ++) {
				String str = getCode(i);
				if (str.equals(code)) {
					mSelect = i;
					return true;
				}
			}
		}
		return false;
	}

	// サーバ選択
//	public boolean selectFromName(String name) {
//		for (int i = 0 ; i < MAX_SERVER ; i ++) {
//			String str = getName(i);
//			if (str.equals(name)) {
//				mSelect = i;
//				return true;
//			}
//		}
//		return false;
//	}

	// 選択中のサーバ
	public int getSelect() {
		return mSelect;
	}

	public String getCode(){
		return getCode(mSelect);
	}

	public String getCode(int index){
		String uri = getUserPassURI(index);

		long val = 0;
		byte buf[] = uri.getBytes();

		if (uri.equals("")) {
			return "";
		}

		for (int i = 0 ; i < buf.length ; i ++) {
			val ^= ((long)buf[i] & 0xFF) << (i % 56);
		}
		return new Long(val).toString();
	}

	// サーバ名前取得
	public String getName() {
		return getName(mSelect);
	}

	// サーバ名前取得
	public String getName(int index) {
		if (index == INDEX_LOCAL) {
			// ローカル名
			return mLocalName;
		}
		else{
			// サーバ名
			return mServer[index].getName();
		}
	}

	// ホスト名取得
	public String getHost() {
		return getHost(mSelect);
	}

	// ホスト名取得
	public String getHost(int index) {
		if (index == INDEX_LOCAL) {
			return "";
		}
		else{
			return mServer[index].getHost();
		}
	}

	// ユーザ名取得
	public String getUser() {
		return(getUser(mSelect));
	}

	public String getUser(int index) {
		if (index == INDEX_LOCAL) {
			// ユーザ
			return "";
		}
		else{
			// ユーザ
			return mServer[index].getUser();
		}
	}

	// パスワード取得
	public String getPass() {
		return(getPass(mSelect));
	}

	public String getPass(int index) {
		if (index == INDEX_LOCAL) {
			// パスワード
			return "";
		}
		else{
			// パスワード
			return mServer[index].getPass();
		}
	}

	// サーバパスの取得
	public String getPath() {
		return getPath(mSelect);
	}

	// サーバパスの取得
	public String getPath(int index) {
		if (index == INDEX_LOCAL) {
			// ローカル名
			return mLocalPath;
		}
		else{
			// サーバ名
			return mServer[index].getPath();
		}
	}

	// サーバパスの取得
	public void setPath(String path) {
		if (mSelect == INDEX_LOCAL) {
			// ローカル名
			mLocalPath = path;
		}
		else{
			// サーバ名
			mServer[mSelect].setPath(path);
		}
		savePath();
	}

	// サーバパスの取得
	public String getURI() {
		return(getURI(mSelect));
	}

	// サーバパスの取得
	public String getURI(int index) {
//		return getUserPassURI(index);
		if (index == INDEX_LOCAL) {
			// ローカル名
			return "";
		}
		else{
			// TODO テストサーバ名
			return "smb://" + mServer[index].getHost();
//			return "http://" + mServer[index].getHost();
		}
	}

	// サーバパスの取得
	public String getUserPassURI(int index) {
		if (index == INDEX_LOCAL) {
			// ローカル名
			return "";
		}
		else{
			// サーバ名
			String ret = "smb://";
			String user = URLEncoder.encode(mServer[index].getUser());
			String pass = URLEncoder.encode(mServer[index].getPass());
			if (!user.equals("")) {
				ret += user;
				if (!pass.equals("")) {
					ret += ":" + pass;
				}
		 		ret += "@";
			}
	 		ret += mServer[index].getHost();
			return ret;
		}
	}

	public void setData(int index, ServerData data) {
		if (0 <= index && index < MAX_SERVER) {
			mServer[index].setName(data.getName());
			mServer[index].setHost(data.getHost());
			mServer[index].setUser(data.getUser());
			mServer[index].setPass(data.getPass());
			mServer[index].setPath(data.getPath());
			save(index);
		}
	}

	public void save(int index) {
		Editor ed = mSharedPrefer.edit();
		// サーバ情報
		ed.putString("smb-name" + index, mServer[index].getName());
		ed.putString("smb-host" + index, mServer[index].getHost());
		ed.putString("smb-user" + index, mServer[index].getUser());
		ed.putString("smb-pass" + index, mServer[index].getPass());
		ed.putString("smb-path" + index, mServer[index].getPath());
		ed.commit();
	}

	// パス情報の保存
	public void savePath() {
		Editor ed = mSharedPrefer.edit();
		if (mSelect == INDEX_LOCAL) {
			// ローカル情報
			if (mLocalPath != null) {
				ed.putString("path", mLocalPath);
			}
		}
		else{
			String path = getPath();
			// サーバ情報
			if (path != null) {
				ed.putString("smb-path" + mSelect, path);
			}
		}
		ed.commit();
	}
}
