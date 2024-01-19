package jkml.downloader.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.StringUtils;

public class ProfileManager {

	private final Logger logger = LoggerFactory.getLogger(ProfileManager.class);

	static Profile sanitizeProfile(Profile profile) {
		if (profile.getType() != null || profile.getPageUrl() == null) {
			return profile;
		}

		var host = profile.getPageUrl().getHost();
		if (host == null) {
			return profile;
		}

		host = host.toLowerCase();
		if (host.equals("github.com") || host.endsWith(".github.com")) {
			profile.setType(Type.GITHUB);
		} else if (host.endsWith(".cdn.mozilla.net")) {
			profile.setType(Type.MOZILLA);
		} else {
			profile.setType(Type.STANDARD);
		}

		return profile;
	}

	static List<String> validateProfile(Profile profile) {
		var errMsgList = new ArrayList<String>();

		if (StringUtils.isNullOrBlank(profile.getName())) {
			errMsgList.add("Profile must contain a name");
		}

		if (profile.getOutputDirectory() == null) {
			errMsgList.add("Profile must contain an outputDirectory");
		}

		if (profile.getType() == null || profile.getType() == Type.STANDARD) {
			validateDefaultProfile(profile, errMsgList);
		} else {
			validateMozillaOrGitHubProfile(profile, errMsgList, profile.getType().name());
		}

		return errMsgList;
	}

	private static List<String> validateDefaultProfile(Profile profile, List<String> errMsgList) {
		if ((profile.getFileUrl() == null && profile.getPageUrl() == null)
				|| (profile.getFileUrl() != null && profile.getPageUrl() != null)) {
			errMsgList.add("Profile must contain either a fileUrl or pageUrl");
		}

		if (profile.getFileUrl() == null && profile.getPageUrl() != null && profile.getLinkPattern() == null) {
			errMsgList.add("Profile contains a pageUrl but no linkPattern");
		}

		return errMsgList;
	}

	private static List<String> validateMozillaOrGitHubProfile(Profile profile, List<String> errMsgList, String name) {
		if (profile.getPageUrl() == null || profile.getLinkPattern() == null) {
			errMsgList.add(name + " profile must contain pageUrl and linkPattern");
		}
		return errMsgList;
	}

	public List<Profile> loadProfiles(Path jsonFile) throws IOException {
		Profile[] profileArray = null;
		try (var reader = Files.newBufferedReader(jsonFile)) {
			var gson = GsonUtils.createGson();
			profileArray = gson.fromJson(reader, Profile[].class);
		}

		// Validate profiles
		var profileList = new ArrayList<Profile>(profileArray.length);
		for (var profile : profileArray) {
			if (profile == null) {
				continue;
			}

			var errors = validateProfile(sanitizeProfile(profile));
			if (errors.isEmpty()) {
				profileList.add(profile);
			} else {
				logger.error("Invalid profile: {}", profile.getName());
				errors.forEach(logger::error);
			}
		}

		return profileList;
	}

}
