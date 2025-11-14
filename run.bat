@echo off
echo Building Flipping Optimizer plugin...
call gradlew.bat shadowJar

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful! Starting RuneLite...
    echo.
    java -ea -jar build\libs\Flip_off-1.0-SNAPSHOT-all.jar
) else (
    echo.
    echo Build failed. Please check the errors above.
    exit /b 1
)
