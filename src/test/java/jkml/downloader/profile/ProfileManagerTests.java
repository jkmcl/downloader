package jkml.downloader.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
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
		var errors = new ArrayList<String>();
		ProfileManager.validate(profile, errors::add);
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
	void testLoad() throws IOException {
		var path = TestUtils.getResoureAsPath("profiles.json");
		var manager = new ProfileManager();
		assertEquals(5, manager.load(path).size());
	}

	@Test
	void testLoad_exception() {
		var path = TestUtils.getResoureAsPath("profiles-null.json");
		var manager = new ProfileManager();
		assertThrows(JsonParseException.class, () -> manager.load(path));
	}

	@ParameterizedTest
	@CsvSource({ "profiles.json, 0", "profiles-error.json, 2" })
	void testValidate(String name, int errorCount) throws IOException {
		var path = TestUtils.getResoureAsPath(name);
		var manager = new ProfileManager();
		var errors = manager.validate(manager.load(path));
		if (!errors.isEmpty()) {
			logger.info("Errors:");
			errors.forEach(e -> logger.info("  {}", e));
		}
		assertEquals(errorCount, errors.size());
	}

}
