package jkml.downloader.html;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

class MozillaVersion implements Comparable<MozillaVersion> {

	private static final Pattern SPLIT_PATTERN = Pattern.compile("\\.");

	private final String string;

	private final List<Integer> partList;

	private MozillaVersion(String string, List<Integer> partList) {
		this.string = string;
		this.partList = partList;
	}

	public static MozillaVersion parse(String string) {
		var partList = new ArrayList<Integer>();

		var index = -1;
		var lastNonZeroIndex = -1;
		for (String part : SPLIT_PATTERN.split(string, -1)) {
			++index;
			try {
				var i = Integer.parseInt(part);
				if (i < 0) {
					throw new IllegalArgumentException("Negative number in version: " + part);
				}
				if (i > 0) {
					lastNonZeroIndex = index;
				}
				partList.add(i);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid character(s) in version: " + part);
			}
		}

		return new MozillaVersion(string, partList.subList(0, lastNonZeroIndex + 1));
	}

	List<Integer> getPartList() {
		return partList;
	}

	@Override
	public String toString() {
		return string;
	}

	@Override
	public int hashCode() {
		return Objects.hash(partList);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof MozillaVersion)) {
			return false;
		}
		return hashCode() == obj.hashCode();
	}

	private int getPart(int index) {
		return (index < partList.size()) ? partList.get(index) : 0;
	}

	@Override
	public int compareTo(MozillaVersion obj) {
		var n = Math.max(partList.size(), obj.partList.size());
		for (var i = 0; i < n; ++i) {
			var thisPart = getPart(i);
			var thatPart = obj.getPart(i);
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
