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

	public static Throwable getRootCause(Throwable throwable) {
		Throwable cause;
		while ((cause = throwable.getCause()) != null && cause != throwable) {
			throwable = cause;
		}
		return throwable;
	}

}
