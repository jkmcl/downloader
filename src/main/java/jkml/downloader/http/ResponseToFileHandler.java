package jkml.downloader.http;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.util.FileUtils;
import jkml.downloader.util.TimeUtils;

class ResponseToFileHandler extends ResponseHandler<FileResult> {

	private final Logger logger = LoggerFactory.getLogger(ResponseToFileHandler.class);

	private final URI uri;

	private final Path filePath;

	private Instant lastMod = null;

	private Path tmpPath = null;

	private WritableByteChannel channel = null;

	private FileResult result = null;

	public ResponseToFileHandler(URI uri, Path filePath) {
		this.uri = uri;
		this.filePath = filePath;
	}

	private static void checkFileName(URI uri, HttpResponse response) throws IOException {
		var headerFileName = HttpUtils.getFirstParameter(response, HttpHeaders.CONTENT_DISPOSITION, "filename");
		if (headerFileName == null) {
			return;
		}
		if (!headerFileName.equals(FileUtils.getFileName(uri))) {
			throw new IOException("File name in response header is different from that in URL: " + headerFileName);
		}
	}

	static void checkFileContent(Path source, Path target) throws IOException {
		if (Files.notExists(target)) {
			return;
		}

		var sourceSize = Files.size(source);
		var targetSize = Files.size(target);

		if (sourceSize * 2 < targetSize) {
			throw new IOException("New file is smaller than half of existing file: " + source);
		}

		if (sourceSize == targetSize && Files.mismatch(source, target) == -1L) {
			throw new IOException("Content of new file is identical to that of existing file: " + source);
		}
	}

	private void closeChannel() {
		if (channel == null) {
			return;
		}
		try {
			channel.close();
			channel = null;
		} catch (IOException e) {
			logger.error("Failed to close channel", e);
		}
	}

	@Override
	protected boolean isValid(int code) {
		return code == HttpStatus.SC_NOT_MODIFIED || code == HttpStatus.SC_OK;
	}

	private FileResult preprocess(HttpResponse response) throws IOException {
		if (response.getCode() == HttpStatus.SC_NOT_MODIFIED) {
			logger.info("Remote file not modified");
			return new FileResult();
		}

		lastMod = HttpUtils.getTimeHeader(response, HttpHeaders.LAST_MODIFIED);
		if (lastMod == null) {
			throw new IOException("Remote file last modified time not available");
		}
		logger.atDebug().log("Remote file last modified time: {}", TimeUtils.Formatter.format(lastMod));

		checkFileName(uri, response);

		tmpPath = filePath.resolveSibling(filePath.getFileName() + ".partial");

		logger.info("Saving remote content");
		return null;
	}

	private FileResult postprocess() throws IOException {
		logger.info("Finished saving remote content");

		checkFileContent(tmpPath, filePath);
		Files.move(tmpPath, filePath, StandardCopyOption.REPLACE_EXISTING);
		Files.setLastModifiedTime(filePath, FileTime.from(lastMod));

		return new FileResult(Files.getLastModifiedTime(filePath).toInstant());
	}

	@Override
	protected void doStart(HttpResponse response, ContentType contentType) throws HttpException, IOException {
		if ((result = preprocess(response)) != null) {
			return;
		}

		channel = Files.newByteChannel(tmpPath,
				StandardOpenOption.WRITE,
				StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING);
	}

	@Override
	protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
		if (channel == null) {
			return;
		}

		do {
			channel.write(src);
		} while (src.hasRemaining());

		if (endOfStream) {
			closeChannel();
			result = postprocess();
		}
	}

	@Override
	protected FileResult buildResult() {
		return result;
	}

	@Override
	public void releaseResources() {
		closeChannel();
	}

}
