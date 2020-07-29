package src.comitton.common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;

public class DEF {
	public static final String DOWNLOAD_URL = "https://docs.google.com/open?id=0Bzx6UxEo3Pg0SXNIQVdRVnVqemM";
	public static final int MESSAGE_FILE_DELETE = 1000;
	public static final int MESSAGE_RECORD_DELETE = 1001;
	public static final int MESSAGE_LASTPAGE = 1002;
	public static final int MESSAGE_SHORTCUT = 1003;
	public static final int MESSAGE_EDITSERVER = 1004;
	public static final int MESSAGE_DOWNLOAD = 1005;
	public static final int MESSAGE_MARKER = 1006;
	public static final int MESSAGE_CLOSE = 1007;
	public static final int MESSAGE_RESUME = 1008;
	public static final int MESSAGE_RENAME = 1010;
	public static final int MESSAGE_LISTMODE = 1011;
	public static final int MESSAGE_SORT = 1012;
	public static final int MESSAGE_FILE_LONGCLICK = 1013;
	public static final int MESSAGE_RECORD_LONGCLICK = 1014;
	public static final int MESSAGE_FILE_RENAME = 1015;
	public static final int MESSAGE_MOVE_PATH_EROOR = 1016;

	public static final int HMSG_LOAD_END = 1;
	public static final int HMSG_READ_END = 2;
	public static final int HMSG_PROGRESS = 3;
	public static final int HMSG_ERROR = 4;
	public static final int HMSG_CACHE = 5;
	public static final int HMSG_LOADING = 6;
	public static final int HMSG_NOISE = 7;
	public static final int HMSG_NOISESTATE = 8;
	public static final int HMSG_THUMBNAIL = 9;
	public static final int HMSG_LOADFILELIST = 10;
	public static final int HMSG_UPDATEFILELIST = 11;
	public static final int HMSG_DRAWENABLE = 13;
	public static final int HMSG_TX_PARSE = 14;
	public static final int HMSG_TX_LAYOUT = 15;

	public static final int MENU_HELP = Menu.FIRST + 0;
	public static final int MENU_SETTING = Menu.FIRST + 1;
	public static final int MENU_SIORI = Menu.FIRST + 2;
	public static final int MENU_ABOUT = Menu.FIRST + 3;
	public static final int MENU_DISPDUAL = Menu.FIRST + 4;
	public static final int MENU_DISPHALF = Menu.FIRST + 5;
	public static final int MENU_DISPNORM = Menu.FIRST + 6;
	public static final int MENU_SHORTCUT = Menu.FIRST + 7;
	public static final int MENU_SERVER = Menu.FIRST + 8;
	public static final int MENU_REFRESH = Menu.FIRST + 9;
	public static final int MENU_NOISE = Menu.FIRST + 10;
	public static final int MENU_QUIT = Menu.FIRST + 11;
	public static final int MENU_THUMBDEL = Menu.FIRST + 12;
	public static final int MENU_ONLINE = Menu.FIRST + 13;
	public static final int MENU_MARKER = Menu.FIRST + 14;
	public static final int MENU_SHARE = Menu.FIRST + 15;
	public static final int MENU_SHARER = Menu.FIRST + 16;
	public static final int MENU_SHAREL = Menu.FIRST + 17;
	public static final int MENU_DELSHARE = Menu.FIRST + 18;
	public static final int MENU_REVERSE = Menu.FIRST + 19;
	public static final int MENU_ROTATE = Menu.FIRST + 20;
	public static final int MENU_CHG_OPE = Menu.FIRST + 21;
	public static final int MENU_PAGEWAY = Menu.FIRST + 22;
	public static final int MENU_SCRLWAY = Menu.FIRST + 23;
	public static final int MENU_THUMBSWT = Menu.FIRST + 24;
	public static final int MENU_IMGCONF = Menu.FIRST + 25;
	public static final int MENU_IMGROTA = Menu.FIRST + 26;
	public static final int MENU_IMGVIEW = Menu.FIRST + 27;
	public static final int MENU_IMGSIZE = Menu.FIRST + 28;
	public static final int MENU_PAGESEL = Menu.FIRST + 29;
	public static final int MENU_SHARPEN = Menu.FIRST + 30;
	public static final int MENU_INVERT = Menu.FIRST + 31;
	public static final int MENU_MGNCUT = Menu.FIRST + 32;
	public static final int MENU_GRAY = Menu.FIRST + 33;
	public static final int MENU_NOTICE = Menu.FIRST + 34;
	public static final int MENU_ADDBOOKMARK = Menu.FIRST + 35;
	public static final int MENU_SELBOOKMARK = Menu.FIRST + 36;
	public static final int MENU_SORT = Menu.FIRST + 37;
	public static final int MENU_ADDDIR = Menu.FIRST + 38;
	public static final int MENU_COLORING = Menu.FIRST + 39;
	public static final int MENU_LISTMODE = Menu.FIRST + 40;
	public static final int MENU_AUTOPLAY = Menu.FIRST + 41;
	public static final int MENU_TOP_SETTING = Menu.FIRST + 42;
	public static final int MENU_SELCHAPTER = Menu.FIRST + 43;
	public static final int MENU_SEARCHTEXT = Menu.FIRST + 44;
	public static final int MENU_SEARCHJUMP = Menu.FIRST + 45;
	public static final int MENU_IMGALGO = Menu.FIRST + 46;
	public static final int MENU_TXTCONF = Menu.FIRST + 47;
	public static final int MENU_BOOKMARK = Menu.FIRST + 1000;
	public static final int MENU_CHAPTER = Menu.FIRST + 2000;

	public static final int MENU_CMARGIN = Menu.FIRST + 101;
	public static final int MENU_CSHADOW = Menu.FIRST + 102;
	public static final int MENU_SETTHUMB = Menu.FIRST + 103;
	public static final int MENU_SETTHUMBCROPPED = Menu.FIRST + 104;

	public static final int REQUEST_SETTING = 101;
	public static final int REQUEST_FILE = 102;
	public static final int REQUEST_HELP = 103;
	public static final int REQUEST_SERVER = 104;
	public static final int REQUEST_IMAGE = 105;
	public static final int REQUEST_TEXT = 106;
	public static final int REQUEST_EXPAND = 107;
	public static final int REQUEST_RECORD = 108;
	public static final int REQUEST_CROP = 1000;

	public static final int VIEWPT_RIGHTTOP = 0;
	public static final int VIEWPT_LEFTTOP = 1;
	public static final int VIEWPT_RIGHTBTM = 2;
	public static final int VIEWPT_LEFTBTM = 3;
	public static final int VIEWPT_CENTER = 4;

	public static final int ZOOMTYPE_ORIG10 = 0;
	public static final int ZOOMTYPE_ORIG15 = 1;
	public static final int ZOOMTYPE_ORIG20 = 2;
	public static final int ZOOMTYPE_DISP15 = 3;
	public static final int ZOOMTYPE_DISP20 = 4;
	public static final int ZOOMTYPE_DISP25 = 5;
	public static final int ZOOMTYPE_DISP30 = 6;

	public static final int TOOLBAR_NONE = -1;
	public static final int TOOLBAR_PARENT = 0;
	public static final int TOOLBAR_REFRESH = 1;
	public static final int TOOLBAR_THUMBNAIL = 2;
	public static final int TOOLBAR_MARKER = 3;
	//	public static final int TOOLBAR_SERVER = 4;
	public static final int TOOLBAR_ADDDIR = 4;
	public static final int TOOLBAR_EXIT = 5;

	// ページめくり表示方向
	public static final int PAGEWAY_RIGHT = 0; // 右から左
	public static final int PAGEWAY_LEFT = 1; // 左から右

	// スクロール方向
	public static final int SCRLWAY_H = 0; // 横→縦
	public static final int SCRLWAY_V = 1; // 縦→横

	// 拡大方法
	public static final int SCALE_ORIGINAL = 0;
	public static final int SCALE_FIT_WIDTH = 1;
	public static final int SCALE_FIT_HEIGHT = 2;
	public static final int SCALE_FIT_ALL = 3;
	public static final int SCALE_FIT_ALLMAX = 4;
	public static final int SCALE_FIT_SPRMAX = 5;
	public static final int SCALE_FIT_WIDTH2 = 6;
	public static final int SCALE_FIT_ALL2 = 7;
	public static final int SCALE_PINCH = 8;

