package src.comitton.pdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import src.comitton.pdf.data.ImageFlate;
import src.comitton.pdf.data.ImageJpeg;
import src.comitton.pdf.data.LexData;
import src.comitton.pdf.data.PdfCrypt;
import src.comitton.pdf.data.PdfImageParams;
import src.comitton.pdf.data.PdfImageRef;
import src.comitton.pdf.data.PdfInfo;
import src.comitton.pdf.data.PdfObject;
import src.comitton.pdf.data.PdfPage;
import src.comitton.pdf.data.XrefData;
import src.comitton.pdf.data.PdfCrypt.PdfCryptFilter;
import src.comitton.stream.FileListItem;
import src.comitton.stream.ImageManager;


import android.util.Log;

public class PDFManager extends PDFLib {
	public static final int OFFSET_PDF_SIGNA_LEN = 0;

//	private final int PDF_TOK_ERROR       = 0;
	private final int PDF_TOK_EOF         = 1;
	private final int PDF_TOK_OPEN_ARRAY  = 2;
	private final int PDF_TOK_CLOSE_ARRAY = 3;
	private final int PDF_TOK_OPEN_DICT   = 4;
	private final int PDF_TOK_CLOSE_DICT  = 5;
	private final int PDF_TOK_OPEN_BRACE  = 6;
	private final int PDF_TOK_CLOSE_BRACE = 7;
	private final int PDF_TOK_NAME        = 8;
	private final int PDF_TOK_INT         = 9;
	private final int PDF_TOK_REAL        = 10;
	private final int PDF_TOK_STRING      = 11;
	private final int PDF_TOK_KEYWORD     = 12;
	private final int PDF_TOK_R           = 13;
	private final int PDF_TOK_TRUE        = 14;
	private final int PDF_TOK_FALSE       = 15;
	private final int PDF_TOK_NULL        = 16;
	private final int PDF_TOK_OBJ         = 17;
	private final int PDF_TOK_ENDOBJ      = 18;
	private final int PDF_TOK_STREAM      = 19;
	private final int PDF_TOK_ENDSTREAM   = 20;
	private final int PDF_TOK_XREF        = 21;
	private final int PDF_TOK_TRAILER     = 22;
	private final int PDF_TOK_STARTXREF   = 23;
//	private final int PDF_NUM_TOKENS      = 24;

	private final int INT_MAX = 0x7FFFFFFF;
	private final int MAX_OBJECT_NUMBER = (10 << 20);

	private final int FAXPARAM_WIDTH  = 0;
	private final int FAXPARAM_HEIGHT = 1;
	private final int FAXPARAM_COLORN = 2;
	private final int FAXPARAM_BPC    = 3;
	private final int FAXPARAM_K      = 4;
	private final int FAXPARAM_EOL    = 5;
	private final int FAXPARAM_EBA    = 6;
	private final int FAXPARAM_CLMS   = 7;
	private final int FAXPARAM_ROWS   = 8;
	private final int FAXPARAM_EOB    = 9;
	private final int FAXPARAM_BI1    = 10;
	private final int FAXPARAM_MAX    = FAXPARAM_BI1 + 1;

	private final int FLATEPARAM_WIDTH  = 0;
	private final int FLATEPARAM_HEIGHT = 1;
	private final int FLATEPARAM_COLORS = 2;
	private final int FLATEPARAM_BPC    = 3;
//	private final int FLATEPARAM_PREDICTOR = 0;
//	private final int FLATEPARAM_COLUMNS   = 1;
	private final int FLATEPARAM_MAX    = FLATEPARAM_BPC + 1;

	private boolean mThumbnail;

	private PdfObject mTrailer;
//	private int mPDFObjValue[];
	private int mMaxOrgLength;

	private XrefData mTable[];

	private int mXrefStart;
	private int mVersion;
	private PdfCrypt mCrypt;

	private byte mWorkBuffer[];

//	private int mPageCap;
	private int mPageLen;
	private ArrayList<PdfObject> mPageRefs;
	private ArrayList<PdfObject> mPageObjs;

	InputStream mStreamTbl[];
	List<PdfImageRef> mImageList = new ArrayList<PdfImageRef>();

	private boolean mRunningFlag;

	public class Stack{
		public PdfObject node;
		public PdfObject kids;
		public int pos;
		public int max;
		public PdfInfo info;
	}

	public PDFManager(ImageManager imgmgr, boolean thumbnail) {
		super(imgmgr);
		mWorkBuffer = new byte[SIZE_BUFFER];
		mThumbnail = thumbnail;
		mRunningFlag = true;
	}

	public FileListItem[] pdfFileList(String file) throws IOException {
		mImgMgr.fileAccessInit(file);
		byte buf[] = mFileBuffer;

		// PDFファイル読み込み
		int readSize;
		long orgpos = 0;
		int maxorglen = 0;
		List<FileListItem> list = new ArrayList<FileListItem>();

		seek(0);
		readSize = mImgMgr.cmpDirectRead(buf, 0, SIZE_BUFFER);
		if (readSize < 4) {
			// ヘッダすらない
			return null;
		}

		// 先頭 %PDF をチェック
		int sig = mImgMgr.getInt(buf, OFFSET_PDF_SIGNA_LEN);
		if (sig != 0x46445025) {
			// %PDFでなければPDFではない
			return null;
		}

		// PDFのバージョン
		mVersion = (buf[5] - 0x30) * 10 + (buf[7] - 0x30);
//		Log.d("PDF/Analyze", "Ver=" + mVersion);

		// リファレンスを読み込み
		try {
			loadImageOffsets();
		}
		catch (Exception e){
			String msg = e.getMessage();
			if (msg == null) {
				msg = "Exception!!";
			}
			Log.e("pdfFileList", msg);
			return null;
		}

		// リスト化
		int size = mImageList.size();
		for (int i = 0 ; i < size ; i ++) {
			PdfImageRef imageref = mImageList.get(i);

			FileListItem fl = new FileListItem();
			String ext;
			switch (imageref.type) {
				case PdfImageRef.PDFIMAGE_FAX:
					ext = " - fax";
					fl.type =  4;
					break;
				case PdfImageRef.PDFIMAGE_FLATE:
					ext = " - flate";
					fl.type =  5;
					break;
				case PdfImageRef.PDFIMAGE_JPEG:
				default:
					ext = " - jpeg";
					fl.type =  1;
					break;
			}
			fl.name = String.format("%1$05d", i + 1) + ext;
			fl.cmppos = imageref.offset;
			fl.orgpos = orgpos;
			fl.cmplen = imageref.length;
			fl.orglen = imageref.length;
			fl.header = 0;
			fl.version = 0;
			fl.nocomp = false;
			fl.param1 = imageref.num;
			fl.param2 = imageref.gen;
			fl.params = imageref.params;
			if (fl.params != null && (imageref.type == PdfImageRef.PDFIMAGE_FAX || imageref.type == PdfImageRef.PDFIMAGE_FLATE)) {
				fl.o_width = imageref.params[FAXPARAM_WIDTH];
				fl.o_height = imageref.params[FAXPARAM_HEIGHT];
			}

			orgpos += imageref.length;

			// リストへ登録
			list.add(fl);
			if (maxorglen < fl.orglen) {
				// 最大サイズを求める
				maxorglen = fl.orglen;
			}
		}

//		sort(list);
		mMaxOrgLength = maxorglen;
		return (FileListItem[]) list.toArray(new FileListItem[0]);
	}

	public int getMaxOrgLength() {
		return mMaxOrgLength;
	}

	public PdfCrypt getCrypt() {
		return mCrypt;
	}

	// PDFファイルのリファレンスを検索
	private void loadImageOffsets() throws Exception {
		mFileLength = (int)mImgMgr.cmpDirectLength();
		int readSize;
		byte buf[] = mFileBuffer;

		int buffSize = buf.length;
		long filePos = mFileLength - buffSize;
		if (filePos < 0) {
			// サイズが小さい場合はファイル先頭から
			buffSize = (int)mFileLength;
			filePos = 0;
		}

		// ファイル末尾へ
		seek((int)filePos);
		readSize = mImgMgr.cmpDirectRead(buf, 0, buffSize);
		if (readSize < buf.length) {
			// 読み込みサイズが不足
			throw new IOException("File Access Error.");
		}

		// リファレンス開始位置
		int stpos = PDFLib.searchBinaryRevers(buf, buffSize - BYTE_STARTXREF.length - 1, BYTE_STARTXREF);
		if (stpos < 0) {
			// リファレンスなし
			return;
		}

		// startxrefと続く改行分進める
		stpos += BYTE_STARTXREF.length;
		stpos += skipWhiteCode(buf, stpos);

		// xrefの開始位置を求める
		int worklen = skipNotWhiteCode(buf, stpos);
		 mXrefStart = convertDigitStr(buf, stpos, worklen);

		// ファイル末尾へ
		seek(mXrefStart);

		// 読み込み初期化
		peekInit();

		// 空白を飛ばす
		while(isWhiteCode(peekByte()) == true) {
			readByte();
		}

		int code = peekByte();

		if (code == 'x') {
			readOldTrailer();
		}
		else if ('0' <= code && code <= '9'){
			readNewTrailer();
		}
		else {
			throw new IOException("PDF Format Error.");
		}

		int size = getInt(getDict(mTrailer, "Size"));
		if (size == 0) {
			throw new Exception("trailer missing Size entry");
		}

		// if (size > xref->len)
		// 	pdf_resize_xref(xref, size);
		mTable = new XrefData[size];

		// mTable を設定
		LexData lexdata = new LexData();
		readXrefSections(mXrefStart, lexdata);

		/* broken pdfs where first object is not free */
		boolean repaired = false;
		if (mTable == null || mTable.length <= 0 || mTable[0].type != 'f') {
			// throw new Exception("first object in xref is not free");
			repaired = true;
		}
		else {
			/* broken pdfs where object offsets are out of range */
			for (int i = 0; i < mTable.length ; i++) {
				if (mTable[i].type == 'n') {
					/* Special case code: "0000000000 * n" means free,
					 * according to some producers (inc Quartz) */
					if (mTable[i].ofs == 0) {
						mTable[i].type = 'f';
					}
					else if (mTable[i].ofs <= 0 || mTable[i].ofs >= mFileLength) {
						throw new Exception("object offset out of range: " + mTable[i].ofs + " ("+ i +" 0 R)");
					}
				}
				if (mTable[i].type == 'o') {
					if (mTable[i].ofs <= 0 || mTable[i].ofs >= mTable.length || mTable[mTable[i].ofs].type != 'n') {
						throw new Exception("invalid reference to an objstm that does not exist: " + mTable[i].ofs + " (" + i + " 0 R)");
					}
				}
			}
		}

//		if (repaired)
//			pdf_repair_xref(xref, &xref->lexbuf.base);

		PdfObject encrypt = getDict(mTrailer, "Encrypt");
		PdfObject id = getDict(mTrailer, "ID");
		if (isDict(encrypt)) {
			mCrypt = newCrypt(encrypt, id);
		}

		/* Allow lazy clients to read encrypted files with a blank password */
		authenticatePassword(mCrypt, "");

		if (repaired)
		{
			repairObjStms();

			boolean hasroot = (getDict(mTrailer, "Root") != null);
			boolean hasinfo = (getDict(mTrailer, "Info") != null);

			PdfObject dict;
			PdfObject obj;
			for (int i = 1; i < mTable.length ; i++)
			{
				if (mTable[i].type == 0 || mTable[i].type == 'f')
					continue;

				try {
					dict = loadObject(i, 0);
				}
				catch (Exception e){
					Log.w("Repaired", "ignoring broken object (" + i + " 0 R)");
					continue;
				}

				if (!hasroot)
				{
					obj = getDict(dict, "Type");
					if (isName(obj) && "Catalog".equals(getName(obj))) {
						PdfObject nobj = new PdfObject(i, 0);
						mTrailer.addDict("Root", nobj);
					}
				}

				if (!hasinfo)
				{
					if (getDict(dict, "Creator") != null || getDict(dict, "Producer") != null) {
						PdfObject nobj = new PdfObject(i, 0);
						mTrailer.addDict("Info", nobj);
					}
				}
			}
		}

		// TODO TrailerのRootから
		// pdf_ocg_set_config(xref, 0);

		// ページ情報読み込み
		loadPageTree();

		for (int pagenum = 0 ; pagenum < mPageLen ; pagenum ++)  {
			PdfPage page = loadPage(pagenum);

			PdfObject contents = page.contents;
			PdfObject resource = page.resources;

			if (isArray(contents)) {
				int n = getArrayLen(contents);
//				stm = openConcat(n, 1);

				for (int i = 0; i < n && (!mThumbnail || mImageList.size() < 1); i++) {
					PdfObject obj = getArray(contents, i);
					if (isIndirect(obj)) {
						int num = getNum(obj);
						int gen = getGen(obj);
						if (isStream(num, gen)) {
							openImageStream(num, gen, num, gen, null);
							// contents streamからImage情報抽出
							runContentsStream(resource);
							switchSteramBuffer(false);
						}
						else {
							Log.w("load_page", "pdf object stream missing (" + num + " " + gen + " R)");
						}
					}
				}
			}
			else {
				int num = getNum(contents);
				int gen = getGen(contents);
				if (isStream(num, gen)) {
					openImageStream(num, gen, num, gen, null);
					// contents streamからImage情報抽出
					runContentsStream(resource);
					switchSteramBuffer(false);
				}
				else {
					Log.w("load_page", "pdf object stream missing (" + num + " " + gen + " R)");
				}
				// loadImage(obj);
			}
//			int count = mImageList.size();
			if (mImgMgr.sendProgress(0, pagenum) == false) {
				throw new Exception("user cancel");
			}
			if (mThumbnail && (mImageList.size() > 1 || pagenum > 10)) {
				break;
			}
		}
		return;
	}

