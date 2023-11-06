# Overview

A tool for downloading the latest version of files defined in a configuration file.

# Modified Time

* The modified time of the downloaded file is set to the time in the `Last-Modified` response header.
* The time in the `If-Modified-Since` request header is set to the modified time of the previously downloaded file if it exists. The file is not downloaded again if the response code is 304 (not modified).

# Location

There are two ways to specify the location of the file:

* URL of the file.
* URL of a web page and a regular expression known as the **Link Pattern**. The page is downloaded first and then the **Link Pattern** is used to find the URL of the file in the page. The first capturing group of the **Link Pattern** is assumed to contain the URL.

# Version Number

An optional version number can be appended to the base name of the file if the latter does not already include it. 

Example: Original file `file.zip` with version number `1.0` is renamed to `file-1.0.zip`

The version number is obtained from one of the following:

* Second capturing group of the **Link Pattern**.
* The same page containing the URL of the file using a separate regular expression known as the **Version Pattern**. The first capturing group of the **Version Pattern** is assumed to contain the version number.

Both are optional and the version number from the **Version Pattern** takes precedence when both are available.

# Other Features

## Non-Standard Refresh Header

The non-standard `Refresh` response header used by some web sites for redirection is supported. Example:

```
Refresh: 0; URL=https://site.com/file.zip
```

## Files on GitHub

(Applicable to page URLs at `github.com`)

URLs of files available for download on the release page of some GitHub repositories are in HTML fragments at other URLs found in the page. These fragments are fetched and added to the page by JavaScript code running on the web browser. 

To mimic this behavior, these fragments are downloaded and and then the **Link Pattern** is used to find the URL of the file in them if it is not found in the page itself.

## Files on Mozilla CDN

(Applicable to page URLs at `*.cdn.mozilla.net`)

Mozilla products such as Firefox and Thunderbird are available on their CDN. The URL is in this format:

`Page URL + "/" + Version + "/" + OS + "/" + Language + "/" + Product + "%20Setup%20" + Version + ".exe"`

Examples:

`https://download-installer.cdn.mozilla.net/pub/thunderbird/releases/115.4.1/win64/en-US/Thunderbird%20Setup%20115.4.1.exe`

`https://download-installer.cdn.mozilla.net/pub/firefox/releases/119.0/win64/en-US/Firefox%20Setup%20119.0.exe`

The page is downloaded first and then all links in the page that resemble version numbers compared. The largest version number is then used to determine the URL of the file to be downloaded. The **Link Pattern** is assumed to be the OS/Language/product string literal in the file URL.

# Profiles

Information about files to be downloaded is specified in a JSON file.

Sample:

```
[
	{
		"name": "Direct download",
		"fileUrl": "https://site.com/file.zip",
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
	},
	{
		"name": "Firefox",
		"pageUrl": "https://download-installer.cdn.mozilla.net/pub/firefox/releases/",
		"linkPattern": "win64/en-US/Firefox",
		"outputDirectory": "target/test-classes/testOutput"
	}
]
```
