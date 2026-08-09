package jkml.downloader;

import java.net.URI;

import jkml.downloader.util.FileUtils;

record Downloadable(URI uri, String fileName) {

	Downloadable(URI uri) {
		this(uri, FileUtils.getFileName(uri));
	}

}
