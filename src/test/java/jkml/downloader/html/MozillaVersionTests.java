package jkml.downloader.html;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MozillaVersionTests {

	@Test
	void testEquals() {
		var x = MozillaVersion.parse("1");
		var y = MozillaVersion.parse("1.0");
		var z = MozillaVersion.parse("1.0.0");

		// Reflexive
		assertTrue(x.equals(x));

		// Symmetric
		assertEquals(true, x.equals(y));
		assertEquals(true, y.equals(x));

		// Transitive
		assertEquals(true, y.equals(z));
		assertEquals(true, x.equals(z));

		assertFalse(x.equals(null));
	}

	@Test
	void testHashCode() {
		var x = MozillaVersion.parse("1");
		var y = MozillaVersion.parse("1.0");
		var z = MozillaVersion.parse("1.0.0");

		assertEquals(x.hashCode(), y.hashCode());
		assertEquals(y.hashCode(), z.hashCode());
	}

	@Test
	void testGetSource() {
		assertEquals("1.0.1", MozillaVersion.parse("1.0.1").getSource());
	}

	@Test
	void testParse_null() {
		assertThrows(NullPointerException.class, () -> MozillaVersion.parse(null));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", " ", "1.", ".", ".1", "1.0b1", "-1" })
	void testParse_invalid(String value) {
		assertThrows(IllegalArgumentException.class, () -> MozillaVersion.parse(value));
	}

	@Test
	void testParse() {
		assertEquals(MozillaVersion.parse("0"), MozillaVersion.parse("0.0"));
		assertEquals(MozillaVersion.parse("1"), MozillaVersion.parse("1.0"));

		assertNotEquals(MozillaVersion.parse("1.0"), MozillaVersion.parse("0.1"));
		assertNotEquals(MozillaVersion.parse("1.0"), MozillaVersion.parse("1.1"));
	}

	@Test
	void testGetParts() {
		assertIterableEquals(List.of(), MozillaVersion.parse("0.0.0").getParts());
		assertIterableEquals(List.of(0, 0, 1), MozillaVersion.parse("0.0.1").getParts());
		assertIterableEquals(List.of(0, 1), MozillaVersion.parse("0.1.0").getParts());
		assertIterableEquals(List.of(0, 1, 1), MozillaVersion.parse("0.1.1").getParts());
		assertIterableEquals(List.of(1), MozillaVersion.parse("1.0.0").getParts());
		assertIterableEquals(List.of(1, 0, 1), MozillaVersion.parse("1.0.1").getParts());
		assertIterableEquals(List.of(1, 1), MozillaVersion.parse("1.1.0").getParts());
		assertIterableEquals(List.of(1, 1, 1), MozillaVersion.parse("1.1.1").getParts());
	}

	@Test
	void testCompareTo_null() {
		var version = MozillaVersion.parse("76.0");
		assertThrows(NullPointerException.class, () -> version.compareTo(null));
	}

	@Test
	void testCompareTo_equal() {
		var x = MozillaVersion.parse("1");
		var y = MozillaVersion.parse("1.0");
		var z = MozillaVersion.parse("1.0.0");

		assertEquals(0, x.compareTo(y));
		assertEquals(0, y.compareTo(z));
		assertEquals(0, x.compareTo(z));
	}

	@ParameterizedTest
	@CsvSource(value = { "75.0|75.0.1", "74.0.1|75.0", "74.0|75.0" }, delimiter = '|')
	void testCompareTo_notEqual(String olderVersionString, String newerVersionString) {
		var olderVersion = MozillaVersion.parse(olderVersionString);
		var newerVersion = MozillaVersion.parse(newerVersionString);

		assertNotEquals(olderVersion, newerVersion);
		assertTrue(olderVersion.compareTo(newerVersion) < 0);
		assertTrue(newerVersion.compareTo(olderVersion) > 0);
	}

}