	// 最終ページの動作
	public static final int LASTMSG_CLOSE = 0;
	public static final int LASTMSG_DIALOG = 1;
	public static final int LASTMSG_NEXT = 2;

	// ソート方法
	public static final int ZIPSORT_NONE = 0; // ソートなし
	public static final int ZIPSORT_FILEMGR = 1; // ファイル名順(ディレクトリ混在)
	public static final int ZIPSORT_FILESEP = 2; // ファイル名順(ディレクトリ分離)
	public static final int ZIPSORT_NEWMGR = 3; // 新しい順(ディレクトリ混在)
	public static final int ZIPSORT_NEWSEP = 4; // 新しい順(ディレクトリ分離)
	public static final int ZIPSORT_OLDMGR = 5; // 古い順(ディレクトリ混在)
	public static final int ZIPSORT_OLDSEP = 6; // 古い順(ディレクトリ分離)

	// ファイル拡張子
	public static final String EXTENSION_SETTING = ".set";

	public static final String KEY_EXPORTSETTING = "ExportSetting";
	public static final String KEY_IMPORTSETTING = "ImportSetting";

	public static final String KEY_CONFHELP = "ConfHelp";
	public static final String KEY_FILEHELP = "FileHelp";
	public static final String KEY_FCLRHELP = "FClrHelp";
	public static final String KEY_IMAGEHELP = "ImageHelp";
	public static final String KEY_IDTLHELP = "IDtlHelp";
	public static final String KEY_COMMHELP = "CommHelp";
	public static final String KEY_NOISEHELP = "NoiseHelp";
	public static final String KEY_CACHEHELP = "CacheHelp";
	public static final String KEY_TEXTHELP = "TextHelp";
	public static final String KEY_IMTXHELP = "ImgTxtHelp";
	public static final String KEY_ITDTLHELP = "ImgTxtDtlHelp";
	public static final String KEY_ITCLRHELP = "ImgTxtClrHelp";
	public static final String KEY_RECHELP = "RecordHelp";

	public static final String KEY_CSTUPDATE = "CustomUpdate";

	public static final String KEY_LISTROTA = "ListRota";
	public static final String KEY_LISTSORT = "ListSort";
	public static final String KEY_FONTTITLE = "FontTitleSp";
	public static final String KEY_FONTMAIN = "FontMainSp";
	public static final String KEY_FONTSUB = "FontSubSp";
	public static final String KEY_FONTTILE = "FontTileSp";
	public static final String KEY_BKPARENT = "BackParent";
	public static final String KEY_ITEMMRGN = "ItemMarginSp";

	public static final String KEY_RDIRVIEW = "RDView";
	public static final String KEY_RBMVIEW = "RBView";
	public static final String KEY_RHISTVIEW = "RHView";
	public static final String KEY_SELECTOR = "Selector";
	public static final String KEY_RECLOCAL = "RHLocalRec";
	public static final String KEY_RECSAMBA = "RHSambaRec";

	public static final String KEY_PRESET = "Preset";

	public static final String KEY_TXTCOLOR = "TxtColor";
	public static final String KEY_DIRCOLOR = "DirColor";
	public static final String KEY_BEFCOLOR = "BefColor";
	public static final String KEY_NOWCOLOR = "NowColor";
	public static final String KEY_AFTCOLOR = "AftColor";
	public static final String KEY_IMGCOLOR = "ImgColor";
	public static final String KEY_INFCOLOR = "InfColor";
	public static final String KEY_BAKCOLOR = "BakColor";

	public static final String KEY_TXTRGB = "TxtRGB";
	public static final String KEY_DIRRGB = "DirRGB";
	public static final String KEY_BEFRGB = "BefRGB";
	public static final String KEY_NOWRGB = "NowRGB";
	public static final String KEY_AFTRGB = "AftRGB";
	public static final String KEY_IMGRGB = "ImgRGB";
	public static final String KEY_INFRGB = "InfRGB";
	public static final String KEY_MRKRGB = "MrkRGB";
	public static final String KEY_BAKRGB = "BakRGB";
	public static final String KEY_CURRGB = "CurRGB";
	public static final String KEY_TITRGB = "TitRGB";
	public static final String KEY_TIBRGB = "TibRGB";
	public static final String KEY_TLDRGB = "TldRGB";
	public static final String KEY_TLBRGB = "TlbRGB";

	// public static final String KEY_TITLECLR = "TitleColor";
	// public static final String KEY_TOOLBCLR = "ToolbarColor";

	public static final String KEY_MGNCOLOR = "MgnColor";
	public static final String KEY_CNTCOLOR = "CntColor";
	public static final String KEY_GUICOLOR = "GuiColor";

	public static final String KEY_MGNRGB = "MgnRGB";
	public static final String KEY_CNTRGB = "CntRGB";
	public static final String KEY_GUIRGB = "GuiRGB";

	public static final String KEY_ORGWIDTH = "OrgWidth";
	public static final String KEY_ORGHEIGHT = "OrgHeight";

	public static final String KEY_VIEWROTA = "ViewRota";
	public static final String KEY_FILESORT = "FileSort";
	public static final String KEY_VIEWPT = "ViewPt";
	public static final String KEY_INISCALE = "IniScale";

	public static final String KEY_CLICKAREA = "ClickAreaSp";
	public static final String KEY_PAGERANGE = "PageRangeSp";
	public static final String KEY_TAPRANGE = "TapRangeSp";
	public static final String KEY_VOLSCRL = "VolScrlSp";
	public static final String KEY_SCROLL = "Scroll";
	public static final String KEY_MARGIN = "Mergin";
	public static final String KEY_LONGTAP = "LongTap";
	public static final String KEY_WADJUST = "WAdjust";
	public static final String KEY_SCALING = "Scaling";
	public static final String KEY_CENTER = "Center";
	public static final String KEY_GRADATION = "Gradation";
	public static final String KEY_ZOOMTYPE = "ZoomType";
	public static final String KEY_SCRLRNGW = "ScrlRngW";
	public static final String KEY_SCRLRNGH = "ScrlRngH";
	public static final String KEY_CHARSET = "Charset";
	public static final String KEY_EFFECTTIME = "EffectTime";
	public static final String KEY_MOMENTMODE = "MomentMode";
	public static final String KEY_PAGESELECT = "PageSelect";
	public static final String KEY_AUTOPLAY = "AutoPlayInt";
	public static final String KEY_IMMENABLE = "ImmEnable";
	public static final String KEY_BOTTOMFILE = "BottomFile";
	public static final String KEY_PINCHENABLE = "PinchEnable";
	public static final String KEY_OLDMENU = "OldMenu";

	public static final String KEY_HIDDENFILE = "HiddenFile";

	public static final String KEY_NOISESCRL = "NoiseScrlSp";
	public static final String KEY_NOISEUNDER = "NoiseUnder";
	public static final String KEY_NOISEOVER = "NoiseOver";
	public static final String KEY_NOISELEVEL = "NoiseLevel";
	public static final String KEY_NOISEDEC = "NoiseDec";

