package jkml.downloader.profile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.regex.Pattern;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

class GsonTypeAdapters {

	private GsonTypeAdapters() {
	}

	public static final TypeAdapter<Pattern> PATTERN = new TypeAdapter<Pattern>() {

		@Override
		public Pattern read(JsonReader in) throws IOException {
			return Pattern.compile(in.nextString());
		}

		@Override
		public void write(JsonWriter out, Pattern value) throws IOException {
			out.value(value.toString());
		}

	}.nullSafe();

	public static final TypeAdapter<Path> PATH = new TypeAdapter<Path>() {

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
