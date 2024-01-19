package jkml.downloader.profile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class GsonUtils {

	private GsonUtils() {
	}

	public static Gson createGson() {
		return new GsonBuilder()
				.disableHtmlEscaping()
				.disableJdkUnsafe()
				.registerTypeAdapter(Pattern.class, GsonUtils.PatternAdapter)
				.registerTypeHierarchyAdapter(Path.class, GsonUtils.PathAdapter)
				.create();
	}

	static final TypeAdapter<Pattern> PatternAdapter = new TypeAdapter<Pattern>() {

		@Override
		public Pattern read(JsonReader in) throws IOException {
			return Pattern.compile(in.nextString());
		}

		@Override
		public void write(JsonWriter out, Pattern value) throws IOException {
			out.value(value.toString());
		}

	}.nullSafe();

	static final TypeAdapter<Path> PathAdapter = new TypeAdapter<Path>() {

		@Override
		public Path read(JsonReader in) throws IOException {
			return Path.of(in.nextString());
		}

		@Override
		public void write(JsonWriter out, Path value) throws IOException {
			out.value(value.toString());
		}

	}.nullSafe();

}
