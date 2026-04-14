package com.financehub.services;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * Accepts JPEG/PNG uploads, normalizes to RGB, scales down, and outputs JPEG under a byte budget.
 */
@Component
public class ProfilePhotoProcessor {

	public static final int MAX_UPLOAD_BYTES = 512 * 1024;
	public static final int MAX_STORED_BYTES = 48 * 1024;
	private static final int MAX_EDGE = 280;

	public byte[] processUpload(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Please choose an image file.");
		}
		if (file.getSize() > MAX_UPLOAD_BYTES) {
			throw new IllegalArgumentException("Image must be at most 512 KB before upload.");
		}
		String ct = file.getContentType();
		if (ct == null || !(ct.equalsIgnoreCase("image/jpeg") || ct.equalsIgnoreCase("image/jpg")
				|| ct.equalsIgnoreCase("image/png"))) {
			throw new IllegalArgumentException("Only JPEG or PNG images are allowed.");
		}
		BufferedImage src = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
		if (src == null) {
			throw new IllegalArgumentException("Could not read the image. Try another file.");
		}
		BufferedImage rgb = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D gBg = rgb.createGraphics();
		gBg.setColor(Color.WHITE);
		gBg.fillRect(0, 0, src.getWidth(), src.getHeight());
		gBg.drawImage(src, 0, 0, null);
		gBg.dispose();

		int w = rgb.getWidth();
		int h = rgb.getHeight();
		double scale = Math.min(1.0, (double) MAX_EDGE / Math.max(w, h));
		int nw = Math.max(1, (int) Math.round(w * scale));
		int nh = Math.max(1, (int) Math.round(h * scale));
		BufferedImage scaled = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 = scaled.createGraphics();
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2.drawImage(rgb, 0, 0, nw, nh, null);
		g2.dispose();

		return compressJpegUnderBudget(scaled);
	}

	private byte[] compressJpegUnderBudget(BufferedImage img) throws IOException {
		BufferedImage current = img;
		float quality = 0.88f;
		for (int attempt = 0; attempt < 12; attempt++) {
			byte[] out = writeJpeg(current, quality);
			if (out.length <= MAX_STORED_BYTES) {
				return out;
			}
			quality -= 0.08f;
			if (quality < 0.42f) {
				current = halfSize(current);
				quality = 0.82f;
			}
		}
		byte[] last = writeJpeg(halfSize(halfSize(current)), 0.5f);
		if (last.length > MAX_STORED_BYTES) {
			throw new IllegalArgumentException("Image could not be compressed enough; try a smaller or simpler picture.");
		}
		return last;
	}

	private static BufferedImage halfSize(BufferedImage src) {
		int w = Math.max(1, src.getWidth() / 2);
		int h = Math.max(1, src.getHeight() / 2);
		BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		Graphics2D g = dst.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(src, 0, 0, w, h, null);
		g.dispose();
		return dst;
	}

	private static byte[] writeJpeg(BufferedImage img, float quality) throws IOException {
		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
		if (!writers.hasNext()) {
			throw new IllegalStateException("No JPEG writer available.");
		}
		ImageWriter writer = writers.next();
		ImageWriteParam param = writer.getDefaultWriteParam();
		if (param.canWriteCompressed()) {
			param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			param.setCompressionQuality(Math.max(0.25f, Math.min(0.95f, quality)));
		}
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
			writer.setOutput(ios);
			writer.write(null, new IIOImage(img, null, null), param);
		}
		writer.dispose();
		return baos.toByteArray();
	}
}