	// public static final String KEY_TITLE = "Title";
	public static final String KEY_SHARPEN = "Sharpen";
	public static final String KEY_NOTICE = "Notice";
	public static final String KEY_NOSLEEP = "NoSleep";
	public static final String KEY_CHGPAGE = "ChgPage";
	public static final String KEY_CHGFLICK = "ChgFlick";
	public static final String KEY_ORGWIDTHTWICE = "OrgWidthTwice";
	public static final String KEY_REDUCE = "Reduce";
	public static final String KEY_MEMFREE = "MemFree";
	public static final String KEY_HALFHEIGHT = "HalfHeight";
	public static final String KEY_SCALEBMP = "ScaleBmp";
	public static final String KEY_BACKMODE = "BackMode";
	public static final String KEY_VOLKEY = "VolKey";
	public static final String KEY_SCRLWAY = "ScrlWay";
	public static final String KEY_THUMBNAIL = "Thumbnail";
	public static final String KEY_THUMBCACHE = "ThumbCache";
	public static final String KEY_ROTATEBTN = "RotateBtn";
	public static final String KEY_ACCESSLAMP = "AccessLamp";
	public static final String KEY_MARGINCUT = "MarginCut";
	public static final String KEY_TAPEXPAND = "TapExpand";
	public static final String KEY_TAPPATTERN = "TapPattern";
	public static final String KEY_PAGENUMBER = "PageNumber";
	public static final String KEY_TAPRATE = "TapRate";
	public static final String KEY_OLDPAGESEL = "OldPageSel";
	public static final String KEY_RESUMEOPEN = "ResumeOpen";
	public static final String KEY_CONFIRMBACK = "ConfirmBack";
	public static final String KEY_CLEARTOP = "ClearTop";
	public static final String KEY_HISTNUM = "RLSave3";
	public static final String KEY_EXTENSION = "Extension";
	public static final String KEY_THUMBSORT = "ThumbSort";
	public static final String KEY_PARENTMOVE = "ParentMove";
	public static final String KEY_FILEDELMENU = "FileDelMenu";
	public static final String KEY_FILERENMENU = "FileRenMenu";
	public static final String KEY_BRIGHT = "Bright";
	public static final String KEY_GAMMA = "Gamma";
	public static final String KEY_BKLIGHT = "BkLight";
	public static final String KEY_MOIRE = "Moire";
	public static final String KEY_GRAY = "Gray";
	public static final String KEY_INVERT = "Invert";
	public static final String KEY_COLORING = "Coloring";
	public static final String KEY_TOPSINGLE = "TopSingle";
	public static final String KEY_MAXTHREAD = "MaxThread";
	public static final String KEY_THUMBNAILTAP = "ThumbnailTap";
	public static final String KEY_LOUPESIZE = "LoupeSize";

	public static final String KEY_SHOWTOOLBAR = "ShowToolbar";
	public static final String KEY_SHOWSELECTOR = "ShowSelector";
	public static final String KEY_TOOLBARNAME = "ToolbarName";
	public static final String KEY_TOOLBARSEEK = "ToolbarSeek";
	public static final String KEY_LISTTHUMBSEEK = "ListThumbSeek";
	public static final String KEY_THUMBSEEK = "ThumbSeek";
	public static final String KEY_THUMBSIZEW = "ThumbSizeW";
	public static final String KEY_THUMBSIZEH = "ThumbSizeH";

	public static final String KEY_PNUMDISP = "PnumDisp";
	public static final String KEY_PNUMFORMAT = "PnumFormat";
	public static final String KEY_PNUMPOS = "PnumPos";
	public static final String KEY_PNUMSIZE = "PnumSizeSp";

	public static final String KEY_LASTPAGE = "LastPage";
	public static final String KEY_SAVEPAGE = "SavePage";
	public static final String KEY_PREVREV = "PrevRev";
	public static final String KEY_INITVIEW = "InitView";
	public static final String KEY_QUALITY = "Quality";
	public static final String KEY_PAGEWAY = "PageWay";
	public static final String KEY_FITDUAL = "FitDual";
	public static final String KEY_CMARGIN = "CMargin";
	public static final String KEY_CSHADOW = "CShadow";
	public static final String KEY_NOEXPAND = "NoExpand";
	public static final String KEY_VIBFLAG = "VibFlag";
	public static final String KEY_EFFECTLIST = "EffectList";
	public static final String KEY_DELSHARE = "DelShare";
	public static final String KEY_TAPSCRL = "TapScrl";
	public static final String KEY_FLICKPAGE = "FlickPage";
	public static final String KEY_FLICKEDGE = "FlickEdge";
	public static final String KEY_TOPMENU = "TopMenu";

	// スケーリング設定
	public static final String KEY_ALGOMODE = "AlgoMode";

	public static final String KEY_MEMSIZE = "MemSize";
	public static final String KEY_MEMNEXT = "MemNext";
	public static final String KEY_MEMPREV = "MemPrev";

	public static final int DEFAULT_INISCHALE = 5; //全体を表示(見開き対応)
	public static final int DEFAULT_INITVIEW = 1; //見開き表示
	public static final int DEFAULT_QUALITY = 1; //画質を優先する
	public static final int DEFAULT_CLICKAREA = 30; //上下の操作エリアサイズ:60sp
	public static final int DEFAULT_PAGERANGE = 5; //ページ選択の感度:1ページ/5sp
	public static final int DEFAULT_SCROLL = 2;
	public static final int DEFAULT_ORGWIDTH = 0;
	public static final int DEFAULT_ORGHEIGHT = 0;
	public static final int DEFAULT_TAPRANGE = 6;
	public static final int DEFAULT_MARGIN = 0;
	public static final int DEFAULT_LONGTAP = 4;
	public static final int DEFAULT_WADJUST = 25; // -25～+25(1%単位)
	public static final int DEFAULT_SCALING = 25; // -25～+25(1%単位)
	public static final int DEFAULT_CENTER = 2; // ドット
	public static final int DEFAULT_GRADATION = 5; // 0～30%(1%単位)
	public static final int DEFAULT_FONTTITLE = 10; // 28sp
	public static final int DEFAULT_FONTMAIN = 14; // 28sp
	public static final int DEFAULT_FONTSUB = 8; // 22sp
	public static final int DEFAULT_FONTTILE = 6; // 16sp
	public static final int DEFAULT_MEMSIZE = 18; // キャッシュサイズ:200MByte
	public static final int DEFAULT_MEMNEXT = 16; // キャッシュ前方:32ページ
	public static final int DEFAULT_MEMPREV = 8; // キャッシュ後方:16ページ
	public static final int DEFAULT_NOISESCRL = 1; // 20ドット
	public static final int DEFAULT_NOISEUNDER = 8; // 800
	public static final int DEFAULT_NOISEOVER = 15; // 1500
	public static final int DEFAULT_VOLSCRL = 28; // 32ドット
	public static final int DEFAULT_SCRLRNGW = 6; // 35% ((6+1)*5)
	public static final int DEFAULT_SCRLRNGH = 6; // 35% ((6+1)*5)
	public static final int DEFAULT_ITEMMARGIN = 10; // スクロール感度:10sp
	public static final int DEFAULT_EFFECTTIME = 5; // スクロール時間:250msec
	public static final int DEFAULT_MOMENTMODE = 8; // スクロール減速:フレーム1/8ずつ減速
	public static final int DEFAULT_AUTOPLAY = 2; // 1.5sec(0.5 * (5 + 1))
	public static final boolean DEF_SAVEPAGE = true; // ページ移動時にしおりを保存
	public static final int DEFAULT_TAPPATTERN = 0; // タッチ位置のパターン:左右分割
	public static final int DEFAULT_TAPRATE = 0; // 10% : 90%
	public static final boolean DEFAULT_CHGPAGE = true; // タップ操作の入替え:YES(縦書き、漫画)
	public static final boolean DEFAULT_PREVREV = true; // 前ページに戻った時に逆から表示

	public static final boolean DEFAULT_PNUMDISP = false; // ページ番号表示しない
	public static final int DEFAULT_PNUMFORMAT = 0; // page / total
	public static final int DEFAULT_PNUMPOS = 4; // 中央下
	public static final int DEFAULT_PNUMSIZE = 10; // 16px (8 + 6)

	public static final int DEFAULT_TOOLBARSEEK = 14; // 38 (8+24)
	public static final int DEFAULT_THUMBSIZEW = 23; // 270 (23 * 10 + 40)
	public static final int DEFAULT_THUMBSIZEH = 28; // 320 (28 * 10 + 40)
	public static final int DEFAULT_LISTTHUMBSIZEH = 20; // 240 (20 * 10 + 40)

