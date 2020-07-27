
package src.comitton.pdf.data;

public class PdfImageRef {
	public static final int PDFIMAGE_JPEG = 0;
	public static final int PDFIMAGE_FAX = 1;
	public static final int PDFIMAGE_FLATE = 2;

	public int type;
	public int offset;
	public int length;
	public int num;
	public int gen;
	public int hascrypt;
	public int params[];

	public PdfImageRef(int t, int o, int l, int n, int g, int h, int p[]) {
		type = t;
		offset = o;
		length = l;
		num = n;
		gen = g;
		hascrypt = h;
		params = p;
	}
}
