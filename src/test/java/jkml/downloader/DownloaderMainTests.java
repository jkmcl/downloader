package jkml.downloader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class DownloaderMainTests {

	@Test
	void testMain_noArg() {
		assertDoesNotThrow(() -> DownloaderMain.main(new String[] {}));
	}

	@Test
	void testMain_noFile() {
		assertDoesNotThrow(() -> DownloaderMain.main(new String[] { "no_such_file.txt" }));
	}

	@Test
	void testMain() {
		assertDoesNotThrow(() -> DownloaderMain.main(new String[] { "src/test/resources/profiles-empty.json" }));
	}

}
