package jkml.downloader.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonParseException;

import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.TestUtils;

class ProfileUtilsTests {

	private static final Logger logger = LoggerFactory.getLogger(ProfileUtilsTests.class);

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
		var errors = ProfileUtils.validate(profile);
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

	@Test
	void testReadProfiles() throws IOException {
		var path = TestUtils.getResoureAsPath("profiles.json");
		assertEquals(5, ProfileUtils.readProfiles(path).size());
	}

	@Test
	void testReadProfiles_exception() {
		var path = TestUtils.getResoureAsPath("profiles-null.json");
		assertThrows(JsonParseException.class, () -> ProfileUtils.readProfiles(path));
	}

	private static void testInferAndValidate(Path path, int errorCount) throws IOException {
		var profiles = ProfileUtils.readProfiles(path);
		var errors = ProfileUtils.inferAndValidate(profiles);
		if (!errors.isEmpty()) {
			logger.info("Errors:");
			errors.forEach(e -> logger.info("  {}", e));
		}
		assertEquals(errorCount, errors.size());
	}

	@ParameterizedTest
	@CsvSource({ "profiles.json, 0", "profiles-error.json, 2" })
	void testInferAndValidate(String name, int errorCount) throws IOException {
		var path = TestUtils.getResoureAsPath(name);
		testInferAndValidate(path, errorCount);
	}

}
