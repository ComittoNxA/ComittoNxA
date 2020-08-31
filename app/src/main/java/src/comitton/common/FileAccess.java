package src.comitton.common;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.FileAttributes;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.msfscc.fileinformation.FileStandardInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.protocol.commons.EnumWithValue;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskEntry;
import com.hierynomus.smbj.share.DiskShare;
import com.rapid7.client.dcerpc.mssrvs.ServerService;
import com.rapid7.client.dcerpc.mssrvs.dto.NetShareInfo0;
import com.rapid7.client.dcerpc.transport.RPCTransport;
import com.rapid7.client.dcerpc.transport.SMBTransportFactories;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import java.lang.SecurityException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jcifs.CIFSContext;
import jcifs.context.SingletonContext;
import jcifs.smb.NtlmPasswordAuthenticator;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbRandomAccessFile;

import jp.dip.muracoro.comittona.FileSelectActivity;

import src.comitton.data.FileData;
import src.comitton.exception.FileAccessException;

public class FileAccess {
//	public static final int TYPE_FILE = 0;
//	public static final int TYPE_DIR = 1;

//	public static final int KEY_NAME = 0;
//	public static final int KEY_IS_DIRECTORY = 1;
//	public static final int KEY_LENGTH = 2;
//	public static final int KEY_LAST_MODIFIED = 3;

//	private static final int REQUEST_CODE = 1;

	public static final int SMBLIB_JCIFS = 0;
	public static final int SMBLIB_SMBJ = 1;

	private static int SMBLIB = SMBLIB_SMBJ;
	private static Session lastSmbjesSsion = null;
	private static DiskShare lastSmbjShare = null;
	private static String lastSessionHost = "";
	private static String lastSessionShare = "";
	private static String lastSessionUser = "";
	private static String lastSessionPass = "";

	private static FileSelectActivity mActivity;


	public static void setActivity(FileSelectActivity activity) {
		mActivity = activity;
	}

	public static int getSmbMode() {
		return SMBLIB;
	}

	public static void setSmbMode(int smblib) {
		SMBLIB = smblib;
	}

	// Url文字列作成
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

//	// ユーザ認証付きSambaアクセス
//	public static SmbFile jcifsFile(String url) throws MalformedURLException {
//		String user = null;
//		String pass = null;
//
//		// パラメタチェック
//		if (url.indexOf("smb://") == 0) {
//			int idx = url.indexOf("@");
//			if (idx >= 0) {
//				String userpass = url.substring(6, idx);
//				idx = userpass.indexOf(":");
//				if (idx >= 0) {
//					user = userpass.substring(0, idx);
//					user = URLDecoder.decode(user);
//					pass = userpass.substring(idx + 1);
//					pass = URLDecoder.decode(pass);
//				}
//				else {
//					user = userpass;
//					pass = "";
//				}
//			}
//		}
//		return jcifsFile(url, user, pass);
//	}

