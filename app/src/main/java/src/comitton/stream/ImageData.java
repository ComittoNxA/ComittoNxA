package src.comitton.stream;

public class ImageData {
	public static int HALF_NONE  = 0;
	public static int HALF_LEFT  = 1;
	public static int HALF_RIGHT = 2;
	
	public ImageData() {
		Page = -1;
		Width = 0;
		Height = 0;
		SclWidth = 0;
		SclHeight = 0;
		FitWidth = 0;
		FitHeight = 0;
		HalfMode = HALF_NONE;
	}

    public int Page;
    public int Width;
    public int Height;
    public int SclWidth;
    public int SclHeight;
    public int FitWidth;
    public int FitHeight;
    public int HalfMode;
}
