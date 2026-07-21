@echo off
title Force Sync GitHub Repository
color 0C

cd /d "%~dp0"

echo ============================================
echo        WARNING!
echo This will replace ALL files on GitHub
echo with your LOCAL project.
echo ============================================
echo.

set /p confirm=Type YES to continue: 

if /I not "%confirm%"=="YES" (
    echo Cancelled.
    pause
    exit /b
)

echo.
echo Fetching latest...
git fetch origin

echo.
echo Resetting to local files...

git rm -r --cached .

if exist ".gitignore" (
    git add .
) else (
    git add -A
)

echo.
set /p msg=Commit message: 

if "%msg%"=="" (
    set msg=Force synchronize repository
)

git commit -m "%msg%"

if errorlevel 1 (
    echo.
    echo Nothing to commit.
)

echo.
echo Force pushing...
git push --force-with-lease origin main

if errorlevel 1 (
    echo.
    echo Push failed.
    pause
    exit /b
)

echo.
echo ============================================
echo Repository synchronized successfully.
echo ============================================
pause