	// ユーザ認証付きSambaアクセス
	public static SmbFile jcifsFile(String url, String user, String pass) throws MalformedURLException {
		SmbFile sfile = null;
		NtlmPasswordAuthenticator smbAuth;
		CIFSContext context = null;
		String domain = "";
		String host = "";
		String share = "";
		String path = "";
		int idx;

////		// SMBの基本設定
////		// SMB3はデバッグビルドでしか動作しないため、SMB2を使用する
//		Properties prop = new Properties();
//		prop.setProperty("jcifs.smb.client.minVersion", "SMB1");
//		prop.setProperty("jcifs.smb.client.maxVersion", "SMB210"); // SMB1, SMB202, SMB210, SMB300, SMB302, SMB311
//		prop.setProperty("jcifs.traceResources", "true");
//		prop.setProperty("jcifs.smb.lmCompatibility", "3");
//		prop.setProperty("jcifs.smb.client.useExtendedSecuruty", "true");
//		prop.setProperty("jcifs.smb.useRawNTLM", "true");
//		prop.setProperty("jcifs.smb.client.signingPreferred", "true");
//		prop.setProperty("jcifs.smb.client.useSMB2Negotiation", "true");
//		prop.setProperty("jcifs.smb.client.ipcSigningEnforced", "true");
//
//		prop.setProperty("jcifs.smb.client.signingEnforced", "true");
//		prop.setProperty("jcifs.smb.client.disableSpnegoIntegrity", "true");
//
//		try {
//			// BaseContextではコネクションが足りなくなるため、SingletonContextを使用する
////			Configuration config = new PropertyConfiguration(prop);
////			context = new BaseContext(config);
//			SingletonContext.init(prop);
//		} catch (CIFSException e) {
//			Log.d("FileAccess", "jcifsFile " + e.getMessage());
//		}


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

		Log.d("FileAccess", "jcifsFile domain=" + domain + ", user=" + user + ", pass=" + pass + ", host=" + host + ", share=" + share + ", path=" + path);

		if (domain != null && domain.length() != 0) {
			smbAuth = new NtlmPasswordAuthenticator(domain, user, pass);
			context = SingletonContext.getInstance().withCredentials(smbAuth);

		} else if (user != null && user.length() != 0) {
			smbAuth = new NtlmPasswordAuthenticator(user, pass);
			context = SingletonContext.getInstance().withCredentials(smbAuth);

		} else {
			// Anonymousは動作しないのでGuestで代替する
//			context = SingletonContext.getInstance().withAnonymousCredentials();
			context = SingletonContext.getInstance().withGuestCrendentials();
		}

		sfile = new SmbFile(url, context);
		return sfile;
	}


	// smbj認証
	public static Session smbjSession(String host, String user, String pass) {
		// 前回と同じならそれを返す
		if (lastSessionHost.equals(host) && lastSessionUser.equals(user) && lastSessionPass.equals(pass)) {
			return lastSmbjesSsion;
		}

		Connection connection = null;
		AuthenticationContext auth = null;
		Session session = null;

		String domain = "";
		int idx;

		if (user != null && user.length() != 0) {
			idx = user.indexOf(";");
			if (idx >= 0){
				domain = user.substring(0, idx);
				user = user.substring(idx + 1);
			}
		}

		Log.d("FileAccess", "smbjSession domain=" + domain + ", user=" + user + ", pass=" + pass + ", host=" + host);

		SMBClient client = new SMBClient();
		try {
			connection = client.connect(host);
		}
		catch (IOException e) {
			e.printStackTrace();
		}

		if (domain != null && domain.length() != 0) {
			auth = new AuthenticationContext(user, pass.toCharArray(), domain);

		} else if (user != null && user.length() != 0) {
			auth = new AuthenticationContext(user, pass.toCharArray(), null);

		} else {
			auth = AuthenticationContext.anonymous();

		}

		session = connection.authenticate(auth);
		lastSmbjesSsion = session;
		lastSessionHost = host;
		lastSessionUser = user;
		lastSessionPass = pass;
		return session;
	}

	// smbj認証
	public static DiskShare smbjShare(String url, String user, String pass) {
		Session session = null;
		DiskShare smbjShare = null;

		String host = "";
		String share = "";
		String path = "";
		int idx;

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

		// 前回と同じならそれを返す
		if (lastSessionHost.equals(host) && lastSessionShare.equals(share) && lastSessionUser.equals(user) && lastSessionPass.equals(pass)) {
			if (lastSmbjShare.isConnected()) {
				return lastSmbjShare;
			}
		}
		Log.d("FileAccess", "smbjShare user=" + user + ", pass=" + pass + ", host=" + host + ", share=" + share + ", path=" + path);

		session = smbjSession(host, user, pass);

		try {
			smbjShare = (DiskShare) session.connectShare(share);
		}
		catch (SMBApiException e) {
			Log.d("FileAccess", "smbjShare " + e.getMessage());
		}
		lastSmbjShare = smbjShare;
		lastSessionHost = host;
		lastSessionShare = share;
		lastSessionUser = user;
		lastSessionPass = pass;
		return smbjShare;
	}

