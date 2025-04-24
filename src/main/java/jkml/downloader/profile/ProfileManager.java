package jkml.downloader.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.StringUtils;

public class ProfileManager {

	private final Logger logger = LoggerFactory.getLogger(ProfileManager.class);

	private List<String> errors = new ArrayList<>();

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

	static List<String> validate(Profile profile) {
		var errors = new ArrayList<String>();

		if (StringUtils.isNullOrBlank(profile.getName())) {
			errors.add("Profile must contain a name");
		}

		if (profile.getOutputDirectory() == null) {
			errors.add("Profile must contain an outputDirectory");
		}

		var type = profile.getType();
		if (type == Type.DIRECT || type == Type.REDIRECT) {
			if (profile.getFileUrl() == null) {
				errors.add(type.name() + " profile must contain a fileUrl");
			}
		} else {
			if (profile.getPageUrl() == null) {
				errors.add(type.name() + " profile must contain a pageUrl");
			}
			if (profile.getLinkPattern() == null) {
				errors.add(type.name() + " profile must contain a linkPattern");
			}
		}

		return errors;
	}

	private static List<Profile> readProfiles(Path path) throws IOException {
		try (var reader = Files.newBufferedReader(path)) {
			var array = GsonUtils.createGson().fromJson(reader, Profile[].class);
			var list = new ArrayList<Profile>(array.length);
			Collections.addAll(list, array);
			return list;
		}
	}

	public List<Profile> loadProfiles(Path path) {
		logger.info("Loading profiles from file: {}", path);

		try {
			var profiles = readProfiles(path);
			errors = new ArrayList<>();
			for (var i = 0; i < profiles.size(); ++i) {
				for (var error : validate(inferType(profiles.get(i)))) {
					errors.add("Invalid profile[%d]: %s".formatted(i, error));
				}
			}
			if (!errors.isEmpty()) {
				profiles.clear();
			}
			return profiles;
		} catch (Exception e) {
			logger.error("Exception caught", e);
			errors.clear();
			errors.add("Failed to load profiles");
			return new ArrayList<>();
		}
	}

	public List<String> getErrors() {
		return errors;
	}

}
