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

	private static Profile inferType(Profile profile) {
		if (profile.getType() != null) {
			return profile;
		}

		if (profile.getFileUrl() != null) {
			profile.setType(Type.DIRECT);
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

		var type = profile.getType();
		if (type == Type.DIRECT || type == Type.REDIRECT) {
			if (profile.getFileUrl() == null) {
				errMsgList.add(type.name() + " profile must contain a fileUrl");
			}
		} else {
			if (profile.getPageUrl() == null) {
				errMsgList.add(type.name() + " profile must contain a pageUrl");
			}
			if (profile.getLinkPattern() == null) {
				errMsgList.add(type.name() + " profile must contain a linkPattern");
			}
		}

		return errMsgList;
	}

	public List<Profile> loadProfiles(Path jsonFile) throws IOException {
		Profile[] profileArray = null;
		try (var reader = Files.newBufferedReader(jsonFile)) {
			profileArray = GsonUtils.createGson().fromJson(reader, Profile[].class);
		}

		// Validate profiles
		var profileList = new ArrayList<Profile>(profileArray.length);
		for (var profile : profileArray) {
			if (profile == null) {
				continue;
			}

			var errors = validateProfile(inferType(profile));
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
