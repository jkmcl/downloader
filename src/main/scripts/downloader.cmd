@echo off

setlocal

if not defined DOWNLOADER_HOME set "DOWNLOADER_HOME=%~dp0"
if "%DOWNLOADER_HOME:~-1%"=="\" set "DOWNLOADER_HOME=%DOWNLOADER_HOME:~0,-1%"

if not defined JAVA_HOME (
	echo The required JAVA_HOME environment variable is not defined >&2
	exit /b 1
)

"%JAVA_HOME%\bin\java.exe" -jar "%DOWNLOADER_HOME%\@project.build.finalName@.jar" %*
