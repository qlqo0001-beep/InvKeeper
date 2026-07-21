@echo off
title Git Push

cd /d "%~dp0"

git add .

set /p msg=Commit message :

if "%msg%"=="" (
    echo Commit message is required.
    pause
    exit /b
)

git commit -m "%msg%"
git push origin main

pause