package jkml.downloader.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;

import org.apache.hc.client5.http.entity.DeflateInputStream;

enum ContentEncoding {

	GZIP(GZIPInputStream::new), DEFLATE(DeflateInputStream::new);

	@FunctionalInterface
	private interface InputStreamFactory {
		InputStream newInputStream(InputStream in) throws IOException;
	}

	private final InputStreamFactory inputStreamFactory;

	ContentEncoding(InputStreamFactory inputStreamFactory) {
		this.inputStreamFactory = inputStreamFactory;
	}

	@Override
	public String toString() {
		return name().toLowerCase();
	}

	public static ContentEncoding fromString(String str) {
		return valueOf(str.toUpperCase());
	}

	public static String[] strings() {
		var result = new String[values().length];
		var i = 0;
		for (var v : values()) {
			result[i++] = v.toString();
		}
		return result;
	}

	public byte[] decode(byte[] bytes) {
		try (var is = inputStreamFactory.newInputStream(new ByteArrayInputStream(bytes))) {
			return is.readAllBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

}
