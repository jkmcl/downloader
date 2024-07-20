package jkml.downloader.util;

import java.util.ArrayList;

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
		var causes = new ArrayList<Throwable>();
		while (throwable != null && !causes.contains(throwable)) {
			causes.add(throwable);
			throwable = throwable.getCause();
		}
		return causes.isEmpty() ? null : causes.get(causes.size() - 1);
	}

}
