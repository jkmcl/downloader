package jkml.downloader.html;

/**
 * Finds {@code src} attributes and returns their values.
 */
class SrcAttributeFinder {

	private static final String MARKER = "src=\"";

	private final String html;

	private int pos;

	private int start;

	private int end;

	SrcAttributeFinder(String html) {
		this.html = html;
		start = -1;
	}

	/**
	 * Attempts to find the next {@code src} attribute.
	 *
	 * @return {@code true} if a {@code src} attribute is found
	 */
	boolean find() {
		start = html.indexOf(MARKER, pos);
		if (start == -1) {
			return false;
		}
		start += MARKER.length();

		end = html.indexOf('"', start);
		if (end == -1) {
			start = -1;
			return false;
		}

		pos = end + 1;
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
		if (start == -1) {
			throw new IllegalStateException();
		}
		return html.substring(start, end);
	}

}
