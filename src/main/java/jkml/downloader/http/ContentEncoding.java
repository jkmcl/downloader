package jkml.downloader.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.GZIPInputStream;

import org.apache.hc.client5.http.entity.DeflateInputStream;
import org.apache.hc.client5.http.entity.InputStreamFactory;

enum ContentEncoding {

	GZIP(GZIPInputStream::new), DEFLATE(DeflateInputStream::new);

	private final InputStreamFactory inputStreamFactory;

	private ContentEncoding(InputStreamFactory inputStreamFactory) {
		this.inputStreamFactory = inputStreamFactory;
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}

	public static ContentEncoding fromString(String str) {
		if (str == null) {
			return null;
		}
		var upperCase = str.toUpperCase();
		if (upperCase.isBlank() || "IDENTITY".equals(upperCase)) {
			return null;
		}
		return valueOf(upperCase);
	}

	public static String[] strings() {
		var values = values();
		var strings = new String[values.length];
		for (var i = 0; i < values.length; ++i) {
			strings[i] = values[i].toString();
		}
		return strings;
	}

	public byte[] decode(byte[] bytes) throws IOException {
		try (var is = inputStreamFactory.create(new ByteArrayInputStream(bytes))) {
			return is.readAllBytes();
		}
	}

	public void decode(Path source, Path target) throws IOException {
		try (var is = inputStreamFactory.create(Files.newInputStream(source))) {
			Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}

}