	private void readOldTrailer() throws Exception {
		LexData lexdata = new LexData();
		int len;
		int t;
		int tok;
		int c;
		String s;

		s = readLine();
		if (s.length() < 4 || s.substring(0, 4).equals("xref") == false) {
			throw new Exception("cannot find xref marker");
		}

		while (true) {
			c = peekByte();
			if (!isDigitCode(c)) {
				break;
			}

			s = readLine();
			String sep[] = s.split(" "); /* ignore ofs */
			if (sep.length < 2) {
				throw new Exception("invalid range marker in xref");
			}
			len = atoi(sep[1]);

			/* broken pdfs where the section is not on a separate line */
			if (sep.length >= 3 && sep[2].length() > 0) {
				t = tell();
				seek(t + -(2 + sep[2].length()));
			}

			t = tell();
			if (t < 0) {
				throw new Exception("cannot tell in file");
			}
			seek(t + 20 * len);
		}

		tok = lexValue(lexdata);
		if (tok != PDF_TOK_TRAILER){
			throw new Exception("expected trailer marker");
		}

		tok = lexValue(lexdata);
		if (tok != PDF_TOK_OPEN_DICT) {
			throw new Exception("expected trailer dictionary");
		}

		mTrailer = parseDict(lexdata);
		return;
	}

	private void readNewTrailer() {
		try {
			mTrailer = parseIndObject(null);
		}
		catch (Exception e){
			;
		}
		return;
	}

	private void repairObjStms() throws Exception
	{
		PdfObject dict;
		int i;

		for (i = 0; i < mTable.length ; i++)
		{
			if (mTable[i].stmofs != 0) {
				dict = loadObject(i, 0);
				if ("ObjStm".equals(getName(getDict(dict, "Type")))) {
					repairObjStm(i, 0);
				}
			}
		}

		/* Ensure that streamed objects reside inside a known non-streamed object */
		for (i = 0; i < mTable.length; i++) {
			if (mTable[i].type == 'o' && mTable[mTable[i].ofs].type != 'n') {
				throw new Exception("invalid reference to non-object-stream: " + mTable[i].ofs + " (" + i + " 0 R)");
			}
		}
	}

	private void repairObjStm(int num, int gen) throws Exception
	{
		PdfObject obj;
		int tok;
		int i, n, count;
		LexData buf = new LexData();

		obj = loadObject(num, gen);

		count = getInt(getDict(obj, "N"));

		obj = null;

		openStream(num, gen);
		for (i = 0; i < count; i++)
		{
			tok = lexValue(buf);
			if (tok != PDF_TOK_INT) {
				throw new Exception("corrupt object stream (" + num + " " + gen + " R)");
			}

			n = buf.i;
			if (n < 0) {
				Log.w("repairObjStm", "ignoring object with invalid object number (" + n + " " + i + " R)");
				continue;
			}
			else if (n > MAX_OBJECT_NUMBER) {
				Log.w("repairObjStm", "ignoring object with invalid object number (" + n + " " + i + " R)");
				continue;
			}
			if (n >= mTable.length) {
				//pdf_resize_xref(xref, n + 1);
				XrefData work[] = new XrefData[n];
				for (int cnt = 0 ; cnt < mTable.length ; cnt ++) {
					work[cnt] = mTable[cnt];
				}
				mTable = work;
			}

			mTable[n].ofs = num;
			mTable[n].gen = i;
			mTable[n].stmofs = 0;
			mTable[n].obj = null;
			mTable[n].type = 'o';

			tok = lexValue(buf);
			if (tok != PDF_TOK_INT) {
				throw new Exception("corrupt object stream (" + num + " " + gen + " R)");
			}
		}
	}

	private PdfObject parseIndObject(int result[]) throws Exception {
		PdfObject obj;
		LexData lexdata = new LexData();
		int num = 0, gen = 0, stm_ofs;
		int tok;
		int a, b;
		boolean isSkip = false;

//		fz_var(obj);

		tok = lexValue(lexdata);
		if (tok != PDF_TOK_INT) {
			throw new Exception("expected object number");
		}
		num = lexdata.i;

		tok = lexValue(lexdata);
		if (tok != PDF_TOK_INT) {
			throw new Exception("expected generation number (" + num + " ? obj)");
		}
		gen = lexdata.i;

		tok = lexValue(lexdata);
		if (tok != PDF_TOK_OBJ)
			throw new Exception("expected 'obj' keyword (" + num + " " + gen + " ?)");

		tok = lexValue(lexdata);

		switch (tok)
		{
		case PDF_TOK_OPEN_ARRAY:
			obj = parseArray(lexdata);
			break;

		case PDF_TOK_OPEN_DICT:
			obj = parseDict(lexdata);
			break;

		case PDF_TOK_NAME:
			obj = new PdfObject(lexdata.b);
			break;

		case PDF_TOK_REAL:
			obj = new PdfObject(lexdata.f);
			break;

		case PDF_TOK_STRING:
			obj = new PdfObject(lexdata.s);
			break;

		case PDF_TOK_TRUE:
			obj = new PdfObject(true);
			break;

		case PDF_TOK_FALSE:
			obj = new PdfObject(false);
			break;

		case PDF_TOK_NULL:
			obj = new PdfObject();
			break;

		case PDF_TOK_INT:
			a = lexdata.i;
			tok = lexValue(lexdata);

			if (tok == PDF_TOK_STREAM || tok == PDF_TOK_ENDOBJ)
			{
				obj = new PdfObject(a);
				// goto skip;
				isSkip = true;
				break;
			}
			if (tok == PDF_TOK_INT)
			{
				b = lexdata.i;
				tok = lexValue(lexdata);
				if (tok == PDF_TOK_R)
				{
					obj = new PdfObject(a, b);
					break;
				}
			}
			throw new Exception("expected 'R' keyword (" + num + " " + gen + " R)");

		case PDF_TOK_ENDOBJ:
			obj = new PdfObject();
			isSkip = true;
			break;
		default:
			throw new Exception("syntax error in object (" + num + " " + gen + " R)");
		}

		if (isSkip == false) {
			try {
				tok = lexValue(lexdata);
			}
			catch (Exception e) {
				throw new Exception("cannot parse indirect object (" + num + " " + gen + " R)");
			}
		}

//	skip:
		if (tok == PDF_TOK_STREAM) {
			int c = readByte();
			while (c == ' ')
				c = readByte();
			if (c == '\r')
			{
				c = peekByte();
				if (c != '\n')
					throw new Exception("line feed missing after stream begin marker (" + num + " " + gen + " R)");
				else
					readByte();
			}
			stm_ofs = tell();
		}
		else if (tok == PDF_TOK_ENDOBJ) {
			stm_ofs = 0;
		}
		else {
			stm_ofs = 0;
			Log.w("parseIndObject", "expected 'endobj' or 'stream' keyword (" + num + " " + gen + " R)");
		}

		if (result != null && result.length >= 3) {
			result[0] = num;
			result[1] = gen;
			result[2] = stm_ofs;
		}
		return obj;
	}

	private PdfObject parseArray(LexData lexdata) throws Exception
	{
		PdfObject ary;
		PdfObject obj = null;
		int a = 0, b = 0, n = 0;
		int tok;
		PdfObject op = null;
		boolean isFinish = false;

		// fz_var(obj);

		ary = new PdfObject(new ArrayList<PdfObject>(4));

		while (isFinish == false) {
			tok = lexValue(lexdata);

			if (tok != PDF_TOK_INT && tok != PDF_TOK_R)
			{
				if (n > 0)
				{
					obj = new PdfObject(a);
					ary.addArray(obj);
					obj = null;
				}
				if (n > 1)
				{
					obj = new PdfObject(b);
					ary.addArray(obj);
					obj = null;
				}
				n = 0;
			}

			if (tok == PDF_TOK_INT && n == 2)
			{
				obj = new PdfObject(a);
				ary.addArray(obj);
				obj = null;
				a = b;
				n --;
			}

			switch (tok)
			{
			case PDF_TOK_CLOSE_ARRAY:
				op = ary;
				isFinish = true;
				break;

			case PDF_TOK_INT:
				if (n == 0)
					a = lexdata.i;
				if (n == 1)
					b = lexdata.i;
				n ++;
				break;

			case PDF_TOK_R:
				if (n != 2) {
					throw new Exception("cannot parse indirect reference in array");
				}
				obj = new PdfObject(a, b);
				ary.addArray(obj);
				obj = null;
				n = 0;
				break;

			case PDF_TOK_OPEN_ARRAY:
				obj = parseArray(lexdata);
				ary.addArray(obj);
				obj = null;
				break;

			case PDF_TOK_OPEN_DICT:
				obj = parseDict(lexdata);
				ary.addArray(obj);
				obj = null;
				break;
			case PDF_TOK_NAME:
				obj = new PdfObject(lexdata.b);
				ary.addArray(obj);
				obj = null;
				break;
			case PDF_TOK_REAL:
				obj = new PdfObject(lexdata.f);
				ary.addArray(obj);
				obj = null;
				break;
			case PDF_TOK_STRING:
				obj = new PdfObject(lexdata.s);
				ary.addArray(obj);
				obj = null;
				break;
			case PDF_TOK_TRUE:
				obj = new PdfObject(true);
				ary.addArray(obj);
				obj = null;
				break;
			case PDF_TOK_FALSE:
				obj = new PdfObject(false);
				ary.addArray(obj);
				obj = null;
				break;
			case PDF_TOK_NULL:
				obj = new PdfObject();
				ary.addArray(obj);
				obj = null;
				break;

			default:
				throw new Exception("cannot parse token in array");
			}
//	end:
		}
		return op;
	}

