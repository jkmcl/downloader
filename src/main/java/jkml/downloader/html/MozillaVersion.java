package jkml.downloader.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

class MozillaVersion {

	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\.");

	private final String source;

	private final List<Integer> parts;

	private MozillaVersion(String source, List<Integer> parts) {
		this.source = source;
		this.parts = parts;
	}

	public static MozillaVersion parse(String source) {
		var parts = new ArrayList<Integer>();

		for (var part : SPLIT_PATTERN.split(source, -1)) {
			try {
				var i = Integer.parseInt(part);
				if (i < 0) {
					throw new IllegalArgumentException("Negative number in version: " + part);
				}
				parts.add(i);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid character(s) in version: " + part);
			}
		}

		return new MozillaVersion(source, Collections.unmodifiableList(parts));
	}

	public List<Integer> getParts() {
		return parts;
	}

	@Override
	public String toString() {
		return source;
	}

	private static int getPart(List<Integer> parts, int index) {
		return (index < parts.size()) ? parts.get(index) : 0;
	}

	public static int compare(MozillaVersion v1, MozillaVersion v2) {
		var parts1 = v1.getParts();
		var parts2 = v2.getParts();
		var n = Math.max(parts1.size(), parts2.size());
		for (var i = 0; i < n; ++i) {
			var p1 = getPart(parts1, i);
			var p2 = getPart(parts2, i);
			if (p1 < p2) {
				return -1;
			}
			if (p1 > p2) {
				return 1;
			}
		}
		return 0;
	}

}
