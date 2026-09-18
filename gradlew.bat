@echo off
if exist "%~dp0gradle-8.9\bin\gradle.bat" (
  call "%~dp0gradle-8.9\bin\gradle.bat" %*
  exit /b %errorlevel%
)
gradle %*
