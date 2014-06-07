package jp.co.heppokoact.util;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;

/**
 * イメージを処理するユーティリティ
 *
 * @author M.Yoshida
 */
public class ImageUtil {

	/**
	 * BufferedImageをOutputStreamに変換する。
	 *
	 * @param image 変換するイメージ
	 * @return OutputStream
	 * @throws IOException OutputStreamへの変換に失敗した場合
	 */
	public static ByteArrayInputStream convToInputStream(BufferedImage image)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "bmp", out);
		return new ByteArrayInputStream(out.toByteArray());
	}

	/**
	 * 引数のイメージが持つピクセルが等しいかどうかを調べる。
	 *
	 * @param image1 比較するイメージ１
	 * @param image2 比較するイメージ２
	 * @return 等しければtrue
	 */
	public static boolean equals(BufferedImage image1, BufferedImage image2) {
		if (image1 == null || image2 == null) {
			return image1 == image2;
		}

		// 比較する両キャプチャのピクセルを取得
		int[] pixels1 = obtainAllPixels(image1);
		int[] pixels2 = obtainAllPixels(image2);

		// ピクセルの中身を比較
		return Arrays.equals(pixels1, pixels2);
	}

	/**
	 * 引数のイメージから全ピクセルを取得する。
	 *
	 * @param target ピクセルを取得するイメージ
	 * @return 引数のイメージの全ピクセル
	 */
	public static int[] obtainAllPixels(BufferedImage target) {
		Raster raster = target.getData();
		return raster.getPixels(0, 0, target.getWidth(), target.getHeight(), (int[]) null);
	}

}