	// ユーザ認証付きSambaストリーム
	public static SmbRandomAccessFile jcifsAccessFile(String url, String user, String pass) throws IOException {
		Log.d("FileAccess", "smbRandomAccessFile url=" + url + ", user=" + user + ", pass=" + pass);
		SmbRandomAccessFile stream;
		try {
			if (!exists(url, user, pass)) {
				throw new IOException("File not found.");
			}
		} catch (FileAccessException | IOException e) {
			throw new IOException("File not found.");
		}
		SmbFile sfile = jcifsFile(url, user, pass);
		stream = new SmbRandomAccessFile(sfile, "r");
		return stream;
	}

	// smbjのInput
	public static com.hierynomus.smbj.share.File smbjAccessFile(String url, String user, String pass) throws IOException {
		Log.d("FileAccess", "smbjAccessFile url=" + url + ", user=" + user + ", pass=" + pass);
		InputStream stream;
		try {
			if (!exists(url, user, pass)) {
				throw new IOException("File not found.");
			}
		} catch (FileAccessException | IOException e) {
			throw new IOException("File not found.");
		}

		// smbjの場合
		String host = "";
		String share = "";
		String path = "";
		int idx = 0;

		host = url.substring(6);
		idx = host.indexOf("/");
		if (idx >= 0){
			path = host.substring(idx + 1);
			host = host.substring(0, idx);
		}
		idx = path.indexOf("/", 1);
		if (idx >= 0){
			share = path.substring(0, idx);
			path = path.substring(idx + 1);
		}

		DiskShare smbjShare;
		smbjShare = FileAccess.smbjShare(url, user, pass);

		com.hierynomus.smbj.share.File smbjFileRead = smbjShare.openFile(path,
				new HashSet<>(Arrays.asList(AccessMask.GENERIC_READ)),
				new HashSet<>(Arrays.asList(FileAttributes.FILE_ATTRIBUTE_NORMAL)),
				new HashSet<>(Arrays.asList(SMB2ShareAccess.FILE_SHARE_READ)),
				SMB2CreateDisposition.FILE_OPEN,
				null);

		Log.d("FileAccess", "smbjAccessFile remoteSmbjFile=" + smbjFileRead.toString() + ", size=" + smbjFileRead.getFileInformation(FileStandardInformation.class).getEndOfFile());

//		stream = smbjFileRead.getInputStream();
		return smbjFileRead;
	}

