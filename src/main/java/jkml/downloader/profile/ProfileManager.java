package jkml.downloader.profile;

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

	private List<Profile> profiles = List.of();

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

	private static List<String> inferTypeAndValidate(Profile[] profiles) {
		var errors = new ArrayList<String>();
		for (int i = 0, n = profiles.length; i < n; ++i) {
			for (var err : validate(inferType(profiles[i]))) {
				errors.add(String.format("Invalid profile[%d]: %s", i, err));
			}
		}
		return errors;
	}

	public boolean loadProfiles(Path path) {
		logger.info("Loading profiles from file: {}", path);

		Profile[] tmpProfiles;
		try (var reader = Files.newBufferedReader(path)) {
			tmpProfiles = GsonUtils.createGson().fromJson(reader, Profile[].class);
		} catch (Exception e) {
			logger.error("Failed to load profiles", e);
			tmpProfiles = new Profile[0];
		}

		List<String> tmpErrors = new ArrayList<>();
		if (tmpProfiles.length == 0) {
			tmpErrors.add("Failed to load profiles");
		} else {
			tmpErrors.addAll(inferTypeAndValidate(tmpProfiles));
		}

		if (tmpErrors.isEmpty()) {
			profiles = List.of(tmpProfiles);
			errors = List.of();
			return true;
		} else {
			profiles = List.of();
			errors = List.copyOf(tmpErrors);
			return false;
		}
	}

	public List<Profile> getProfiles() {
		return profiles;
	}

	public List<String> getErrors() {
		return errors;
	}

}