	private PdfObject parseDict(LexData lexdata) throws Exception
	{
		PdfObject dict;
		PdfObject key = null;
		PdfObject val = null;
		int tok = -1;
		int a, b;
		boolean isSkip = false;

		// new dict
		dict = new PdfObject(new HashMap<String, PdfObject>(8));

		// fz_var(key);
		// fz_var(val);

		while (true)
		{
			if (isSkip == false) {
				tok = lexValue(lexdata);
			}
			else {
				isSkip = false;
			}
	//skip:
			if (tok == PDF_TOK_CLOSE_DICT)
				break;

			/* for BI .. ID .. EI in content streams */
			if (tok == PDF_TOK_KEYWORD && lexdata.getName().equals("ID")){
				break;
			}
			if (tok != PDF_TOK_NAME){
				throw new Exception("invalid key in dict");
			}
			// nameデータ
			key = new PdfObject(lexdata.b);

			tok = lexValue(lexdata);

			switch (tok)
			{
			case PDF_TOK_OPEN_ARRAY:
				val = parseArray(lexdata);
				break;

			case PDF_TOK_OPEN_DICT:
				val = parseDict(lexdata);
				break;

			case PDF_TOK_NAME:
				val = new PdfObject(lexdata.b);
				break;
			case PDF_TOK_REAL:
				val = new PdfObject(lexdata.f);
				break;
			case PDF_TOK_STRING:
				val = new PdfObject(lexdata.s);
				break;
			case PDF_TOK_TRUE:
				val = new PdfObject(true);
				break;
			case PDF_TOK_FALSE:
				val = new PdfObject(false);
				break;
			case PDF_TOK_NULL:
				val = new PdfObject();
				break;

			case PDF_TOK_INT:
				/* 64-bit to allow for numbers > INT_MAX and overflow */
				a = lexdata.i;
				tok = lexValue(lexdata);
				if (tok == PDF_TOK_CLOSE_DICT || tok == PDF_TOK_NAME ||
					(tok == PDF_TOK_KEYWORD && lexdata.getName().equals("ID")))
				{
					val = new PdfObject(a);
					dict.addDict(key, val);
					val = null;
					key = null;
					isSkip = true;
					continue;
				}
				if (tok == PDF_TOK_INT)
				{
					b = lexdata.i;
					tok = lexValue(lexdata);
					if (tok == PDF_TOK_R)
					{
						val = new PdfObject(a, b);
						break;
					}
				}
				throw new Exception("invalid indirect reference in dict");

			default:
				throw new Exception("unknown token in dict");
			}

			dict.addDict(key, val);
			val = null;
			key = null;
		}
		return dict;
	}

	private int lexValue(LexData lexdata) throws IOException {
		while (true) {
			int c = readByte();

			if (c == READBYTE_EOF) {
				return PDF_TOK_EOF;
			}
			else if (isWhiteCode(c)) {
				lexWhite();
			}
			else if (c == '%') {
				lexComment();
			}
			else if (c == '/') {
				lexName(lexdata);
				return PDF_TOK_NAME;
			}
			else if (c == '(') {
				return lexString(lexdata);
			}
			else if (c == ')') {
				Log.d("lexValue", "lexical error (unexpected ')')");
				continue;
			}
			else if (c == '<') {
				c = readByte();
				if (c == '<')
				{
					return PDF_TOK_OPEN_DICT;
				}
				else
				{
					unreadByte();
					return lexHexString(lexdata);
				}
			}
			else if (c == '>') {
				c = readByte();
				if (c == '>')
				{
					return PDF_TOK_CLOSE_DICT;
				}
				Log.d("lexValue", "lexical error (unexpected '>')");
				continue;
			}
			else if (c ==  '[') {
				return PDF_TOK_OPEN_ARRAY;
			}
			else if (c == ']') {
				return PDF_TOK_CLOSE_ARRAY;
			}
			else if (c == '{') {
				return PDF_TOK_OPEN_BRACE;
			}
			else if (c == '}') {
				return PDF_TOK_CLOSE_BRACE;
			}
			else if (isNumber(c)) {
				return lexNumber(lexdata, c);
			}
			else {
				unreadByte();
				lexName(lexdata);
				return tokenFromKeyword(lexdata.getName());
			}
		}
	}

	// 空白を読み飛ばす
	private void lexWhite() throws IOException {
		int c;
		do {
			c = readByte();
		} while ((c <= 32) && (isWhiteCode(c)));
		if (c != READBYTE_EOF)
			unreadByte();
	}

	// コメントを読み飛ばす
	void lexComment() throws IOException {
		int c;
		do {
			c = readByte();
		} while ((c != '\012') && (c != '\015') && (c != READBYTE_EOF));
	}

	// 10進数数値(int/float自動判別)
	private int lexNumber(LexData lexdata, int c) throws IOException {
		int neg = 0;
		int i = 0;
		int n = 0;
		int d = 0;
		float v;
		int type = 0;	// 0:int 1:float(整数) 2:float(少数以下)

		/* Initially we might have +, -, . or a digit */
		switch (c) {
			case '.':
				type = 1;
			case '-':
				neg = 1;
				break;
			case '+':
				break;
			default: /* Must be a digit */
				i = c - '0';
				break;
		}

		while (true) {
			if (type == 0) {
				c = readByte();
				if (c == '.') {
					type = 1;
				}
				else if (isDigitCode(c)) {
					i = 10 * i + c - '0';
				}
				else {
					if (c != READBYTE_EOF) {
						// 数字以外ならもどす
						unreadByte();
					}
					if (neg != 0) {
						// -付き数値
						i *= -1;
					}
					lexdata.setValue(i);
					return PDF_TOK_INT;
				}
			}
			else if (type == 1){
				/* In here, we've seen a dot, so can accept just digits */
				// loop_after_dot:
				n = 0;
				d = 1;
				while (true) {
					c = readByte();
					if (isDigitCode(c)){
						if (d >= INT_MAX/10) {
							type = 2;
							break;
						}
						n = n * 10 + (c - '0');
						d *= 10;
					}
					else {
						if (c != READBYTE_EOF) {
							unreadByte();
						}
						v = (float)i + ((float)n / (float)d);
						if (neg != 0) {
							v = -v;
						}
						lexdata.setValue(v);
						return PDF_TOK_REAL;
					}

				}
			}
			else if (type == 2) {
				// underflow:
				/* Ignore any digits after here, because they are too small */
				while (true) {
					c = readByte();
					if (isDigitCode(c)){
						break;
					}
					else {
						if (c != READBYTE_EOF) {
							unreadByte();
						}
						v = (float)i + ((float)n / (float)d);
						if (neg != 0) {
							v = -v;
						}
						lexdata.setValue(v);
						return PDF_TOK_REAL;
					}
				}
			}
		}
	}

	// 名前の取得
	private void lexName(LexData lexdata) throws IOException {
		byte wkbuff[] = mWorkBuffer;
		int index = 0;
		int size = mWorkBuffer.length;

		while (index < size) {
			int c = readByte();
			if (isWhiteCode(c) || isDelimiterCode(c)) {
				unreadByte();
				break;
			}
			else if (c == READBYTE_EOF) {
				break;
			}
			else if (c == '#') {
				int d;
				c = readByte();
				if (isDigitCode(c)) {
					d = (c - '0') << 4;
				}
				else if (isHexLowerCode(c)) {
					d = (c - 'a' + 10) << 4;
				}
				else if (isHexUpperCode(c)) {
					d = (c - 'A' + 10) << 4;
				}
				else {
					if (c != READBYTE_EOF) {
						unreadByte();
					}
					break;
				}
				c = readByte();
				if (isDigitCode(c)) {
					c -= '0';
				}
				else if (isHexLowerCode(c)) {
					c -= 'a' - 10;
				}
				else if (isHexUpperCode(c)) {
					c -= 'A' - 10;
				}
				else {
					if (c != READBYTE_EOF) {
						unreadByte();
					}
					wkbuff[index ++] = (byte)(d);
					break;
				}
				wkbuff[index ++] = (byte)(d + c);
			}
			else {
				wkbuff[index ++] = (byte)c;
			}
		}
		
		lexdata.setValue(new String(wkbuff, 0, index, "Shift_JIS"));
	}

	// 文字列読込（使用しないため読み飛ばし）
	private int lexString(LexData lexdata) throws IOException
	{
		byte s[] = mWorkBuffer;
//		int e = mWorkBuffer.length;
		int index = 0;

		int bal = 1;
		int c;

		while (true) {
			c = readByte();
			if (c == READBYTE_EOF) {
				break;
			}
			else if (c == '(') {
				bal++;
				s[index ++] = (byte)c;
			}
			else if (c == ')') {
				bal --;
				if (bal == 0) {
					break;
				}
				s[index ++] = (byte)c;
			}
			else if (c == '\\') {
				c = readByte();
				if (c == READBYTE_EOF) {
					break;
				}
				switch (c) {
					case 'n':
						s[index ++] = '\n';
						break;
					case 'r':
						s[index ++] = '\r';
						break;
					case 't':
						s[index ++] = '\t';
						break;
					case 'b':
						s[index ++] = '\b';
						break;
					case 'f':
						s[index ++] = '\f';
						break;
					case '(':
						s[index ++] = '(';
						break;
					case ')':
						s[index ++] = ')';
						break;
					case '\\':
						s[index ++] = '\\';
						break;
					case '0':
					case '1':
					case '2':
					case '3':
					case '4':
					case '5':
					case '6':
					case '7':
					case '8':
					case '9': {
						int oct = c - '0';
						c = readByte();
						if (c >= '0' && c <= '9') {
							oct = oct * 8 + (c - '0');
							c = readByte();
							if (c >= '0' && c <= '9') {
								oct = oct * 8 + (c - '0');
							}
							else if (c != READBYTE_EOF) {
								unreadByte();
							}
						}
						else if (c != READBYTE_EOF) {
							unreadByte();
						}
						s[index ++] = (byte)oct;
						break;
					}
					case '\n':
						break;
					case '\r':
						c = readByte();
						if ((c != '\n') && (c != READBYTE_EOF))
							unreadByte();
						break;
					default:
						s[index ++] = (byte)c;
						break;
				}
			}
			else {
				s[index ++] = (byte)c;
			}
		}

		lexdata.setValue(s, index);
		return PDF_TOK_STRING;
	}

	private int lexHexString(LexData lexdata) throws IOException
	{
		boolean x = false;
		int c;
		byte work[] = new byte[100];	// TODO とりあえず100
		int xch = 0;
		int index = 0;

		while (true) {
			c = readByte();
			if (isWhiteCode(c)) {
				;
			}
			else if (isHexCode(c)){
				if (x)
				{
					work[index] = (byte)(xch * 16 + unhex(c));
					index ++;
				}
				else
				{
					xch = unhex(c);
				}
				x = !x;
			}
			else if(c == '>' || c == READBYTE_EOF) {
				break;
			}
			else {
				Log.d("lexHexString", "ignoring invalid character in hex string: '" + c + "'");
			}
		}
		lexdata.setValue(work, index);
		return PDF_TOK_STRING;
	}

	private int tokenFromKeyword(String key)
	{
		switch (key.charAt(0)) {
			case 'R':
				if (key.equals("R")) return PDF_TOK_R;
				break;
			case 't':
				if (key.equals("true")) return PDF_TOK_TRUE;
				if (key.equals("trailer")) return PDF_TOK_TRAILER;
				break;
			case 'f':
				if (key.equals("false")) return PDF_TOK_FALSE;
				break;
			case 'n':
				if (key.equals("null")) return PDF_TOK_NULL;
				break;
			case 'o':
				if (key.equals("obj")) return PDF_TOK_OBJ;
				break;
			case 'e':
				if (key.equals("endobj")) return PDF_TOK_ENDOBJ;
				if (key.equals("endstream")) return PDF_TOK_ENDSTREAM;
				break;
			case 's':
				if (key.equals("stream")) return PDF_TOK_STREAM;
				if (key.equals("startxref")) return PDF_TOK_STARTXREF;
				break;
			case 'x':
				if (key.equals("xref")) return PDF_TOK_XREF;
				break;
			default:
				break;
		}
		return PDF_TOK_KEYWORD;
	}

	private PdfObject readXref(int ofs, LexData lexdata) throws Exception
	{
		int c;
		PdfObject trailer = null;

		seek(ofs);

		while (isWhiteCode(peekByte())) {
			readByte();
		}

		c = peekByte();
		if (c == 'x') {
			trailer = readOldXref(lexdata);
		}
		else if (c >= '0' && c <= '9') {
			trailer = readNewXref(lexdata);
		}
		else {
			throw new Exception("cannot recognize xref format");
		}
		return trailer;
	}

	private void readXrefSections(int ofs, LexData lexdata) throws Exception {
		int xrefstmofs = 0;
		int prevofs = 0;
		// fz_var(trailer);
		// fz_var(xrefstmofs);
		// fz_var(prevofs);

		do
		{
			PdfObject trailer = readXref(ofs, lexdata);

			/* FIXME: do we overwrite free entries properly? */
			if (trailer != null) {
				xrefstmofs = getInt(getDict(trailer, "XRefStm"));
				prevofs = getInt(getDict(trailer, "Prev"));
			}

			if (xrefstmofs < 0) {
				throw new Exception("negative xref stream offset");
			}
			if (prevofs < 0) {
				throw new Exception("negative xref stream offset for previous xref stream");
			}

			/* We only recurse if we have both xrefstm and prev.
			 * Hopefully this happens infrequently. */
			if (xrefstmofs != 0 && prevofs != 0) {
				readXrefSections(xrefstmofs, lexdata);
			}

			if (prevofs != 0) {
				ofs = prevofs;
			}
			else if (xrefstmofs != 0) {
				ofs = xrefstmofs;
			}
			trailer = null;
		}
		while (prevofs != 0 || xrefstmofs != 0);
	}