	// ローカルファイルのOutputStream
	public static OutputStream localOutputStream(String url) throws FileAccessException {
		Log.d("FileAccess", "localOutputStream url=" + url);
		boolean result;
		if (url.startsWith("/")) {
			// ローカルの場合
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				File orgfile = new File(url);
				DocumentFile documentFile = FileAccess.getDocumentFile(orgfile, false);
				Log.d("FileAccess", "localOutputStream documentfile=" + documentFile);

				if (documentFile == null || !documentFile.exists()) {
					// ファイルがなければ作成する
					int idx = url.lastIndexOf("/");
					if (idx > 0) {
						String path = url.substring(0, idx);
						String item = url.substring(idx + 1);
						try {
							documentFile = FileAccess.getDocumentFile(new File(path), true);
							Log.d("FileAccess", "localOutputStream Parent=" + documentFile);
							documentFile.createFile("*/*", item);
							documentFile = FileAccess.getDocumentFile(orgfile, false);
						Log.d("FileAccess", "localOutputStream createfile=" + documentFile);
						} catch (Exception e) {
							Log.e("FileAccess", "localOutputStream " + e.getMessage());
							e.printStackTrace();
						}
					}
				}
				try {
					return mActivity.getContentResolver().openOutputStream(documentFile.getUri(), "w");
				} catch (IllegalStateException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			else {
				try {
					File orgfile = new File(url);
					if (!orgfile.exists()) {
						// ファイルがなければ作成する
						orgfile.createNewFile();
					}
					return new FileOutputStream(orgfile);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
		return null;
	}

	// ファイル存在チェック
	public static boolean exists(String url) throws FileAccessException {
		String user = null;
		String pass = null;

		// パラメタチェック
		if (url.startsWith("/")) {
			return exists(url, "", "");
		}
		else if (url.indexOf("smb://") == 0) {
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
		return exists(url, user, pass);
	}

	// ファイル存在チェック
	public static boolean exists(String url, String user, String pass) throws FileAccessException {
		Log.d("FileAccess", "exists url=" + url + ", user=" + user + ", pass=" + pass);
		boolean result = false;
		if (url.startsWith("/")) {
			// ローカルの場合/
			File orgfile = new File(url);
			result = orgfile.exists();
		}
		else if (SMBLIB == SMBLIB_JCIFS) {
			// jcifsの場合
			SmbFile orgfile;
			try {
				orgfile = FileAccess.jcifsFile(url, user, pass);
			} catch (MalformedURLException e) {
				throw new FileAccessException(e);
			}
			try {
				result = orgfile.exists();
			} catch (SmbException e) {
				throw new FileAccessException(e);
			}
		}
		else if (SMBLIB == SMBLIB_SMBJ) {
			// smbjの場合
			String host = "";
			String share = "";
			String path = "";
			int idx = 0;

			host = url.substring(6);
			idx = host.indexOf("/");
			if (idx >= 0){
				path = host.substring(idx + 1);
				host = host.substring(0, idx);
			}
			idx = path.indexOf("/", 1);
			if (idx >= 0){
				share = path.substring(0, idx);
				path = path.substring(idx + 1);
			}

			DiskShare smbjShare;
			smbjShare = FileAccess.smbjShare(url, user, pass);

			if (smbjShare.fileExists(path) || smbjShare.folderExists(path)) {
				result = true;
			}
		}
		return result;
	}

	public static boolean isDirectory(String url, String user, String pass) throws MalformedURLException, SmbException {
		Log.d("FileAccess", "isDirectory url=" + url + ", user=" + user + ", pass=" + pass);
		boolean result = false;
		if (url.startsWith("/")) {
			// ローカルの場合/
			File orgfile = new File(url);
			result = orgfile.isDirectory();
		}
		else if (SMBLIB == SMBLIB_JCIFS) {
			// jcifsの場合
			SmbFile orgfile;
			orgfile = FileAccess.jcifsFile(url, user, pass);
			try {
				result = orgfile.isDirectory();
			} catch (SmbException e) {
				result = false;
			}
		}
		else if (SMBLIB == SMBLIB_SMBJ) {
			// smbjの場合
			String host = "";
			String share = "";
			String path = "";
			int idx = 0;

			host = url.substring(6);
			idx = host.indexOf("/");
			if (idx >= 0){
				path = host.substring(idx + 1);
				host = host.substring(0, idx);
			}
			idx = path.indexOf("/", 1);
			if (idx >= 0){
				share = path.substring(0, idx);
				path = path.substring(idx + 1);
			}

			DiskShare smbjShare;
			smbjShare = FileAccess.smbjShare(url, user, pass);

			if (smbjShare.folderExists(path)) {
				result = true;
			}
		}
		return result;
	}

	public static ArrayList<FileData> listFiles(String url, String user, String pass) throws SmbException {
		Log.d("FileAccess", "listFiles url=" + url + ", user=" + user + ", pass=" + pass);
		boolean isLocal;

		String host = "";
		String share = "";
		String path = "";
		int idx = 0;

		if (url.startsWith("/")) {
			isLocal = true;
		}
		else {
			isLocal = false;

			// URLをホスト、共有フォルダ、パスに分解する
			host = url.substring(6);
			idx = host.indexOf("/");
			if (idx >= 0){
				path = host.substring(idx + 1);
				host = host.substring(0, idx);
			}
			idx = path.indexOf("/", 1);
			if (idx >= 0){
				share = path.substring(0, idx);
				path = path.substring(idx + 1);
			}
		}

		Log.d("FileAccess", "listFiles isLocal=" + isLocal);

		// ファイルリストを取得
		File lfiles[] = null;
		SmbFile jcifsFile = null;
		SmbFile[] jcifsFiles = null;
		Session smbjSession = null;
		DiskShare smbjShare = null;
		ArrayList<FileIdBothDirectoryInformation> smbjFiles = null;
		String[] fnames = null;
		ArrayList<FileData> fileList = new ArrayList<FileData>();
		int length = 0;

		if (isLocal) {
			// ローカルの場合のファイル一覧取得
			lfiles = new File(url).listFiles();
			if (lfiles == null || lfiles.length == 0) {
				return fileList;
			}
			length = lfiles.length;
		}

		else if (SMBLIB == SMBLIB_JCIFS) {
			// jcifsの場合のファイル一覧取得
			try {
				jcifsFile = FileAccess.jcifsFile(url, user, pass);
			} catch (MalformedURLException e) {
				return fileList;
			}
			try {
				if ((url).indexOf("/", 6) == (url).length() - 1) {
					// ホスト名までしか指定されていない場合
					fnames = jcifsFile.list();
					if (fnames == null || fnames.length == 0) {
						return fileList;
					}
					length = fnames.length;
				}
				else {
					// 共有ポイントまで指定済みの場合
					jcifsFiles = jcifsFile.listFiles();
					if (jcifsFiles == null || jcifsFiles.length == 0) {
						return fileList;
					}
					length = jcifsFiles.length;
				}
			} catch (SmbException e) {
				return fileList;
			}
		}

		else if (SMBLIB == SMBLIB_SMBJ) {
			// smbjの場合のファイル一覧取得
			try {
				if ((url).indexOf("/", 6) == (url).length() - 1) {
					// ホスト名までしか指定されていない場合
					// smbj-rpcで共有名のリストを取得する
					smbjSession = FileAccess.smbjSession(host, user, pass);
					final RPCTransport transport = SMBTransportFactories.SRVSVC.getTransport(smbjSession);
					final ServerService serverService = new ServerService(transport);
					final List<NetShareInfo0> shares = serverService.getShares0();
					fnames = new String[shares.size()];
					for (int i = 0; i < shares.size(); i++){
						fnames[i] = shares.get(i).getNetName();
					}
					
					if (fnames == null || fnames.length == 0) {
						return fileList;
					}
					length = fnames.length;
				}
				else {
					// 共有ポイントまで指定済みの場合
					smbjShare = FileAccess.smbjShare(url, user, pass);
					smbjFiles = (ArrayList<FileIdBothDirectoryInformation>)smbjShare.list(path);
					if (smbjFiles == null || smbjFiles.size() == 0) {
						return fileList;
					}
					length = smbjFiles.size();
				}
			} catch (Exception e) {
				return fileList;
			}
		}
		
		Log.d("FileAccess", "listFiles length=" + length);

		// FileData型のリストを作成
		boolean flag = false;
		String name = "";
		long size = 0;
		long date = 0;
		short type = 0;
		short exttype = 0;

		for (int i = 0; i < length; i++) {
			if (isLocal) {
				name = lfiles[i].getName();
				flag = lfiles[i].isDirectory();
				size = lfiles[i].length();
				date = lfiles[i].lastModified();
			}
			else if (SMBLIB == SMBLIB_JCIFS) {
				// jcifsの場合
				if ((url).indexOf("/", 6) == (url).length() - 1) {
					// ホスト名までしか指定されていない場合
					name = fnames[i];
					// 全部フォルダ扱い
					flag = true;
				}
				else {
					// 共有ポイントまで指定済みの場合
					name = jcifsFiles[i].getName();
					int len = name.length();
					if (name != null && len >= 1 && name.substring(len - 1).equals("/")) {
						flag = true;
					} else {
						flag = false;
					}
					size = jcifsFiles[i].length();
					date = jcifsFiles[i].lastModified();
				}
			}
			else if (SMBLIB == SMBLIB_SMBJ) {
				// smbjの場合
				if ((url).indexOf("/", 6) == (url).length() - 1) {
					// ホスト名までしか指定されていない場合
					name = fnames[i];
					// 全部フォルダ扱い
					flag = true;
				}
				else {
					// 共有ポイントまで指定済みの場合
					name = smbjFiles.get(i).getFileName();
					int len = name.length();
					if (name.equals(".") || name.equals("..")) {
						continue;
					}
					else if ((smbjFiles.get(i).getFileAttributes() & FileAttributes.FILE_ATTRIBUTE_DIRECTORY.getValue()) != 0) {
						flag = true;
					} else {
						flag = false;
					}
					size = smbjFiles.get(i).getEndOfFile();
					date = smbjFiles.get(i).getChangeTime().toEpochMillis();
				}
			}
			
			if (flag) {
				// ディレクトリの場合
				int len = name.length();
				if (len >= 1 && !name.substring(len - 1).equals("/")) {
					name += "/";
				}
				type = FileData.FILETYPE_DIR;
				exttype = FileData.EXTTYPE_NONE;
			} else {
				// 通常のファイル
				int len = name.length();
				if (len < 5) {
					continue;
				}
				String ext = DEF.getFileExt(name);
				if (ext.equals(".jpg") || ext.equals(".jpeg") || ext.equals(".png") || ext.equals(".gif")/* || ext.equals(".bmp")*/) {
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
				}
				else if (ext.equals(".txt") || ext.equals(".xhtml") || ext.equals(".html")) {
					type = FileData.FILETYPE_TXT;
					exttype = FileData.EXTTYPE_TXT;
				}
				else {
				type = FileData.FILETYPE_NONE;
				exttype = FileData.EXTTYPE_NONE;
				}
			}

			FileData fileData = new FileData();
			fileData.setType(type);
			fileData.setExtType(exttype);
			fileData.setName(name);
			fileData.setSize(size);
			fileData.setDate(date);

			fileList.add(fileData);
		}

		if (fileList.size() > 0) {
			Collections.sort(fileList, new FileDataComparator());
		}
		return fileList;
	}

	public static class FileDataComparator implements Comparator<FileData> {

		@Override
		public int compare(FileData f1, FileData f2) {
			if (f1.getType() != FileData.FILETYPE_DIR && f2.getType() == FileData.FILETYPE_DIR) {
				return -1;
			}
			else if (f1.getType() == FileData.FILETYPE_DIR && f2.getType() != FileData.FILETYPE_DIR) {
				return 1;
			}
			else {
				return f1.getName().compareTo(f2.getName());
			}
		}
	}
	
	public static boolean renameTo(String uri, String path, String fromfile, String tofile, String user, String pass) throws FileAccessException {
		Log.d("FileAccess", "renameTo url=" + uri + ", path=" + path + ", fromfile=" + fromfile + ", tofile=" + tofile + ", user=" + user + ", pass=" + pass);
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

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				boolean isDirectory = fromfile.endsWith("/");
				DocumentFile documentFile = FileAccess.getDocumentFile(orgfile, isDirectory);
				Log.d("FileAccess", "renameTo documentfile=" + documentFile);

				if (documentFile != null) {
					// ファイルをリネームする。
					try {
						Log.d("FileAccess", "renameTo ファイルをリネームします。");

						File dest = new File(tofile);
						documentFile.renameTo(tofile);
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					}
				}
			}
			else {
				orgfile.renameTo(dstfile);
				return dstfile.exists();
			}
		}
		else if (SMBLIB == SMBLIB_JCIFS) {
			// jcifsの場合
			SmbFile orgfile;
			try {
				orgfile = FileAccess.jcifsFile(uri + path + fromfile, user, pass);
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
				dstfile = FileAccess.jcifsFile(uri + path + tofile, user, pass);
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
				return dstfile.exists();
			} catch (SmbException e) {
				throw new FileAccessException(e);
			}
		}
		else if (SMBLIB == SMBLIB_SMBJ) {
			// smbjの場合

			if (exists(uri + path + fromfile, user, pass) == false) {
				// 変更前ファイルが存在しなければエラー
				throw new FileAccessException("File not found.");
			}

			if (exists(uri + path + tofile, user, pass) == true) {
				// 変更後ファイルが存在すればエラー
				throw new FileAccessException("File access error.");
			}

			String host = "";
			String share = "";
			String entryPath = "";
			int idx = 0;

			host = (uri + path).substring(6);
			idx = host.indexOf("/");
			if (idx >= 0){
				entryPath = host.substring(idx + 1);
				host = host.substring(0, idx);
			}
			idx = entryPath.indexOf("/", 1);
			if (idx >= 0){
				share = entryPath.substring(0, idx);
				entryPath = entryPath.substring(idx + 1);
			}

			try {
				// ファイル名変更
				DiskShare smbjShare;
				smbjShare = FileAccess.smbjShare(uri + path, user, pass);

				Log.d("FileAccess", "renameTo FromFile=" + entryPath + fromfile + ", ToFile=" + entryPath + tofile);
				DiskEntry orgfile = smbjShare.open(entryPath + fromfile,
						EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_WRITE),
						null,
						SMB2ShareAccess.ALL,
						SMB2CreateDisposition.FILE_OPEN,
						null);

				orgfile.rename((entryPath + tofile).replaceAll("/", "\\\\"));
				orgfile.closeNoWait();
				return exists(uri + path + tofile, user, pass);
			}
			catch (Exception e) {
				Log.d("FileAccess", "renameTo " + e.getMessage());
				throw new FileAccessException(e.getMessage());
			}

		}
		return false;
	}

	// ファイル削除
	public static boolean delete(String url, String user, String pass) throws FileAccessException {
		Log.d("FileAccess", "delete url=" + url + ", user=" + user + ", pass=" + pass );
		boolean result;
		if (url.startsWith("/")) {
			// ローカルの場合
			File orgfile = new File(url);

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				boolean isDirectory = url.endsWith("/");
				DocumentFile documentFile = FileAccess.getDocumentFile(orgfile, isDirectory);
				Log.d("FileAccess", "delete documentfile=" + documentFile);

				if (documentFile != null) {
					// ファイルを削除する。
					try {
						Log.d("FileAccess", "delete ファイルを削除します。");
						documentFile.delete();
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					}
				}
			}
			else {
				orgfile.delete();
			}
			return orgfile.exists();
		}
		else if (SMBLIB == SMBLIB_JCIFS) {
			// jcifsの場合
			SmbFile orgfile;
			try {
				orgfile = FileAccess.jcifsFile(url, user, pass);
			} catch (MalformedURLException e) {
				throw new FileAccessException(e);
			}
			try {
				orgfile.delete();
				return orgfile.exists();
			} catch (SmbException e) {
				throw new FileAccessException(e);
			}
		}
		else if (SMBLIB == SMBLIB_SMBJ) {
			// smbjの場合
			String host = "";
			String share = "";
			String path = "";
			int idx = 0;

			host = url.substring(6);
			idx = host.indexOf("/");
			if (idx >= 0){
				path = host.substring(idx + 1);
				host = host.substring(0, idx);
			}
			idx = path.indexOf("/", 1);
			if (idx >= 0){
				share = path.substring(0, idx);
				path = path.substring(idx + 1);
			}

			DiskShare smbjShare;
			smbjShare = FileAccess.smbjShare(url, user, pass);
			if (smbjShare.fileExists(path)) {
				smbjShare.rm(path);
			}
			if (smbjShare.folderExists(path)) {
				smbjShare.rmdir(path, true);
			}
			return exists(url, user, pass);
		}
		return false;
	}

	// ディレクトリ作成
	public static boolean mkdir(String url, String item, String user, String pass) throws FileAccessException {
		Log.d("FileAccess", "mkdir url=" + url + ", item=" + item + ", user=" + user + ", pass=" + pass );
		boolean result;
		if (url.startsWith("/")) {
			// ローカルの場合
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				boolean isDirectory = url.endsWith("/");
				File orgfile = new File(url);
				DocumentFile documentFile = FileAccess.getDocumentFile(orgfile, isDirectory);
				Log.d("FileAccess", "mkdir documentfile=" + documentFile);
				
				if (documentFile != null) {
					// ファイルを削除する。
					try {
						Log.d("FileAccess", "mkdir ディレクトリを削除します。 item=" + item);
						DocumentFile ret = documentFile.createDirectory(item);
						if (ret != null) {
							return true;
						}
						else {
							return false;
						}
					} catch (IllegalStateException e) {
						e.printStackTrace();
					} catch (SecurityException e) {
						e.printStackTrace();
					}
				}
			}
			else {
				File orgfile = new File(url + item);
				return orgfile.mkdir();
			}

		}
//		else {
//			// サーバの場合
//			SmbFile orgfile;
//			try {
//				orgfile = FileAccess.jcifsFile(url + item, user, pass);
//			} catch (MalformedURLException e) {
//				throw new FileAccessException(e);
//			}
//			try {
//				orgfile.mkdir();
//				result = orgfile.exists();
//			} catch (SmbException e) {
//				throw new FileAccessException(e);
//			}
//		}
		return false;
	}

