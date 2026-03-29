package jkml.downloader.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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

	public static List<String> validate(Profile profile) {
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
