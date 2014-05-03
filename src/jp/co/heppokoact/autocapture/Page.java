package jp.co.heppokoact.autocapture;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.Arrays;

/**
 * 電子書籍のページ
 *
 * @author M.Yoshida
 */
public class Page {

	/** このページのイメージを確定するのに必要なキャプチャ一致枚数 */
	private static final int FIX_THRESHOLD = 3;
	/** {@link #image}の初期値のためのNULLオブジェクト */
	private static final BufferedImage NULL_IMAGE = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);

	/** このページのページ番号 */
	private int pageNumber;
	/** このページのイメージ */
	private BufferedImage image = NULL_IMAGE;
	/** キャプチャ一致枚数 */
	private int matchCount;

	/**
	 * コンストラクタ
	 *
	 * @param pageNumber ページ番号
	 */
	public Page(int pageNumber) {
		this.pageNumber = pageNumber;
	}

	/**
	 * このページのイメージ候補を与える。
	 *
	 * このページのイメージ{@link #image}と引数のイメージを比較し、
	 * 一致していればキャプチャ一致枚数{@link #matchCount}をインクリメントする。
	 * 一致していなければ引数のイメージを新しいこのページのイメージとして{@link #image}にセットし、
	 * キャプチャ一致枚数{@link #matchCount}をリセットする。
	 *
	 * @param currentImage このページのイメージ候補
	 */
	public void submitImage(BufferedImage currentImage) {
		if (isSameImage(currentImage)) {
			matchCount++;
		} else {
			image = currentImage;
			matchCount = 1;
		}
	}

	/**
	 * 引数のイメージをこのページのイメージと比較する。
	 *
	 * @param currentImage 比較するイメージ
	 * @return 一致していればtrue
	 */
	private boolean isSameImage(BufferedImage currentImage) {
		// 比較する両キャプチャのピクセルを取得
		int[] currentPixels = obtainAllPixels(currentImage);
		int[] prevPixels = obtainAllPixels(image);

		// ピクセルの中身を比較
		return Arrays.equals(currentPixels, prevPixels);
	}

	/**
	 * 引数のイメージから全ピクセルを取得する。
	 *
	 * @param target ピクセルを取得するイメージ
	 * @return 引数のイメージの全ピクセル
	 */
	private int[] obtainAllPixels(BufferedImage target) {
		Raster raster = target.getData();
		return raster.getPixels(0, 0, target.getWidth(), target.getHeight(), (int[]) null);
	}

	/**
	 * このページのイメージが確定したかどうかを調べる。
	 *
	 * @return 確定していればtrue
	 */
	public boolean isFixed() {
		return matchCount >= FIX_THRESHOLD;
	}

	/**
	 * このページと引数のページが同じイメージであるかを調べる。
	 *
	 * @param page 同じイメージであるかを調べるページ
	 * @return 同じならtrue
	 */
	public boolean isSameImage(Page page) {
		return isSameImage(page.getImage());
	}

	/**
	 * このページのイメージを取得する。
	 *
	 * @return このページのイメージ
	 */
	public BufferedImage getImage() {
		return image;
	}

	/**
	 * このページのページ番号を取得する。
	 *
	 * @return このページのページ番号
	 */
	public int getPageNumber() {
		return pageNumber;
	}

}
