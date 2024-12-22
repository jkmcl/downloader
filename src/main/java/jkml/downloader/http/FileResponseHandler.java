package jkml.downloader.http;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.util.FileUtils;
import jkml.downloader.util.TimeUtils;

class FileResponseHandler extends ResponseHandler<FileResult> {

	private final Logger logger = LoggerFactory.getLogger(FileResponseHandler.class);

	private final Set<StandardOpenOption> openOptions = EnumSet.of(
			StandardOpenOption.WRITE,
			StandardOpenOption.CREATE,
			StandardOpenOption.TRUNCATE_EXISTING);

	private final URI uri;

	private final Path path;

	private ContentEncoding contentEncoding;

	private Instant lastModified;

	private Path tmpPath;

	private WritableByteChannel channel;

	public FileResponseHandler(URI uri, Path path) {
		this.uri = uri;
		this.path = path;
	}

	static void checkFileName(String fileName, HttpResponse response) throws IOException {
		var headerFileName = HttpUtils.getParameter(response, HttpHeaders.CONTENT_DISPOSITION, "filename");

		if (headerFileName != null && !headerFileName.equals(fileName)) {
			throw new IOException("Mismatched file name in response header: " + headerFileName);
		}
	}

	static void checkFileContent(Path newFile, Path oldFile) throws IOException {
		if (Files.notExists(oldFile)) {
			return;
		}

		if (Files.size(newFile) * 2 < Files.size(oldFile)) {
			throw new IOException("New file smaller than half of existing file: " + newFile);
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
	protected void start(HttpResponse response, ContentEncoding contentEncoding, ContentType contentType) throws IOException {
		if (response.getCode() == HttpStatus.SC_NOT_MODIFIED) {
			logger.info("Remote file not modified");
			return;
		}

		this.contentEncoding = contentEncoding;

		if ((lastModified = HttpUtils.getTimeHeader(response, HttpHeaders.LAST_MODIFIED)) == null) {
			throw new IOException("Remote file last modified time not available");
		}
		logger.atDebug().log("Remote file last modified time: {}", TimeUtils.Formatter.format(lastModified));

		var fileName = FileUtils.getFileName(uri);
		checkFileName(fileName, response);
		tmpPath = path.resolveSibling(fileName + ".partial");

		logger.info("Saving remote content");
		channel = Files.newByteChannel(tmpPath, openOptions);
	}

	@Override
	protected void data(ByteBuffer src, boolean endOfStream) throws IOException {
		do {
			channel.write(src);
		} while (src.hasRemaining());

		if (endOfStream) {
			closeChannel();
			logger.info("Finished saving remote content");

			// Decode file content
			if (contentEncoding != null) {
				logger.debug("Decoding remote content");
				var decodedPath = Path.of(tmpPath + ".decoded");
				contentEncoding.decode(tmpPath, decodedPath);
				Files.move(decodedPath, tmpPath, StandardCopyOption.REPLACE_EXISTING);
				logger.debug("Finished decoding remote content");
			}

			// Check file content
			checkFileContent(tmpPath, path);

			// Update file last modified time
			Files.setLastModifiedTime(tmpPath, FileTime.from(lastModified));

			// Rename file
			Files.move(tmpPath, path, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	@Override
	protected FileResult buildResult() {
		return (lastModified == null) ? new FileResult() : new FileResult(lastModified);
	}

	@Override
	public void releaseResources() {
		closeChannel();
	}

}
