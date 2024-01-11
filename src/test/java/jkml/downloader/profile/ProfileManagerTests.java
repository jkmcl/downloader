package jkml.downloader.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.html.Occurrence;
import jkml.downloader.http.Referer;
import jkml.downloader.http.RequestOptions;
import jkml.downloader.http.UserAgent;
import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.TestUtils;

class ProfileManagerTests {

	private static final String PROFILES_FILE_NAME = "profiles.json";

	private final Logger logger = LoggerFactory.getLogger(ProfileManagerTests.class);

	private static Profile createProfile() {
		var profile = new Profile();
		profile.setName("name");
		profile.setType(Type.STANDARD);
		profile.setFileUrl(URI.create("https://localhost/file.zip"));
		profile.setPageUrl(URI.create("https://localhost/page.html"));
		profile.setLinkPattern(Pattern.compile("(file\\.zip)"));
		profile.setVersionPattern(Pattern.compile("[.0-9]+"));
		profile.setOutputDirectory(TestUtils.getOutputDirectory());
		profile.setLinkOccurrence(Occurrence.FIRST);
		profile.setRequestOptions(new RequestOptions(
				UserAgent.CHROME, Referer.NONE));
		return profile;
	}

	@Test
	void testValidate1() {
		logger.info("Testing profile validation - pass with fileUrl and without pageUrl");
		var profile = createProfile();
		profile.setPageUrl(null);
		var errMsgList = ProfileManager.validateProfile(profile);
		assertTrue(errMsgList.isEmpty());
	}

	@Test
	void testValidate2() {
		logger.info("Testing profile validation - pass with pageUrl and without fileUrl");
		var profile = createProfile();
		profile.setFileUrl(null);
		var errMsgList = ProfileManager.validateProfile(profile);
		assertTrue(errMsgList.isEmpty());
	}

	@Test
	void testValidate3() {
		logger.info("Testing profile validation - fail with fileUrl and with pageUrl");
		var profile = createProfile();
		var errMsgList = ProfileManager.validateProfile(profile);
		assertEquals(1, errMsgList.size());
	}

	@Test
	void testValidate4() {
		logger.info("Testing profile validation - fail without fileUrl and without pageUrl");
		var profile = createProfile();
		profile.setFileUrl(null);
		profile.setPageUrl(null);
		var errMsgList = ProfileManager.validateProfile(profile);
		assertEquals(1, errMsgList.size());
	}

	@Test
	void testValidate5() {
		logger.info("Testing profile validation - fail without outputDirectory");
		var profile = createProfile();
		profile.setPageUrl(null);
		profile.setOutputDirectory(null);
		var errMsgList = ProfileManager.validateProfile(profile);
		assertEquals(1, errMsgList.size());
	}

	@Test
	void testValidate6() {
		logger.info("Testing profile validation - fail with pageUrl and without linkPattern");
		var profile = createProfile();
		profile.setFileUrl(null);
		profile.setLinkPattern(null);
		var errMsgList = ProfileManager.validateProfile(profile);
		assertEquals(1, errMsgList.size());
	}

	@Test
	void testValidate7() {
		logger.info("Testing profile validation - fail with null or empty name");
		var profile = createProfile();
		profile.setName("");
		profile.setPageUrl(null);
		var errMsgList = ProfileManager.validateProfile(profile);
		assertEquals(1, errMsgList.size());
	}

	@Test
	void testValidate8() {
		logger.info("Testing GitHub profile validation - fail with missing link pattern");
		var profile = createProfile();
		profile.setFileUrl(null);
		profile.setLinkPattern(null);
		profile.setType(Type.GITHUB);
		var errMsgList = ProfileManager.validateProfile(profile);
		assertEquals(1, errMsgList.size());
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
