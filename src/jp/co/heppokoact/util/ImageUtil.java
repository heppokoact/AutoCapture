package jp.co.heppokoact.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ImageUtil {

	public static ByteArrayInputStream convToInputStream(BufferedImage image)
			throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ImageIO.write(image, "bmp", out);
		return new ByteArrayInputStream(out.toByteArray());
	}

}
