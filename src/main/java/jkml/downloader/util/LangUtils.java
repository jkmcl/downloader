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
			loader = ClassLoader.getSystemClassLoader();
		}
		return loader;
	}

	public static Throwable getRootCause(Throwable throwable) {
		var causes = new ArrayList<Throwable>();
		var cause = throwable;
		while (cause != null && !causes.contains(cause)) {
			causes.add(cause);
			cause = cause.getCause();
		}
		return causes.isEmpty() ? null : causes.getLast();
	}

	public static <V> V getUninterruptibly(Future<V> future) throws ExecutionException {
		var interrupted = false;
		try {
			while (true) {
				try {
					return future.get();
				} catch (InterruptedException _) {
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
