package src.comitton.filelist;

import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;

public class Bookshelf {
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
	public static SmbFile authSmbFile(String url, String user, String pass) throws MalformedURLException {
		SmbFile sfile;
		if (user != null && user.length() > 0) {
			NtlmPasswordAuthentication npa = new NtlmPasswordAuthentication("", user, pass);
			sfile = new SmbFile(url, npa);
		}
		else {
			sfile = new SmbFile(url);
		}
		return sfile;
	}

	// ユーザ認証付きSambaストリーム
	public static SmbFileInputStream authSmbFileInputStream(String url, String user, String pass) throws MalformedURLException, SmbException, UnknownHostException {
		SmbFileInputStream stream;
		if (user != null && user.length() > 0) {
			SmbFile sfile = authSmbFile(url, user, pass);
			stream = new SmbFileInputStream(sfile);
		}
		else {
			stream = new SmbFileInputStream(url);
		}
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
}
