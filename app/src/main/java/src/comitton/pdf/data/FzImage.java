package src.comitton.pdf.data;

public class FzImage {
	// fz_storable storable;
	public int w;
	public int h;
	public FzImage mask;
	public ColorSpace colorspace;
	// FzPixmap *(*get_pixmap)(fz_context *, fz_image *, int w, int h);
	public static final int FZ_MAX_COLORS = 32;
}
