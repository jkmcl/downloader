package jkml.downloader.html;

/**
 * Finds {@code src} attributes and returns their values.
 */
class SrcAttributeFinder {

	private static final String START = "src=\"";

	private final String html;

	private int pos;

	private String value;

	SrcAttributeFinder(String html) {
		this.html = html;
	}

	/**
	 * Attempts to find the next {@code src} attribute.
	 *
	 * @return {@code true} if a {@code src} attribute is found
	 */
	boolean find() {
		var start = html.indexOf(START, pos);
		if (start == -1) {
			value = null;
			return false;
		}
		start += START.length();

		var end = html.indexOf('"', start);
		if (end == -1) {
			value = null;
			return false;
		}

		pos = end + 1;
		value = html.substring(start, end);
		return true;
	}

	/**
	 * Returns the value of the {@code src} attribute found previously.
	 *
	 * @return Value of the {@code src} attribute found previously.
	 * @throws IllegalStateException if {@link #find()} has not been called, or the
	 *                               previous call returned {@code false}.
	 */
	String getValue() {
		if (value == null) {
			throw new IllegalStateException();
		}
		return value;
	}

}
