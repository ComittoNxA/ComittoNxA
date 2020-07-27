package src.comitton.pdf.data;

import android.annotation.SuppressLint;

@SuppressLint("UseValueOf")
public class LexData {
	public final short TYPE_NONE   = 0;
	public final short TYPE_INT    = 1;
	public final short TYPE_FLOAT  = 2;
	public final short TYPE_STRING = 3;
	public final short TYPE_BUFFER = 4;

	public short   type;
	public Integer i;
	public Float   f;
	public byte[]  s;
	public String  b;

	@SuppressLint({ "UseValueOf", "UseValueOf" })
	public void setValue(int val) {
		clear();
		i = new Integer(val);
		type = TYPE_INT;
	}

	public void setValue(float val) {
		clear();
		f = new Float(val);
		type = TYPE_FLOAT;
	}

	public void setValue(byte buf[], int length) {
		clear();
		s = new byte[length]; 
		System.arraycopy(buf, 0, s, 0, length);
		type = TYPE_STRING;
	}
	public void setValue(String name) {
		clear();
		b = name;
		type = TYPE_BUFFER;
	}

	public void clear() {
		type = TYPE_NONE;
		i = null;
		f = null;
		s = null;
		b = null;
	}

	public String getName() {
		return b;
	}

	//	public String getString() {
//		String result = "";
//
//		if (b != null) {
//			try {
//				result = new String(b, "Shift_JIS");
//			}
//			catch (Exception e) {
//				;
//			}
//		}
//		return result;
//	}
}
