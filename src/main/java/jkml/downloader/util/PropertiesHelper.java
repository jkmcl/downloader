package jkml.downloader.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

public class PropertiesHelper {

	private final Properties properties = new Properties();

	private PropertiesHelper() {
	}

	public static PropertiesHelper create(String name) {
		try (var stream = LangUtils.getClassLoader().getResourceAsStream(name)) {
			if (stream == null) {
				throw new IllegalArgumentException("Resource not found: " + name);
			}
			var instance = new PropertiesHelper();
			instance.properties.load(stream);
			return instance;
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

	public String getRequired(String key) {
		var value = properties.getProperty(key);
		if (value == null) {
			throw new IllegalArgumentException("Property not found: " + key);
		}
		return value;
	}

}
