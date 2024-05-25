package jkml.downloader.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.TestUtils;

class ProfileManagerTests {

	private static final Logger logger = LoggerFactory.getLogger(ProfileManagerTests.class);

	private static Profile createProfile() {
		var profile = new Profile();
		profile.setName("name");
		profile.setType(Type.STANDARD);
		profile.setFileUrl(URI.create("https://localhost/file.zip"));
		profile.setPageUrl(URI.create("https://localhost/page.html"));
		profile.setLinkPattern(Pattern.compile("(file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("[.0-9]+"));
		profile.setOutputDirectory(TestUtils.outputDirectory());
		return profile;
	}

	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		logger.info("# Executing {}", testInfo.getDisplayName());
	}

	@Test
	void testValidate_noNameOrDir() {
		var profile = createProfile();
		profile.setName(null);
		profile.setOutputDirectory(null);
		assertEquals(2, ProfileManager.validate(profile).size());
	}

	@Test
	void testValidate_direct() {
		var profile = createProfile();
		profile.setType(Type.DIRECT);
		assertTrue(ProfileManager.validate(profile).isEmpty());

		profile.setFileUrl(null);
		assertEquals(1, ProfileManager.validate(profile).size());
	}

	@Test
	void testValidate_redirect() {
		var profile = createProfile();
		profile.setType(Type.REDIRECT);
		assertTrue(ProfileManager.validate(profile).isEmpty());

		profile.setFileUrl(null);
		assertEquals(1, ProfileManager.validate(profile).size());
	}

	@Test
	void testValidate_indirect() {
		var profile = createProfile();
		profile.setType(Type.STANDARD);
		assertTrue(ProfileManager.validate(profile).isEmpty());

		profile.setPageUrl(null);
		profile.setLinkPattern(null);
		assertEquals(2, ProfileManager.validate(profile).size());
	}

	@ParameterizedTest
	@CsvSource({ "profiles.json, 5" })
	void testLoadProfiles(String name, int expectedSize) throws Exception {
		var manager = new ProfileManager();
		assertTrue(manager.loadProfiles(TestUtils.getResoureAsPath(name)));
		assertTrue(manager.getErrors().isEmpty());
		assertEquals(expectedSize, manager.getProfiles().size());
	}

	@ParameterizedTest
	@ValueSource(strings = { "profiles-error.json" })
	void testLoadProfiles_error(String name) throws Exception {
		var manager = new ProfileManager();
		assertFalse(manager.loadProfiles(TestUtils.getResoureAsPath(name)));
		assertEquals(2, manager.getErrors().size());
		assertTrue(manager.getProfiles().isEmpty());
	}

	@ParameterizedTest
	@ValueSource(strings = { "http.properties", "profiles-null.json" })
	void testLoadProfiles_exception(String name) {
		var manager = new ProfileManager();
		assertThrows(IOException.class, () -> manager.loadProfiles(TestUtils.getResoureAsPath(name)));
	}

}