	public static final int MAX_SCROLL = 9;
	public static final int MAX_CLICKAREA = 100;
	public static final int MAX_PAGERANGE = 25;
	public static final int MAX_ORGWIDTH = 32;
	public static final int MAX_ORGHEIGHT = 32;
	public static final int MAX_TAPRANGE = 25;
	public static final int MAX_MARGIN = 20;
	public static final int MAX_LONGTAP = 16;
	public static final int MAX_WADJUST = DEFAULT_WADJUST * 2;
	public static final int MAX_SCALING = DEFAULT_SCALING * 2;
	public static final int MAX_CENTER = 30; // 30ドット
	public static final int MAX_GRADATION = 20; // 20%
	public static final int MAX_FONTTITLE = 44; // 50ドット
	public static final int MAX_FONTMAIN = 44; // 50ドット
	public static final int MAX_FONTSUB = 44; // 50ドット
	public static final int MAX_FONTTILE = 44; // 50ドット
	public static final int MAX_MEMSIZE = 98; // 1000MByte
	public static final int MAX_MEMNEXT = 100; // 200ページ
	public static final int MAX_MEMPREV = 100; // 200ページ
	public static final int MAX_NOISESCRL = 39; // 200ドット
	public static final int MAX_NOISEUNDER = 50; // 5000
	public static final int MAX_NOISEOVER = 50; // 5000
	public static final int MAX_VOLSCRL = 39; // 200ドット
	public static final int MAX_SCRLRNGW = 19; // 100% ((19+1)*5)
	public static final int MAX_SCRLRNGH = 19; // 100% ((19+1)*5)
	public static final int MAX_ITEMMARGIN = 30; // 50ドット
	public static final int MAX_EFFECTTIME = 20; // 1000msec
	public static final int MAX_MOMENTMODE = 16; // 1000msec
	public static final int MAX_AUTOPLAY = 59; // 30sec (0.5*(59+1))

	public static final int MAX_TOOLBARSEEK = 36; // 60 (36+24)
	public static final int MAX_THUMBSIZE = 60; // 640 (60 * 10 + 40)
	public static final int MAX_LISTTHUMBSIZE = 60; // 640 (60 * 10 + 40)

	public static final int MAX_PNUMSIZE = 54; // 6 + 24 = 60px
	// テキストビュアー設定
	public static final String KEY_TX_INISCALE = "txIniScale";
	public static final String KEY_TX_INITVIEW = "txInitView";
	public static final String KEY_TX_VIEWROTA = "txViewRota";
	public static final String KEY_TX_PAPER = "txPaperSize";
	public static final String KEY_TX_PICSIZE = "txPicSize";
	public static final String KEY_TX_NOTICE = "txNotice";
	public static final String KEY_TX_NOSLEEP = "txNoSleep";
	public static final String KEY_TX_CMARGIN = "txCMargin";
	public static final String KEY_TX_CSHADOW = "txCShadow";
	public static final String KEY_TX_EFFECT = "txEffect";
	public static final String KEY_TX_FONTDL = "txFontDL";
	public static final String KEY_TX_FONTNAME = "txFontName";
	public static final String KEY_TX_PAGESELECT = "txPageSelect";
	public static final String KEY_TX_ASCMODE = "txAscMode";

	public static final String KEY_TX_MGNRGB = "txMgnRGB";
	public static final String KEY_TX_CNTRGB = "txCntRGB";
	public static final String KEY_TX_GUIRGB = "txGuiRGB";

	public static final String KEY_TX_TVTRGB = "txTvtRGB";
	public static final String KEY_TX_TVBRGB = "txTvbRGB";
	public static final String KEY_TX_TVGRGB = "txTvgRGB";
	public static final String KEY_TX_GRADATION = "txGradation";
	public static final String KEY_TX_HITRGB = "txHitRGB";

	public static final String KEY_TX_FONTTOP = "txFontTopSp";
	public static final String KEY_TX_FONTBODY = "txFontBodySp";
	public static final String KEY_TX_FONTRUBI = "txFontRubiSp";
	public static final String KEY_TX_FONTINFO = "txFontInfoSp";
	public static final String KEY_TX_SPACEW = "txSpaceW";
	public static final String KEY_TX_SPACEH = "txSpaceH";
	public static final String KEY_TX_MARGINW = "txMarginW";
	public static final String KEY_TX_MARGINH = "txMarginH";

	public static final String KEY_TX_SCRLRNGW = "txScrlRngW";
	public static final String KEY_TX_SCRLRNGH = "txScrlRngH";

	public static final String KEY_TX_BKLIGHT = "txBright";

	public static final int DEFAULT_TX_FONTTOP = 18; // 32(8～64)
	public static final int DEFAULT_TX_FONTBODY = 14; // 24(8～64)
	public static final int DEFAULT_TX_FONTRUBI = 6; // 16(8～64)
	public static final int DEFAULT_TX_FONTINFO = 10; // 18(8～64)
	public static final int DEFAULT_TX_SPACEW = 8; // 8(0～50)
	public static final int DEFAULT_TX_SPACEH = 2; // 8(0～50)
	public static final int DEFAULT_TX_MARGINW = 16; // 32(0～100,x2)
	public static final int DEFAULT_TX_MARGINH = 16; // 32(0～100,x2)
	public static final int DEFAULT_TX_SCRLRNGW = 6; // 35% ((6+1)*5)
	public static final int DEFAULT_TX_SCRLRNGH = 6; // 35% ((6+1)*5)

	public static final int MAX_TX_FONTTOP = 56; // 64ドット
	public static final int MAX_TX_FONTBODY = 56; // 64ドット
	public static final int MAX_TX_FONTRUBI = 56; // 64ドット
	public static final int MAX_TX_FONTINFO = 56; // 64ドット
	public static final int MAX_TX_SPACEW = 50; // 50ドット
	public static final int MAX_TX_SPACEH = 50; // 50ドット
	public static final int MAX_TX_MARGINW = 50; // 100ドット
	public static final int MAX_TX_MARGINH = 50; // 100ドット
	public static final int MAX_TX_SCRLRNGW = 19; // 100% ((19+1)*5)
	public static final int MAX_TX_SCRLRNGH = 19; // 100% ((19+1)*5)

	public static final int COLOR_TX_TVTRGB = 0xFF000000;
	public static final int COLOR_TX_TVBRGB = 0xFFFFFFC8;
	public static final int COLOR_TX_TVGRGB = 0xFFFFFFFF;
	public static final int COLOR_TX_HITRGB = 0xFFEBC5E2;

	public static final int VOLKEY_NONE = 0;
	public static final int VOLKEY_DOWNTONEXT = 1;
	public static final int VOLKEY_UPTONEXT = 2;

	// サムネイルサイズ
	public static final int THUMBID_NONE = -1;
	public static final int THUMBSIZE_NONE = -1;

	public static final int THUMBSTATE_NONE = -1;
	public static final int THUMBSTATE_NOLOAD = -2;
	public static final int THUMBSTATE_ERROR = -3;

	public static final int CACHE_MAXSTORE = 500;

	// 3秒以内にbackを押せば終了
	public static final long MILLIS_EXITTIME = 2000;
	public static final long MILLIS_DELETECHE = (10 * 60 * 1000); // 10分

	public static final int PAPERSEL_SCREEN = 0;
	public static final int PAPERSIZE[][] = { { 0, 0 }, { 800, 1280 }, { 720, 1280 }, { 540, 960 }, { 480, 800 } };

	public static final String URL_IMAGEDETIAL = "";
	public static final String URL_FILESELECT = "https://docs.google.com/document/d/197jmNnXY3BP4F9HHmJCuroWslbTFH8nWTBVWbn8T4dE/edit";
	public static final String URL_COMMON = "";
	public static final String URL_IMAGEVIEW = "";
	public static final String URL_SERVER = "";

	public static final int DISPMODE_TX_DUAL = 0;
	public static final int DISPMODE_TX_HALF = 1;
	public static final int DISPMODE_TX_SERIAL = 2;

	public static final int LASTOPEN_NONE = 0;
	public static final int LASTOPEN_TEXT = 1;
	public static final int LASTOPEN_IMAGE = 2;

	public static final int SHOWMENU_NONE = 0;
	public static final int SHOWMENU_ALWAYS = 1;
	public static final int SHOWMENU_LOCAL = 2;
	public static final int SHOWMENU_SERVER = 3;

