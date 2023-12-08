package jkml.downloader.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

public class PropertiesHelper {

	private final Properties properties = new Properties();

	public PropertiesHelper(String name) {
		try (var stream = LangUtils.getClassLoader().getResourceAsStream(name)) {
			if (stream == null) {
				throw new IllegalArgumentException("Resource not found: " + name);
			}
			properties.load(stream);
		} catch (IOException e) {
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

	public String getRequiredProperty(String key) {
		var value = properties.getProperty(key);
		if (value == null) {
			throw new IllegalArgumentException("Property not found: " + key);
		}
		return value;
	}

}
