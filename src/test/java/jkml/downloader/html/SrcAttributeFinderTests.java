package jkml.downloader.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SrcAttributeFinderTests {

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "src", "src=", "src=\"" })
	void testFind_false(String html) {
		var finder = new SrcAttributeFinder(html);
		assertFalse(finder.find());
		assertThrows(IllegalStateException.class, finder::getValue);
	}

	@ParameterizedTest
	@ValueSource(strings = { "src=\"\"", "src=\"a\"" })
	void testFind_true(String html) {
		var finder = new SrcAttributeFinder(html);
		assertTrue(finder.find());
		assertNotNull(finder.getValue());
	}

	@Test
	void testGetValue() {
		var finder = new SrcAttributeFinder("src=\"\" src=\"a\"");

		assertThrows(IllegalStateException.class, finder::getValue);

		assertTrue(finder.find());
		assertEquals("", finder.getValue());

		assertTrue(finder.find());
		assertEquals("a", finder.getValue());

		assertFalse(finder.find());
		assertThrows(IllegalStateException.class, finder::getValue);
	}

}
