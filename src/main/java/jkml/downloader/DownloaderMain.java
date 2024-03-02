package jkml.downloader;

import java.nio.file.Files;
import java.nio.file.Path;

class DownloaderMain {

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			System.out.println("Usage: " + DownloaderMain.class.getName() + " <file>");
			return;
		}

		var file = Path.of(args[0]);

		if (Files.notExists(file)) {
			System.out.println("File does not exist: " + file);
			return;
		}

		try (var downloader = new Downloader(System.out)) {
			downloader.download(file);
		}
	}

}