	private PdfObject readOldXref(LexData lexdata) throws Exception
	{
		int ofs = 0;
		int len = 0;
		String s;
		String sep[];
		int tok;
		int i;
		int c;
		PdfObject trailer;

		s = readLine();
		if (s.length() < 4 || s.substring(0, 4).equals("xref") == false) {
			throw new Exception("cannot find xref marker");
		}

		while (true) {
			c = peekByte();
			if (!isDigitCode(c)) {
				break;
			}

			s = readLine();
			sep = s.split(" ");

			if (sep.length >= 1) {
				ofs = atoi(sep[0]);
			}
			if (sep.length >= 2) {
				len = atoi(sep[1]);
			}

			/* broken pdfs where the section is not on a separate line */
			if (sep.length >= 3 && sep[2].length() > 0)
			{
				Log.w("readOldXref", "broken xref section. proceeding anyway.");
				int t = tell();
				seek(t + -(2 + sep[2].length()));
			}

			/* broken pdfs where size in trailer undershoots entries in xref sections */
//			if (ofs + len > xref->len)
//			{
//				fz_warn(xref->ctx, "broken xref section, proceeding anyway.");
//				pdf_resize_xref(xref, ofs + len);
//			}

			for (i = ofs; i < ofs + len; i++)
			{
				s = read(20);
				if (s == null) {
					throw new Exception("cannot read xref table");
				}
				if (mTable[i] == null || mTable[i].type == 0) {
					/* broken pdfs where line start with white space */
					int pos = 0;
					while (pos < s.length()) {
						if (!isWhiteCode(s.charAt(pos))) {
							break;
						}
						pos ++;
					}
					if (pos > 0 && s.length() > pos) {
						s = s.substring(pos);
					}

					sep = s.split(" ");
					if (sep.length < 3) {
						throw new Exception("xref format: " + s  + " (" + i + " " + mTable[i].gen + " R)");
					}
					mTable[i] = new XrefData();
					mTable[i].ofs = atoi(sep[0]);
					if (s.length() >= 11) {
						mTable[i].gen = atoi(sep[1]);
					}
					else {
						mTable[i].gen = 0;
					}

					if (mImgMgr.sendProgress(1, i) == false) {
						throw new Exception("user cancel");
					}

					char s17 = 0;
					if (sep[2].length() > 0) {
						s17 = sep[2].charAt(0);
					}
					mTable[i].type = s17;
					if (s17 != 'f' && s17 != 'n' && s17 != 'o')
						throw new Exception("unexpected xref type: " + Integer.toHexString(s17) + " (" + i + " " + mTable[i].gen + " R)");
				}
			}
		}

		tok = lexValue(lexdata);
		if (tok != PDF_TOK_TRAILER) {
			throw new Exception("expected trailer marker");
		}

		tok = lexValue(lexdata);
		if (tok != PDF_TOK_OPEN_DICT) {
			throw new Exception("expected trailer dictionary");
		}

		trailer = parseDict(lexdata);
		return trailer;
	}

	private PdfObject readNewXref(LexData lexdata) throws Exception {
		PdfObject trailer = null;
		PdfObject index = null;
		PdfObject obj = null;
		int num, gen, stm_ofs;
		int size, w0, w1, w2;
		int t;

		int ret[] = {0, 0, 0};
		trailer = parseIndObject(ret);
		num = ret[0];
		gen = ret[1];
		stm_ofs = ret[2];

		obj = getDict(trailer, "Size");
		if (obj == null) {
			throw new Exception("xref stream missing Size entry (" + num + " " + gen + " R)");
		}

		size = getInt(obj);
		if (size > mTable.length) {
			mTable = new XrefData[size];
		}

		if (num < 0 || num >= mTable.length){
			throw new Exception("object id (" + num + " " + gen + " R) out of range (0.." + (mTable.length - 1) + ")");
		}

		obj = getDict(trailer, "W");
		if (obj == null) {
			throw new Exception("xref stream missing W entry (" + num + " " + gen + " R)");
		}
		w0 = getInt(getArray(obj, 0));
		w1 = getInt(getArray(obj, 1));
		w2 = getInt(getArray(obj, 2));

		if (w0 < 0)
			Log.w("readNewXref", "xref stream objects have corrupt type");
		if (w1 < 0)
			Log.w("readNewXref", "xref stream objects have corrupt offset");
		if (w2 < 0)
			Log.w("readNewXref", "xref stream objects have corrupt generation");

		w0 = w0 < 0 ? 0 : w0;
		w1 = w1 < 0 ? 0 : w1;
		w2 = w2 < 0 ? 0 : w2;

		index = getDict(trailer, "Index");

		// ストリームのデータを読み込む
		openStreamWithOffset(num, gen, trailer, stm_ofs);

		try {
			if (index == null)
			{
				readNewXrefSection(0, size, w0, w1, w2);
			}
			else
			{
				int n = getArrayLen(index);
				for (t = 0; t < n; t += 2)
				{
					int i0 = getInt(getArray(index, t + 0));
					int i1 = getInt(getArray(index, t + 1));
					readNewXrefSection(i0, i1, w0, w1, w2);
				}
			}
		}
		finally {
			// mFilterMode = FILTER_NONE;
			switchSteramBuffer(false);
		}
		return trailer;
	}

	private void openStream(int num, int gen) throws Exception {
		openImageStream(num, gen, num, gen, null);
		return;
	}

	private void openImageStream(int num, int gen, int orig_num, int orig_gen, PdfImageParams params) throws Exception {
		XrefData x;

		if (mTable == null || num < 0 || num >= mTable.length)
			throw new Exception("object id out of range (" + num + " " + gen + "d R)");

		x = mTable[num];

		cacheObject(num, gen);

		if (x.stmofs == 0 && x.stmbuf == null) {
			throw new Exception("object is not a stream");
		}

		openFilter(x.obj, orig_num, orig_gen, x.stmofs, params);

		setStreamBuffer();
		switchSteramBuffer(true);
		return;
	}

	private void openStreamWithOffset(int num, int gen, PdfObject dict, int stm_ofs) throws Exception {
		if (stm_ofs == 0)
			throw new Exception("object is not a stream");

		openFilter(dict, num, gen, stm_ofs, null);

		setStreamBuffer();
		switchSteramBuffer(true);
		return;
	}

	/*
	 * Construct a filter to decode a stream, constraining
	 * to stream length and decrypting.
	 */
	private void openFilter(PdfObject stmobj, int num, int gen, int offset, PdfImageParams imparams) throws Exception
	{
		PdfObject filters;
		PdfObject params;

		filters = getDictSa(stmobj, "Filter", "F");
		params = getDictSa(stmobj, "DecodeParms", "DP");

		openRawFilter(stmobj, num, num, gen, offset);

		if (filters != null) {
			if (filters.kind == PdfObject.PDF_NAME)
				buildFilter(filters, params, num, gen, imparams);
			else if (getArrayLen(filters) > 0)
				buildFilterChain(filters, params, num, gen, imparams);
		}
		return;
	}


	/*
	 * Build a filter for reading raw stream data.
	 * This is a null filter to constrain reading to the stream length (and to
	 * allow for other people accessing the file), followed by a decryption
	 * filter.
	 *
	 * orig_num and orig_gen are used purely to seed the encryption.
	 */
	private void openRawFilter(PdfObject stmobj, int num, int orig_num, int orig_gen, int offset) throws Exception
	{
		boolean hascrypt;
		int len;

		if (mTable != null && num > 0 && num < mTable.length && mTable[num] != null && mTable[num].stmbuf != null) {
			openBuffer(mTable[num].stmbuf);
			return;
		}

		/* don't close chain when we close this filter */
		keepStream();

		len = getInt(getDict(stmobj, "Length"));
		try {
			openNull(len, offset);
		}
		catch(Exception e) {
			releaseStream();
			throw e;
		}

		try {
			hascrypt = streamHasCrypt(stmobj);
			if (mCrypt != null && !hascrypt)
				openCrypt(mCrypt, mCrypt.stmf, orig_num, orig_gen);
		}
		catch(Exception e) {
			releaseStream();
			throw e;
		}

		return;
	}

