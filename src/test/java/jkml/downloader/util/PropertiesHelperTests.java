package jkml.downloader.util;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PropertiesHelperTests {

	private static final String FILE_NAME = "test.properties";

	@Test
	void testPropertiesHelper() {
		assertNotNull(PropertiesHelper.create(FILE_NAME));
		assertThrows(IllegalArgumentException.class, () -> PropertiesHelper.create("no such file"));
	}

	@Test
	void testGetRequired() {
		var helper = PropertiesHelper.create(FILE_NAME);
		assertNotNull(helper.getRequired("a"));
		assertThrows(IllegalArgumentException.class, () -> helper.getRequired("no such key"));
	}

}
