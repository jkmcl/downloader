package jkml.downloader.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

public class TimeUtils {

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	private TimeUtils() {
	}

	public static String format(TemporalAccessor temporal) {
		return formatter.format(temporal);
	}
}
