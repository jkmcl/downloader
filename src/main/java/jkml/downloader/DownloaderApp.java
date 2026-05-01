package jkml.downloader;

import java.nio.file.Path;

public class DownloaderApp {

	public static void main(String... args) {
		if (args.length != 1) {
			System.out.printf("Usage: %s <file>%n", DownloaderApp.class.getName());
			return;
		}
		try (var downloader = new Downloader()) {
			downloader.download(Path.of(args[0]));
		}
	}

}
