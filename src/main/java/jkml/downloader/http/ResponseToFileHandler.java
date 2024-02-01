package jkml.downloader.http;

import java.io.IOException;
import java.io.UncheckedIOException;
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

import jkml.downloader.util.TimeUtils;

class ResponseToFileHandler extends ResponseHandler<FileResult> {

	private final Logger logger = LoggerFactory.getLogger(ResponseToFileHandler.class);

	private final Path path;

	private Instant modifiedTime;

	private Path tmpPath;

	private WritableByteChannel channel;

	public ResponseToFileHandler(Path path) {
		this.path = path;
	}

	static void checkFileName(HttpResponse response, String fileName) throws IOException {
		var headerFileName = HttpUtils.getFirstParameter(response, HttpHeaders.CONTENT_DISPOSITION, "filename");
		if (headerFileName == null) {
			return;
		}

		if (!headerFileName.equals(fileName)) {
			throw new IOException("Mismatched file name in response header: " + headerFileName);
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
			throw new UncheckedIOException(e.getMessage(), e);
		}
	}

	@Override
	protected boolean isValid(int code) {
		return code == HttpStatus.SC_NOT_MODIFIED || code == HttpStatus.SC_OK;
	}

	@Override
	protected void doStart(HttpResponse response, ContentType contentType) throws HttpException, IOException {
		if (response.getCode() == HttpStatus.SC_NOT_MODIFIED) {
			logger.info("Remote file not modified");
			return;
		}

		modifiedTime = HttpUtils.getTimeHeader(response, HttpHeaders.LAST_MODIFIED);
		if (modifiedTime == null) {
			throw new IOException("Remote file last modified time not available");
		}
		logger.atDebug().log("Remote file last modified time: {}", TimeUtils.Formatter.format(modifiedTime));

		var fileName = path.getFileName().toString();
		checkFileName(response, fileName);

		logger.info("Saving remote content");
		tmpPath = path.resolveSibling(fileName + ".partial");
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
			checkFileContent(tmpPath, path);
			Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
			Files.setLastModifiedTime(path, FileTime.from(modifiedTime));
			logger.info("Finished saving remote content");
		}
	}

	@Override
	protected FileResult buildResult() {
		return (modifiedTime == null) ? new FileResult() : new FileResult(modifiedTime);
	}

	@Override
	public void releaseResources() {
		closeChannel();
	}

}