	/*
	 * Scan stream dictionary for an explicit /Crypt filter
	 */
	private boolean streamHasCrypt(PdfObject stm) {
		PdfObject filters;
		PdfObject obj;
		int i;

		filters = getDictSa(stm, "Filter", "F");
		if (filters != null)
		{
			if ("Crypt".equals(getName(filters))) {
				return true;
			}
			if (isArray(filters)) {
				int n = getArrayLen(filters);
				for (i = 0; i < n; i++)
				{
					obj = getArray(filters, i);
					if ("Crypt".equals(getName(obj))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/*
	 * Create a filter given a name and param dictionary.
	 */
	private void buildFilter(PdfObject f, PdfObject p, int num, int gen, PdfImageParams params) throws Exception
	{
		String s = f.getName();

		int predictor = getInt(getDict(p, "Predictor"));
		int columns = getInt(getDict(p, "Columns"));
		int colors = getInt(getDict(p, "Colors"));
		int bpc = getInt(getDict(p, "BitsPerComponent"));

		if (predictor <= 0) {
			predictor = 1;
		}
		if (columns <= 0) {
			columns = 1;
		}
		if (colors <= 0) {
			colors = 1;
		}
		if (bpc <= 0) {
			bpc = 8;
		}

//		if (s.equals("ASCIIHexDecode") || s.equals("AHx")) {
//			openAhxd();
//			return;
//		}
//		else if (s.equals("ASCII85Decode") || s.equals("A85"))
//			openA85d();
//			return;
//
//		if (s.equals("CCITTFaxDecode") || s.equals("CCF"))
//		{
//			PdfObject k = p.getDict("K");
//			PdfObject eol = p.getDict("EndOfLine");
//			PdfObject eba = p.getDict("EncodedByteAlign");
//			PdfObject clms = p.getDict("Columns");
//			PdfObject rows = p.getDict("Rows");
//			PdfObject eob = p.getDict("EndOfBlock");
//			PdfObject bi1 = p.getDict("BlackIs1");
//			if (params != null) {
//				/* We will shortstop here */
//				ImageFax imagefax = new ImageFax();
//				imagefax.k = (k != null ? getInt(k) : 0);
//				imagefax.eol = getBool(eol, false);
//				imagefax.eba = getBool(eba, false);
//				imagefax.columns = getInt(clms, 1728);
//				imagefax.rows = getInt(rows, 0);
//				imagefax.eob = getBool(eob, true);
//				imagefax.bi1 = getBool(bi1, false);
//				params.setData(imagefax);
//				return;
//			}
//			openFaxd((k != null ? getInt(k) : 0),
//					(eol != null ? getBool(eol) : false),
//					(eba != null ? getBool(eba) : false),
//					(clms != null ? getInt(clms) : 1728),
//					(rows != null ? getInt(rows) : 0),
//					(eob != null ? getBool(eob) : true),
//					(bi1 != null ? getBool(bi1) : false));
//			return;
//		}
//
		if (s.equals("DCTDecode") || s.equals("DCT"))
		{
			PdfObject ct = getDict(p, "ColorTransform");
			if (params != null)
			{
				/* We will shortstop here */
				ImageJpeg imagejpeg = new ImageJpeg();
				params.setData(imagejpeg);
				imagejpeg.ct = getInt(ct, -1);
				return;
			}
			// openDctd(getInt(ct, -1));
			return;
		}
//		else if (s.equals("RunLengthDecode") || s.equals("RL")) {
//			if (params != null)
//			{
//				/* We will shortstop here */
//				params.setType(params.PDF_IMAGE_RLD);
//				return;
//			}
//			openRld();
//			return;
//		}
		else if (s.equals("FlateDecode") || s.equals("Fl")) {
			if (params != null) {
				/* We will shortstop here */
				ImageFlate imageflate = new ImageFlate();
				params.setData(imageflate);
				imageflate.predictor = predictor;
				imageflate.columns = columns;
				imageflate.colors = colors;
				imageflate.bpc = bpc;
				return;
			}

			// JNI の関数呼び出し
			openFlated();
			if (predictor > 1) {
				openPredict(predictor, columns, colors, bpc);
			}
			return;
		}
//
//		else if (s.equals("LZWDecode") || s.equals("LZW"))
//		{
//			PdfObject ec = p.getDict("EarlyChange");
//			if (params != null)
//			{
//				/* We will shortstop here */
//				ImageLzw imagelzw = new ImageLzw();
//				params.setData(imagelzw);
//				imagelzw.predictor = predictor;
//				imagelzw.columns = columns;
//				imagelzw.colors = colors;
//				imagelzw.bpc = bpc;
//				imagelzw.ec = getInt(ec, 1);
//				return;
//			}
//			openLzwd(getInt(ec, 1));
//			if (predictor > 1)
//				openPredict(predictor, columns, colors, bpc);
//			return;
//		}
//
//		else if (s.equals("JBIG2Decode"))
//		{
//			fz_buffer *globals = NULL;
//			PdfObject obj = p.getDict("JBIG2Globals");
//			if (obj != null) {
//				globals = pdf_load_stream(xref, pdf_to_num(obj), pdf_to_gen(obj));
//			}
//			/* fz_open_jbig2d takes possession of globals */
//			fz_open_jbig2d(chain, globals);
//			return;
//		}
//
//		else if (s.equals("JPXDecode"))
//			return; /* JPX decoding is special cased in the image loading code */
//
//		else if (s.equals("Crypt"))
//		{
//			PdfObject name;
//
//			if (mCrypt == null)
//			{
//				throw new Exception("crypt filter in unencrypted document");
//				return;
//			}
//
//			name = p.getDict("Name");
//			if (name.b != null)
//				return pdf_open_crypt_with_filter(mCrypt, name.getName(), num, gen);
//
//			return;
//		}

		Log.w("buildFilter", "unknown filter name (" + s + ")");
		return;
	}

	/*
	 * Build a chain of filters given filter names and param dicts.
	 * If head is given, start filter chain with it.
	 * Assume ownership of head.
	 */
	void buildFilterChain(PdfObject fs, PdfObject ps, int num, int gen, PdfImageParams params) throws Exception
	{
		PdfObject f;
		PdfObject p;
		int i, n;

		n = getArrayLen(fs);
		for (i = 0; i < n; i++) {
			f = getArray(fs, i);
			p = getArray(ps, i);
			buildFilter(f, p, num, gen, params);
		}
		return;
	}

//	/*
//	 * Build a filter for reading raw stream data.
//	 * This is a null filter to constrain reading to the stream length (and to
//	 * allow for other people accessing the file), followed by a decryption
//	 * filter.
//	 *
//	 * orig_num and orig_gen are used purely to seed the encryption.
//	 */
//	PdfStream openRawFilter(PdfObject stmobj, int num, int orig_num, int orig_gen, int offset)
//	{
//		int hascrypt;
//		int len;
//
//		if (num > 0 && num < mTable.length && mTable[num].stm_buf) {
//			openBuffer(mTable[num].stm_buf);
//			return
//		}
//
//		/* don't close chain when we close this filter */
//		keepStream();
//
//		len = getInt(stmobj.getDict("Length"));
//		chain = openNull(len, offset);
//
//		fz_try(ctx)
//		{
//			hascrypt = pdf_stream_has_crypt(ctx, stmobj);
//			if (xref->crypt && !hascrypt)
//				chain = pdf_open_crypt(chain, xref->crypt, orig_num, orig_gen);
//		}
//		fz_catch(ctx)
//		{
//			fz_close(chain);
//			fz_rethrow(ctx);
//		}
//
//		return chain;
//	}

	private void readNewXrefSection(int i0, int i1, int w0, int w1, int w2) throws Exception
	{
		int i, n;

		if (i0 < 0 || i1 < 0){
			throw new Exception("negative xref stream entry index");
		}
		if (i0 + i1 > mTable.length) {
			throw new Exception("xref stream has too many entries");
		}

		for (i = i0; i < i0 + i1; i++)
		{
			int a = 0;
			int b = 0;
			int c = 0;

			if (isEOF()) {
				throw new Exception("truncated xref stream");
			}

			for (n = 0; n < w0; n++)
				a = (a << 8) + (((int)readByte()) & 0xFF);
			for (n = 0; n < w1; n++)
				b = (b << 8) + (((int)readByte()) & 0xFF);
			for (n = 0; n < w2; n++)
				c = (c << 8) + (((int)readByte()) & 0xFF);

			mTable[i] = new XrefData();
			if (mTable[i].type == 0) {
				int t = (w0 != 0) ? a : 1;
				mTable[i].type = t == 0 ? 'f' : t == 1 ? 'n' : t == 2 ? 'o' : 0;
				mTable[i].ofs = (w1 != 0) ? b : 0;
				mTable[i].gen = (w2 != 0)? c : 0;
			}

			if (mImgMgr.sendProgress(1, i) == false) {
				throw new Exception("user cancel");
			}
		}
	}


	private void loadPageTree() throws Exception {
		PdfObject catalog;
		PdfObject pages;
		PdfObject count;

		catalog = getDict(mTrailer, "Root");
		pages = getDict(catalog, "Pages");
		count = getDict(pages, "Count");

		if (!isDict(pages)) {
			throw new Exception("missing page tree");
		}
		if (!isInt(count) || getInt(count) < 0) {
			throw new Exception("missing page count");
		}

//		mPageCap = getInt(count);
		mPageLen = 0;
		mPageRefs = new ArrayList<PdfObject>();
		mPageObjs = new ArrayList<PdfObject>();


		PdfInfo info = new PdfInfo();
		loadPageTreeNode(pages, info);
	}

	// node : pagesオブジェクト
	private void loadPageTreeNode(PdfObject node, PdfInfo info) throws Exception {
		PdfObject dict;
		PdfObject kids;
		PdfObject count;

		ArrayList<Stack> stack = new ArrayList<Stack>();
		int stacklen = -1;

		do {
			if (node == null || markDict(node) != 0) {
				/* NULL node, or we've been here before.
				 * Nothing to do. */
			}
			else {
				kids = getDict(node, "Kids");
				count = getDict(node, "Count");
				if (isArray(kids) && isInt(count)) {
					/* Push this onto the stack */
//					info.resources = getDict(node, "Resources");
//					info.mediabox = getDict(node, "MediaBox");
//					info.cropbox = getDict(node, "CropBox");
					info.rotate = getDict(node, "Rotate");
					stacklen++;

					// スタック用意
					Stack work = new Stack();
					work.kids = kids;
					work.node = node;
					work.pos = -1;
					work.max = getArrayLen(kids);
					work.info = info;
					stack.add(work);
				}
				else if ((dict = getDictObject(node)) != null) {
//					if (info.resources != null && getDict(dict, "Resources") == null) {
//						dict.addDict("Resources", info.resources);
//					}
//					if (info.mediabox != null && getDict(dict, "MediaBox") == null) {
//						dict.addDict("MediaBox", info.mediabox);
//					}
//					if (info.cropbox != null && getDict(dict, "CropBox") == null) {
//						dict.addDict("CropBox", info.cropbox);
//					}
					if (info.rotate != null && getDict(dict, "Rotate") == null) {
						dict.addDict("Rotate", info.rotate);
					}

					mPageRefs.add(keepObj(node));
					mPageObjs.add(keepObj(dict));
					mPageLen ++;
					unmarkDict(node);
					if (mImgMgr.sendProgress(2, mPageLen) == false) {
						throw new Exception("user cancel");
					}
					if (mThumbnail && mPageLen > 10) {
						// サムネイル時は10ページだけでOK
						break;
					}
				}
			}
			/* Get the next node */
			if (stacklen < 0) {
				break;
			}
			while (true) {
				Stack work = stack.get(stacklen);
				work.pos ++;

				if (work.pos != work.max) {
					break;
				}
				unmarkDict(work.node);
				stack.remove(stacklen);
				stacklen--;
				if (stacklen < 0) { /* No more to pop! */
					break;
				}
				node = work.node;
				info = work.info;
				unmarkDict(node); /* Unmark it, cos we're about to mark it again */
			}
			if (stacklen >= 0) {
				Stack work = stack.get(stacklen);
				node = getArray(work.kids, work.pos);
			}
		}
		while (stacklen >= 0);

		while (stacklen >= 0) {
			Stack work = stack.get(stacklen);
			unmarkDict(work.node);
			stacklen --;
		}
	}


	private PdfPage loadPage(int number) throws Exception {
		PdfPage   page;
		PdfObject pageobj;
		PdfObject obj;

		pageobj = mPageObjs.get(number);
//		pageref = mPageRefs.get(number);

		page = new PdfPage();
		page.resources = null;
		page.contents = null;
		page.transparency = 0;

		page.rotate = getInt(getDict(pageobj, "Rotate"));
		/* Snap page->rotate to 0, 90, 180 or 270 */
		if (page.rotate < 0) {
			page.rotate = 360 - ((-page.rotate) % 360);
		}
		if (page.rotate >= 360) {
			page.rotate = page.rotate % 360;
		}
		page.rotate = 90*((page.rotate + 45)/90);
		if (page.rotate > 360) {
			page.rotate = 0;
		}

		page.resources = getDict(pageobj, "Resources");
		if (page.resources != null) {
			keepObj(page.resources);
		}

		obj = getDict(pageobj, "Contents");
		page.contents = keepObj(obj);

		if (resourcesUseBlending(page.resources) != false) {
			page.transparency = 1;
		}
		return page;
	}


	private boolean resourcesUseBlending(PdfObject rdb) throws Exception {
		PdfObject obj;
		boolean useBM = false;

		if (rdb == null) {
			return false;
		}

		/* Have we been here before and stashed an answer? */
		obj = getDict(rdb, ".useBM");
		if (obj != null) {
			return getBool(obj);
		}

		/* stop on cyclic resource dependencies */
		if (markDict(rdb) != 0) {
			return false;
		}

		boolean isFound = false;
		try {
			while (true) {
				Set<String>keys;

				obj = getDict(rdb, "ExtGState");
				keys = getDictKeys(obj);
				if (keys != null) {
					for (String key : keys) {
						if (extgstateUsesBlending(getDict(obj, key)) != 0) {
							isFound = true;
							break;
						}
						if (mRunningFlag == false) {
							// 処理中断
							throw new Exception("user cancel");
						}
					}
				}
				if (isFound) {
					break;
				}

				obj = getDict(rdb, "Pattern");
				keys = getDictKeys(obj);
				if (keys != null) {
					for (String key : keys) {
						if (patternUsesBlending(getDict(obj, key)) != 0) {
							isFound = true;
							break;
						}
						if (mRunningFlag == false) {
							// 処理中断
							throw new Exception("user cancel");
						}
					}
				}
				if (isFound) {
					break;
				}

				obj = getDict(rdb, "XObject");
				keys = getDictKeys(obj);
				if (keys != null) {
					for (String key : keys) {
						if (xobjectUsesBlending(getDict(obj, key)) != false) {
							isFound = true;
							break;
						}
						if (mRunningFlag == false) {
							// 処理中断
							throw new Exception("user cancel");
						}
					}
				}
				break;
			}

			if (isFound)
			{
				useBM = true;
			}
		}
		finally	{
			unmarkDict(rdb);
		}

		putMarkerBool(rdb, ".useBM", useBM);
		return useBM;
	}

	private int extgstateUsesBlending(PdfObject dict)
	{
		PdfObject obj = getDict(dict, "BM");
		if (isName(obj) && "Normal".equals(getName(obj))) {
			return 1;
		}
		return 0;
	}

	private int patternUsesBlending(PdfObject dict) throws Exception
	{
		PdfObject obj;
		obj = getDict(dict, "Resources");
		if (resourcesUseBlending(obj) != false)
			return 1;
		obj = getDict(dict, "ExtGState");
		return extgstateUsesBlending(obj);
	}

	private boolean xobjectUsesBlending(PdfObject dict) throws Exception
	{
		PdfObject obj = getDict(dict, "Resources");
		return resourcesUseBlending(obj);
	}

	private PdfObject resolv(PdfObject ref)
	{
		int sanity = 10;
		int num;
		int gen;

		while (ref != null && ref.kind == PdfObject.PDF_INDIRECT) {
			num = getNum(ref);
			gen = getGen(ref);

			if (--sanity == 0)
			{
				Log.w("resolvIndirect", "Too many indirections (possible indirection cycle involving " + num + " " + gen + " R)");
				return null;
			}

			try {
				cacheObject(num, gen);
			}
			catch(Exception e) {
				Log.w("resolvIndirect", "cannot load object (" + num + " " + gen + " R) into cache");
				return null;
			}

			if (mTable[num].obj == null) {
				return null;
			}
			ref = mTable[num].obj;
		}

		return ref;
	}

	/*
	 * object loading
	 */

	private void cacheObject(int num, int gen) throws Exception {
		XrefData x;
		int rnum, rgen;

		if (num < 0 || num >= mTable.length)
			throw new Exception("object out of range (" + num + " " + gen + " R); xref size " + mTable.length);

		x = mTable[num];

		if (x.obj != null)
			return;

		if (x.type == 'f') {
			x.obj = new PdfObject();
			return;
		}
		else if (x.type == 'n')
		{
			boolean flagstack = switchSteramBuffer(false);
			seek(x.ofs);

			int result[] = {0, 0, 0};
			x.obj = parseIndObject(result);
			rnum = result[0];
			rgen = result[1];
			x.stmofs = result[2];

			switchSteramBuffer(flagstack);

			if (rnum != num) {
				x.obj = null;
				throw new Exception("found object (" + rnum + " " + rgen + " R) instead of (" + num + " " + gen + " R)");
			}

			// TODO 暗号化
			if (mCrypt != null) {
				cryptObject(mCrypt, x.obj, num, gen);
			}
		}
		else if (x.type == 'o')
		{
			if (x.obj == null)
			{
				try {
					loadObjectStream(x.ofs, 0);
				}
				catch (Exception e) {
					throw new Exception("cannot load object stream containing object (" + num + " " + gen + " R)");
				}
				if (x.obj == null)
					throw new Exception("object (" + num + " " + gen + " R) was not found in its object stream");
			}
		}
		else
		{
			throw new Exception("assert: corrupt xref struct");
		}
	}


	protected void cryptObject(PdfCrypt crypt, PdfObject obj, int num, int gen)
	{
		byte key[] = new byte[32];
		int keylen;

		keylen = computeObjectKey(crypt, crypt.strf, num, gen, key);

		cryptObjectImp(crypt, obj, key, keylen);
	}

	/*
	 * PDF 1.7 algorithm 3.1 and ExtensionLevel 3 algorithm 3.1a
	 *
	 * Decrypt all strings in obj modifying the data in-place.
	 * Recurse through arrays and dictionaries, but do not follow
	 * indirect references.
	 */

	private void cryptObjectImp(PdfCrypt crypt, PdfObject obj, byte key[], int keylen)
	{
		int i, n;
		if (obj == null) {
			return;
		}
		
		if (isIndirect(obj)) {
			return;
		}

		if (isString(obj)) {
//			byte s[] = obj.s;
			n = getStringLen(obj);

			// TODO 暗号化処理作成
			if (crypt.strf.method == PdfCrypt.PDF_CRYPT_RC4) {
//				fz_arc4 arc4;
//				fz_arc4_init(&arc4, key, keylen);
//				fz_arc4_encrypt(&arc4, s, s, n);
			}
			if (crypt.strf.method == PdfCrypt.PDF_CRYPT_AESV2 || crypt.strf.method == PdfCrypt.PDF_CRYPT_AESV3) {
				if (n == 0) {
					/* Empty strings are permissible */
				}
				else if ((n & 15) != 0 || n < 32) {
					Log.w("cryptObjectImp", "invalid string length for aes encryption");
				}
				else {
//					byte iv[] = new byte[16];
//					fz_aes aes;
//					memcpy(iv, s, 16);
//					aes_setkey_dec(&aes, key, keylen * 8);
//					aes_crypt_cbc(&aes, AES_DECRYPT, n - 16, iv, s + 16, s);
//					/* delete space used for iv and padding bytes at end */
//					if (s[n - 17] < 1 || s[n - 17] > 16) {
//						fz_warn(ctx, "aes padding out of range");
//					}
//					else {
//						pdf_set_str_len(obj, n - 16 - s[n - 17]);
//						
//					}
				}
			}
		}
		else if (isArray(obj)) {
			n = getArrayLen(obj);
			for (i = 0; i < n; i++)
			{
				cryptObjectImp(crypt, getArray(obj, i), key, keylen);
			}
		}
		else if (isDict(obj)) {
			Set<String>keys;

			keys = getDictKeys(obj);
			if (keys != null) {
				for (String work : keys) {
					cryptObjectImp(crypt, getDict(obj, work), key, keylen);
				}
			}
		}
	}

	/*
	 * compressed object streams
	 */

	private void loadObjectStream(int num, int gen) throws Exception {
		PdfObject objstm = null;
		int numbuf[] = null;
		int ofsbuf[] = null;

		PdfObject obj;
		int first;
		int count;
		int i;
		int tok;
		LexData lexdata = new LexData();

		// ファイルポインタ保存
		int t = tell();

		try {
			objstm = loadObject(num, gen);

			count = getInt(getDict(objstm, "N"));
			first = getInt(getDict(objstm, "First"));

			if (count < 0)
				throw new Exception("negative number of objects in object stream");
			if (first < 0)
				throw new Exception("first object in object stream resides outside stream");

			numbuf = new int[count];
			ofsbuf = new int[count];

			openStream(num, gen);
			for (i = 0; i < count; i++)
			{
				tok = lexValue(lexdata);
				if (tok != PDF_TOK_INT) {
					throw new Exception("corrupt object stream (" + num + " " + gen + " R)");
				}
				numbuf[i] = lexdata.i;

				tok = lexValue(lexdata);
				if (tok != PDF_TOK_INT) {
					throw new Exception("corrupt object stream (" + num + " " + gen + " R)");
				}
				ofsbuf[i] = lexdata.i;
			}

			seek(first);

			for (i = 0; i < count; i++)
			{
				seek(first + ofsbuf[i]);

				obj = parseStreamObject(lexdata);

				if (numbuf[i] < 1 || numbuf[i] >= mTable.length)
				{
					throw new Exception("object id (" + numbuf[i] + " 0 R) out of range (0.." + (mTable.length - 1) + ")");
				}

				if (mTable[numbuf[i]].type == 'o' && mTable[numbuf[i]].ofs == num)
				{
					mTable[numbuf[i]].obj = obj;
				}
			}
		}
		catch (Exception e) {
			// ファイルポインタを元の位置に
			seek(t);
			throw e;
		}
		finally {
			switchSteramBuffer(false);
		}
	}

	PdfObject loadObject(int num, int gen) throws Exception {
		try {
			cacheObject(num, gen);
		}
		catch (Exception e) {
			throw new Exception("cannot load object (" + num + " " + gen + " R) into cache");
		}

		assert(mTable[num].obj != null);

		return keepObj(mTable[num].obj);
	}


	PdfObject parseStreamObject(LexData lexdata) throws Exception {
		int tok;

		tok = lexValue(lexdata);

		switch (tok)
		{
			case PDF_TOK_OPEN_ARRAY:
				return parseArray(lexdata);
			case PDF_TOK_OPEN_DICT:
				return parseDict(lexdata);
			case PDF_TOK_NAME:
				return new PdfObject(lexdata.b);
			case PDF_TOK_REAL:
				return new PdfObject(lexdata.f);
			case PDF_TOK_STRING:
				return new PdfObject(lexdata.s);
			case PDF_TOK_TRUE:
				return new PdfObject(true);
			case PDF_TOK_FALSE:
				return new PdfObject(false);
			case PDF_TOK_NULL:
				return new PdfObject();
			case PDF_TOK_INT:
				return new PdfObject(lexdata.i);
		}
		throw new Exception("unknown token in object stream");
	}


//	private void runStream(PdfObject rdb, LexData lexdata) throws Exception
//	{
//		int tok = PDF_TOK_ERROR;
//		boolean in_array;
//		int count = 0;
////		boolean ignoring_errors = false;
//
//		/* make sure we have a clean slate if we come here from flush_text */
//		in_array = false;
//
//		String name = "";
////		PdfObject obj;
//		boolean in_text = false;
//
//
//		do {
//			tok = lexValue(lexdata);
//
//			if (in_array)
//			{
//				if (tok == PDF_TOK_CLOSE_ARRAY) {
//					in_array = false;
//				}
//				else if (tok == PDF_TOK_REAL) {
////						pdf_gstate *gstate = csi->gstate + csi->gtop;
////						pdf_show_space(csi, -buf->f * gstate->size * 0.001f);
//				}
//				else if (tok == PDF_TOK_INT) {
////						pdf_gstate *gstate = csi->gstate + csi->gtop;
////						pdf_show_space(csi, -buf->i * gstate->size * 0.001f);
//				}
//				else if (tok == PDF_TOK_STRING) {
////						pdf_show_string(csi, (unsigned char *)buf->scratch, buf->len);
//				}
//				else if (tok == PDF_TOK_KEYWORD) {
////						if (!strcmp(buf->scratch, "Tw") || !strcmp(buf->scratch, "Tc"))
////							fz_warn(ctx, "ignoring keyword '%s' inside array", buf->scratch);
////						else
////							fz_throw(ctx, "syntax error in array");
//				}
//				else if (tok == PDF_TOK_EOF)
//					break;
//				else
//					throw new Exception("syntax error in array");
//			}
//
//			else switch (tok)
//			{
//			case PDF_TOK_ENDSTREAM:
//			case PDF_TOK_EOF:
//				tok = PDF_TOK_EOF;
//				break;
//
//			case PDF_TOK_OPEN_ARRAY:
//				if (in_text) {
//					parseArray(lexdata);
//				}
//				else {
//					in_array = true;
//				}
//				break;
//
//			case PDF_TOK_OPEN_DICT:
//				parseDict(lexdata);
//				break;
//
//			case PDF_TOK_NAME:
//				PdfObject work = new PdfObject(lexdata.b);
//				name = work.getName();
//				break;
//
//			case PDF_TOK_INT:
//				break;
//
//			case PDF_TOK_REAL:
//				break;
//
//			case PDF_TOK_STRING:
//				break;
//
//			case PDF_TOK_KEYWORD:
//				if (runKeyword(name, rdb, lexdata.getName()))
//				{
//					tok = PDF_TOK_EOF;
//				}
//				break;
//
//			default:
//				throw new Exception("syntax error in content stream");
//			}
//			count ++;
//			if (count > 50) {
//				break;
//			}
//		}
//		while (tok != PDF_TOK_EOF);
//	}

	private void runStreamFast(PdfObject rdb, LexData lexdata) throws Exception
	{
		String name = "";


		int iPos = mBufferPos;
		int iDlm1 = 0;
		int iDlm2 = 0;
//		boolean flagDelim = false;
		int tok; 
		while (true) {
			while (iPos < mBufferLen - 1) {
				if (mBuffer[iPos] == (byte)'D' && mBuffer[iPos + 1] == (byte)'o' && iPos == iDlm1) {
					break;
				}
				else if (isWhiteCode(mBuffer, iPos)) {
					// 区切り位置
					if (iDlm1 != iPos) {
						iDlm2 = iDlm1;
					}
					iDlm1 = iPos + 1;
//					flagDelim = true;
				}
				else {
//					flagDelim = false;
				}
				iPos ++;
			}
			if (iPos >= mBufferLen - 1) {
				break;
			}

			mBufferPos = iDlm2;
			tok = lexValue(lexdata);
			if (tok == PDF_TOK_NAME) {
				PdfObject work = new PdfObject(lexdata.b);
				name = work.getName();
	
				if (runKeyword(name, rdb, "Do")) {
					break;
				}
			}
			iPos += 2;
		}
	}

	private boolean runKeyword(String name, PdfObject rdb, String key) throws Exception {
		if ("BI".equals(key)) {
			// 読み飛ばし
			LexData work = new LexData();
			parseDict(work);
		}
		else if ("Do".equals(key)) {
			PdfObject dict;
			PdfObject obj;
			PdfObject subtype;

			dict = getDict(rdb, "XObject");
			if (dict == null) {
				throw new Exception("cannot find XObject dictionary when looking for: '" + name + "'");
			}

			obj = getDict(dict, name);
			if (obj == null) {
				throw new Exception("cannot find xobject resource: '" + name + "'");
			}

			subtype = getDict(obj, "Subtype");
			if (!isName(subtype)) {
				throw new Exception("no XObject subtype specified");
			}

			if ("Image".equals(getName(subtype))) {
				// 画像 & ファイルリストに保存
				int num = getNum(obj);
				int gen = getGen(obj);
				cacheObject(num, gen);

				PdfObject image = mTable[num].obj;
				int offset = mTable[num].stmofs;
				int len = getInt(getDict(image, "Length"));
				String decode = getName(getDict(image, "Filter"));
				int width = getInt(getDict(image, "Width"));
				int height = getInt(getDict(image, "Height"));
				int bpc = getInt(getDict(image, "BitsPerComponent"));
//				String color = getName(getDict(image, "ColorSpace"));
				boolean hascrypt = streamHasCrypt(image);
				int predictor = getInt(getDict(image, "Predictor"));
				int columns = getInt(getDict(image, "Columns"));
				int colors = getInt(getDict(image, "Colors"));


				if (decode != null && (decode.equals("DCTDecode") || decode.equals("DCT"))) {
//					Log.d("loadImage", "pos=" + Integer.toHexString(offset) + ", len=" + len);

					PdfImageRef imageref = new PdfImageRef(PdfImageRef.PDFIMAGE_JPEG, offset, len, num, gen, hascrypt ? 1 : 0, null);
					mImageList.add(imageref);
					return true;
				}
				else if (decode != null && (decode.equals("CCITTFaxDecode") || decode.equals("CCF"))) {
					int n = 1;
//					if (color != null) {
//						if (color.equals("DeviceRGB")) {
//							n = 3;
//						}
//						else if(color.equals("DeviceCMYK")) {
//							n = 4;
//						}
//					}

					PdfObject param = getDict(image, "DecodeParms");

					// 画像展開用のパラメタ
					int faxParam[] = new int [FAXPARAM_MAX];
					faxParam[FAXPARAM_WIDTH]  = width;
					faxParam[FAXPARAM_HEIGHT] = height;
					faxParam[FAXPARAM_COLORN] = n;
					faxParam[FAXPARAM_BPC]    = bpc;
					faxParam[FAXPARAM_K]      = getInt(getDict(param, "K"), 0);
					faxParam[FAXPARAM_EOL]    = getBool(getDict(param, "EndOfLine"), false) ? 1 : 0;
					faxParam[FAXPARAM_EBA]    = getBool(getDict(param, "EncodedByteAlign"), false) ? 1 : 0;
					faxParam[FAXPARAM_CLMS]   = getInt(getDict(param, "Columns"), 1728);
					faxParam[FAXPARAM_ROWS]   = getInt(getDict(param, "Rows"), 0);
					faxParam[FAXPARAM_EOB]    = getBool(getDict(param, "EndOfBlock"), true) ? 1 : 0;
					faxParam[FAXPARAM_BI1]    = getBool(getDict(param, "BlackIs1"), false) ? 1 : 0; 
					PdfImageRef imageref = new PdfImageRef(PdfImageRef.PDFIMAGE_FAX, offset, len, num, gen, hascrypt ? 1 : 0, faxParam);
					mImageList.add(imageref);
					return true;
				}
				else if (decode != null && (decode.equals("FlateDecode") || decode.equals("Fl"))) {
					int n = 1;
					if (predictor <= 0) {
						predictor = 1;
					}
					if (columns <= 0) {
						columns = 1;
					}
					if (colors <= 0) {
						colors = 1;
					}
					if (bpc <= 0) {
						bpc = 8;
					}

					// 画像展開用のパラメタ
					int flateParam[] = new int [FLATEPARAM_MAX];
					flateParam[FLATEPARAM_WIDTH]  = width;
					flateParam[FLATEPARAM_HEIGHT] = height;
//					faxParam[FAXPARAM_COLORN] = n;
//					faxParam[FAXPARAM_BPC]    = bpc;
//					flateParam[FLATEPARAM_PREDICTOR] = predictor;
//n					flateParam[FLATEPARAM_COLUMNS]   = columns;
					flateParam[FLATEPARAM_COLORS]    = colors;
					flateParam[FLATEPARAM_BPC]       = bpc;
					PdfImageRef imageref = new PdfImageRef(PdfImageRef.PDFIMAGE_FLATE, offset, len, num, gen, hascrypt ? 1 : 0, flateParam);
					mImageList.add(imageref);
					return true;
				}

//				openStream(num, gen);
				
			}
		}
		return false;
	}

	private PdfCrypt newCrypt(PdfObject dict, PdfObject id) throws Exception {
		PdfCrypt crypt;
		PdfObject obj;
	
		crypt = new PdfCrypt();
	
		/* Common to all security handlers (PDF 1.7 table 3.18) */
	
		obj = getDict(dict, "Filter");
		if (!isName(obj)) {
			throw new Exception("unspecified encryption handler");
		}
		if ("Standard".equals(getName(obj)) == false)
		{
			throw new Exception("unknown encryption handler: '" + getName(obj) + "'");
		}
	
		crypt.v = 0;
		obj = getDict(dict, "V");
		if (isInt(obj)) {
			crypt.v = getInt(obj);
		}
		if (crypt.v != 1 && crypt.v != 2 && crypt.v != 4 && crypt.v != 5) { 
			throw new Exception("unknown encryption version");
		}
	
		/* Standard security handler (PDF 1.7 table 3.19) */
	
		obj = getDict(dict, "R");
		if (isInt(obj)) {
			crypt.r = getInt(obj);
		}
		else if (crypt.v <= 4) {
			Log.w("newCrypt", "encryption dictionary missing revision value, guessing...");
			if (crypt.v < 2)
				crypt.r = 2;
			else if (crypt.v == 2)
				crypt.r = 3;
			else if (crypt.v == 4)
				crypt.r = 4;
		}
		else
		{
			throw new Exception("encryption dictionary missing version and revision value");
		}
	
		obj = getDict(dict, "O");
		if (isString(obj) && getStringLen(obj) == 32)
			crypt.o = getStringBuf(obj, 32);
		/* /O and /U are supposed to be 48 bytes long for revision 5, they're often longer, though */
		else if (crypt.r == 5 && isString(obj) && getStringLen(obj) >= 48)
			crypt.o = getStringBuf(obj, 48);
		else
		{
			throw new Exception("encryption dictionary missing owner password");
		}
	
		obj = getDict(dict, "U");
		if (isString(obj) && getStringLen(obj) == 32) {
			crypt.u = getStringBuf(obj, 32);
		}
		/* /O and /U are supposed to be 48 bytes long for revision 5, they're often longer, though */
		else if (crypt.r == 5 && isString(obj) && getStringLen(obj) >= 48) {
			crypt.u = getStringBuf(obj, 48);
		}
		else if (isString(obj) && getStringLen(obj) < 32) {
			Log.w("newCrypt", "encryption password key too short (" + getStringLen(obj) + ")");
			crypt.u = getStringBuf(obj, getStringLen(obj));
		}
		else
		{
			throw new Exception("encryption dictionary missing user password");
		}
	
		obj = getDict(dict, "P");
		if (isInt(obj)) {
			crypt.p = getInt(obj);
		}
		else {
			Log.w("newCrypt", "encryption dictionary missing permissions");
			crypt.p = 0xfffffffc;
		}
	
		if (crypt.r == 5)
		{
			obj = getDict(dict, "OE");
			if (!isString(obj) || getStringLen(obj) != 32)
			{
				throw new Exception("encryption dictionary missing owner encryption key");
			}
			crypt.oe = getStringBuf(obj, 32);
	
			obj = getDict(dict, "UE");
			if (!isString(obj) || getStringLen(obj) != 32)
			{
				throw new Exception("encryption dictionary missing user encryption key");
			}
			crypt.ue = getStringBuf(obj, 32);
		}
	
		crypt.encrypt_metadata = true;
		obj = getDict(dict, "EncryptMetadata");
		if (isBool(obj))
			crypt.encrypt_metadata = getBool(obj);
	
		/* Extract file identifier string */
	
		if (isArray(id) && getArrayLen(id) == 2) {
			obj = getArray(id, 0);
			if (isString(obj)) {
				crypt.id = keepObj(obj);
			}
		}
		else {
			Log.w("newCrypt", "missing file identifier, may not be able to do decryption");
		}
	
		/* Determine encryption key length */
	
		crypt.length = 40;
		if (crypt.v == 2 || crypt.v == 4)
		{
			obj = getDict(dict, "Length");
			if (isInt(obj)) {
				crypt.length = getInt(obj);
			}
	
			/* work-around for pdf generators that assume length is in bytes */
			if (crypt.length < 40) {
				crypt.length = crypt.length * 8;
			}
	
			if (crypt.length % 8 != 0) {
				throw new Exception("invalid encryption key length");
			}
			if (crypt.length < 0 || crypt.length > 256)
			{
				throw new Exception("invalid encryption key length");
			}
		}
	
		if (crypt.v == 5) {
			crypt.length = 256;
		}
	
		if (crypt.v == 1 || crypt.v == 2) {
			crypt.stmf.method = PdfCrypt.PDF_CRYPT_RC4;
			crypt.stmf.length = crypt.length;
	
			crypt.strf.method = PdfCrypt.PDF_CRYPT_RC4;
			crypt.strf.length = crypt.length;
		}
	
		if (crypt.v == 4 || crypt.v == 5) {
			crypt.stmf.method = PdfCrypt.PDF_CRYPT_NONE;
			crypt.stmf.length = crypt.length;
	
			crypt.strf.method = PdfCrypt.PDF_CRYPT_NONE;
			crypt.strf.length = crypt.length;
	
			obj = getDict(dict, "CF");
			if (isDict(obj)) {
				crypt.cf = keepObj(obj);
			}
			else {
				crypt.cf = null;
			}
	
			try {
				obj = getDict(dict, "StmF");
				if (isName(obj))
					parseCryptFilter(crypt.stmf, crypt, getName(obj));
	
				obj = getDict(dict, "StrF");
				if (isName(obj))
					parseCryptFilter(crypt.strf, crypt, getName(obj));
			}
			catch(Exception e)
			{
				throw new Exception("cannot parse string crypt filter (" + getNum(obj) + " " + getGen(obj) + " R)");
			}
	
			/* in crypt revision 4, the crypt filter determines the key length */
			if (crypt.strf.method != PdfCrypt.PDF_CRYPT_NONE)
				crypt.length = crypt.stmf.length;
		}
		return crypt;
	}

	/*
	 * Parse a CF dictionary entry (PDF 1.7 table 3.22)
	 */

	private void parseCryptFilter(PdfCryptFilter cf, PdfCrypt crypt, String name) throws Exception {
		PdfObject obj;
		PdfObject dict;
		boolean isIdentity = "Identity".equals("Identity");
		boolean isStdcf = (!isIdentity && "StdCF".equals(name));

		if (isIdentity == false && isStdcf == false) {
			throw new Exception("Crypt Filter not Identity or StdCF (" + getNum(crypt.cf) + " " + getGen(crypt.cf) + " R)");
		}

		cf.method = PdfCrypt.PDF_CRYPT_NONE;
		cf.length = crypt.length;

		if (crypt.cf == null)
		{
			cf.method = (isIdentity ? PdfCrypt.PDF_CRYPT_NONE : PdfCrypt.PDF_CRYPT_RC4);
			return;
		}

		dict = getDict(crypt.cf, name);
		if (!isDict(dict)) {
			throw new Exception("cannot parse crypt filter (" + getNum(crypt.cf) + " " + getGen(crypt.cf) + " R)");
		}

		obj = getDict(dict, "CFM");
		if (isName(obj)) {
			if ("None".equals(getName(obj))) {
				cf.method = PdfCrypt.PDF_CRYPT_NONE;
			}
			else if ("V2".equals(getName(obj))) {
				cf.method = PdfCrypt.PDF_CRYPT_RC4;
			}
			else if ("AESV2".equals(getName(obj))) {
				cf.method = PdfCrypt.PDF_CRYPT_AESV2;
			}
			else if ("AESV3".equals(getName(obj))) {
				cf.method = PdfCrypt.PDF_CRYPT_AESV3;
			}
			else {
				throw new Exception("unknown encryption method: " + getName(obj));
			}
		}

		obj = getDict(dict, "Length");
		if (isInt(obj)) {
			cf.length = getInt(obj);
		}

		/* the length for crypt filters is supposed to be in bytes not bits */
		if (cf.length < 40) {
			cf.length = cf.length * 8;
		}

		if ((cf.length % 8) != 0)
			throw new Exception("invalid key length: " + cf.length);

		if ((crypt.r == 1 || crypt.r == 2 || crypt.r == 4) && (cf.length < 0 || cf.length > 256)) {
			throw new Exception("invalid key length: " + cf.length);
		}
		if (crypt.r == 5 && (cf.length != 128 && cf.length != 192 && cf.length == 256)) {
			throw new Exception("invalid key length: " + cf.length);
		}
	}
	
	/*
	 * Entry points
	 */
	
	private void runContentsStream(PdfObject rdb) throws Exception
	{
		LexData buf = new LexData(); /* we must be re-entrant for type3 fonts */;

//		runStream(rdb, buf);
		runStreamFast(rdb, buf);
	}

	//	private PdfImage loadImage(PdfObject rdb, PdfObject dict, int forcemask)
//	{
////		Stream stm = null;
//		PdfImage image = null;
//		PdfObject obj;
//		PdfObject res;
//
//		int w, h, bpc, n;
//		boolean imagemask;
//		boolean interpolate;
//		boolean indexed;
//		FzImage mask = null; /* explicit mask/softmask image */
//		int usecolorkey;
//
//		int i;
////		fz_context *ctx = xref->ctx;
//
////		fz_var(stm);
/////		fz_var(mask);
//
//		image = new PdfImage();
//
//		try {
//			/* special case for JPEG2000 images */
//			if (isJpxImage(dict)) {
//				loadJpx(dict, image);
//
////				if (forcemask)
////				{
////					fz_pixmap *mask_pixmap;
////					if (image.n != 2) {
////						throw new Exception("softmask must be grayscale");
////					}
////					mask_pixmap = fz_alpha_from_gray(ctx, image.tile, 1);
////					fz_drop_pixmap(ctx, image.tile);
////					image.tile = mask_pixmap;
////			}
//				break; /* Out of fz_try */
//			}
//
//			w = getInt(getDictSa(dict, "Width", "W"));
//			h = getInt(getDictSa(dict, "Height", "H"));
//			bpc = getInt(getDictSa(dict, "BitsPerComponent", "BPC"));
//			imagemask = getBool(getDictSa(dict, "ImageMask", "IM"));
//			interpolate = getBool(getDictSa(dict, "Interpolate", "I"));
//
//			indexed = false;
//			usecolorkey = 0;
//			mask = null;
//
//			if (imagemask) {
//				bpc = 1;
//			}
//
//			if (w <= 0) {
//				throw new Exception("image width is zero (or less)");
//			}
//			if (h <= 0) {
//				throw new Exception("image height is zero (or less)");
//			}
//			if (bpc <= 0) {
//				throw new Exception("image depth is zero (or less)");
//			}
//			if (bpc > 16) {
//				throw new Exception("image depth is too large: " + bpc);
//			}
//			if (w > (1 << 16)) {
//				throw new Exception("image is too wide");
//			}
//			if (h > (1 << 16)) {
//				throw new Exception("image is too high");
//			}
//
//			obj = getDictSa(dict, "ColorSpace", "CS");
//			if (obj != null && !imagemask && forcemask == 0) {
//				/* colorspace resource lookup is only done for inline images */
//				if (isName(obj)) {
//					res = getDict(getDict(rdb, "ColorSpace"), obj.getName());
//					if (res != null) {
//						obj = res;
//					}
//				}
//
//				image.base.colorspace = loadColorspace(obj);
//				if ("Indexed".equals(image.base.colorspace.name)) {
//					indexed = true;
//				}
//				n = image.base.colorspace.n;
//			}
//			else {
//				n = 1;
//			}
//
//			obj = getDictSa(dict, "Decode", "D");
//			if (obj != null)
//			{
//				for (i = 0; i < n * 2; i++)
//					image.decode[i] = getReal(getArray(obj, i));
//			}
//			else
//			{
//				float maxval = indexed ? (1 << bpc) - 1 : 1;
//				for (i = 0; i < n * 2; i++)
//					image.decode[i] = ((i & 0x01) != 0 ? maxval : 0);
//			}
//
//			obj = getDictSa(dict, "SMask", "Mask");
//			if (isDict(obj))
//			{
//				/* Not allowed for inline images */
////				if (!cstm)
////				{
//					mask = (FzImage)pdf_load_image_imp(rdb, obj, null, 1);
////				}
//			}
//			else if (isArray(obj)) {
//				usecolorkey = 1;
//				for (i = 0; i < n * 2; i++) {
//					if (!isInt(getArray(obj, i))) {
//						Log.w("loadImage", "invalid value in color key mask");
//						usecolorkey = 0;
//					}
//					image.colorkey[i] = getInt(getArray(obj, i));
//				}
//			}
//
//			/* Now, do we load a ref, or do we load the actual thing? */
//			image.params.type = PdfImageParams.PDF_IMAGE_RAW;
//			// FZ_INIT_STORABLE(&image->base, 1, pdf_free_image);
//			// image.base.get_pixmap = pdf_image_get_pixmap;
//			image.base.w = w;
//			image.base.h = h;
//			image.n = n;
//			image.bpc = bpc;
//			image.interpolate = interpolate;
//			image.imagemask = imagemask;
//			image.usecolorkey = usecolorkey;
//			image.base.mask = mask;
//			image.params.colorspace = image.base.colorspace; /* Uses the same ref as for the base one */
//			if (!indexed/* && !cstm */) {
//				/* Just load the compressed image data now and we can
//				 * decode it on demand. */
//				int num = dict.getNum();
//				int gen = dict.getGen();
//				image.buffer = loadImageStream(num, gen, num, gen, image.params);
//				break; /* Out of fz_try */
//			}
//
//			/* We need to decompress the image now */
//			if (cstm != null)
//			{
//				int stride = (w * image->n * image->bpc + 7) / 8;
//				stm = pdf_open_inline_stream(xref, dict, stride * h, cstm, NULL);
//			}
//			else
//			{
//				stm = pdf_open_stream(xref, pdf_to_num(dict), pdf_to_gen(dict));
//			}
//
//			image->tile = decomp_image_from_stream(ctx, stm, image, cstm != NULL, indexed, 1, 0);
//		return image;
//	}
//
//	private byte[] loadImageStream(int num, int gen, int orig_num, int orig_gen, PdfImageParams params) {
//		PdfObject dict;
//		PdfObject obj;
//		int i, len, n;
//		byte buf[];
//
//		if (num > 0 && num < mTable.length && mTable[num].stm_buf != null)
//			return fz_keep_buffer(xref->ctx, xref->table[num].stm_buf);
//
//		dict = loadObject(num, gen);
//
//		len = getInt(getDict(dict, "Length"));
//		obj = getDict(dict, "Filter");
//		len = guessFilterLength(len, getName(obj));
//		n = getArrayLen(obj);
//		for (i = 0; i < n; i++) {
//			len = guessFilterLength(len, getName(getArray(obj, i)));
//		}
//
//		stm = pdf_open_image_stream(num, gen, orig_num, orig_gen, params);
//
//		fz_try(ctx)
//		{
//			buf = fz_read_all(stm, len);
//		}
//		fz_always(ctx)
//		{
//			fz_close(stm);
//		}
//		fz_catch(ctx)
//		{
//			fz_throw(ctx, "cannot read raw stream (%d %d R)", num, gen);
//		}
//
//		return buf;
//	}

	// 共通処理
	protected int getInt(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null) {
			return obj.getInt();
		}
		return 0;
	}

	protected int getInt(PdfObject obj, int defVal) {
		obj = resolv(obj);
		if (obj != null) {
			return obj.getInt();
		}
		return defVal;
	}

	protected int getReal(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null) {
			return obj.getInt();
		}
		return 0;
	}

	protected boolean getBool(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null) {
			return obj.getBool();
		}
		return false;
	}

	protected boolean getBool(PdfObject obj, boolean defVal) {
		obj = resolv(obj);
		if (obj != null) {
			return obj.getBool();
		}
		return defVal;
	}

	protected String getName(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null) {
			return obj.getName();
		}
		return null;
	}

	protected byte[] getStringBuf(PdfObject obj, int length) {
		obj = resolv(obj);
		if (obj != null) {
			byte buff[] = new byte[length];
			System.arraycopy(obj.s, 0, buff, 0, length);
			return buff;
		}
		return null;
	}

	protected int getStringLen(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_STRING) {
			return obj.s.length;
		}
		return 0;
	}

