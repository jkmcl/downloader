package jkml.downloader.util;

public class LangUtils {

	private LangUtils() {
	}

	public static ClassLoader getClassLoader() {
		var loader = Thread.currentThread().getContextClassLoader();
		if (loader == null) {
			loader = LangUtils.class.getClassLoader();
			if (loader == null) {
				loader = ClassLoader.getSystemClassLoader();
			}
		}
		return loader;
	}

}
