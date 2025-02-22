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

	private List<String> errors = List.of();

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

	private static Profile[] readProfiles(Path path) throws IOException {
		try (var reader = Files.newBufferedReader(path)) {
			return GsonUtils.createGson().fromJson(reader, Profile[].class);
		}
	}

	public List<Profile> loadProfiles(Path path) {
		logger.info("Loading profiles from file: {}", path);

		errors = new ArrayList<>();
		try {
			var profiles = readProfiles(path);
			for (var i = 0; i < profiles.length; ++i) {
				var vldnErrors = validate(inferType(profiles[i]));
				for (var ve : vldnErrors) {
					errors.add(String.format("Invalid profile[%d]: %s", i, ve));
				}
				vldnErrors.clear();
			}
			if (errors.isEmpty()) {
				return List.of(profiles);
			}
			errors = List.copyOf(errors);
		} catch (Exception e) {
			logger.error("Exception caught", e);
			errors = List.of("Failed to load profiles");
		}

		return List.of();
	}

	public List<String> getErrors() {
		return errors;
	}

}
