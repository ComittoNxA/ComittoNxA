package src.comitton.pdf.data;


public class XrefData {
	public char    type;
	public int     ofs;
	public int     gen;
	public int     stmofs;
	public byte    stmbuf[];
	public PdfObject obj;

	public XrefData() {
		type = 0;
		ofs = 0;
		gen = 0;
		obj = null;
	}
}
