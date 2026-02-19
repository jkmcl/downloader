package jkml.downloader.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;

class LangUtilsTests {

	@Test
	void testGetClassLoader() {
		assertNotNull(LangUtils.getClassLoader());
	}

	@Test
	void testGetRootCause() {
		var rootCause = new IllegalArgumentException();
		var cause = new Exception(rootCause);

		assertNull(LangUtils.getRootCause(null));
		assertEquals(rootCause, LangUtils.getRootCause(rootCause));
		assertEquals(rootCause, LangUtils.getRootCause(cause));
	}

	@Test
	void testGetUninterruptibly() throws Exception {
		@SuppressWarnings("unchecked")
		Future<Object> mockFuture = mock(Future.class);

		when(mockFuture.get()).thenReturn(this);
		assertNotNull(LangUtils.getUninterruptibly(mockFuture));

		when(mockFuture.get()).thenThrow(InterruptedException.class).thenReturn(this);
		assertNotNull(LangUtils.getUninterruptibly(mockFuture));
	}

}
