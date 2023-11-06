package jkml.downloader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class ApplicationTests {

	@Test
	void testMain() {
		assertDoesNotThrow(() -> Application.main(new String[] {}));
		assertDoesNotThrow(() -> Application.main(new String[] { "no_such_file.txt" }));
	}

}
