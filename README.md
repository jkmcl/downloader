# Overview

Downloader is a profile-driven tool for downloading new files or newer versions of existing files.

# Usage

Downloader is packaged as an executable JAR file. At runtime, it expects one command-line argument: the path to a JSON file that defines one or more download profiles:

```shell
java -jar "${DOWNLOADER_JAR_FILE_PATH}" "${DOWNLOAD_PROFILES_JSON_FILE_PATH}"
```

# Download Profiles

Files to download are defined by download profiles in a JSON file. Example:

```json
[
	{
		"name": "Direct download",
		"fileUrl": "https://site.com/file.zip",
		"outputDirectory": "target/test-classes/testOutput"
	},
	{
		"name": "Download from location in redirect response",
		"fileUrl": "https://site.com/api?version=latest",
		"type": "REDIRECT",
		"outputDirectory": "target/test-classes/testOutput"
	},
	{
		"name": "File with or without version number in file name",
		"pageUrl": "https://site.com/page.html",
		"linkPattern": "href=\"([^\"]*/file\\.zip)",
		"outputDirectory": "target/test-classes/testOutput"
	},
	{
		"name": "File with version number in parent component of path",
		"pageUrl": "https://site.com/page.html",
		"linkPattern": "href=\"([^\"]*/v([.0-9]+)/file\\.zip)",
		"outputDirectory": "target/test-classes/testOutput"
	},
	{
		"name": "File with version number in page",
		"pageUrl": "https://site.com/page.html",
		"linkPattern": "href=\"([^\"]*/file\\.zip)",
		"versionPattern": "<b>File v([.0-9]+)</b>",
		"outputDirectory": "target/test-classes/testOutput"
	}
]
```

Downloader uses each profile to decide how to locate and download a file. There are currently four profile types:

* DIRECT
* REDIRECT
* STANDARD
* GITHUB

All profile types share these common properties:

* `name`: Profile name

* `outputDirectory`: Directory where the downloaded file is saved

* `type` (optional): If omitted, Downloader infers the profile type as `DIRECT` when `fileUrl` is present, or as `STANDARD` when it is not. If `pageUrl` points to a `github.com` URL, Downloader infers the type as `GITHUB`. The `REDIRECT` type cannot be inferred and must be defined explicitly.

## DIRECT

This profile type tells Downloader to download the file directly.

Properties:

* `fileUrl`: File URL

## REDIRECT

This profile type tells Downloader to retrieve a redirect response (HTTP status 301 and similar) and then download the file from the URL provided in the `Location` or non-standard `Refresh` response header.

Properties:

* `fileUrl`: URL that returns a redirect response

## STANDARD

This profile type tells Downloader to fetch a web page, extract the file URL (and optionally the file version) from it, and then download the file from that URL.

Properties:

* `pageUrl`: page URL

* `linkPattern`: A regular expression used to extract the file URL from the page and, optionally, the file version from the file URL. The first capturing group provides the file URL. If present, the second capturing group provides the file version.

* `versionPattern` (optional): A regular expression used to extract the file version from the page. The first capturing group provides the file version.

If either regular expression captures a file version and the file URL does not already include that version in its base name, the version is appended to the downloaded file's base name. For example, a file originally named `file.zip` with version `1.0` found on the page is renamed to `file-1.0.zip`.

If both regular expressions capture a version, the value from `versionPattern` is used.

## GITHUB

This profile type is an extension of `STANDARD` and is inferred when `pageUrl` points to a `github.com` URL.

GitHub release pages often contain fragments loaded dynamically from other URLs. Downloader retrieves these fragments and performs the same file URL and version extraction on the fragments if the file URL is not found on the page.

# Common Features

The following features apply to all profile types.

## File Modified Time

The `If-Modified-Since` request header is set to the modified time of the previously downloaded file, if it exists. The file is not downloaded again if the response indicates that it has not been modified (HTTP status 304).

The modified time of the downloaded file is set to the time from the `Last-Modified` response header.

## Non-Standard Refresh Response Header

Downloader supports the non-standard `Refresh` response header used by some websites for redirects. Example:

```text
Refresh: 0; URL=https://site.com/file.zip
```
