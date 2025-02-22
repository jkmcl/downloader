package jkml.downloader.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.nio.file.Path;
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

	@BeforeEach
	void beforeEach(TestInfo testInfo) {
		logger.info("# Executing {}", testInfo.getDisplayName());
	}

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

	private static void testValidate(Profile profile, int errorCount) {
		var errors = ProfileManager.validate(profile);
		if (!errors.isEmpty()) {
			logger.info("Errors:");
			errors.forEach(e -> logger.info("  {}", e));
		}
		assertEquals(errorCount, errors.size());
	}

	@Test
	void testValidate_noNameOrDir() {
		var profile = createProfile();
		profile.setName(null);
		profile.setOutputDirectory(null);
		testValidate(profile, 2);
	}

	@Test
	void testValidate_direct() {
		var profile = createProfile();
		profile.setType(Type.DIRECT);
		testValidate(profile, 0);

		profile.setFileUrl(null);
		testValidate(profile, 1);
	}

	@Test
	void testValidate_redirect() {
		var profile = createProfile();
		profile.setType(Type.REDIRECT);
		testValidate(profile, 0);

		profile.setFileUrl(null);
		testValidate(profile, 1);
	}

	@Test
	void testValidate_indirect() {
		var profile = createProfile();
		profile.setType(Type.STANDARD);
		testValidate(profile, 0);

		profile.setPageUrl(null);
		profile.setLinkPattern(null);
		testValidate(profile, 2);
	}

	private static void testLoadProfiles(Path path, int profileCount, int errorCount) {
		var manager = new ProfileManager();
		var profiles = manager.loadProfiles(path);
		var errors = manager.getErrors();
		if (!errors.isEmpty()) {
			logger.info("Errors:");
			errors.forEach(e -> logger.info("  {}", e));
		}
		assertEquals(profileCount, profiles.size());
		assertEquals(errorCount, errors.size());
	}

	@ParameterizedTest
	@CsvSource({ "profiles.json, 5" })
	void testLoadProfiles(String name, int expectedSize) {
		testLoadProfiles(TestUtils.getResoureAsPath(name), expectedSize, 0);
	}

	@ParameterizedTest
	@ValueSource(strings = { "profiles-error.json" })
	void testLoadProfiles_invalid(String name) {
		testLoadProfiles(TestUtils.getResoureAsPath(name), 0, 2);
	}

	@ParameterizedTest
	@ValueSource(strings = { "http.properties", "profiles-null.json" })
	void testLoadProfiles_failedToLoad(String name) {
		testLoadProfiles(TestUtils.getResoureAsPath(name), 0, 1);
	}

}
