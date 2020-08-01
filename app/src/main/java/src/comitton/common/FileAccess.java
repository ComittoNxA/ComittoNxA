package src.comitton.common;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;

import java.lang.SecurityException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import jcifs.CIFSContext;
import jcifs.CIFSException;
import jcifs.Configuration;
import jcifs.SmbResource;
import jcifs.config.PropertyConfiguration;
import jcifs.context.BaseContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jp.dip.muracoro.comittona.FileSelectActivity;
import src.comitton.exception.FileAccessException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;


public class FileAccess {
//	public static final int TYPE_FILE = 0;
//	public static final int TYPE_DIR = 1;

	private static final int COMMAND_RENAMETO = 0;
	private static final int COMMAND_DELETE = 1;

	private static final int KEY_NAME = 0;
	private static final int KEY_IS_DIRECTORY = 1;
	private static final int KEY_LENGTH = 2;
	private static final int KEY_LAST_MODIFIED = 3;

	private static FileSelectActivity mActivity;
	private static Map<String, SmbResource> mResourceMap = new HashMap<String, SmbResource>();

	private static final int REQUEST_CODE = 1;

	// ユーザ認証付きSambaアクセス
	public static SmbFile authSmbFile(String url) throws MalformedURLException {
		String user = null;
		String pass = null;

		// パラメタチェック
		if (url.indexOf("smb://") == 0) {
			int idx = url.indexOf("@");
			if (idx >= 0) {
				String userpass = url.substring(6, idx);
				idx = userpass.indexOf(":");
				if (idx >= 0) {
					user = userpass.substring(0, idx);
					user = URLDecoder.decode(user);
					pass = userpass.substring(idx + 1);
					pass = URLDecoder.decode(pass);
				}
				else {
					user = userpass;
					pass = "";
				}
			}
		}
		return authSmbFile(url, user, pass);
	}

