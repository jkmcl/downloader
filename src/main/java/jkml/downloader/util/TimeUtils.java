package jkml.downloader.util;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeUtils {

	public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	private TimeUtils() {
	}

}
