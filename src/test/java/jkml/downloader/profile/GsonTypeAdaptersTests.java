package jkml.downloader.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;

class GsonTypeAdaptersTests {

	private final GsonBuilder gsonBuilder = new GsonBuilder().disableHtmlEscaping();

	@Test
	void testPath() throws IOException {
		var gson = gsonBuilder.registerTypeHierarchyAdapter(Path.class, GsonTypeAdapters.PATH).create();
		var source = "expected";
		var adapter = gson.getAdapter(Path.class);
		var object = adapter.fromJsonTree(new JsonPrimitive(source));
		assertEquals("\"" + source + "\"", adapter.toJsonTree(object).toString());
	}

	@Test
	void testPattern() throws IOException {
		var gson = gsonBuilder.registerTypeAdapter(Pattern.class, GsonTypeAdapters.PATTERN).create();
		var source = "expected";
		var adapter = gson.getAdapter(Pattern.class);
		var object = adapter.fromJsonTree(new JsonPrimitive(source));
		assertEquals("\"" + source + "\"", adapter.toJsonTree(object).toString());
	}

}
