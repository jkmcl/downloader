package jkml.downloader.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class LangUtilsTests {

	@Test
	void testGetClassLoader() {
		assertNotNull(LangUtils.getClassLoader());
	}

	@Test
	void testGetRootCause() {
		var rootCause = new IllegalArgumentException();
		var cause = new Exception(rootCause);

		assertEquals(rootCause, LangUtils.getRootCause(rootCause));
		assertEquals(rootCause, LangUtils.getRootCause(cause));
	}

}