	public static final int ColorList[] = { Color.rgb(0, 0, 0) // 0
			, Color.rgb(255, 255, 255) // 1
			, Color.rgb(0, 0, 255) // 2
			, Color.rgb(255, 0, 0) // 3
			, Color.rgb(255, 0, 255) // 4
			, Color.rgb(0, 255, 0) // 5
			, Color.rgb(255, 255, 0) // 6
			, Color.rgb(0, 255, 255) // 7
			, Color.rgb(129, 129, 129) // 8
			, Color.rgb(128, 128, 255) // 9
			, Color.rgb(255, 128, 128) // 10
			, Color.rgb(255, 128, 255) // 11
			, Color.rgb(128, 255, 128) // 12
			, Color.rgb(255, 255, 128) // 13
			, Color.rgb(128, 255, 255) // 14
			, Color.rgb(193, 193, 193) // 15
			, Color.rgb(0, 0, 97) // 16
			, Color.rgb(97, 0, 0) // 17
			, Color.rgb(97, 0, 97) // 18
			, Color.rgb(0, 97, 0) // 19
			, Color.rgb(97, 97, 0) // 20
			, Color.rgb(0, 97, 97) // 21
			, Color.rgb(97, 97, 97) }; // 22
	public static final int GuideList[] = { 0x80000000 // 0 : 黒
			, 0x80000070 // 1 : 青
			, 0x80700000 // 2 : 赤
			, 0x80700070 // 3 : マゼンタ
			, 0x80007000 // 4 : 緑
			, 0x80707000 // 5 : 黄
			, 0x80007070 }; // 6 : シアン

	public static final int RotateBtnList[] = { 0, KeyEvent.KEYCODE_FOCUS // フォーカスキー
			, KeyEvent.KEYCODE_CAMERA // シャッターキー
	};
	public static final String CharsetList[] = {"UTF-8", "Shift_JIS", "EUC-JP", "EUC-KR", "Big5", "CB2312"};

	public static final int ROTATE_AUTO = 0;
	public static final int ROTATE_PORTRAIT = 1;
	public static final int ROTATE_LANDSCAPE = 2;
	public static final int ROTATE_PSELAND = 3;

	public static final int TEXTSIZE_MESSAGE = 18;
	public static final int TEXTSIZE_SUMMARY = 14;
	public static final int TEXTSIZE_EDIT = 20;

	public static final int THUMBNAIL_PAGESIZE = 2 * 1024 * 1024;	// サムネイルバッファサイズ(4MB)
	public static final int THUMBNAIL_BLOCK = 8 * 1024;				// サムネイルブロックサイズ(8KB)
	public static final int THUMBNAIL_MAXPAGE = 4;					// サムネイルバッファ数

	// 縦長チェック
	static public boolean checkPortrait(int cx, int cy) {
		return cx <= cy ? true : false;
	}

	static public boolean checkPortrait(int cx, int cy, int rotate) {
		if (rotate == 0 || rotate == 2) {
			return cx <= cy ? true : false;
		}
		else {
			return cy <= cx ? true : false;
		}
	}

	static public boolean checkHiddenFile(String path) {
		String folder[] = path.split("/");
		if (folder.length == 0) {
			return true;
		}
		// ファイル名が .で始まる
		String top = folder[folder.length - 1].substring(0, 1);
		if (top.equals(".")) {
			return true;
		}
		return false;
	}

	// 色を取得
	public static int getColorValue(SharedPreferences sp, String keyOld, String keyRGB, int defColor) {
		String str = "";
		int index = -1;
		int val;

		// 旧設定から読み込み
		if (keyOld != null) {
			str = sp.getString(keyOld, "");
		}
		if (!str.equals("")) {
			index = Integer.parseInt(str);
		}
		if (0 <= index && index < DEF.ColorList.length) {
			// 旧設定がある場合は色情報を取得
			val = DEF.ColorList[index];
		}
		else {
			// 色情報
			val = sp.getInt(keyRGB, DEF.ColorList[defColor]);
		}
		return val;
	}

	// 色を取得
	public static int getGuideValue(SharedPreferences sp, String keyOld, String keyRGB, int defColor) {
		String str = "";
		int index = -1;
		int val;

		// 旧設定から読み込み
		if (keyOld != null) {
			str = sp.getString(keyOld, "");
		}
		if (!str.equals("")) {
			index = Integer.parseInt(str);
		}
		if (0 <= index && index < DEF.GuideList.length) {
			// 旧設定がある場合は色情報を取得
			val = DEF.GuideList[index];
		}
		else {
			// 色情報
			val = sp.getInt(keyRGB, DEF.GuideList[defColor]);
		}
		return val;
	}

	// 色の補正
	public static int calcColor(int color, int gap) {
		int r = (color >> 16) & 0x000000FF;
		int g = (color >> 8) & 0x000000FF;
		int b = (color >> 0) & 0x000000FF;
		r += gap;
		g += gap;
		b += gap;
		r = r > 255 ? 255 : r;
		g = g > 255 ? 255 : g;
		b = b > 255 ? 255 : b;
		r = r < 0 ? 0 : r;
		g = g < 0 ? 0 : g;
		b = b < 0 ? 0 : b;
		return Color.rgb(r, g, b);
	}

	// 色の補正
	public static int calcGradation(int color, int to, int rate) {
		int r = (color >> 16) & 0x000000FF;
		int g = (color >> 8) & 0x000000FF;
		int b = (color >> 0) & 0x000000FF;
		r = (to - r) * rate / 100 + r;
		g = (to - g) * rate / 100 + g;
		b = (to - b) * rate / 100 + b;
		return Color.rgb(r, g, b);
	}

	// 色のマージ
	public static int margeColor(int from, int to, int rate, int total) {
		int r1 = (from >> 16) & 0x000000FF;
		int g1 = (from >> 8) & 0x000000FF;
		int b1 = (from >> 0) & 0x000000FF;
		int r2 = (to >> 16) & 0x000000FF;
		int g2 = (to >> 8) & 0x000000FF;
		int b2 = (to >> 0) & 0x000000FF;
		r1 += (r2 - r1) * rate / total;
		g1 += (g2 - g1) * rate / total;
		b1 += (b2 - b1) * rate / total;
		return Color.rgb(r1, g1, b1);
	}

