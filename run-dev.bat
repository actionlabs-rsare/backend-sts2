@echo off
cd /d "%~dp0"
set SPRING_PROFILES_ACTIVE=dev
set STS_DEV_AUTH_ENABLED=true
set DB_HOST=localhost
set DB_PORT=5432
set DB_NAME=sts
set DB_USERNAME=sts
set DB_PASSWORD=sts
call gradlew.bat bootRun --console=plain
