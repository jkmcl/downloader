package jkml.downloader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class DownloaderAppTests {

	@Test
	void testMain_noArg() {
		assertDoesNotThrow(() -> DownloaderApp.main(new String[] {}));
	}

	@Test
	void testMain() {
		assertDoesNotThrow(() -> DownloaderApp.main(new String[] { "no_such_file.json" }));
	}

}
