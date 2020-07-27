package src.comitton.pdf.data;


public class PdfCrypt {
	public static final int PDF_CRYPT_NONE    = 0;
	public static final int PDF_CRYPT_RC4     = 1;
	public static final int PDF_CRYPT_AESV2   = 2;
	public static final int PDF_CRYPT_AESV3   = 3;
	public static final int PDF_CRYPT_UNKNOWN = 4;

	public class PdfCryptFilter
	{
		public int method;
		public int length;
	}

	public PdfObject id;

	public int v;
	public int length;
	public PdfObject cf;
	public PdfCryptFilter stmf = new PdfCryptFilter();
	public PdfCryptFilter strf = new PdfCryptFilter();

	public int r;
	public byte o[] = new byte[48];
	public byte u[] = new byte[48];
	public byte oe[] = new byte[32];
	public byte ue[] = new byte[32];
	public int p;
	public boolean encrypt_metadata;

	public byte key[] = new byte[32]; /* decryption key generated from password */
}