	// ユーザ認証付きSambaアクセス
	private static SmbFile authSmbFile(String url, String user, String pass) throws MalformedURLException {
		SmbFile sfile = null;
		NtlmPasswordAuthenticator smbAuth;
		SmbResource resource = null;
		CIFSContext context = null;
		String domain = "";
		String host = "";
		String share = "";
		String path = "";
		String key = "";
		int idx;

		Properties prop = new Properties();
		prop.setProperty("jcifs.smb.client.minVersion", "SMB202");
		prop.setProperty("jcifs.smb.client.maxVersion", "SMB311");
		prop.put( "jcifs.traceResources", "true" );
		Configuration config = null;

		host = url.substring(6);
		idx = host.indexOf("/");
		if (idx >= 0){
			path = host.substring(idx + 1);
			host = host.substring(0, idx);
		}
		idx = path.indexOf("/", 1);
		if (idx >= 0){
			share = path.substring(0, idx);
			path = path.substring(idx);
		}

		if (user != null && user.length() != 0) {
			idx = user.indexOf(";");
			if (idx >= 0){
				domain = user.substring(0, idx);
				user = user.substring(idx + 1);
			}
		}

		Log.d("FileAccess", "authSmbFile domain=" + domain + ", user=" + user + ", pass=" + pass + ", host=" + host + ", share=" + share + ", path=" + path);

		key = "smb://";
		if (domain.length() != 0) {
			key += domain + ";";
		}
		if (user.length() != 0) {
			key += user;
			if (pass.length() != 0) {
				key += ":" + pass + "@";
			}
		}
		key += host + "/" + share + "/";

		if (mResourceMap.containsKey(key)) {
			resource = mResourceMap.get(key);
		}
		else {

			String encUrl = null;

			if (domain != null && domain.length() != 0) {
				smbAuth = new NtlmPasswordAuthenticator(domain, user, pass);
			} else if (user != null && user.length() != 0) {
				smbAuth = new NtlmPasswordAuthenticator(user, pass);
			} else {
				smbAuth = new NtlmPasswordAuthenticator();
			}

			try {
				config = new PropertyConfiguration(prop);
				BaseContext bc = new BaseContext(config);
				context = bc.withCredentials(smbAuth);
				resource = context.get(key);
				mResourceMap.put(key, resource);
			} catch (CIFSException e) {
				e.printStackTrace();
			}
		}

		try {
			sfile = new SmbFile(resource, path);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		return sfile;
	}

	// ユーザ認証付きSambaストリーム
	public static SmbFileInputStream authSmbFileInputStream(String url, String user, String pass) throws MalformedURLException, SmbException, UnknownHostException {
		SmbFileInputStream stream;
		SmbFile sfile = authSmbFile(url, user, pass);
		stream = new SmbFileInputStream(sfile);
		return stream;
	}

	public static String createUrl(String url, String user, String pass) {
		if (url == null) {
			return "";
		}
		if (url.length() <= 6) {
			return url;
		}
		if (url.substring(0, 6).equals("smb://") == false || user == null || user.length() == 0) {
			return url;
		}
		// サーバ名
		String ret = "smb://" + URLEncoder.encode(user); 
		if (pass != null && pass.length() > 0) {
			ret += ":" + URLEncoder.encode(pass);
		}
		ret += "@" + url.substring(6);
		return ret;
	}

	public static ArrayList<String> getInnerFile(String uri, String path, String user, String pass) {
		boolean isLocal;

		File lfiles[] = null;
		SmbFile sfile = null;
		SmbFile[] sfiles = null;

		if (uri == null || uri.length() == 0) {
			isLocal = true;
		}
		else {
			isLocal = false;
		}

		if (isLocal) {
			// ローカルの場合のファイル一覧取得
			lfiles = new File(path).listFiles();
			if (lfiles == null) {
				return null;
			}
		}
		else {
			// サーバの場合のファイル一覧取得
			try {
				sfile = FileAccess.authSmbFile(uri + path, user, pass);
			} catch (MalformedURLException e) {
				// 
			}
			try {
				sfiles = sfile.listFiles();
			} catch (SmbException e) {
				// 
			}
			if (sfiles == null) {
				return null;
			}
		}

		int length;
		if (isLocal) {
			length = lfiles.length;
		} else {
			length = sfiles.length;
		}

		ArrayList<String> file_list = new ArrayList<String>(length);
		ArrayList<String> dir_list = new ArrayList<String>(length);
		String name;
		boolean flag;
		for (int i = 0; i < length; i++) {
			if (isLocal) {
				name = lfiles[i].getName();
				flag = lfiles[i].isDirectory();
			}
			else {
				name = sfiles[i].getName();
				int len = name.length();
				if (name != null && len >= 1 && name.substring(len - 1).equals("/")) {
					flag = true;
				} else {
					flag = false;
				}
			}

			if (!flag) {
				// 通常のファイル
				String ext = DEF.getExtension(name);
				if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".gif")/* || ext.equals(".bmp")*/
						|| ext.equals(".zip") || ext.equals(".rar") || ext.equals(".cbz") || ext.equals(".cbr") || ext.equals(".pdf") || ext.equals(".epub")) {
					 file_list.add(name);
				}
			}else{
				dir_list.add(name + "/");
			}
		}
		file_list.addAll(dir_list);
		if (file_list.size() > 0) {
				Collections.sort(file_list);
				return file_list;
		}
		return null;
	}
	
	public static boolean renameTo(String uri, String path, String fromfile, String tofile, String user, String pass) throws FileAccessException {
		if (tofile.indexOf('/') > 0) {
			throw new FileAccessException("Invalid file name.");
		}

		if (uri == null || uri.length() == 0) {
			// ローカルの場合のファイル一覧取得
			File orgfile = new File(path + fromfile);
			if (orgfile.exists() == false) {
				// 変更前ファイルが存在しなければエラー
				throw new FileAccessException("File not found.");
			}
			File dstfile = new File(path + tofile);
			if (dstfile.exists() == true) {
				// 変更後ファイルが存在すればエラー
				throw new FileAccessException("File access error.");
			}

			boolean isDirectory = fromfile.endsWith("/");
			DocumentFile documentFile = FileAccess.getDocumentFile(orgfile, isDirectory);
			Log.d("FileAccess", "renameTo documentfile=" + documentFile);

			if (documentFile != null){
				// ファイルをリネームする。
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					try {
						Log.d("FileSelectActivity", "onActivityResult ファイルをリネームします。");

						File dest = new File(tofile);
						documentFile.renameTo(tofile);
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					}
				}
			}

		}
		else {
			// サーバの場合
			SmbFile orgfile;
			try {
				orgfile = FileAccess.authSmbFile(uri + path + fromfile, user, pass);
				if (orgfile.exists() == false) {
					// 変更前ファイルが存在しなければエラー
					throw new FileAccessException("File not found.");
				}
			} catch (MalformedURLException e) {
				throw new FileAccessException(e);
			} catch (SmbException e) {
				throw new FileAccessException(e);
			}

			SmbFile dstfile;
			try {
				dstfile = FileAccess.authSmbFile(uri + path + tofile, user, pass);
				if (dstfile.exists() == true) {
					// 変更後ファイルが存在すればエラー
					throw new FileAccessException("File access error.");
				}
			} catch (MalformedURLException e) {
				throw new FileAccessException(e);
			} catch (SmbException e) {
				throw new FileAccessException(e);
			}

			// ファイル名変更
			try {
				orgfile.renameTo(dstfile);
			} catch (SmbException e) {
				throw new FileAccessException(e);
			}
		}
		return true;
	}

	// ファイル存在チェック
	public static boolean exist(String uri, String path, String file, String user, String pass) throws FileAccessException {
		boolean result;
		if (uri == null || uri.length() == 0) {
			// ローカルの場合
			File orgfile = new File(path + file);
			result = orgfile.exists();
		}
		else {
			// サーバの場合
			SmbFile orgfile;
			try {
				orgfile = FileAccess.authSmbFile(uri + path + file, user, pass);
			} catch (MalformedURLException e) {
				throw new FileAccessException(e);
			}
			try {
				result = orgfile.exists();
			} catch (SmbException e) {
				throw new FileAccessException(e);
			}
		}
		return result;
	}

	// ファイル削除
	public static boolean delete(String uri, String path, String file, String user, String pass) throws FileAccessException {
		boolean result;
		if (uri == null || uri.length() == 0) {
			// ローカルの場合
			File orgfile = new File(path + file);

			boolean isDirectory = file.endsWith("/");
			DocumentFile documentFile = FileAccess.getDocumentFile(orgfile, isDirectory);
			Log.d("FileAccess", "renameTo documentfile=" + documentFile);

			if (documentFile != null){
				// ファイルをリネームする。
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
					try {
						Log.d("FileSelectActivity", "onActivityResult ファイルを削除します。");
						documentFile.delete();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					}
				}
			}

		}
		else {
			// サーバの場合
			SmbFile orgfile;
			try {
				orgfile = FileAccess.authSmbFile(uri + path + file, user, pass);
			} catch (MalformedURLException e) {
				throw new FileAccessException(e);
			}
			try {
				orgfile.delete();
				result = orgfile.exists();
			} catch (SmbException e) {
				throw new FileAccessException(e);
			}
		}
		return false;
	}

	public static void setActivity(FileSelectActivity activity) {
		mActivity = activity;
	}

	// treeUri(=ドライブのルート)のDocumentFileを取得する
	public static DocumentFile getDocumentFile(final File file, final boolean isDirectory) {
		String baseFolder = getExtSdCardFolder(file);
		DocumentFile document = null;

		if (baseFolder == null) {
			return null;
		}

		String relativePath = null;
		try {
			String fullPath = file.getCanonicalPath();
			relativePath = fullPath.substring(baseFolder.length() + 1);
		}
		catch (IOException e) {
			return null;
		}

		Set<String> treeUriSet = null;
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			treeUriSet = sharedPreferences.getStringSet("permit-uriSet", new HashSet<String>());
		}


		Iterator<String> it = treeUriSet.iterator();
		while(it.hasNext()) {
			String treeUriString = it.next();
			Uri treeUri = Uri.parse(treeUriString);

			if (treeUri == null) {
				return null;
			}

			// start with root of SD card and then parse through document tree.
			document = DocumentFile.fromTreeUri(mActivity, treeUri);

			String[] parts = relativePath.split("\\/");
			for (int i = 0; i < parts.length; i++) {
				DocumentFile nextDocument = document.findFile(parts[i]);

				if (nextDocument == null) {
					if ((i < parts.length - 1) || isDirectory) {
						nextDocument = document.createDirectory(parts[i]);
					}
					else {
						nextDocument = document.createFile("image", parts[i]);
					}
				}
				document = nextDocument;
			}
			if (document != null) {
				return document;
			}
		}

		return document;
	}

	public static String getExtSdCardFolder(final File file) {
		String[] extSdPaths = getExtSdCardPaths();
		try {
			for (int i = 0; i < extSdPaths.length; i++) {
				if (file.getCanonicalPath().startsWith(extSdPaths[i])) {
					return extSdPaths[i];
				}
			}
		}
		catch (IOException e) {
			return null;
		}
		return null;
	}

	/**
	 * Get a list of external SD card paths. (KitKat or higher.)
	 *
	 * @return A list of external SD card paths.
	 */
	private static String[] getExtSdCardPaths() {
		List<String> paths = new ArrayList<>();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			for (File file : mActivity.getExternalFilesDirs("external")) {
				if (file != null && !file.equals(mActivity.getExternalFilesDir("external"))) {
					int index = file.getAbsolutePath().lastIndexOf("/Android/data");
					if (index < 0) {
						Log.w("FileAccess", "Unexpected external file dir: " + file.getAbsolutePath());
					}
					else {
						String path = file.getAbsolutePath().substring(0, index);
						try {
							path = new File(path).getCanonicalPath();
						}
						catch (IOException e) {
							// Keep non-canonical path.
						}
						paths.add(path);
					}
				}
			}
		}
		return paths.toArray(new String[paths.size()]);
	}



}
