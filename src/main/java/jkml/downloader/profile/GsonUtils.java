package jkml.downloader.profile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.regex.Pattern;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

class GsonUtils {

	private GsonUtils() {
	}

	public static Gson createGson() {
		return new GsonBuilder()
				.disableHtmlEscaping()
				.disableJdkUnsafe()
				.registerTypeAdapter(Pattern.class, PatternAdapter)
				.registerTypeAdapter(Instant.class, InstantAdapter)
				.registerTypeHierarchyAdapter(Path.class, PathAdapter)
				.setStrictness(Strictness.STRICT)
				.create();
	}

	private static final TypeAdapter<Pattern> PatternAdapter = new TypeAdapter<Pattern>() {

		@Override
		public Pattern read(JsonReader in) throws IOException {
			return Pattern.compile(in.nextString());
		}

		@Override
		public void write(JsonWriter out, Pattern value) throws IOException {
			out.value(value.toString());
		}

	}.nullSafe();

	private static final TypeAdapter<Instant> InstantAdapter = new TypeAdapter<Instant>() {

		@Override
		public Instant read(JsonReader in) throws IOException {
			return Instant.parse(in.nextString());
		}

		@Override
		public void write(JsonWriter out, Instant value) throws IOException {
			out.value(value.toString());
		}

	}.nullSafe();

	private static final TypeAdapter<Path> PathAdapter = new TypeAdapter<Path>() {

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
