# Overview

Downloader is a profile-driven tool for downloading new files or the newer version of existing files.


# Usage

The Downloader artifact is an "executable" JAR file. At run-time, it expects a single command line argument that provides the path of a JSON file containing one or more download profiles:

```
java -jar "${DOWNLOADER_JAR_FILE_PATH}" "${DOWNLOAD_PROFILES_JSON_FILE_PATH}"
```


# Download Profiles

Files to be downloaded are defined in profiles in a JSON file. The following is a sample:

```
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
		"linkPattern": "href=\"(.+/file\\.zip)\"",
		"outputDirectory": "target/test-classes/testOutput"
	},
	{
		"name": "File with version number in parent component of path",
		"pageUrl": "https://site.com/page.html",
		"linkPattern": "href=\"(.+/v([.0-9]+)/file\\.zip)\"",
		"outputDirectory": "target/test-classes/testOutput"
	},
	{
		"name": "File with version number in page",
		"pageUrl": "https://site.com/page.html",
		"linkPattern": "href=\"(.+/file\\.zip)\"",
		"versionPattern": "<b>File v([.0-9]+)</b>",
		"outputDirectory": "target/test-classes/testOutput"
	}
]
```

Downloader determines how to locate and download each file based on its profile. Different profile types share some common fields but may have different mandatory field requirements. There are currently 4 profile types:
* DIRECT
* REDIRECT
* STANDARD
* GITHUB

The `name` and `outputDirectory` fields are common to all types and are mandatory. The value of the former serves as a label of the profile and that of the latter is the path of the directory where the downloaded file is saved.

When the optional `type` field is absent, Downloader infers the type to be DIRECT if the `fileUrl` field is present or STANDARD if it is absent. Downloader further infers the type to be GITHUB if the `pageUrl` field value is a URL in the `github.com` domain.

The REDIRECT type cannot be inferred and must be defined explicitly.


## DIRECT

This profile type tells Downloader to download the file directly.

The `fileUrl` field is mandatory and its value is the file URL.


## REDIRECT

This profile type tells Downloader to retrieve a redirect response (HTTP status 301, etc.) and then downloads the file from the URL in the `Location` response header.

The `fileUrl` field is mandatory and its value is the URL providing the redirect response.


## STANDARD

This profile type tells Downloader to retrieve a web page, extract the file URL (and optionally the file version) from the page and then download the file from the URL.

The `pageUrl` field is mandatory and its value is the page URL.

The `linkPattern` field is mandatory its value is a regular expression used to extract the file URL from the page and optionally the file version from the file URL. The first capturing group of this regular expression provides the file URL. If defined, the second capturing group of this regular expression provides the file version.

The `versionPattern` field is optional and its value is a regular expression used to extract the file version from the page. The first capturing group of this regular expression provides the file version.

If either of the regular expressions captures the file version and the file base name in the file URL does not already include it, the file version is appended to the file base name of the downloaded file. For example, a downloaded file originally named `file.zip` with version `1.0` found on the page is renamed to `file-1.0.zip`.

If both regular expressions capture a version, the one captured by `versionPattern` is appended.


## GITHUB

This profile type is an extension to STANDARD and is inferred when the `pageUrl` field value is a URL in the `github.com` domain.

URLs of files available for download on the release page of some GitHub repositories are in page fragments at other URLs found on the page. These fragments are typically fetched and added to the page by JavaScript code running on the web browser.

Downloader extracts the URLs of these fragments and then retrieves these fragments and performs the same file URL/version extraction on them if the file URL is not found on the original page.


# Common Features

The following features are common to all profile types.


## Modified Time

* The `If-Modified-Since` request header is added and set to the modified time of the previously downloaded file if it exists. The file is not downloaded again if the response indicates that it has not been modified (HTTP status 304).
* The modified time of the downloaded file is set to the time in the `Last-Modified` response header.


## Non-Standard Refresh Response Header

The non-standard `Refresh` response header used by some web sites for redirection is supported. Example:

```
Refresh: 0; URL=https://site.com/file.zip
```
