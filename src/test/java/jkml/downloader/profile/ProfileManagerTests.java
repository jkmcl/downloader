package jkml.downloader.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.html.Occurrence;
import jkml.downloader.http.RequestOptions;
import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.TestUtils;

class ProfileManagerTests {

	private static final String PROFILES_FILE_NAME = "profiles.json";

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
		profile.setLinkOccurrence(Occurrence.FIRST);
		profile.setRequestOptions(new RequestOptions());
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
		assertEquals(2, ProfileManager.validateProfile(profile).size());
	}

	@Test
	void testValidate_direct() {
		var profile = createProfile();
		profile.setType(Type.DIRECT);
		assertTrue(ProfileManager.validateProfile(profile).isEmpty());

		profile.setFileUrl(null);
		assertEquals(1, ProfileManager.validateProfile(profile).size());
	}

	@Test
	void testValidate_redirect() {
		var profile = createProfile();
		profile.setType(Type.REDIRECT);
		assertTrue(ProfileManager.validateProfile(profile).isEmpty());

		profile.setFileUrl(null);
		assertEquals(1, ProfileManager.validateProfile(profile).size());
	}

	@Test
	void testValidate_indirect() {
		var profile = createProfile();
		profile.setType(Type.STANDARD);
		assertTrue(ProfileManager.validateProfile(profile).isEmpty());

		profile.setPageUrl(null);
		profile.setLinkPattern(null);
		assertEquals(2, ProfileManager.validateProfile(profile).size());
	}

	@Test
	void testLoadProfiles() throws Exception {
		var profileManager = new ProfileManager();

		var profileList = profileManager.loadProfiles(TestUtils.getResoureAsPath(PROFILES_FILE_NAME));

		assertFalse(profileList.isEmpty());
	}

	@Test
	void testLoadProfiles_null() throws Exception {
		var profileManager = new ProfileManager();

		var profileList = profileManager.loadProfiles(TestUtils.getResoureAsPath("profiles-null.json"));

		assertEquals(2, profileList.size());
	}

	@Test
	void testLoadProfiles_error() throws Exception {
		var profileManager = new ProfileManager();

		var profileList = profileManager.loadProfiles(TestUtils.getResoureAsPath("profiles-error.json"));

		assertEquals(1, profileList.size());
	}

}
