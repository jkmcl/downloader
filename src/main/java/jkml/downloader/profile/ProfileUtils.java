package jkml.downloader.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jkml.downloader.profile.Profile.Type;
import jkml.downloader.util.StringUtils;

public class ProfileUtils {

	private ProfileUtils() {
	}

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

	public static List<String> inferAndValidate(List<Profile> profiles) {
		var errors = new ArrayList<String>();
		for (var i = 0; i < profiles.size(); ++i) {
			for (var error : validate(inferType(profiles.get(i)))) {
				errors.add("Invalid profile[%d]: %s".formatted(i, error));
			}
		}
		return errors;
	}

	public static List<Profile> readProfiles(Path path) throws IOException {
		Profile[] array;
		try (var reader = Files.newBufferedReader(path)) {
			array = GsonUtils.createGson().fromJson(reader, Profile[].class);
		}
		var list = new ArrayList<Profile>(array.length);
		Collections.addAll(list, array);
		return list;
	}

}
