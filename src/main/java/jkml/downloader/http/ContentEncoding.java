package jkml.downloader.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;

import org.apache.hc.client5.http.entity.DeflateInputStream;
import org.apache.hc.client5.http.entity.InputStreamFactory;

enum ContentEncoding {

	GZIP(GZIPInputStream::new), DEFLATE(DeflateInputStream::new);

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
		var values = values();
		var strings = new String[values.length];
		for (var i = 0; i < values.length; ++i) {
			strings[i] = values[i].toString();
		}
		return strings;
	}

	public byte[] decode(byte[] bytes) {
		try (var is = inputStreamFactory.create(new ByteArrayInputStream(bytes))) {
			return is.readAllBytes();
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

}
