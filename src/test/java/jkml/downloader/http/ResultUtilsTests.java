package jkml.downloader.http;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ResultUtilsTests {

	private static final String ERR_MSG = "hello";

	@Test
	void testFileResult() {
		var result = ResultUtils.fileResult(new Throwable(ERR_MSG));
		assertTrue(result.errorMessage().contains(ERR_MSG));
	}

	@Test
	void testLinkResult() {
		var result = ResultUtils.linkResult(new Throwable(ERR_MSG));
		assertTrue(result.errorMessage().contains(ERR_MSG));
	}

	@Test
	void testTextResult() {
		var result = ResultUtils.textResult(new Throwable(ERR_MSG));
		assertTrue(result.errorMessage().contains(ERR_MSG));
	}

}
