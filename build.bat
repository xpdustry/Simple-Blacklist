@echo off
powershell .\gradlew :build
move /y .\build\libs\simple-blacklist.jar .\
rd /s /q .\build