package src.comitton.pdf.data;

public class PdfImageParams {
	public static final int PDF_IMAGE_NONE  = 0;
	public static final int PDF_IMAGE_RAW   = 1;
	public static final int PDF_IMAGE_FAX   = 2;
	public static final int PDF_IMAGE_JPEG  = 3;
	public static final int PDF_IMAGE_RLD   = 4;
	public static final int PDF_IMAGE_FLATE = 5;
	public static final int PDF_IMAGE_LZW   = 6;
	public static final int PDF_IMAGE_JPX   = 7;

	public int type;
	public ColorSpace colorspace;
	private Object union;

	public PdfImageParams () {
		type = PDF_IMAGE_NONE;
	}

	public void setData(ImageFax imagefax) {
		union = imagefax;
		type = PDF_IMAGE_FAX;
	}

	public void setData(ImageJpeg imagejpeg) {
		union = imagejpeg;
		type = PDF_IMAGE_JPEG;
	}

	public void setData(ImageFlate imageflate) {
		union = imageflate;
		type = PDF_IMAGE_FLATE;
	}

	public void setData(ImageLzw imagelzw) {
		union = imagelzw;
		type = PDF_IMAGE_LZW;
	}

	public void setType(int t) {
		union = null;
		type = t;
	}

	public ImageFax getFaxData() {
		if (type != PDF_IMAGE_FAX) {
			return null;
		}
		return (ImageFax)union;
	}

	public ImageJpeg getJpegData() {
		if (type != PDF_IMAGE_JPEG) {
			return null;
		}
		return (ImageJpeg)union;
	}

	public ImageFlate getFlateData() {
		if (type != PDF_IMAGE_FLATE) {
			return null;
		}
		return (ImageFlate)union;
	}

	public ImageLzw getLzwData() {
		if (type != PDF_IMAGE_LZW) {
			return null;
		}
		return (ImageLzw)union;
	}
}
