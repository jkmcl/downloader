package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import jkml.downloader.util.TestUtils;

class ContentEncodingTests {

	@Test
	void testToString() {
		for (var val : ContentEncoding.values()) {
			assertEquals(val.name().toLowerCase(), val.toString());
		}
	}

	@Test
	void testFromString() {
		assertNull(ContentEncoding.fromString(null));
		assertNull(ContentEncoding.fromString(""));
		assertNull(ContentEncoding.fromString("identity"));
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

	@Test
	void testDecode_file() throws IOException {
		var originalFile = Path.of("src/test/resources/test.properties");
		var originalFileName = originalFile.getFileName().toString();

		var outDir = TestUtils.outputDirectory();
		var encodedFile = outDir.resolve(originalFileName + ".encoded");
		var decodedFile = outDir.resolve(originalFileName + ".decoded");

		Files.deleteIfExists(encodedFile);
		Files.deleteIfExists(decodedFile);
		Files.createDirectories(outDir);

		try (var is = Files.newInputStream(originalFile);
				var os = new GZIPOutputStream(Files.newOutputStream(encodedFile))) {
			is.transferTo(os);
		}

		ContentEncoding.GZIP.decode(encodedFile, decodedFile);

		assertEquals(-1, Files.mismatch(originalFile, decodedFile));
	}

}
