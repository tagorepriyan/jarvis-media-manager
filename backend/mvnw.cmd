@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
for %%I in ("%SCRIPT_DIR%..") do set "REPO_ROOT=%%~fI"
call "%REPO_ROOT%\mvnw.cmd" -f "%SCRIPT_DIR%pom.xml" %*
