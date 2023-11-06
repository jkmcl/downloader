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
	void testGetRequiredProperty() throws Exception {
		var helper = new PropertiesHelper(FILE_NAME);
		assertNotNull(helper.getRequiredProperty("a"));
		assertThrows(IllegalArgumentException.class, () -> helper.getRequiredProperty("no such key"));
	}

}
