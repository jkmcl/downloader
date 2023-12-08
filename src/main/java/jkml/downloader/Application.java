package jkml.downloader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jkml.downloader.core.DownloaderCore;

public class Application {

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.out.println("Usage: " + Application.class.getName() + " <file>");
			return;
		}

		var file = Path.of(args[0]);

		if (Files.notExists(file)) {
			System.out.println("File does not exist: " + file);
			return;
		}

		try (var core = new DownloaderCore("classic".equals(System.getProperty("http.client")))) {
			core.download(file);
		}
	}

}
