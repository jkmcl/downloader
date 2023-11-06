package jkml.downloader.util;

public class StringUtils {

	public static final String EMPTY = "";

	private StringUtils() {
	}

	public static boolean isNullOrBlank(String string) {
		return string == null || string.isBlank();
	}

}
