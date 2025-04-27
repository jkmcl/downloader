package jkml.downloader.http;

public class ResultUtils {

	private ResultUtils() {
	}

	private static String errorMessage(Throwable exception) {
		return "%s: %s".formatted(exception.getClass().getName(), exception.getMessage());
	}

	public static FileResult fileResult(Throwable exception) {
		return new FileResult(Status.ERROR, null, errorMessage(exception));
	}

	public static LinkResult linkResult(Throwable exception) {
		return new LinkResult(Status.ERROR, null, errorMessage(exception));
	}

	public static TextResult textResult(Throwable exception) {
		return new TextResult(Status.ERROR, null, errorMessage(exception));
	}

}
