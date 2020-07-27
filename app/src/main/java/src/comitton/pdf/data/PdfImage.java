package src.comitton.pdf.data;

public class PdfImage {
	public static final int FZ_MAX_COLORS = 32;

	public FzImage base;
	public int n, bpc;
	public PdfImageParams params;
	public byte buffer[];
	public int colorkey[] = new int [FZ_MAX_COLORS * 2];
	public float decode[] = new float [FZ_MAX_COLORS * 2];
	public boolean imagemask;
	public boolean interpolate;
	public int usecolorkey;
}
