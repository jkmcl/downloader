@echo off

setlocal

set "DOWNLOADER_HOME=%~dp0"
set "DOWNLOADER_HOME=%DOWNLOADER_HOME:~0,-1%"

if not "%JAVA_HOME%" == "" goto hasJavaHome
	echo The required JAVA_HOME environment variable is not defined >&2
	exit /b 1
:hasJavaHome

"%JAVA_HOME%\bin\java.exe" -jar "%DOWNLOADER_HOME%\@project.build.finalName@" %*
