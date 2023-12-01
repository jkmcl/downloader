package jkml.downloader.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

class MozillaVersion implements Comparable<MozillaVersion> {

	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\.");

	private final String source;

	private final List<Integer> parts;

	private MozillaVersion(String source, List<Integer> parts) {
		this.source = source;
		this.parts = parts;
	}

	public static MozillaVersion parse(String source) {
		var parts = new ArrayList<Integer>();

		var index = -1;
		var lastNonZeroIndex = -1;
		for (String part : SPLIT_PATTERN.split(source, -1)) {
			++index;
			try {
				var i = Integer.parseInt(part);
				if (i < 0) {
					throw new IllegalArgumentException("Negative number in version: " + part);
				}
				if (i > 0) {
					lastNonZeroIndex = index;
				}
				parts.add(i);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid character(s) in version: " + part);
			}
		}

		return new MozillaVersion(source, parts.subList(0, lastNonZeroIndex + 1));
	}

	public String getSource() {
		return source;
	}

	public List<Integer> getParts() {
		return parts;
	}

	@Override
	public int hashCode() {
		return Objects.hash(parts);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MozillaVersion)) {
			return false;
		}
		return Objects.equals(parts, ((MozillaVersion) obj).parts);
	}

	private int getPart(int index) {
		return (index < parts.size()) ? parts.get(index) : 0;
	}

	@Override
	public int compareTo(MozillaVersion other) {
		var n = Math.max(parts.size(), other.parts.size());
		for (var i = 0; i < n; ++i) {
			var thisPart = getPart(i);
			var thatPart = other.getPart(i);
			if (thisPart < thatPart) {
				return -1;
			}
			if (thisPart > thatPart) {
				return 1;
			}
		}
		return 0;
	}

}
