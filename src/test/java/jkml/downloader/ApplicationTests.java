package jkml.downloader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ApplicationTests {

	@Test
	void testMain_noArg() {
		assertDoesNotThrow(() -> Application.main(new String[] {}));
	}

	@Test
	void testMain_noFile() {
		assertDoesNotThrow(() -> Application.main(new String[] { "no_such_file.txt" }));
	}

	@Test
	void testMain() {
		assertDoesNotThrow(() -> Application.main(new String[] { "src/test/resources/profiles-empty.json" }));
	}

}
