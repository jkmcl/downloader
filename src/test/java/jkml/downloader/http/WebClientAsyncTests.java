package jkml.downloader.http;

class WebClientAsyncTests extends WebClientTests {

	@Override
	protected WebClient createWebClient() {
		return new WebClient(false);
	}

}