	static public String makeCode(String str, int thum_cx, int thum_cy) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			String path = str + ":" + thum_cx + "x" + thum_cy;
			digest.update(path.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				hexString.append(Integer.toHexString(messageDigest[i] & 0x00FF));
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			Log.e("makeCode", "NoSuchAlgorithmException");
		}
		return "";
	}

	static public boolean setRotation(Activity act, int rotate) {
		int way = act.getRequestedOrientation();
		// 回転制御
		if (rotate == DEF.ROTATE_PORTRAIT || rotate == ROTATE_PSELAND) {
			// 縦固定 又は 疑似横画面
			act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
			if (way == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
				return true;
			}
		}
		else if (rotate == DEF.ROTATE_LANDSCAPE) {
			// 横固定
			act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
			if (way == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
				return true;
			}
		}
		else {
			// 回転あり
			act.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
		}
		return false;
	}

	static public int calcThumbnailScale(int width, int height, int tcx, int tcy) {
		if (height >= tcy) {
			return height / tcy;
		}
		else {
			return 1;
		}
	}

	static public String getSizeStr(int val, String summ1, String summ2) {
		String str;

		if (val == 0) {
			str = summ1;
		}
		else {
			str = val * 100 + " " + summ2;
		}
		return str;
	}

	// スクロール倍率の計算
	static public int calcScroll(int val) {
		return val + 1;
	}

	// ドット数の計算(等倍)
	static public int calcRange1x(int val) {
		return val;
	}

	// 上下操作エリア
	static public int calcClickArea(int val) {
		return val * 2;
	}

	// 上下操作エリア (sp計算)
	static public int calcClickAreaPix(int val, float density) {
		return (int) (calcClickArea(val) * density);
	}

	// ページ選択感度
	static public int calcPageRange(int val) {
		return val;
	}

	// ページ選択感度 (sp計算)
	static public int calcPageRangePix(int val, float density) {
		return (int) (calcPageRange(val) * density);
	}

	// スクロール開始感度
	static public int calcTapRange(int val) {
		return val * 2;
	}

	// スクロール開始感度 (sp計算)
	static public int calcTapRangePix(int val, float density) {
		return (int) (calcTapRange(val) * density);
	}

	// イメージ、テキストのマージン
	static public int calcDispMargin(int val) {
		return val * 2;
	}

	// ミリ秒の計算
	static public int calcMSec(int val) {
		return val * 200;
	}

	// エフェクト時間の計算(msec)
	static public int calcEffectTime(int val) {
		return val * 50;
	}

	// 自動再生間隔の計算(100msec)
	static public int calcAutoPlay(int val) {
		return (val + 1) * 500;
	}

	// 縦横比(%)の計算
	static public int calcWAdjust(int val) {
		return (val - DEF.DEFAULT_WADJUST) + 100;
	}

	// 補正(%)の計算
	static public int calcScaling(int val) {
		return (val - DEF.DEFAULT_SCALING) + 100;
	}

	// フォントサイズの計算
	static public int calcFont(int val) {
		return (val + 6); // 最小値:2sp
	}

	// フォントサイズの計算(px)
	static public int calcFontPix(int val, float density) {
		return calcSpToPix(calcFont(val), density); // 最小値:8ドット
	}

	// ポイントをpxに変換
	static public int calcSpToPix(int val, float density) {
		return (int) (val * density);
	}

	// ツールバーサイズの計算
	static public int calcToolbarSize(int val) {
		return (val + 24); // 最小値:12sp
	}

	// ツールバーサイズの計算
	static public int calcToolbarPix(int val, float density) {
		return (int) (calcToolbarSize(val) * density); // 最小値:12sp
	}

	// サムネイルサイズの計算
	static public int calcThumbnailSize(int val) {
		return (val * 10 + 40); // 最小値:40px
	}

	// メモリサイズ
	static public int calcMemSize(int val) {
		return val * 10 + 20;
	}

	// キャッシュページ数
	static public int calcMemPage(int val) {
		return val * 2;
	}

	// スクロールドット数
	static public int calcScrlSpeed(int val) {
		return (val + 1);
	}

	// スクロールドット数 (sp単位)
	static public int calcScrlSpeedPix(int val, float density) {
		return (int) (calcScrlSpeed(val) * density);
	}

	// スクロールドット数
	static public int calcScrlRange(int val) {
		return (val + 1) * 5;
	}

	// ノイズレベル
	static public int calcNoiseLevel(int val) {
		return val * 100;
	}

	// ページ番号表示
	static public int calcPnumSize(int val) {
		return val + 6;
	}

	// ページ番号表示
	static public int calcPnumSizePix(int val, float density) {
		return (int) (calcPnumSize(val) * density);
	}

	// 保持件数
	static public int calcSaveNum(int val) {
		return val * 20;
	}

	// サマリ文字列作成
	static public String getScrollStr(int val, String summ1, String summ2) {
		return summ1 + " " + calcScroll(val) + " " + summ2;
	}

	// 操作エリアの文字列
	static public String getClickAreaStr(int val, String summ1) {
		return calcClickArea(val) + " " + summ1;
	}

	// ページ選択感度文字列
	static public String getPageRangeStr(int val, String summ1) {
		return calcPageRange(val) + " " + summ1;
	}

	// スクロール開始感度文字列
	static public String getTapRangeStr(int val, String summ1) {
		return calcTapRange(val) + " " + summ1;
	}

	// イメージ、テキストのマージン文字列
	static public String getDispMarginStr(int val, String summ1) {
		return calcDispMargin(val) + " " + summ1;
	}

	// サマリ文字列作成
	static public String getMarginStr(int val, String summ1) {
		return val + " " + summ1;
	}

	// テキストの字間文字列
	static public String getTextSpaceStr(int val, String summ1) {
		return val + " " + summ1;
	}

	// サマリ文字列作成(ミリ秒)
	static public String getMSecStr(int val, String summ1) {
		int msec = calcMSec(val);
		return (msec / 1000) + "." + (msec / 100 % 10) + " " + summ1;
	}

	// サマリ文字列作成(エフェクト時間)
	static public String getEffectTimeStr(int val, String summ1) {
		int msec = calcEffectTime(val);
		String m1 = "" + (msec / 1000);
		String m2 = "0" + (msec / 10 % 100);
		if (m2.length() >= 3) {
			m2 = m2.substring(1);
		}
		return m1 + "." + m2 + " " + summ1;
	}

	// サマリ文字列作成(自動再生間隔)
	static public String getAutoPlayStr(int val, String summ1) {
		int itv = calcAutoPlay(val);
		String m1 = "" + (itv / 1000);
		String m2 = "" + (itv / 100 % 10);
		return m1 + "." + m2 + " " + summ1;
	}

	// サマリ文字列作成(減速率)
	static public String getMomentModeStr(int val, String summ1, String summ2) {
		String str;

		if (val == 0) {
			str = summ1;
		}
		else if (val == MAX_MOMENTMODE) {
			str = summ2;
		}
		else {
			str = "" + val;
		}
		return str;
	}

	// サマリ文字列作成(100 : ?)
	static public String getAdjustStr(int val, String summ1, String summ2) {
		// 75～125
		int percent = calcWAdjust(val);
		String str;

		// if (percent != 100) {
		str = percent + " : 100" + " " + summ1;
		// }
		// else{
		// str = summ2;
		// }
		return str;
	}

	// サマリ文字列作成(-25%～+25%)
	static public String getScalingStr(int val, String summ1, String summ2) {
		// -25 ～ +25
		int percent = calcScaling(val) - 100;
		String str;

		if (percent < 0) {
			str = percent + " " + summ1;
		}
		else if (percent > 0) {
			str = "+" + percent + " " + summ1;
		}
		else {
			str = summ2;
		}
		return str;
	}

	// サマリ文字列作成(1 dot)
	static public String getCenterStr(int val, String summ1) {
		// 0.1%単位
		return val + " " + summ1;
	}

	// サマリ文字列作成(0%～)
	static public String getGradationStr(int val) {
		// 1%単位
		return val + " %";
	}

	// フォント用サマリ文字列作成(xx sp)
	static public String getFontSpStr(int val, String summ1) {
		// ドット単位
		return calcFont(val) + " " + summ1;
	}

	// 余白サマリ文字列作成(xx sp)
	static public String getMarginSpStr(int val, String summ1) {
		// ドット単位
		return val + " " + summ1;
	}

	// ツールバーサイズ文字列作成(xx sp)
	static public String getToolbarSeekStr(int val, String summ1) {
		// ドット単位
		return calcToolbarSize(val) + " " + summ1;
	}

	// リストサムネイルサイズ文字列作成(xx sp)
	static public String getListThumbSeekStr(int val, String summ1) {
		// ドット単位
		return "H: " + calcThumbnailSize(val) + " " + summ1;
	}

	// サムネイルサイズサマリ文字列作成(??sp x ??sp)
	static public String getThumbnailStr(int w, int h, String summ1) {
		// ドット単位
		return "W: " + calcThumbnailSize(w) + " " + summ1 + " x " + "H: " + calcThumbnailSize(h) + " " + summ1;
	}

	// キャッシュメモリサイズ
	static public String getMemSizeStr(int val, String summ1, String summ2) {
		// if (val == 0) {
		// return summ2;
		// }
		return calcMemSize(val) + " " + summ1;
	}

	// キャッシュページ数
	static public String getMemPageStr(int val, String summ1) {
		return calcMemPage(val) + " " + summ1;
	}

	// スクロールドット数
	static public String getScrlSpeedStr(int val, String summ1) {
		return calcScrlSpeed(val) + " " + summ1;
	}

	// スクロール量
	static public String getScrlRangeStr(int val, String summ1, String summ2) {
		return summ1 + ' ' + calcScrlRange(val) + " " + summ2;
	}

	// ページ番号表示サイズ
	static public String getPnumSizeStr(int val, String summ) {
		return calcPnumSize(val) + " " + summ;
	}

	// 音量レベル
	static public String getNoiseLevelStr(int val) {
		return "" + calcNoiseLevel(val);
	}

	// 2バイト数値取得
	static public short getShort(byte b[], int pos) {
		int val;
		val = ((int) b[pos] & 0x000000FF) | (((int) b[pos + 1] << 8) & 0x0000FF00);

		return (short) val;
	}

	// 4バイト数値取得
	static public int getInt(byte b[], int pos) {
		int val;
		val = ((int) b[pos] & 0x000000FF) | (((int) b[pos + 1] << 8) & 0x0000FF00) | (((int) b[pos + 2] << 16) & 0x00FF0000) | (((int) b[pos + 3] << 24) & 0xFF000000);

		return val;
	}

	static public String getFontDirectory() {
		return Environment.getExternalStorageDirectory() + "/comittona/font/";
	}

	static public String getConfigDirectory() {
		return Environment.getExternalStorageDirectory() + "/comittona/conf/";
	}

	// パス部分のみ取得
	static public String getDir(String filepath) {
		int prev = 0;

		if (filepath == null) {
			return "";
		}
		while (true) {
			int pos = filepath.indexOf('/', prev);
			if (pos == -1) {
				break;
			}
			prev = pos + 1;
		}
		return filepath.substring(0, prev);
	}

	// ファイル拡張子の取得
	static public String getFileExt(String filepath) {
		String ext = "";
		if (filepath != null) {
			int pos = filepath.lastIndexOf('.');

			if (pos >= 0) {
				// 拡張子切り出し
				ext = filepath.substring(pos).toLowerCase();
			}
		}
		return ext;
	}

	// 拡張子の取得
	public static String getExtension(String filename) {
		if (filename != null) {
			int extpos = filename.lastIndexOf('.');
			int slashpos = filename.lastIndexOf('/');
			if (slashpos < extpos && extpos >= 1) {
				return filename.substring(extpos).toLowerCase();
			}
		}
		return "";
	}

	// 番号とか考慮しながら文字列比較
	// private static final String SERIAL_STR[] = { "上", "中", "下", "短", "外" };
	private static final int CHTYPE_NUM = 1;
	private static final int CHTYPE_CHAR = 2;
	private static final int CHTYPE_JNUM = 3;
	private static final int CHTYPE_SERIAL = 4;

	static public int compareFileName(String name1, String name2) {
		int len1 = name1.length();
		int len2 = name2.length();
		int i1, i2;
		char ch1, ch2;
		int ct1, ct2;

		for (i1 = i2 = 0; i1 < len1 && i2 < len2; i1++, i2++) {
			ch1 = name1.charAt(i1);
			ch2 = name2.charAt(i2);
			ct1 = getCharType(ch1);
			ct2 = getCharType(ch2);

			if (ct1 != ct2) {
				// 文字種が違う場合はそのまま比較
				if (ch1 != ch2) {
					return ch1 - ch2;
				}
			}
			else if (ct1 == CHTYPE_NUM) {
//				Log.d("DEF","compareFileName 文字1=" + ch1 + ", 文字2=" + ch2);
				String num1 = getNumbers(name1, i1);
				String num2 = getNumbers(name2, i2);
				int nlen1 = num1.length();
				int nlen2 = num2.length();

				// カンマを取り除く
				num1 = num1.replace(",", "");
				num2 = num2.replace(",", "");

				boolean minus1 = num1.startsWith("-");
				boolean minus2 = num2.startsWith("-");

				if ( minus1 && !minus2 ){
					// num2が大きい
					return -1;
				}
				else if ( !minus1 && minus2 ){
					// num1が大きい
					return 1;
				}
				else {
//					Log.d("DEF","compareFileName 数字1=" + num1 + ", 数字2=" + num2);
					//小数点の位置
					int index_dot1 = num1.indexOf(".");
					int index_dot2 = num2.indexOf(".");

					//小数以下の桁数
					int col_dec1;
					int col_dec2;
					if (index_dot1 == -1) {
						col_dec1 = 0;
					} else {
						col_dec1 = nlen1 - index_dot1 - 1;
					}
					if (index_dot2 == -1) {
						col_dec2 = 0;
					} else {
						col_dec2 = nlen2 - index_dot2 - 1;
					}

					// 小数点以下の桁数を合わせる
					int col_diff = col_dec1 - col_dec2;
					for (int i = 1; i <= col_diff; i++) {
						num2 = num2 + "0";
					}
					for (int i = -1; i >= col_diff; i--) {
						num1 = num1 + "0";
					}
//					Log.d("DEF","compareFileName 数字1=" + num1 + ", 数字2=" + num2 + ", 小数点位置1=" + index_dot1 + ", 小数点位置2=" + index_dot2 + ", 小数桁1=" + col_dec1 + ", 小数桁2=" + col_dec2);
					num1 = num1.replace(".", "");
					num2 = num2.replace(".", "");
//					Log.d("DEF","compareFileName 数字1=" + num1 + ", 数字2=" + num2 + ", 小数点位置1=" + index_dot1 + ", 小数点位置2=" + index_dot2 + ", 小数桁1=" + col_dec1 + ", 小数桁2=" + col_dec2);

					int num_len1 = num1.length();
					int num_len2 = num2.length();

					if (!minus1 && !minus2) {
						// どちらも正の数

						if (num_len1 < num_len2) {
							int difflen = num_len2 - num_len1;
							for (int i = 0; i < difflen; i++) {
								int diff = getNumber('０') - getNumber(num2.charAt(i));
								if (diff != 0) {
									// num1が大きければプラス
									return diff;
								}
							}
							// 残り部分で比較
							num2 = num2.substring(difflen);
						} else if (nlen1 > nlen2) {
							int difflen = num_len1 - num_len2;
							for (int i = 0; i < difflen; i++) {
								int diff = getNumber(num1.charAt(i)) - getNumber('0');
								if (diff != 0) {
									// num1が大きければプラス
									return diff;
								}
							}
							// 残り部分で比較
							num1 = num1.substring(difflen);
						}
						// 数字が異なる場合は比較
						for (int i = 0; i < num1.length(); i++) {
							int diff = getNumber(num1.charAt(i)) - getNumber(num2.charAt(i));
							if (diff != 0) {
								// num1が大きければプラス
								return diff;
							}
						}
					}
					else {
						// どちらも負の数

						// マイナスを取り除く
						num1 = num1.replace("-", "");
						num2 = num2.replace("-", "");

						if (num_len1 < num_len2) {
							int difflen = num_len2 - num_len1;
							for (int i = 0; i < difflen; i++) {
								int diff = getNumber('０') - getNumber(num2.charAt(i));
								if (diff != 0) {
									// num1が大きければマイナス
									return -diff;
								}
							}
							// 残り部分で比較
							num2 = num2.substring(difflen);
						} else if (nlen1 > nlen2) {
							int difflen = num_len1 - num_len2;
							for (int i = 0; i < difflen; i++) {
								int diff = getNumber(num1.charAt(i)) - getNumber('0');
								if (diff != 0) {
									// num1が大きければマイナス
									return -diff;
								}
							}
							// 残り部分で比較
							num1 = num1.substring(difflen);
						}
						// 数字が異なる場合は比較
						for (int i = 0; i < num1.length(); i++) {
							int diff = getNumber(num1.charAt(i)) - getNumber(num2.charAt(i));
							if (diff != 0) {
								// num1が大きければマイナス
								return -diff;
							}
						}
					}
					i1 += nlen1 - 1;
					i2 += nlen2 - 1;
				}
			}
			else if (ct1 == CHTYPE_JNUM) {
				String num1 = getJnums(name1, i1);
				String num2 = getJnums(name2, i2);
				int nlen1 = num1.length();
				int nlen2 = num2.length();
				if (nlen1 < nlen2) {
					int difflen = nlen2 - nlen1;
					for (int i = 0 ; i < difflen ; i ++) {
						if (getJnum(num2.charAt(i)) != 0) {
							// num2の方が大きい
							return -1;
						}
					}
					// 残り部分で比較
					num2 = num2.substring(difflen);
				}
				else if (nlen1 > nlen2) {
					int difflen = nlen1 - nlen2;
					for (int i = 0 ; i < difflen ; i ++) {
						if (getJnum(num1.charAt(i)) > 0) {
							// num1の方が大きい
							return 1;
						}
					}
					// 残り部分で比較
					num1 = num1.substring(difflen);
				}
				// 数字が異なる場合は比較
				for (int i = 0 ; i < num1.length() ; i ++) {
					int diff = getJnum(num1.charAt(i)) - getJnum(num2.charAt(i));
					if (diff != 0) {
						// num1の方が大きい
						return diff;
					}
				}
				i1 += nlen1 - 1;
				i2 += nlen2 - 1;
			}
			else if (ct1 == CHTYPE_CHAR) {
				if (ch1 != ch2) {
					return ch1 - ch2;
				}
			}
			else {
				int s1 = getSerial(ch1);
				int s2 = getSerial(ch2);
				if (s1 != s2) {
					return s1 - s2;
				}
			}
		}
		return len1 - len2;
	}

	static private String getNumbers(String str, int idx) {
		int i;
		for (i = idx; i < str.length(); i++) {
			int ch = str.charAt(i);
			if ((ch < '0' || '9' < ch) && '-' != ch && '.' != ch && ',' != ch) {
				break;
			}
		}
		return str.substring(idx, i);
	}

	static private int getNumber(char ch) {
		switch (ch) {
			case '-':
				return -1;
			case '0':
				return 0;
			case '1':
				return 1;
			case '2':
				return 2;
			case '3':
				return 3;
			case '4':
				return 4;
			case '5':
				return 5;
			case '6':
				return 6;
			case '7':
				return 7;
			case '8':
				return 8;
			case '9':
				return 9;
			case '.':
				return 10;
			case ',':
				return 11;
		}
		return -2;
	}

	static private String getJnums(String str, int idx) {
		int i;
		for (i = idx; i < str.length(); i++) {
			char ch = str.charAt(i);
			if (getCharType(ch) != CHTYPE_JNUM) {
				break;
			}
		}
		return str.substring(idx, i);
	}

	static private int getSerial(char ch) {
		switch (ch) {
			case '上':
				return 0;
			case '前':
				return 1;
			case '中':
				return 2;
			case '後':
				return 3;
			case '下':
				return 4;
			case '短':
				return 5;
			case '外':
				return 6;
		}
		return -1;
		// int result[] = {-1, 0};
		// int i;
		//
		// for (i = 0 ; i < SERIAL_STR.length ; i ++) {
		// int len = SERIAL_STR[i].length();
		// if (str.length() >= idx + len) {
		// if (str.substring(idx, len).equals(SERIAL_STR[i])) {
		// result[0] = i;
		// result[1] = len;
		// break;
		// }
		// }
		// }
		// return result;
	}

	static private int getJnum(char ch) {
		switch (ch) {
			case '〇':
				return 0;
			case '零':
				return 1;
			case '一':
				return 2;
			case '壱':
				return 3;
			case '二':
				return 4;
			case '弐':
				return 5;
			case '三':
				return 6;
			case '参':
				return 7;
			case '四':
				return 8;
			case '肆':
				return 9;
			case '五':
				return 10;
			case '伍':
				return 11;
			case '六':
				return 12;
			case '陸':
				return 13;
			case '七':
				return 14;
			case '漆':
				return 15;
			case '八':
				return 16;
			case '捌':
				return 17;
			case '九':
				return 18;
			case '玖':
				return 19;
			case '十':
				return 20;
			case '拾':
				return 21;
			case '什':
				return 22;
			case '廿':
				return 23;
			case '卅':
				return 24;
			case '丗':
				return 25;
			case '百':
				return 26;
			case '佰':
				return 27;
			case '千':
				return 28;
			case '仟':
				return 29;
			case '阡':
				return 30;
			case '万':
				return 31;
			case '萬':
				return 32;
			case '億':
				return 33;
			case '兆':
				return 34;
		}
		return -1;
	}

	static private int getCharType(char ch) {
		if (('0' <= ch && ch <= '9') || '-' == ch || '.' == ch || ',' == ch) {
			return CHTYPE_NUM;
		}

		switch (ch) {
			case '上':
			case '中':
			case '下':
			case '前':
			case '後':
			case '短':
			case '外':
				return CHTYPE_SERIAL;
		}

		switch (ch) {
			case '〇':
			case '零':
			case '一':
			case '壱':
			case '二':
			case '弐':
			case '三':
			case '参':
			case '四':
			case '肆':
			case '五':
			case '伍':
			case '六':
			case '陸':
			case '七':
			case '漆':
			case '八':
			case '捌':
			case '九':
			case '玖':
			case '十':
			case '拾':
			case '什':
			case '廿':
			case '卅':
			case '丗':
			case '百':
			case '佰':
			case '千':
			case '仟':
			case '阡':
			case '万':
			case '萬':
				return CHTYPE_JNUM;
		}
		return CHTYPE_CHAR;
	}

	// 縮小取り込みの倍率計算
	static public int calcScale(int w, int h, int type, int lw, int lh) {
		int lw2 = DEF.checkPortrait(w, h) ? lw : lw * 2;
		if (type == 1) {
			// JPEGではライブラリで1/2^xに縮小できる
			if (w >= lw2 * 8 && h >= lh * 8) {
				return 8;
			}
			else if (w >= lw2 * 4 && h >= lh * 4) {
				return 4;
			}
			else if (w >= lw2 * 2 && h >= lh * 2) {
				return 2;
			}
		}
		else if (type != 2 && type != 6) {
			// pngとgif以外
			if (w >= lw2 * 2 && h >= lh * 2) {
				int w_scale = w / lw2;
				int h_scale = h / lh;
				return w_scale < h_scale ? w_scale : h_scale;
			}
		}
		return 1;
	}

	static public int divRoundUp(int val, int div) {
		return val / div + (val % div != 0 ? 1 : 0);
	}

	// Comittoの諸々保存先のパスを返す
	static public String getBaseDirectory() {
		return Environment.getExternalStorageDirectory() + "/comittona/";
	}

	// 設定画面のメッセージのフォントサイズ
	static public int getMessageTextSize(float density) {
		return (int) (TEXTSIZE_MESSAGE * density);
	}

	// 設定画面のメッセージのフォントサイズ
	static public int getSummaryTextSize(float density) {
		return (int) (TEXTSIZE_SUMMARY * density);
	}

	static public boolean checkExportKey(String key) {
		// サーバー情報としおり情報をExport/Import対象に含める
		/*
		if (key.indexOf('/') >= 0) {
			return false;
		}
		else if (key.startsWith("smb-")) {
			return false;
		}
		*/
		if (key.indexOf('/') >= 0) {
			// 安全のため、しおり削除の条件に一致しないKeyは対象外としておく
			int len = key.length();
			if( len >= 1 && key.substring( 0, 1 ).equals( "/" ) ){
			}
			else if( len >= 6 && key.substring( 0, 6 ).equals( "smb://" ) ){
			}
			else {
				return false;
			}
		}
		else if (key.equals("path")) {
			return false;
		}
		else if (key.equals("ImportSetting")) {
			return false;
		}
		return true;
	}

	// NxT専用キーの判別
	static public boolean checkTonlyExportKey(String key) {
		if (key.indexOf('/') >= 0) {
			// 安全のため、しおり削除の条件に一致しないKeyは対象外としておく
			int len = key.length();
			if( len >= 1 && key.substring( 0, 1 ).equals( "/" ) ){
				return true;
			}
			else if( len >= 6 && key.substring( 0, 6 ).equals( "smb://" ) ){
				return true;
			}
		}
		else if (key.startsWith("smb-")) {
			return true;
		}
		return false;
	}

	// 定義からフラグ読み込み
	static public boolean getBoolean(SharedPreferences sp, String key, boolean defval) {
		boolean intval = defval;
		try {
			// 読み込み
			intval = sp.getBoolean(key, defval);
		}
		catch (Exception e) {
			Log.e("DEF.getBoolean", "error(key=" + key + ")");
		}
		return intval;
	}

	// 定義から数値読み込み
	static public int getInt(SharedPreferences sp, String key, int defval) {
		int intval = defval;
		try {
			// 読み込み
			intval = sp.getInt(key, defval);
		}
		catch (Exception e) {
			Log.e("DEF.getInt", "error(key=" + key + ")");
		}
		return intval;
	}

	// 定義から文字列読み込み
	static public int getInt(SharedPreferences sp, String key, String defval) {
		String strval = defval;
		int intval = 0;
		try {
			strval = sp.getString(key, defval);
		}
		catch (Exception e) {
			Log.e("DEF.getInt", "getStr error(key=" + key + ")");
		}

		try {
			// 読み込み
			intval = Integer.parseInt(strval);
		}
		catch (Exception e) {
			Log.e("DEF.getInt", "parseInt error(key=" + key + ", str=" + strval + ")");
		}
		return intval;
	}

}
