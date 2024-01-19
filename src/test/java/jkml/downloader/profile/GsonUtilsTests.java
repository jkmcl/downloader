package jkml.downloader.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonPrimitive;

class GsonUtilsTests {

	@Test
	void testPatternAdapter() throws IOException {
		var source = "expected";
		var adapter = GsonUtils.createGson().getAdapter(Pattern.class);
		var object = adapter.fromJsonTree(new JsonPrimitive(source));
		assertEquals("\"" + source + "\"", adapter.toJsonTree(object).toString());
	}

	@Test
	void testPathAdapter() throws IOException {
		var source = "expected";
		var adapter = GsonUtils.createGson().getAdapter(Path.class);
		var object = adapter.fromJsonTree(new JsonPrimitive(source));
		assertEquals("\"" + source + "\"", adapter.toJsonTree(object).toString());
	}

}
