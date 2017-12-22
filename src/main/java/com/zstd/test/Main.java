package com.zstd.test;

import com.github.luben.zstd.ZstdInputStream;
import com.github.luben.zstd.ZstdOutputStream;
import io.airlift.compress.zstd.ZstdDecompressor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {

	private static byte[] compress(byte[] bytes, int level) throws IOException {
		return compress(bytes, new ByteArrayOutputStream(Math.max(bytes.length / 2, 64)), level);
	}

	private static byte[] compress(byte[] bytes, ByteArrayOutputStream outputStream, int level) throws IOException {
		try (OutputStream compressedStream = new ZstdOutputStream(outputStream, level)) {
			compressedStream.write(bytes);
			compressedStream.flush();
		}
		return outputStream.toByteArray();
	}

	private static byte[] decompressWithNativeZstd(byte[] bytes) throws IOException {
		try (InputStream compressedStream = new ZstdInputStream(new ByteArrayInputStream(bytes))) {
			return readAll(compressedStream);
		}
	}

	private static byte[] decompressWithJavaZstd(byte[] bytes, int uncompressedSize) throws IOException {
		final ZstdDecompressor decompressor = new ZstdDecompressor();
		final byte[] decompressedBytes = new byte[uncompressedSize];

		decompressor.decompress(bytes, 0, bytes.length, decompressedBytes, 0, decompressedBytes.length);

		return decompressedBytes;
	}

	private static byte[] readAll(InputStream inputStream) throws IOException {
		final int bufferSize = Math.max(Math.min(inputStream.available(), 10240), 2048);
		final byte[] buf = new byte[bufferSize];
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream(bufferSize);

		int len;
		while ((len = inputStream.read(buf)) >= 0) {
			outputStream.write(buf, 0, len);
		}

		return outputStream.toByteArray();
	}

	private static byte[] truncateZeros(byte[] bytes) {
		int i = bytes.length - 1;
		for (; i > 0 && bytes[i] == 0; i--);
		return Arrays.copyOf(bytes, i + 1);
	}

	public static void main(String[] args) throws IOException {
		long currentTime;
		final int[] levels = new int[]{1, 5, 11};
		final byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
		System.out.println("File size before compression: " + bytes.length + "\n");

		for (int level : levels) {
			System.out.println("Level " + level);
			final byte[] compressedBytes = compress(bytes, level);
			System.out.println("File size after compression: " + compressedBytes.length);

			currentTime = System.currentTimeMillis();
			final byte[] decompressedBytes = decompressWithNativeZstd(compressedBytes);
			System.out.println("Time for native decompression: " + (System.currentTimeMillis() - currentTime) + "ms");
			System.out.println("File size after native decompression: " + decompressedBytes.length);

			currentTime = System.currentTimeMillis();
			byte[] decompressedBytes2 = decompressWithJavaZstd(compressedBytes, bytes.length);
			System.out.println("Time for Java decompression: " + (System.currentTimeMillis() - currentTime) + "ms");
			decompressedBytes2 = truncateZeros(decompressedBytes2);
			System.out.println("File size after Java decompression: " + decompressedBytes2.length);
			System.out.println();
		}
	}
}