	private PdfObject getDict(PdfObject obj, String key) {
		obj = resolv(obj);
		if (obj != null) {
			return obj.getDict(key);
		}
		return null;
	}

	// なければもう一個のパラメタで取得
	private PdfObject getDictSa(PdfObject obj, String key, String abbrev) {
		PdfObject v;
		v = getDict(obj, key);
		if (v != null) {
			return v;
		}
		return getDict(obj, abbrev);
	}

	private PdfObject getDictObject(PdfObject obj)
	{
		obj = resolv(obj);
		return (obj != null && obj.kind == PdfObject.PDF_DICT ? obj : null);
	}

	protected PdfObject getArray(PdfObject obj, int index) {
		obj = resolv(obj);
		if (obj != null) {
			return obj.getArray(index);
		}
		return null;
	}

	private int getNum(PdfObject obj) {
//		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_INDIRECT) {
			return obj.getNum();
		}
		return 0;
	}

	private int getGen(PdfObject obj) {
//		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_INDIRECT) {
			return obj.getGen();
		}
		return 0;
	}

	protected boolean isBool(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_BOOL) {
			return true;
		}
		return false;
	}

	protected boolean isInt(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_INT) {
			return true;
		}
		return false;
	}

	protected boolean isReal(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_REAL) {
			return true;
		}
		return false;
	}

	protected boolean isName(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_NAME) {
			return true;
		}
		return false;
	}

	protected boolean isString(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_STRING) {
			return true;
		}
		return false;
	}

	protected boolean isArray(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_ARRAY) {
			return true;
		}
		return false;
	}

	protected boolean isDict(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_DICT) {
			return true;
		}
		return false;
	}

	protected boolean isIndirect(PdfObject obj) {
//		obj = resolv(obj);
		if (obj != null && obj.kind == PdfObject.PDF_INDIRECT) {
			return true;
		}
		return false;
	}

	protected PdfObject keepObj(PdfObject obj) {
//		obj = resolv(obj);
		if (obj != null) {
			obj.incRefs();
		}
		return obj;
	}

	private Set<String> getDictKeys(PdfObject obj) {
		if (obj != null) {
			return obj.getDictKeys();
		}
		return null;
	}

	private int markDict(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind != PdfObject.PDF_DICT) {
			return 0;
		}
		int result = obj.mark;
		obj.mark ++;
		return result;
	}

	private void unmarkDict(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind != PdfObject.PDF_DICT) {
			return;
		}
		obj.mark = 0;
		return;
	}


	private int getArrayLen(PdfObject obj) {
		obj = resolv(obj);
		if (obj != null && obj.kind != PdfObject.PDF_ARRAY) {
			return 0;
		}
		return obj.ary.size();
	}

//	private int getDictLen(PdfObject obj) {
//		obj = resolv(obj);
//		if (obj != null && obj.kind != PdfObject.PDF_DICT) {
//			return 0;
//		}
//		return obj.dict.size();
//	}

	private void putMarkerBool(PdfObject obj, String marker, boolean val)
	{
		obj = resolv(obj);
		obj.addDict(marker, new PdfObject(val));
	}

	private boolean isStream(int num, int gen) throws Exception {
		if (num < 0 || num >= mTable.length) {
			return false;
		}
		cacheObject(num, gen);

		return (mTable[num].stmofs != 0/* || mTable[num].stmbuf != null*/);
	}

	private void keepStream() {

	}

	private void releaseStream() {

	}

	// 読み込み処理中断
	public void setBreakTrigger() {
		mRunningFlag = false;
		return;
	}
}
