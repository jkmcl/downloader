package jkml.downloader.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonPrimitive;

class GsonUtilsTests {

	@Test
	void testPatternAdapter() {
		var source = "expected";
		var adapter = GsonUtils.createGson().getAdapter(Pattern.class);
		var object = adapter.fromJsonTree(new JsonPrimitive(source));
		assertEquals("\"" + source + "\"", adapter.toJsonTree(object).toString());
	}

	@Test
	void testInstantAdapter() {
		var source = Instant.now().toString();
		var adapter = GsonUtils.createGson().getAdapter(Instant.class);
		var object = adapter.fromJsonTree(new JsonPrimitive(source));
		assertEquals("\"" + source + "\"", adapter.toJsonTree(object).toString());
	}

	@Test
	void testPathAdapter() {
		var source = "expected";
		var adapter = GsonUtils.createGson().getAdapter(Path.class);
		var object = adapter.fromJsonTree(new JsonPrimitive(source));
		assertEquals("\"" + source + "\"", adapter.toJsonTree(object).toString());
	}

}