	public static boolean isPermit(final File file) {
		String treeUriString = "";
		Log.d("FileAccess", "isPermit START");
		String baseFolder = getExtSdCardFolder(file);
		Log.d("FileAccess", "isPermit baseFolder=" + baseFolder);

		if (baseFolder == null) {
			return true;
		}

		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);

		treeUriString = sharedPreferences.getString("permit-uri:" + baseFolder, "");
		if (treeUriString != null && treeUriString.length() != 0) {
			return true;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// Android ７以上なら
			String[] parts = file.toString().substring(baseFolder.length() + 1).split("\\/");
			for (int i = 0; i < parts.length; i++) {
				baseFolder = baseFolder + "/" + parts[i];
				Log.d("FileAccess", "isPermit baseFolder=" + baseFolder);
				treeUriString = sharedPreferences.getString("permit-uri:" + baseFolder, "");
				if (treeUriString != null && treeUriString.length() != 0) {
					return true;
				}
			}

		}
		return false;
	}


	// treeUri(=ドライブのルート)のDocumentFileを取得する
	// 書き込み許可が取得済みであれば、storage access frameworkのDocumentFileを返す
	// 書き込み許可がまだであれば、nullを返す
	public static DocumentFile getDocumentFile(final File file, final boolean isDirectory) {
		Log.d("FileAccess", "getDocumentFile START");
		DocumentFile document = null;

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			// Android 10.0 なら
			String baseFolder = getExtSdCardFolder(file);
			Log.d("FileAccess", "getDocumentFile baseFolder=" + baseFolder);

			if (baseFolder == null) {
				return null;
			}

			String relativePath = null;
			try {
				String fullPath = file.getCanonicalPath();
				relativePath = fullPath.substring(baseFolder.length() + 1);
			} catch (IOException e) {
				return null;
			}

			String treeUriString = "";
			SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mActivity);
			treeUriString = sharedPreferences.getString("permit-uri:" + baseFolder, "");

			if (treeUriString != null && treeUriString.length() != 0) {
				Uri treeUri = Uri.parse(treeUriString);
				if (treeUri == null) {
					return null;
				}
				// start with root of SD card and then parse through document tree.
				document = DocumentFile.fromTreeUri(mActivity, treeUri);
			}

			String[] parts = file.toString().substring(baseFolder.length() + 1).split("\\/");
			for (int i = 0; i < parts.length; i++) {
				if (treeUriString == null || treeUriString.length() == 0) {
					baseFolder = baseFolder + "/" + parts[i];
					Log.d("FileAccess", "getDocumentFile baseFolder=" + baseFolder);
					treeUriString = sharedPreferences.getString("permit-uri:" + baseFolder, "");
					if (treeUriString != null && treeUriString.length() != 0) {
						Uri treeUri = Uri.parse(treeUriString);
						if (treeUri == null) {
							return null;
						}
						// start with root of SD card and then parse through document tree.
						document = DocumentFile.fromTreeUri(mActivity, treeUri);
					}
				}
				else {
					Log.d("FileAccess", "getDocumentFile path=" + parts[i]);
					DocumentFile nextDocument = document.findFile(parts[i]);

					if (nextDocument == null) {
						if ((i < parts.length - 1) || isDirectory) {
							nextDocument = document.createDirectory(parts[i]);
						} else {
							nextDocument = document.createFile("*/*", parts[i]);
						}
					}
					document = nextDocument;
				}
			}

			if (document != null) {
				return document;
			}
		}
//		else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//			// Android 7 以上なら
//			document = DocumentFile.fromFile(file);
//		}
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
//				if (file != null && !file.equals(mActivity.getExternalFilesDir("external"))) {
				if (file != null) {
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
