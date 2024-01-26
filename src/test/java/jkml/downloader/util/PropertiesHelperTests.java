package jkml.downloader.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PropertiesHelperTests {

	private static final String FILE_NAME = "test.properties";

	@Test
	void testPropertiesHelper() {
		assertDoesNotThrow(() -> new PropertiesHelper(FILE_NAME));
		assertThrows(IllegalArgumentException.class, () -> new PropertiesHelper("no such file"));
	}

	@Test
	void testGetRequired() throws Exception {
		var helper = new PropertiesHelper(FILE_NAME);
		assertNotNull(helper.getRequired("a"));
		assertThrows(IllegalArgumentException.class, () -> helper.getRequired("no such key"));
	}

}
