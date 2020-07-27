package src.comitton.stream;

import java.io.IOException;
import java.io.InputStream;

import src.comitton.pdf.PDFLib;


import android.util.Log;

public class PDFThumbnail extends InputStream {
	private static final int SIZE_BUFFER = 1024;

	private int mDataSize;
	private WorkStream mWorkStream;
	private long mFirstDataPos;

	public PDFThumbnail(WorkStream ws) throws IOException {
		// 初期化
		mDataSize = 0;
		mWorkStream = ws;
		mFirstDataPos = 0;

		// ZIPファイル読み込み
		byte buf[] = new byte[SIZE_BUFFER];
		int readSize;
		long datapos = 0;

		readSize = ws.read(buf, 0, SIZE_BUFFER);

		// 先頭 %PDF をチェック
		if (buf[0] != (byte) 0x25 || buf[1] != (byte) 0x50 || buf[2] != (byte) 0x44 || buf[3] != (byte) 0x46) {
			// %PDFでなければPDFではない
			return;
		}

		int i = 8;
		// ヘッダの次の改行コード飛ばし
		if (buf[i] == (byte) 0x0d && buf[i + 1] == (byte) 0x0a) {
			i += 2;
		}
		else if (buf[i] == (byte) 0x0a) {
			i++;
		}
		// バイナリを飛ばして最初のobjへ
		while (i < readSize) {
			if (i <= readSize - 2 && buf[i] == (byte) 0x0d && buf[i + 1] == (byte) 0x0a) {
				i += 2;
				break;
			}
			else if (buf[i] == (byte) 0x0a) {
				i++;
				break;
			}
			i++;
		}
		if (i >= readSize) {
			return;
		}

		// バッファの先頭を endobj の次にする
		PDFLib.moveBinary(buf, i, readSize);
		readSize -= i;
		datapos = i;

		int objstpos;
		int objedpos;
		int streampos;
		int mode = 0;
		boolean isEOF = false;

		while (true) {
			if (readSize < SIZE_BUFFER && isEOF == false) {
				// 読み込む余地がある場合は読み込み
				int result = ws.read(buf, readSize, SIZE_BUFFER - readSize);

				if (result > 0) {
					readSize += result;
				}
				else {
					// 最後まできた
					isEOF = true;
				}
			}
			if (readSize <= 10) {
				break;
			}

			if (mode == 0) {
				// obj検索
				objstpos = PDFLib.searchBinary(buf, 0, readSize, PDFLib.BYTE_OBJSTART);
				if (objstpos < 0) {
					// 見つからないからもっと読む
					if (readSize == SIZE_BUFFER) {
						// データの終わりではない
						PDFLib.moveBinary(buf, readSize - 10, readSize);
						datapos += readSize - 10;
						readSize = 10;
						continue;
					}
					else {
						// これ以上データなし
						break;
					}
				}
				else {
					// 見つけたのでバッファの頭に移動
					objstpos += 4; // " obj" 分だけ進める
					PDFLib.moveBinary(buf, objstpos, readSize);
					readSize -= objstpos;
					datapos += objstpos;
					mode = 1;
				}
			}
			else if (mode == 1) {
				// endobj検索
				streampos = PDFLib.searchBinary(buf, 0, readSize, PDFLib.BYTE_STREAM);
				objedpos = PDFLib.searchBinary(buf, 0, readSize, PDFLib.BYTE_OBJEND);
				if (objedpos >= 0 && objedpos < streampos) {
					// endobjよりもstreamの方が先にくるはず、後に来たら対のものではない

					// endobj 分だけ進める
					objedpos += 6;

					// バッファの先頭を endobj の次にする
					PDFLib.moveBinary(buf, objedpos, readSize);
					readSize -= objedpos;
					datapos += objedpos;
					mode = 0;
				}
				else if (streampos >= 0) {
					// streamがあった場合は パラメタを文字列化
					String params = new String(buf, 0, streampos).toLowerCase();

					streampos += 6; // "stream" 分進める
					if (buf[streampos] == (byte) 0x0d) {
						// 0x0d0a分進める
						streampos += 2;
					}
					else if (buf[streampos] == (byte) 0x0a) {
						// 0x0a分進める
						streampos++;
					}
					datapos += streampos;

					// データサイズを探す
					int parampos = params.indexOf("/length");
					if (parampos >= 0) {
						// データサイズの設定あり
						int length = PDFLib.getPDFValue(params, parampos + "/length".length());
						if (length >= 0) {
							// サイズ設定が 0 以上
							int type = 0;
							if (buf[streampos] == (byte) 0xff && buf[streampos + 1] == (byte) 0xd8) {
								// JPEGファイルのヘッダあり
								type = 1;
							}
							else if (buf[streampos] == (byte) 0x89 && buf[streampos + 1] == (byte) 0x50) {
								// PNGファイルのヘッダあり
								type = 2;
							}

							if (type != 0) {
								// 画像だった
								mDataSize = length;
								mFirstDataPos = datapos;
								ws.seek(mFirstDataPos);
								break;
							}
							// Streamの終わりまでシークで進める
							datapos += length;
						}
					}

					// 再読み込み設定
					readSize = 0;
					mode = 2;
					ws.seek(datapos);
				}
				else {
					// 見つからないからもっと読む
					if (readSize == SIZE_BUFFER) {
						// データの終わりではない
						PDFLib.moveBinary(buf, readSize - 10, readSize);
						datapos += readSize - 10;
						readSize = 10;
						continue;
					}
					else {
						// これ以上データなし
						break;
					}
				}
			}
			else {
				// stream の後の endobj 検索
				objedpos = PDFLib.searchBinary(buf, 0, readSize, PDFLib.BYTE_OBJEND);
				if (objedpos < 0) {
					// 見つからないからもっと読む
					if (readSize == SIZE_BUFFER) {
						// データの終わりではない
						PDFLib.moveBinary(buf, readSize - 10, readSize);
						datapos += readSize - 10;
						readSize = 10;
						continue;
					}
					else {
						// これ以上データなし
						break;
					}
				}
				else {
					// endobj 分だけ進める
					objedpos += 6;

					// バッファの先頭を endobj の次にする
					PDFLib.moveBinary(buf, objedpos, readSize);
					readSize -= objedpos;
					datapos += objedpos;
					mode = 0;
				}
			}
		}
	}

	// データ位置へ移動
	public void seekFirstData() {
		try {
			mWorkStream.seek(mFirstDataPos);
		}
		catch (IOException e) {
			//
			Log.e("PdfInputStream/Seek", e.getMessage());
		}
	}

	@Override
	public int read() throws IOException {
		// 処理不要
		return 0;
	}

	@Override
	public int read(byte buf[], int off, int len) throws IOException {
		int size = len;
		int ret = -1;
		if (size > mDataSize) {
			size = mDataSize;
		}
		if (size > 0) {
			ret = mWorkStream.read(buf, off, size);
		}
		if (ret == 0) {
			ret = -1;
		}
		return ret;
	}
}
