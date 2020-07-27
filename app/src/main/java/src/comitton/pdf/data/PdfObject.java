package src.comitton.pdf.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import android.annotation.SuppressLint;

@SuppressLint({ "UseValueOf", "UseValueOf" })
public class PdfObject {
	public static final int PDF_NULL     = 0;
	public static final int PDF_BOOL     = 1;
	public static final int PDF_INT      = 2;
	public static final int PDF_REAL     = 3;
	public static final int PDF_STRING   = 4;
	public static final int PDF_NAME     = 5;
	public static final int PDF_ARRAY    = 6;
	public static final int PDF_DICT     = 7;
	public static final int PDF_INDIRECT = 8;

	public byte kind;
	public int  refs;
	public Boolean bl;
	public Float f;
	public Integer i;
	public Integer r_num;
	public Integer r_gen;
	public String b;
	public byte s[];
	public ArrayList<PdfObject> ary;
	public HashMap<String,PdfObject> dict;

	public int mark;
	
	public PdfObject() {
		refs = 1;
		kind = PDF_NULL;
	}

	public PdfObject(boolean val) {
		refs = 1;
		kind = PDF_BOOL;
		bl = val;
	}

	public PdfObject(int val) {
		refs = 1;
		kind = PDF_INT;
		i = val;
	}

	public PdfObject(String val) {
		refs = 1;
		kind = PDF_NAME;
		b = val;
	}


	public PdfObject(byte val[]) {
		refs = 1;
		kind = PDF_STRING;
		s = val;
	}

	public PdfObject(float val) {
		refs = 1;
		kind = PDF_REAL;
		f = val;
	}

	public PdfObject(int num, int gen) {
		refs = 1;
		kind = PDF_INDIRECT;
		r_num = new Integer(num);
		r_gen = new Integer(gen);
	}

	public PdfObject(ArrayList<PdfObject> a) {
		refs = 1;
		kind = PDF_ARRAY;
		ary = a;
	}

	public void incRefs() {
		refs ++;
	}

	public void addArray(PdfObject obj) {
		if (kind == PDF_ARRAY) {
			ary.add(obj);
		}
	}

	public PdfObject getArray(int index) {
		if (kind == PDF_ARRAY) {
			return ary.get(index);
		}
		return null;
	}

	public PdfObject(HashMap<String, PdfObject> h) {
		refs = 1;
		kind = PDF_DICT;
		dict = h;
		mark = 0;
	}

	// 辞書追加
	public void addDict(String keystr, PdfObject val) {
		if (kind == PDF_DICT) {
			dict.put(keystr, val);
		}
	}

	public void addDict(PdfObject key, PdfObject val) {
		if (kind == PDF_DICT) {
			String keystr = key.getName();
			dict.put(keystr, val);
		}
	}

	// 辞書参照
	public PdfObject getDict(String key) {
		if (kind != PDF_DICT || dict == null) {
			return null;
		}
		return dict.get(key);
	}

	public String getName() {
		if (kind == PDF_NAME) {
			return new String(b);
		}
		return "";
	}

	public String getString() {
		if (kind == PDF_NAME) {
			return new String(b);
		}
		return "";
	}

	public int getInt() {
		if (kind == PDF_INT && i != null) {
			return i;
		}
		else if (kind == PDF_REAL && f != null) {
			return (int)(f + 0.5f);
		}
		return 0;
	}

	public Set<String> getDictKeys() {
		if (kind != PDF_DICT || dict == null) {
			return null;
		}
		return dict.keySet();
	}
	
	public boolean getBool() {
		if (kind == PDF_BOOL && bl != null) {
			return bl;
		}
		return false;
	}

	public int getNum() {
		if (kind == PDF_INDIRECT && r_num != null) {
			return r_num;
		}
		return 0;
	}

	public int getGen() {
		if (kind == PDF_INDIRECT && r_gen != null) {
			return r_gen;
		}
		return 0;
	}
}
