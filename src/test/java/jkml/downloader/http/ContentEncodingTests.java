package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

class ContentEncodingTests {

	@Test
	void testToString() {
		for (var val : ContentEncoding.values()) {
			assertEquals(val.name().toLowerCase(), val.toString());
		}
	}

	@Test
	void testFromString() {
		for (var val : ContentEncoding.values()) {
			assertEquals(val, ContentEncoding.fromString(val.toString()));
		}
	}

	@Test
	void testStrings() {
		var values = ContentEncoding.values();
		var strings = ContentEncoding.strings();
		assertEquals(values.length, strings.length);
		for (var i = 0; i < values.length; ++i) {
			assertEquals(values[i].toString(), strings[i]);
		}
	}

	@Test
	void testDecode() throws IOException {
		var expected = "Hello".getBytes(StandardCharsets.UTF_8);
		try (var baos = new ByteArrayOutputStream()) {
			try (var os = new GZIPOutputStream(baos)) {
				os.write(expected);
			}
			var encodedBytes = baos.toByteArray();
			assertArrayEquals(expected, ContentEncoding.GZIP.decode(encodedBytes));
		}
	}

}
