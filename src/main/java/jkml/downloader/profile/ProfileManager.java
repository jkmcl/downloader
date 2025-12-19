package jkml.downloader.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jkml.downloader.http.RequestOptions;
import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.StringUtils;

public class ProfileManager {

	private final Logger logger = LoggerFactory.getLogger(ProfileManager.class);

	private static Profile normalize(Profile profile) {
		if (profile.getRequestOptions() == null) {
			profile.setRequestOptions(new RequestOptions());
		}

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

	static void validate(Profile profile, Consumer<String> errorHandler) {

		if (StringUtils.isNullOrBlank(profile.getName())) {
			errorHandler.accept("Profile must contain a name");
		}

		if (profile.getOutputDirectory() == null) {
			errorHandler.accept("Profile must contain an outputDirectory");
		}

		var type = profile.getType();
		if (type == Type.DIRECT || type == Type.REDIRECT) {
			if (profile.getFileUrl() == null) {
				errorHandler.accept(type.name() + " profile must contain a fileUrl");
			}
		} else {
			if (profile.getPageUrl() == null) {
				errorHandler.accept(type.name() + " profile must contain a pageUrl");
			}
			if (profile.getLinkPattern() == null) {
				errorHandler.accept(type.name() + " profile must contain a linkPattern");
			}
		}
	}

	public List<String> validate(List<Profile> profiles) {
		var errors = new ArrayList<String>();
		for (var i = 0; i < profiles.size(); ++i) {
			var idx = i; // effectively final
			validate(profiles.get(i), err -> errors.add("Invalid profile[%d]: %s".formatted(idx, err)));
		}
		return errors;
	}

	public List<Profile> load(Path path) throws IOException {
		logger.info("Loading profiles from file: {}", path);

		Profile[] array;
		try (var reader = Files.newBufferedReader(path)) {
			array = GsonUtils.createGson().fromJson(reader, Profile[].class);
		}

		var list = new ArrayList<Profile>(array.length);
		for (var p : array) {
			list.add(normalize(p));
		}

		logger.info("Loaded profile count: {}", list.size());
		return list;
	}

}
