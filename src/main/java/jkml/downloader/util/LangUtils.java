package jkml.downloader.util;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

	public static <V> V getUninterruptibly(Future<V> future) throws ExecutionException {
		var interrupted = false;
		try {
			while (true) {
				try {
					return future.get();
				} catch (InterruptedException e) {
					interrupted = true;
				}
			}
		} finally {
			if (interrupted) {
				Thread.currentThread().interrupt();
			}
		}
	}

}
